# ADR-024 — What the test number means, and why a green build has to prove it ran

Status: accepted, 2026-08-01 (run v1.1c)
Supersedes nothing. Amends the reporting convention used since v0.1.

## Context

Three defects of the same shape have now been found in this repository, each by
a person reading task output rather than by the build:

1. **v1.0** — `PersonaAcceptanceTest` sat in `SKIPPED` while the run reported
   success.
2. **v1.1** — Roborazzi goldens were captured and never compared.
   `captureRoboImage` asserts nothing unless `roborazzi.test.verify` is set, and
   the flag lived in a sentence in a handoff document.
3. **v1.1b** — the whole contour. The command the handoff prescribed,
   `./gradlew check -Proborazzi.test.verify=true`, returned **BUILD SUCCESSFUL
   in 6s with every test task `UP-TO-DATE`**, and on the next attempt restored
   36 result files `FROM-CACHE`. The property is not a task input, so Gradle
   correctly concluded there was nothing to do. Reproduced again on 2026-08-01
   at the top of run v1.1c.

A separate reporting problem sits next to it. Three run reports have quoted
"392 tests", "419", "443" without saying what the number counts, and the numbers
are not comparable to each other or to the count of `@Test` in the sources.

## Decision

### 1. The number is EXECUTIONS

`check` runs android modules' tests twice — `testDebugUnitTest` and
`testReleaseUnitTest` — so **every android test is counted twice**. The number
that appears in run reports is therefore *executions*, not tests.

Report it as such, and report the second number alongside it:

| Name | 2026-08-01 value | What it counts |
|---|---|---|
| **Executions** | 443 | `tests=` summed over `*/build/test-results/**/TEST-*.xml` |
| **Distinct (module, class) pairs** | 48 | test classes actually run |
| Skipped | 7 | all in `tools/litertlm-smoke`, env-gated |

Do not compare an executions figure from one run to a distinct-test figure from
another. `tools/ci-verify.py` prints both and says which is which, so the number
in a report can be copied from a machine instead of remembered.

### 2. A green build has to have executed

`-Panima.ci=true` is the whole contour, one switch:

* every `Test` task loses `upToDateWhen` and the build cache
  (root `build.gradle.kts`);
* the build **refuses to start** if `roborazzi.test.verify` is not true or
  `record` is on;
* the task graph prints `ANIMA-CI-TEST-TASK <path>` for every real `Test` task,
  which is the only reliable way to tell a module's lifecycle `:app:test` from a
  JVM module's real `:core:model:test` — both print `UP-TO-DATE` in the same
  words.

`roborazzi.test.verify=true` is now the repository default in
`gradle.properties`, so a local `./gradlew check` compares goldens too.
Re-recording is the explicit opt-in it should always have been.

### 3. And something has to check that it did

`tools/ci-verify.py --log <gradle log>` fails the run when:

* any declared `Test` task came back `UP-TO-DATE` or `FROM-CACHE`;
* any `finalizeTestRoborazzi*` task was `SKIPPED` — the observable signature of
  "captured, never compared";
* a golden under `src/test/screenshots` was re-recorded during a verify run;
* fewer than the expected number of goldens were compared, or no results exist;
* any failure or error is present.

It is wired into `.github/workflows/ci.yml` as its own step, and
`CiContourTest` fails the build if any of those four files is edited back to the
comfortable version.

## Consequences

* A `check` that measures nothing is now three independent failures, not a
  success: the build refuses to configure, the tasks cannot be recalled, and the
  verifier reads the evidence afterwards.
* CI got slower and more honest. The old CI step ran `testDebugUnitTest`, which
  never touched `core:model` (116 executions — the largest single block in the
  tree) and never ran the release variant at all.
* Local runs now compare goldens by default. A developer who wants the old
  behaviour has to ask for it in writing on the command line.

## Proof that it bites

Same tree, same day, two runs:

```
$ ./gradlew check -Proborazzi.test.verify=true     # the old prescribed command
BUILD SUCCESSFUL in 14s ... 25 executed, 1720 up-to-date
$ python3 tools/ci-verify.py --log stale.log
FAILED — this run did not prove what a green build claims:
  * 54 test task(s) were UP-TO-DATE/FROM-CACHE instead of executing.
  * only 0 test task(s) executed, expected at least 1.

$ ./gradlew -Panima.ci=true check --no-build-cache
BUILD SUCCESSFUL in 53s ... 61 executed
$ python3 tools/ci-verify.py --log ci.log
  declared by the build : 36    executed : 36    recalled : 0
  goldens compared: 92, changed: 0, silently re-recorded: 0
  EXECUTIONS: 443  failures=0  errors=0  skipped=7
OK — tests executed, goldens compared, results green.
```
