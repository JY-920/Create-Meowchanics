# Cat editor installation-risk report

## Scope

Owned changes: CatEditorTraits.java in both ports and CatEditorRiskProbe.java in both isolated GameTest source sets. Parent subsequently authorized adapting CatEditorMenuProbe.java in both ports. No commits or deployments.

## Implementation

- EXCELLENT: one integer bucket out of 100; 0–59 loses NOW 10–50; 60–69 loses MAX 10–20; 70–79 loses NOW 30–50 and MAX 10–20; 80–99 does nothing.
- GOOD: 0–49 loses NOW 10–50, 50–99 does nothing. COMMON/DEFECT return the same immutable profile without consuming randomness.
- Each loss chooses independently from all six CatStat values; inclusive integer loss ranges. Existing withValues clamps at zero and NOW to MAX.
- Only successful installation calls risk. Attributes are saved using CatAttributeData.set (including its existing effect refresh) and synced with syncCatAttributesToTracking.
- canInstall uses the same validated immutable profile construction as install without writing, consuming the token, or rolling risk. It is usable client-side, while install retains its server-only editable gate.
- Existing raw custom ID/level ownership, disabled/missing/duplicate/conflict/slot/full checks remain intact. Extraction does not invoke risk.

## Evidence

Forge RED command (Zulu 17 JAVA_HOME):

```powershell
./gradlew.bat runGameTestServer --offline --init-script ../tests/accessory-gametest.init.gradle -PcatEditorProbeOnly -PaccessoryNoKubeJS -PaccessoryRunId=editor-risk-red --no-configuration-cache
```

2026-10-04, exit 1, 13 tests, exactly three expected runtime failures:

- separaterowandappearanceslots: Four independent trait bottles and twelve material samples (parent-owned menu test).
- lossesclampatzeroandcurrentneverexceedsmaximum: Missing installation risk behavior.
- exactbucketsinclusiverangesandindependentstats: Missing installation risk behavior.

The other ten tests passed. RED log: forge-1.20.1/build/accessory-gametest/no-kubejs-editor-risk-red/logs/latest.log.

The two pure risk tests were written and observed RED before production edits. A third integration test was added after RED compilation and before production edits, so it was not included in that RED run; it checks nonmutating canInstall, actual successful-install risk, rejected duplicate/no risk, and extraction/no risk.

Deterministic pure tests cover probability boundaries, inclusive minimum/maximum loss values, all six locus indices, independent and same-locus combined losses, immutable input, zero floor and NOW≤MAX.

git diff --check exited 0 (existing unrelated CRLF normalization warnings).

## Combined Forge GREEN

Same command with -PaccessoryRunId=editor-risk-green completed 2026-10-04 12:45, exit 0, BUILD SUCCESSFUL (1m36s). All 15 required tests passed: 3 risk, 2 v2 menu, 5 menu, 1 packet, 4 trait preservation tests.

Log: forge-1.20.1/build/accessory-gametest/no-kubejs-editor-risk-green/logs/latest.log.

Menu probes were adapted to 16 temporary slots, row-local extraction, first-empty-row installation, page-specific quick moves and deferred appearance commits. Additional coverage verifies compacted trait rows, insertion gap rejection, page gates, nonmutating material preview, sample-over-dropdown priority, scoped/whole-body material cost, repeat commit without further cost, reset returning samples, stale-draft rejection, and existing owner/seat/reach/duplicate-packet/close-item-conservation cases.

The runtime emitted existing deprecation/config-first-run warnings, a test-world air/block-entity warning and a server catch-up warning; these did not fail tests. NeoForge runtime, final full production build/package validation and deployment remain parent-owned. Client GUI interaction was not exercised by this server suite.

## Review follow-up: exhausted local material override

Reviewer identified a commit-draft bug: on a vanilla cat, whole-body GOLD x2 plus head STONE x1 correctly commits gold body/stone head, but consuming the final stone sample leaves the remaining gold sample active, changing pendingGenome and permitting a second unintended commit.

Added exhaustedRegionSampleCannotReapplyRemainingWholeSample to both menu probes before production repair. Same Forge command with -PaccessoryRunId=editor-commit-red finished 12:55, exit 1: 16 tests, exactly one failure at the pendingGenome-equals-committed assertion; other 15 passed.

The new test also requires unused sample return, exact material conservation and duplicate-commit idempotence. Existing material cost assertions were subsequently updated for the proposed post-commit sample-return semantics (first commit returns 2 unused gold, repeated commit changes nothing, later whole-body plus unchanged head sample consumes one and returns all others).

Parent implemented clearDraft/epoch synchronization and post-commit sample return in both ports. Follow-up Forge GREEN with -PaccessoryRunId=editor-commit-green completed 2026-10-04 13:00, exit 0, BUILD SUCCESSFUL in 2m17s: all 16 required tests passed, including the formerly failing exhausted-local-sample regression and updated material conservation assertions.

Follow-up log: forge-1.20.1/build/accessory-gametest/no-kubejs-editor-commit-green/logs/latest.log. No Forge Gradle process remains from these test runs. NeoForge verification and final packaging/deployment remain parent-owned.
