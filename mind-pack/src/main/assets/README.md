# mind_pack payload slot

Place ONE licensed model artifact here before building the AAB. The slot is
model-agnostic (ADR-017): any `.task`/`.litertlm` file from the registry
works; `MindModelRegistry` identifies it by file name.

v0.6 default (ADR-018, produced by `tools/qwen-int4/convert.sh`):

    qwen2.5-1.5b-instruct-int4.litertlm
        (int4 ≈1.0–1.1 GB — fits Play's 1.5 GB fast-follow pack limit;
         `.litertlm` because Qwen has no SentencePiece tokenizer and the
         legacy .task bundler can't package it — research-v6 §A.3.
         Verify sha256 against the conversion's MANIFEST.txt. The q8
         artifacts (~1.6 GB) do NOT fit the pack: download/SAF only.)

If the artifact hasn't been produced yet, ship the slot EMPTY (ADR-018
outcome 3): first run offers the sanctioned download/SAF paths — that is
strictly more honest than bundling the English-only Gemma under a
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
