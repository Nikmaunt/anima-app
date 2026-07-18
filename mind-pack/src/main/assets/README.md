# mind_pack payload slot

Place ONE licensed model artifact here before building the AAB. The slot is
model-agnostic (ADR-017): any `.task`/`.litertlm` file from the registry
works; `MindModelRegistry` identifies it by file name.

v0.5 default (ADR-017, docs/model-matrix.md):

    qwen2.5-1.5b-instruct-*.task   (int4 build ~1.1 GB — Play's fast-follow
                                    pack limit is 1.5 GB; the int8 artifact
                                    (1.6 GB) does NOT fit the pack and goes
                                    through download/SAF instead)

Legacy alternative (still recognized, English-only routing):

    gemma3-1b-it-int4.task         (~555 MB, litert-community/Gemma3-1B-IT)

The file is deliberately **gitignored**: weights enter the bundle at build
time from the owner's own licensed download. `MODEL_NOTICES.txt` in this
directory ships in every bundle regardless — it carries the Apache-2.0
notice for Qwen artifacts AND the Gemma ToU §3.1(4) notice, so whichever
recognized model rides the slot, its notice rides along.

Debug/sideload builds carry no packs; the app's model chain falls through
to the downloaded/SAF paths (ADR-010).
