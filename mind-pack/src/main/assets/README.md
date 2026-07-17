# mind_pack payload slot

Place the licensed model artifact here before building the AAB:

    gemma3-1b-it-int4.task   (~657 MB, litert-community/Gemma3-1B-IT,
                              dynamic_int4 variant)

The file is deliberately **gitignored**: weights enter the bundle at
build time from the owner's own licensed download (accepting the Gemma
Terms of Use on Hugging Face / Kaggle). `GEMMA_NOTICE.txt` in this
directory ships in every bundle regardless — it is the §3.1(4) notice
required by the Gemma Terms when distributing the weights.

Debug/sideload builds carry no packs; the app's model chain falls through
to the downloaded/SAF paths (ADR-010).
