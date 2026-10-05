# Cat editor v2 fresh review

## Parent verification of follow-up fixes

The singleton fallback was reproduced RED, fixed by clearing draft controls and returning unused samples on commit, and verified GREEN (Forge 16 / NeoForge 17 server tests). The subsequent delayed-epoch race was reproduced by the actual Forge client probe: a draft selection during pending confirmation incorrectly succeeded. The fix gates client draft controls until initial sync and until confirmation/reset acknowledgement. Screen dispatch respects local rejection; only the matching changed epoch clears pending choices. Stale-appearance failure releases the wait. Both final client runs emit the explicit CAT EDITOR CLIENT PASS marker, including rejected edits during acknowledgement and reset-to-confirmed preview. This is parent verification, not a further independent reviewer verdict.

Read-only production review, 2026-10-04. Reviewed the current files directly (including historically untracked files), the project AGENTS.md, v2 plan and risk report. Scope: both ports' CatEditorMenu, CatEditorTraits, CatEditorScreen, CatStatsGoggleOverlay, CatEditorMenuProbe and CatEditorRiskProbe. No tests rerun, production edits, commits or deployment performed.

## Finding

### [P2] Preserve the committed draft when a consumed override sample becomes empty

- Forge: `forge-1.20.1/src/main/java/cn/laowu/mod/CatEditorMenu.java:183-185`
- NeoForge: `neoforge-1.21.1/src/main/java/cn/laowu/mod/CatEditorMenu.java:183-185`

Successful commit shrinks samples and updates `baseline`, but keeps all remaining samples and dropdown choices active. `pendingGenome()` then reapplies them to that new baseline. Reproduction from an initially vanilla cat: put two gold blocks in the whole-body sample slot and one stone block in the head-primary sample slot, then confirm. The intended gold body / stone head is committed and one of each sample is consumed. The head sample is now empty, so the remaining whole-body gold sample immediately overrides that head in `pendingGenome()`. A duplicate confirmation changes the head to gold and consumes the second gold block, although the user made no new draft edit. The client preview likewise diverges from the just-committed real cat. A singleton block overriding a prior vanilla dropdown selection has the same fallback problem.

The post-commit draft needs to remain equivalent to the committed genome until a new user edit; avoid reactivating overridden controls merely because their higher-priority sample was consumed. Cover the combination of a multi-count whole-body sample and a singleton differing regional sample, as well as singleton sample-over-dropdown, asserting that pending preview equals the committed appearance and a repeated confirm changes neither genome nor item counts. Existing repeated-confirm coverage uses a sample count of three and does not cross the nonempty-to-empty boundary (`CatEditorMenuProbe.java:254-263` in both ports).

## Other reviewed behavior

No additional concrete correctness finding was established in the reviewed paths. The two menu/trait implementations are identical. The probability buckets and loss ranges match the requested distribution; failed install and extraction do not call the risk helper. Preview genome/trait writes target a detached `Cat`, not the source. Server commit compares the current genome with its baseline; owner, seat, reach, registry revision and released-menu checks gate actions. Review did not substitute for the parent's ongoing live-client rendering verification.

## Scoped follow-up review: clearDraft and draft epoch

Reviewed the revised identical menus and new `exhaustedRegionSampleCannotReapplyRemainingWholeSample` probe without rerunning tests. Clearing choices and returning every unused appearance sample after a successful commit resolves the original server-side fallback issue; the new probe covers the singleton regional override and repeated confirm. Failed stale commits still retain inputs. Reset uses the same return path, and no-op commits also advance the draft epoch.

### [P2] Do not clear newer client choices when an older draft acknowledgement arrives

- Forge: `forge-1.20.1/src/main/java/cn/laowu/mod/CatEditorMenu.java:97-99`
- NeoForge: `neoforge-1.21.1/src/main/java/cn/laowu/mod/CatEditorMenu.java:97-99`

The new epoch handler unconditionally clears client choices, while the UI allows new dropdown edits before that acknowledgement arrives. On a connection with latency, click Confirm (or Reset), then select another coat before the server's epoch update returns. The client records the new choice immediately. The server processes COMMIT/RESET, clears its draft, then processes SELECT and retains that new choice. When the earlier epoch acknowledgement reaches the client, `setData` erases the newer local choice; no authoritative choice update follows. The next confirm therefore applies a server-only choice absent from the preview. Ordered packets within each direction do not prevent this cross-direction race. Gate new draft edits until the acknowledgement, or associate/reconcile updates with a draft request version so an old acknowledgement cannot erase newer edits. The initial epoch synchronization should likewise not erase a first edit made before initial data delivery. The added server-only probe does not exercise this client acknowledgement ordering.
