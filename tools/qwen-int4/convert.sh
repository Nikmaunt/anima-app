#!/usr/bin/env bash
# ADR-018 / research-v6 §A: reproducible Qwen2.5-1.5B-Instruct → int4
# .litertlm conversion for the mind pack. Linux (WSL2 is fine), CPU-only,
# no GPU. Budget: ~16 GB RAM, ~25 GB disk, under an hour on 8+ cores.
#
#   bash tools/qwen-int4/convert.sh [WORK_DIR]
#
# Produces under $WORK_DIR (default ~/anima-qwen-int4):
#   qwen2.5-1.5b-instruct-int4.litertlm   ← the pack artifact
#   MANIFEST.txt                          ← hashes + exact pip freeze
#
# Supply chain (ADR-018): the ONLY input is the official Qwen checkpoint,
# pinned to an immutable revision; the toolchain is pinned by version below
# and recorded in full in the manifest. Artifacts and weights never enter
# git (mind-pack assets are gitignored).
set -euo pipefail

# Default workspace on the big disk: on this machine the WSL system VHDX
# lives on an always-nearly-full C:, and a 1.5B conversion needs ~25 GB —
# the v0.6 run filled C: to 100% before learning this. /mnt/d is slower
# (9p) but roomy; conversion is CPU-bound so the trade is fine.
WORK="${1:-/mnt/d/Hermes/anima-qwen-work}"
VENV="$WORK/venv"

# --- pins (recorded in MANIFEST.txt; bump deliberately, never silently) ---
LITERT_TORCH_VERSION=0.9.1        # github.com/google-ai-edge/litert-torch (2026-05-19)
LITERT_LM_BUILDER_VERSION=0.14.0  # .litertlm container builder
HF_REPO=Qwen/Qwen2.5-1.5B-Instruct
HF_REVISION=989aa7980e4cf806f80c7fef2b1adb7bc71aa306  # main @ 2026-07-19, Apache-2.0, ungated
QUANTIZE=dynamic_int4_block32     # quality-first int4 (block128 ≈ 5% smaller if 1.5G is ever tight)
KV_CACHE_MAX_LEN=1280             # matches litert-community convention (ekv1280)

mkdir -p "$WORK"
cd "$WORK"

if [ ! -d "$VENV" ]; then
    python3 -m venv "$VENV"
fi
# shellcheck disable=SC1091
source "$VENV/bin/activate"
python -m pip install --upgrade pip -q

# torch CPU wheel keeps the download ~1/5 the size of the CUDA default.
pip install -q torch --index-url https://download.pytorch.org/whl/cpu
pip install -q "litert-torch==$LITERT_TORCH_VERSION" \
    "litert-lm-builder==$LITERT_LM_BUILDER_VERSION" \
    huggingface_hub

echo "== downloading $HF_REPO @ $HF_REVISION"
python - <<PY
from huggingface_hub import snapshot_download
snapshot_download(
    "$HF_REPO",
    revision="$HF_REVISION",
    local_dir="checkpoint",
    allow_patterns=["*.json", "*.safetensors", "tokenizer*", "merges.txt", "vocab*"],
)
PY

echo "== converting to int4 tflite (CPU-only; takes tens of minutes)"
python -m litert_torch.generative.examples.qwen.convert_to_tflite \
    --model_size=1.5b \
    --checkpoint_path="$WORK/checkpoint" \
    --quantize="$QUANTIZE" \
    --kv_cache_max_len="$KV_CACHE_MAX_LEN" \
    --output_path="$WORK" \
    --output_name_prefix=qwen2.5-1.5b-instruct-int4

TFLITE=$(ls -t "$WORK"/qwen2.5-1.5b-instruct-int4*.tflite | head -1)
echo "== tflite: $TFLITE ($(stat -c%s "$TFLITE") bytes)"

echo "== packaging .litertlm (HF tokenizer — Qwen has no sentencepiece model,"
echo "   so the legacy mediapipe .task bundler is not applicable; litert-torch issue #275)"
python - <<PY
from litert_torch.generative.utilities import litertlm_builder

litertlm_builder.build_litertlm(
    tflite_model_path="$TFLITE",
    hf_tokenizer_model_path="$WORK/checkpoint",
    start_token="<|im_start|>",
    stop_tokens=["<|im_end|>"],
    output_path="$WORK/qwen2.5-1.5b-instruct-int4.litertlm",
)
PY

echo "== manifest"
{
    echo "produced: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "source: $HF_REPO@$HF_REVISION"
    echo "quantize: $QUANTIZE  kv_cache_max_len: $KV_CACHE_MAX_LEN"
    echo "artifact sha256:"
    sha256sum "$WORK/qwen2.5-1.5b-instruct-int4.litertlm"
    echo "tflite sha256:"
    sha256sum "$TFLITE"
    echo "--- pip freeze ---"
    pip freeze
} > "$WORK/MANIFEST.txt"
echo "DONE: $WORK/qwen2.5-1.5b-instruct-int4.litertlm"
