# Project Issues - Index (Documentation Rule D2)

*The one file read at a session's start. One line per item; the full text is in `DOCS/issue_details.md` under the item's ID (read only that section), device procedures in `DOCS/operational_notes.md`. What each version did is in its commit message (`git log`); the long texts before Nov27.5-docs are in tag Nov27.4.*

## ▶️ Resumption State
- **Git**: Nov27.9 committed, tagged and pushed by Claude (Graphs connection strip, O146); relay v6.050 (`c460e25`) pushed earlier.
- **Relay**: SUSPENDED (O133): after Render's reset (probably 2026-12-01) the user deploys relay v6.050; then the live checks of focus item 3.
- **PC**: `Daemon compilation failed: null`: `gradlew --stop`, `gradlew clean`. Trust Gradle over the IDE's live analysis. `run_tests.ps1` takes about 100 s: run it in the background. `install_release.ps1 -NoBuild` after a version bump fails as a downgrade: build again.
- **User decisions**: Rule 17; `//` = continue; O112 waits for the user's false-alarm notes; O122 last; O140 very low; vibration 4 hits in 10 s; noise 2 s within 3 s; MOVING by GPS only; device tests one step at a time, answered by `//`.
- **Wire**: install both apps at Nov23.4 or later together; newer fields (`placeId`, `sit_learn`, `sit_examples`, `occupied`) are ignored by older apps.
- **Phones**: A15 (`R58X40GV2AR`, `adb shell wm dismiss-keyguard`): debug Sensor (Nov27.6 code) as `T`, under the chair cushion; no Monitor. S21FE (`R5CRC14PG4F`): release Monitor Nov27.2, `V`, `Tracker ID: T2` (do not re-pair), placement `Chair`, in personal use: check its foreground app before every tap. With both connected install pinned (`adb -s <serial> install -r`).
- **Tests on a phone**: never `connectedDebugAndroidTest`; `am instrument` with `HiltTestRunner` (`DOCS/operational_notes.md`). Sit data: stream `adb logcat -v time -s REV_Motion:* REV_Event:* REV_SitLearning:*` to a PC file.

## 🟢 Status: PASSED (Nov27.9)
- ISSUE-NOV25.22-O146: the Monitor's Graphs show a `Monitor - Sensor connection` strip (green: both relay links up, no rows missing; red: not or partly; empty: not known yet) from existing fields; `GraphsTest`; suite 1338 passed; chapter 267.2 after O133.

## 🎯 Next Resumption Focus
1. Row 90 O157 (next in queue order without the relay or the user); with the user at hand instead: chapter 266.2 on the A15, O163 part 4 after 254.3's car recording, chapter 258.3, 257.3.
2. SIMP-NOV25.0-S1's look on the S21FE waits for O133 (an alarm both ended and acknowledged).
3. O133: after the reset, deploy relay v6.050, then the live checks 182.2, 226.3, 227.4, 237.3, 237.4, 238.2, 240.3, 241.3, 244.3, 249.3, 250.3, 251.3, 253.2, 254.5, 258.4, 259.3, 263.2, 265.2; then O166 unless the relay leaves Render's free plan.
4. Right after item 3, before new features: O72 in one dedicated version (`/effort high`, plan in its Open Gap).
5. With the S21FE: O112 once the user has noted false alarms.

## Work Queue
Take one item at a time and display it before fixing it (Rule 1); read its section in `DOCS/issue_details.md` first.

| # | Issue | Work | Needs |
| :--- | :--- | :--- | :--- |
| 18 | ISSUE-NOV6.0-O26 | Run the Sensor on the Redmi Note 9 (API 31) | The user; relay live; before O72 |
| 72 | ISSUE-NOV19.0-O122 | Monitor battery use with its permanent relay link | Very low; last |
| 73 | ISSUE-NOV22.5-O133 | Bring the relay back (deploy v6.050, checks) | Blocked until Render's reset |
| 75 | ISSUE-NOV22.10-O138 | An alert style per alarm type | Very low |
| 76 | ISSUE-NOV25.9-O140 | Tamper suspicion: weighted evidence, one siren | Very low; `/effort high` |
| 78 | ISSUE-NOV25.22-O145 | Trails: automatic daily save to a folder | Discussion first |
| 80 | ISSUE-NOV25.22-O147 | Show how long each continuous event lasted | Very low |
| 81 | ISSUE-NOV25.22-O148 | Live audio or video, playback | Very low; discussion only |
| 83 | ISSUE-NOV25.22-O150 | Sit detection: car seat 254.3, barometer 254.4, relay 254.5 | The user in a parked car |
| 85 | ISSUE-NOV25.22-O152 | English or Hebrew (RTL) | Large; decide whether worth it |
| 90 | ISSUE-NOV25.25-O157 | Monitor Setup dialog: no Close, one framed group | Monitor |
| 91 | ISSUE-NOV25.25-O158 | HUD lines as aligned columns | core-ui; S21FE large font |
| 92 | ISSUE-NOV25.25-O159 | Monitor Phone Setup: title, buttons, steps, test button | core-ui; the S21FE |
| 93 | ISSUE-NOV25.25-O160 | Info: `Distance to Sensor` on one line | core-ui |
| 94 | ISSUE-NOV25.25-O161 | Graphs in landscape with a side column | core-ui |
| 96 | ISSUE-NOV26.0-O163 | Sit for many seats: part 4, Feedback, left | `/effort high`; after 254.3 |
| 99 | ISSUE-NOV27.1-O166 | Relay traffic meter (Sensor keeps the month) | Relay, wire; `/effort medium`; after O133 |

## Open Gaps
One line each; the full text is in `DOCS/issue_details.md`. Active warning: Compose `SnapshotStateList` lock verification (monitored).
- **ISSUE-NOV22.5-O133 (High)**: relay suspended (Render free bandwidth); reset, deploy and checks of row 73.
- **ISSUE-NOV5.0-O4 (Medium)**: test chapters needing a person or the S21FE still outstanding.
- **ISSUE-NOV6.0-O26 (Medium)**: the Sensor never run below API 35 (row 18).
- **ISSUE-NOV5.2-O16 (Low)**: sequence restarts at 0 after a fresh install without a settings import.
- **ISSUE-NOV5.0-O7 (Low)**: `(0,0)` rows stored and sent, filtered at display only.
- **ISSUE-NOV5.1-O9 (Low)**: relay cold start longer than the 20 s connection timeout.
- **ISSUE-NOV10.4-O72 (Low)**: Kotlin 1.9.24 kapt Gradle 9 deprecation; upgrade planned (focus 4).
- **ISSUE-NOV17.12-O112 (Low)**: debounce constants not checked against the user's false alarms.
- **ISSUE-NOV25.25-O154 limits (Low)**: what the knock rules give up.
- **ISSUE-NOV25.23-O155 limits (Low)**: what the sitting period gives up.
- **ISSUE-NOV26.0-O163 learning limits (Low)**: what learning, judging and the after-drive question give up.
- **ISSUE-NOV27.1-O167 limits (Low)**: the map CLEAR's kept span.
- **ISSUE-NOV27.2-O170 limits (Low)**: the Monitor's own trail in the background (chapter 264).
- **ISSUE-NOV27.1-O168 limits (Low)**: what trail thinning gives up (chapter 265.2 after O133).
- **ISSUE-NOV25.22-O144 limits (Low)**: the limit lines read at the minute's end (chapter 266.2).
- **ISSUE-NOV25.22-O146 limits (Low)**: the connection strip is empty, not red, while the Sensor is offline (chapter 267.2).
- **ISSUE-NOV11.1-O88 (Low)**: what the alarm push does not yet cover.

## Strategic Simplification Ideas
One line each; the full text is in `DOCS/issue_details.md`.
- **SIMP-NOV23.9-S1**: drop the event prefix fallback once both phones ran Nov23.9+ for 7 days.
- **SIMP-NOV22.8-S1** (low): fewer rows while no Monitor watches.
- **SIMP-NOV22.8-S2** (low): the Monitor's foreground state sent to the Sensor.
- **SIMP-NOV19.6-S1**: Compose `autoSize` for the HUD; blocked by O72.
- **SIMP-NOV9.3-S1** (low): R8 for the release builds, after O72.
