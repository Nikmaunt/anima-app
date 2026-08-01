#!/usr/bin/env python3
"""v1.1c task 1 — refuse a green build that proved nothing.

Three times in this repo a check reported success without measuring anything:
`PersonaAcceptanceTest` sat in SKIPPED, the Roborazzi goldens were captured but
never compared, and finally every test task in the tree came back UP-TO-DATE and
then FROM-CACHE. Each time a person noticed by reading task output. This script
is that person, wired into CI.

It reads two kinds of evidence, both produced by the run itself:

  * the Gradle log  — which test tasks ran, and whether Gradle executed them or
    recalled them; whether `finalizeTestRoborazzi*` ran or was SKIPPED, which is
    the observable difference between comparing goldens and merely taking them;
  * the result files — `*/build/test-results/**/TEST-*.xml` and
    `*/build/test-results/roborazzi/*/results-summary.json`.

Usage:
    python3 tools/ci-verify.py --log build.log [--min-executions N] [--allow-recalled]

Exit code 0 = the run measured something and it was green.
Exit code 1 = the run may have been green, but it did not measure.
"""

from __future__ import annotations

import argparse
import glob
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict

# `> Task :core:model:test`, `> Task :app:testDebugUnitTest FROM-CACHE`.
TASK_LINE = re.compile(r"^> Task (:[\w:.-]+)\s*(\S.*)?$")
# The Roborazzi task that compares and reports. SKIPPED means no comparison
# happened at all — proven in docs/design/v11/phase0-v11b-audit.md 2.3.
FINALIZE_TASK = re.compile(r"^> Task (:[\w:.-]+:finalizeTestRoborazzi\w+)\s*(\S.*)?$")
# Emitted from the task graph by the root build under -Panima.ci=true. This is
# the only reliable way to know which task paths are real `Test` tasks: a
# module's bare `:foo:test` is a lifecycle task on Android and a real Test task
# on JVM, and both report UP-TO-DATE in the same words.
DECLARED_TEST_TASK = re.compile(r"^ANIMA-CI-TEST-TASK (:[\w:.-]+)$")

RECALLED = ("UP-TO-DATE", "FROM-CACHE")


def read_log(path: str):
    declared: set[str] = set()
    outcomes: dict[str, str] = {}
    finalize_ran, finalize_skipped = [], []
    with open(path, encoding="utf-8", errors="replace") as fh:
        for raw in fh:
            line = raw.rstrip("\n").rstrip("\r").strip()
            d = DECLARED_TEST_TASK.match(line)
            if d:
                declared.add(d.group(1))
                continue
            m = TASK_LINE.match(line)
            if m:
                outcomes[m.group(1)] = (m.group(2) or "").strip()
            f = FINALIZE_TASK.match(line)
            if f:
                outcome = (f.group(2) or "").strip()
                (finalize_skipped if outcome == "SKIPPED" else finalize_ran).append(f.group(1))
    executed, recalled = [], []
    for task in sorted(declared):
        outcome = outcomes.get(task, "<never reported>")
        if outcome in RECALLED:
            recalled.append((task, outcome))
        elif outcome == "":
            executed.append(task)
        # NO-SOURCE is a module with no test sources — honest, and not evidence
        # either way. SKIPPED is Gradle refusing to run one, which the census
        # below will notice as missing results.
    return declared, executed, recalled, finalize_ran, finalize_skipped


def census(root: str):
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    per_module = defaultdict(int)
    classes = set()
    pattern = os.path.join(root, "**", "build", "test-results", "**", "TEST-*.xml")
    for f in glob.glob(pattern, recursive=True):
        try:
            r = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        totals["tests"] += int(r.get("tests", 0))
        totals["failures"] += int(r.get("failures", 0))
        totals["errors"] += int(r.get("errors", 0))
        totals["skipped"] += int(r.get("skipped", 0))
        p = f.replace("\\", "/")
        m = re.match(r"(?:.*?)/?([\w./-]*?)/build/test-results/([^/]+)/", p)
        module = m.group(1) if m else p
        per_module[module] += int(r.get("tests", 0))
        classes.add((module, r.get("name")))
    return totals, per_module, classes


def goldens(root: str):
    summaries = {}
    pattern = os.path.join(root, "**", "build", "test-results", "roborazzi", "*", "results-summary.json")
    for f in glob.glob(pattern, recursive=True):
        try:
            with open(f, encoding="utf-8") as fh:
                data = json.load(fh)
        except (OSError, json.JSONDecodeError):
            continue
        s = data.get("summary", {})
        results = data.get("results", [])
        # A "recorded" whose golden path lives under build/ is a generated
        # artefact (the pseudolocalisation sheets), not a golden that failed to
        # compare. Only paths under src/test/screenshots are goldens.
        real = [r for r in results if "/src/test/screenshots/" in r.get("golden_file_path", "").replace("\\", "/")]
        summaries[f.replace("\\", "/")] = dict(
            summary=s,
            golden_total=len(real),
            golden_changed=sum(1 for r in real if r.get("type") == "changed"),
            golden_recorded=sum(1 for r in real if r.get("type") == "recorded"),
        )
    return summaries


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--log", required=True, help="Gradle console log of the run under test")
    ap.add_argument("--root", default=".", help="repository root")
    ap.add_argument("--min-executions", type=int, default=1)
    ap.add_argument("--min-goldens", type=int, default=1)
    ap.add_argument(
        "--allow-recalled",
        action="store_true",
        help="self-test only: report recalled test tasks without failing",
    )
    args = ap.parse_args()

    problems: list[str] = []
    declared, executed, recalled, fin_ran, fin_skipped = read_log(args.log)

    print("== test tasks ==")
    print(f"  declared by the build : {len(declared)}")
    print(f"  executed : {len(executed)}")
    print(f"  recalled : {len(recalled)}")
    for path, outcome in recalled:
        print(f"      {outcome:<12} {path}")

    # 1.1 — the contour has to have been on, or none of the rest is evidence.
    if not declared:
        problems.append(
            "the log carries no ANIMA-CI-TEST-TASK lines, so the run was not made with "
            "-Panima.ci=true and no guarantee about test execution applies to it."
        )

    # 1.4 — a recalled test task is the defect this file exists for.
    if recalled and not args.allow_recalled:
        problems.append(
            f"{len(recalled)} test task(s) were {RECALLED[0]}/{RECALLED[1]} instead of executing. "
            "A recalled result is evidence about a previous tree, not this one. "
            "Run with -Panima.ci=true."
        )
    if len(executed) < args.min_executions:
        problems.append(
            f"only {len(executed)} test task(s) executed, expected at least {args.min_executions}. "
            "A build that runs no tests cannot be green."
        )

    # 1.3 — goldens compared, not merely captured.
    print("== roborazzi ==")
    print(f"  finalizeTestRoborazzi ran     : {len(fin_ran)}")
    print(f"  finalizeTestRoborazzi SKIPPED : {len(fin_skipped)}")
    for path in fin_skipped:
        print(f"      SKIPPED {path}")
    if fin_skipped:
        problems.append(
            f"{len(fin_skipped)} Roborazzi finalize task(s) were SKIPPED — the goldens were captured "
            "and never compared. captureRoboImage asserts nothing without roborazzi.test.verify."
        )

    sums = goldens(args.root)
    total_goldens = sum(v["golden_total"] for v in sums.values())
    changed = sum(v["golden_changed"] for v in sums.values())
    recorded = sum(v["golden_recorded"] for v in sums.values())
    for f, v in sorted(sums.items()):
        print(f"  {f}: {v['summary']} (goldens under src/test/screenshots: {v['golden_total']})")
    print(f"  goldens compared: {total_goldens}, changed: {changed}, silently re-recorded: {recorded}")
    if total_goldens < args.min_goldens:
        problems.append(
            f"only {total_goldens} golden(s) were compared, expected at least {args.min_goldens}. "
            "No results-summary.json means no comparison ran."
        )
    if changed:
        problems.append(f"{changed} golden(s) differ from the tree.")
    if recorded:
        problems.append(
            f"{recorded} golden(s) under src/test/screenshots were RE-RECORDED during a verify run. "
            "That overwrites the reference instead of comparing to it."
        )

    # 1.5 — say what the number is, so three runs' numbers stay comparable.
    totals, per_module, classes = census(args.root)
    print("== census ==")
    print(
        f"  EXECUTIONS: {totals['tests']}  failures={totals['failures']}  "
        f"errors={totals['errors']}  skipped={totals['skipped']}"
    )
    print(f"  distinct (module, test class) pairs: {len(classes)}")
    print(
        "  NOTE: android modules run every test twice (debug + release), so the number above is "
        "EXECUTIONS, not tests. See docs/adr/ADR-024-test-census.md before comparing it to another run."
    )
    for module in sorted(per_module):
        print(f"    {module:<28} {per_module[module]}")
    if totals["failures"] or totals["errors"]:
        problems.append(f"{totals['failures']} failure(s) and {totals['errors']} error(s) in the results.")
    if totals["tests"] == 0:
        problems.append("no test results on disk at all.")

    if problems:
        print("\nFAILED — this run did not prove what a green build claims:")
        for p in problems:
            print(f"  * {p}")
        return 1
    print("\nOK — tests executed, goldens compared, results green.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
