package app.anima.core.data.identity

import android.content.Context
import app.anima.core.data.repo.IdentityRepository
import app.anima.core.model.CreatureConcept
import app.anima.core.model.IdentityOrigin
import app.anima.core.model.assignedTo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v1.1c task 3 — repair an identity that provably contradicts itself, and
 * refuse to touch anything else.
 *
 * ## What "provably" has to mean here
 *
 * The invariant is `concept == CreatureConcept.assignedTo(seed)`. It holds by
 * construction for a soul hatched by this build — the concept is *computed*
 * from the seed and never asked for. So a violation is real damage, and the
 * seed is the authority, because it is the thing derived from the phone.
 *
 * **But it only holds for souls this build hatched.** Every build up to and
 * including v1.1b showed a gallery of eight bodies at hatch and let a tap in
 * Settings replace the body afterwards. For those souls a concept that differs
 * from `assignedTo` is not corruption — it is the owner's own decision, made in
 * a build that asked. Applying the repair to them would delete a lived identity
 * in the name of protecting it, and roughly seven in eight of them would be
 * "repaired". Hence [IdentityOrigin]: absent means CHOSEN, and CHOSEN is never
 * touched. Same for TRANSFERRED, whose body belongs to a previous phone by
 * design.
 *
 * Recorded in `docs/design/v11/phase3-identity-repair.md`, with the discrepancy
 * this run found between the documents and the code.
 *
 * ## What it repairs
 *
 * * **The seed row is `0`.** No `deviceSeed()` can return it; it is a default
 *   written by a broken path. Re-read from the device.
 * * **The seed row is missing while a concept exists.** A body cannot exist
 *   without the number it came from. Re-read.
 * * **`concept != assignedTo(seed)` on an ASSIGNED soul.** The concept is
 *   computed at hatch, so a difference is a bad write. Recompute.
 * * **The concept row is unreadable while a seed exists** — `fromWire` returned
 *   null, i.e. a body this build does not know. Recompute.
 *
 * ## What it never touches
 *
 * The journal, the remembered facts, the time capsules, the chat, the hatch
 * date, the name, the counters. Identity is four rows in `meta`; the lived
 * relationship is everything else, and none of it is derivable, so none of it
 * is repairable. A backup of the identity rows is written first, to a path the
 * owner can be told: `filesDir/identity-backup/identity-<timestamp>.json`.
 */
@Singleton
class IdentityRepair
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val identity: IdentityRepository,
        private val deviceSeed: DeviceSeed,
    ) {
        sealed interface Outcome {
            /** No identity yet — onboarding has not run. Nothing to repair. */
            data object NoIdentity : Outcome

            data object Healthy : Outcome

            /** Damage found, but the era forbids touching it. Reported, not fixed. */
            data class LeftAlone(
                val reason: String,
            ) : Outcome

            data class Repaired(
                val findings: List<String>,
                val backupPath: String,
            ) : Outcome
        }

        suspend fun run(): Outcome {
            val storedSeed = identity.seed()
            val storedConcept = identity.concept()
            val storedConceptWire = identity.conceptWire()
            if (storedSeed == null && storedConceptWire == null) return Outcome.NoIdentity

            val origin = identity.identityOrigin()
            val findings = mutableListOf<String>()

            // 1. The seed. A zero or a missing row cannot have come from a device.
            var seed = storedSeed
            if (seed == null) {
                seed = deviceSeed.value()
                findings += "seed row was missing while a body existed; re-read from the device"
            } else if (seed == 0L) {
                seed = deviceSeed.value()
                findings += "seed was 0, which is a default rather than a number this phone produced"
            }

            // 2. The concept, against that seed.
            val expected = CreatureConcept.assignedTo(seed)
            when {
                storedConceptWire == null ->
                    findings += "no body was recorded at all; derived ${expected.wire} from the seed"

                storedConcept == null ->
                    findings +=
                        "the recorded body '$storedConceptWire' is not one this build knows; " +
                        "derived ${expected.wire} from the seed"

                storedConcept != expected ->
                    findings +=
                        "the recorded body was ${storedConcept.wire} but the seed says ${expected.wire}"
            }

            if (findings.isEmpty()) return Outcome.Healthy

            // 3. The era gate. This is the whole difference between a repair and
            // a deletion, so it comes after the diagnosis and before any write —
            // the owner still gets told what was found.
            if (origin != IdentityOrigin.ASSIGNED) {
                val why =
                    when (origin) {
                        IdentityOrigin.TRANSFERRED ->
                            "this soul came from a soul file; its body belongs to the phone it was born on"
                        else ->
                            "this soul was hatched by a build that let the body be chosen, so the " +
                                "recorded body is a decision, not damage"
                    }
                return Outcome.LeftAlone("$why. Found: ${findings.joinToString("; ")}")
            }

            val backup = writeBackup(storedSeed, storedConceptWire, origin)
            identity.repairIdentity(expected, seed)
            note(
                buildString {
                    appendLine("Anima identity repair at ${Instant.now()}")
                    appendLine("backup: ${backup.absolutePath}")
                    findings.forEach { appendLine("  - $it") }
                    appendLine("result: body ${expected.wire}, seed $seed")
                    appendLine("Nothing else was touched: journal, memories, capsules, dates, name.")
                },
            )
            return Outcome.Repaired(findings, backup.absolutePath)
        }

        /**
         * The identity rows as they were, before anything is written. Small and
         * plain on purpose: this file exists to be readable by a person who has
         * been told a path over a chat window.
         */
        private suspend fun writeBackup(
            seed: Long?,
            conceptWire: String?,
            origin: IdentityOrigin,
        ): File {
            val dir = File(context.filesDir, BACKUP_DIR).apply { mkdirs() }
            val file = File(dir, "identity-${System.currentTimeMillis()}.json")
            val name = identity.name()
            val hatched = identity.hatchedAtMillis()
            file.writeText(
                buildString {
                    appendLine("{")
                    appendLine("""  "written_at": "${Instant.now()}",""")
                    appendLine("""  "reason": "v1.1c identity repair; these are the values BEFORE the repair",""")
                    appendLine("""  "creature_name": ${jsonOrNull(name)},""")
                    appendLine("""  "creature_concept": ${jsonOrNull(conceptWire)},""")
                    appendLine("""  "creature_seed": ${seed ?: "null"},""")
                    appendLine("""  "hatched_at": ${hatched ?: "null"},""")
                    appendLine("""  "identity_origin": "${origin.wire}"""")
                    appendLine("}")
                },
            )
            return file
        }

        private fun jsonOrNull(value: String?): String =
            value?.let { "\"" + it.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" } ?: "null"

        /**
         * Local only, appended to a file of its own next to the crash log —
         * NOT into the crash log, which `AnimaApp` overwrites wholesale on the
         * next crash and which is labelled "the last crash" on screen. The
         * Settings → crash log screen shows both. This is not telemetry and
         * there is nothing to send it to: the app has no network outside two
         * modules and neither of them is this one.
         */
        private fun note(text: String) {
            runCatching {
                val file = File(File(context.filesDir, LOG_DIR), LOG_FILE)
                file.parentFile?.mkdirs()
                file.appendText(text + "\n")
            }
        }

        private companion object {
            const val BACKUP_DIR = "identity-backup"

            // Mirrors feature/settings' CrashLog directory, which core:data must
            // not depend on. Held by IdentityRepairTest so the two cannot drift.
            const val LOG_DIR = "crash"
            const val LOG_FILE = "identity-repair.txt"
        }
    }
