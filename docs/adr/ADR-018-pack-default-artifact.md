# ADR-018: what rides the mind pack by default (v0.6)

Status: accepted
Date: 2026-07-19

## Context

ADR-017 configured the registry default to Qwen2.5-1.5B-Instruct — the only
registry model that speaks all six product languages. But the PACK (the
Play fast-follow slot) still physically carried whatever file the owner
dropped in, and the only files that existed were legacy Gemma (EN-only,
honesty badge for the other five locales) or nothing. The store promise
"speaks your language out of the box" was therefore NOT delivered by the
built AAB. v0.6's job: close that gap or say honestly why not.

Facts (research-v6 §A, all sourced there):

- No official/community int4 artifact of Qwen2.5-1.5B exists; q8 is
  ~1.57–1.60 GB — over the 1.5 GB per-pack budget.
- The official converter (litert-torch 0.9.1) supports Qwen2.5-1.5B with
  `dynamic_int4_block32/128` on CPU, Linux-only; estimated int4 size
  ≈1.0–1.1 GB — fits.
- Qwen2.5 has no SentencePiece tokenizer → the legacy `.task` bundler is
  blocked (issue #275); the `.litertlm` builder accepts HF tokenizers.
- tasks-genai 0.10.35 documentation implies `.litertlm` support; the
  minimum version that reads it is undocumented — smoke-gated on device.

## Supply-chain decision

We CONVERT OURSELVES from the official `Qwen/Qwen2.5-1.5B-Instruct`
checkpoint (Apache-2.0, ungated), pinned to revision
`989aa798…` with sha256 verification of every downloaded file (HF LFS
oids), using version-pinned official Google tooling (litert-torch 0.9.1 +
litert-lm-builder 0.14.0), and record a full manifest (hashes + pip
freeze) next to the artifact. We do NOT ship third-party conversions:
litert-community publishes no int4 for this model, and no other source
meets the "official or reproducibly-built" bar.

Pipeline: `tools/qwen-int4/convert.sh` — one command on a Linux/WSL2
machine, CPU-only, ~1 h. Weights and artifacts never enter git
(mind-pack assets are gitignored).

## Outcome matrix

| int4 artifact state | pack content | UX consequence |
|---|---|---|
| **Produced & smoke-passed** (load → session-ready on device) | `qwen2.5-1.5b-instruct-int4.litertlm` in the mind-pack slot | "speaks your language out of the box" is true; EN-fallback badge retired for pack installs |
| Produced but smoke FAILS on 0.10.35 (`.litertlm` unreadable) | empty slot + first-run download of the same artifact via the sanctioned download path | honest first-run wait; badge logic unchanged until model lands |
| Not producible on owner machine | empty slot + first-run download; legacy Gemma stays a manual SAF option | EN-only plaque for Gemma stays MANDATORY (registry already shows it) |

Explicitly rejected: shipping legacy Gemma in the pack as default. It
contradicts the localized store listing in five of six locales; an honest
empty slot with a first-run download beats a bundled model that answers
in the wrong language ("плашка EN-only" stays for any Gemma the user
installs by hand).

## v0.6 run outcome (2026-07-19)

The conversion WAS attempted on this machine and was stopped by the
environment, not the pipeline: the host's C: drive filled to 100% during
toolchain install (the WSL system VHDX lives on C:), and the WSL service
then wedged mid-`fstrim` in a state only a host reboot clears — outside an
autonomous session's mandate. What was verified live before the wedge:
venv + pinned toolchain install succeeds; the checkpoint download starts
against the pinned revision. `convert.sh` now defaults its workspace to
the D: drive to make the rerun safe.

Consequence: **matrix row 3 applies for this run** — the pack slot ships
EMPTY, first-run offers download/SAF (today's plain-APK behavior), Gemma
remains a manual SAF option with its mandatory EN-only plaque. The
artifact remains one command away on the owner's machine
(`bash tools/qwen-int4/convert.sh` after a reboot, or any Linux box);
S24 checklist v6 §0 gates it on silicon. The registry and bake-off
harness accept the artifact by spec id already (`qwen2.5-1.5b*` hints,
`.litertlm` in MODEL_EXTENSIONS) — verified in code this run.
