# mind_pack payload slot

**CURRENT STATE (v0.8, ADR-021): leave this slot EMPTY. Do not place a
Qwen artifact here.** Not because of size — because the shipped default
engine cannot read it. See "Why empty" below before acting on anything
in this file.

The slot is model-agnostic (ADR-017): any `.task`/`.litertlm` file from
the registry works; `MindModelRegistry` identifies it by file name.

## Why empty (this is the part that changed)

Measured in v0.8 with the real file in the slot: the official q8
`Qwen2.5-1.5B-Instruct…q8_ekv4096.litertlm` compresses to
**1,378,035,869 bytes**, i.e. **8.1% UNDER** Play's 1.5 GB per-pack
limit (`bundletool get-size total`). So the long-standing claim that
"the q8 artifacts (~1.6 GB) do NOT fit the pack" was **wrong** — it
compared the on-disk size against a limit Play applies to the
*compressed download* size.

The real blocker is downstream: `tasks-genai` 0.10.35 — the engine
release builds actually run — refuses to initialize on a Qwen
`.litertlm` at all:

    INVALID_ARGUMENT: SentencePiece tokenizer is not found in the model

Filling this slot today would therefore ship ~1.4 GB to every user in
order to produce an initialization error. ADR-018 outcome **row 2**
applies (artifact exists, runtime can't read it), not row 3.

**`tools/qwen-int4/convert.sh` does not fix this** and is no longer a
release blocker: the error is about the tokenizer format and is
orthogonal to quantization, so an int4 build of the same model would
carry the same HF tokenizer. (Hypothesis — no int4 artifact exists to
test it — but do not spend a Linux machine on it before Phase D is
re-run on real hardware; see docs/manual-checklist-s24-v8.md.)

## When this slot may be filled again

When a runtime that reads our file becomes the default — today the open
branch is LiteRT-LM (ADR-020), whose behaviour on this file is still
UNKNOWN: it ran 76 s on an emulator before being killed for RAM, which
is an environment limit, not an answer. Size will not be the obstacle.

Meanwhile an empty slot is honest: first run offers the sanctioned
download/SAF paths, which beats bundling the English-only Gemma under a
localized listing.

Legacy alternative (still recognized, English-only routing):

    gemma3-1b-it-int4.task         (~555 MB, litert-community/Gemma3-1B-IT)

The file is deliberately **gitignored**: weights enter the bundle at build
time from the owner's own licensed download. `MODEL_NOTICES.txt` in this
directory ships in every bundle regardless — it carries the Apache-2.0
notice for Qwen artifacts AND the Gemma ToU §3.1(4) notice, so whichever
recognized model rides the slot, its notice rides along.

Debug/sideload builds carry no packs; the app's model chain falls through
to the downloaded/SAF paths (ADR-010).
