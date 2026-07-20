#!/usr/bin/env bash
# Internal helper for the v0.6 run: steps 1-2 of convert.sh (venv + download)
# so the long conversion can be driven/verified interactively. Not part of
# the documented owner flow — convert.sh is the entry point.
set -euo pipefail
WORK="${1:-/mnt/d/Hermes/anima-qwen-work}"
VENV="$WORK/venv"
mkdir -p "$WORK"
cd "$WORK"
if [ ! -d "$VENV" ]; then python3 -m venv "$VENV"; fi
source "$VENV/bin/activate"
python -m pip install --upgrade pip -q
pip install -q torch --index-url https://download.pytorch.org/whl/cpu
pip install -q litert-torch==0.9.1 litert-lm-builder==0.14.0 huggingface_hub
echo "INSTALL_OK $(python -c 'import litert_torch;print(getattr(litert_torch,"__version__","?"))')"
python - <<'PY'
from huggingface_hub import snapshot_download
snapshot_download(
    "Qwen/Qwen2.5-1.5B-Instruct",
    revision="989aa7980e4cf806f80c7fef2b1adb7bc71aa306",
    local_dir="checkpoint",
    allow_patterns=["*.json", "*.safetensors", "tokenizer*", "merges.txt", "vocab*"],
)
print("DOWNLOAD_OK")
PY
