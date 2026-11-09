# Baseline Index (R-ID 744, ISSUE-NOV8.5-O47, SIMP-NOV8.5-S1)

Since Nov8.14 the baseline of a version is its **git tag**: the exact files of that version, read with
`git show <tag>:<path>` (for example `git show Nov8.13:DOCS/issues.md`), or listed with `git ls-tree -r --name-only <tag>`.
No copies are made any more. User decision, 2026-11-08 (option C).

- **One line per version**, newest first, added at Completion step 10. The commit is the short hash of `git rev-parse --short=7 <tag>^{commit}`, so a tag that is moved or set on the wrong commit is caught by comparing the two. The version being closed has no commit yet, so its row says `pending`; the next session replaces it with the hash once the user has run the Git block. `BaselineIndexTest` (chapter 157) enforces all of this.
- **The subfolders** (`Nov1.0` to `Nov8.13`) are the old hand-made copies. They are frozen and kept for history (Rule 9), but they are not authoritative: many are partial (ISSUE-NOV8.5-O47). When a copy and the tag disagree, the tag is right.
- **Notes**: `no copy` = no subfolder; `partial copy` = the subfolder lacks files or content; `no tag` = the version was committed inside another version's commit.

| Version | Commit | Notes |
| :--- | :--- | :--- |
| Nov9.3-MonitorStart | pending | |
| Nov9.2-Vibration | 7a26409 | |
| Nov9.1-MicOnStartup | cc81ffc | |
| Nov9.0-LiftTilt | b1f1d38 | |
| Nov8.103-Microphone | e9dd12e | followed by untagged c7f34b8 (CLAUDE.md effort rule, Handover next-version name) |
| Nov8.102-Documentation | 733747b | |
| Nov8.101-Tooling | f5f89eb | |
| Nov8.100-Documentation | e15e708 | |
| Nov8.14 | 3fc4fb4 | |
| Nov8.13 | 981668c | |
| Nov8.12 | 6021425 | Committed during Nov8.13; tag moved from dd4be20 (ISSUE-NOV8.13-O53). Partial copy (Handover, issues only) |
| Nov8.11 | dd4be20 | Tag moved from e45d8fd (ISSUE-NOV8.13-O53) |
| Nov8.10 | e45d8fd | |
| Nov8.8 | d237755 | Partial copy (Handover, issues only) |
| Nov8.7 | d237755 | No tag: committed inside Nov8.8. No copy |
| Nov8.5 | 894265d | |
| Nov8.2 | a23c787 | Partial copy |
| Nov8.1 | a23c787 | No tag: committed inside Nov8.2. Copy exists |
| Nov8.0 | 38ba838 | Partial copy |
| Nov7.38 | b5b58f0 | Partial copy |
| Nov7.36 | a024dfe | Partial copy (no RESOLUTION_ARCHIVE) |
| Nov7.34 | 6634276 | Partial copy |
| Nov7.32 | 7fabb4d | |
| Nov7.30 | b1c199a | |
| Nov7.28 | d52cdd3 | Partial copy (Handover, issues only) |
| Nov7.26 | 836c9fd | Partial copy |
| Nov7.24 | 8726ff3 | |
| Nov7.21 | b98431b | |
| Nov7.19 | b1fbd0d | |
| Nov7.17 | 4c60d55 | |
| Nov7.15 | 69d359e | |
| Nov7.12 | 49460d0 | Partial copy |
| Nov7.11 | 384ae8d | Partial copy (no RESOLUTION_ARCHIVE) |
| Nov7.10 | 0f3bc6e | Partial copy (no RESOLUTION_ARCHIVE) |
| Nov7.9 | 4d9b7da | Partial copy |
| Nov7.8 | 046387d | Partial copy |
| Nov7.7 | 006974e | No copy |
| Nov7.6 | 1cc7a62 | |
| Nov7.5 | d1d3abb | |
| Nov7.3 | f19c6cc | |
| Nov7.2 | 77f2912 | |
| Nov7.1 | e6592be | |
| Nov7.0 | d7c7b54 | |
| Nov6.4 | ba93a58 | |
| Nov6.3 | 06d4103 | |
| Nov6.2 | e4e4985 | |
| Nov6.1 | 205df74 | |
| Nov6.0 | a33bb46 | |
| Nov5.2 | 0d6a348 | |
| Nov5.1 | 1168760 | |
| Nov5.0 | 2fda01f | |
| Nov4.44 | 4e9b9f2 | |
| Nov4.37 | 4e9b9f2 | No tag: committed inside Nov4.44. No copy |
| Nov4.36 | 1e27b08 | |
| Nov4.35 | 991e92e | |
| Nov4.34 | 09f2cef | |
| Nov4.33 | 84b46f7 | |
| Nov4.32 | d8c3763 | |
| Nov4.31 | 09f9e9d | |
| Nov4.30 | b3e36b7 | |
| Nov4.29 | 05f0548 | |
| Nov4.28 | 11c9a0b | |
| Nov4.27 | 8f5f7ed | |
| Nov4.26 | 3bc6f4f | No copy |
| Nov4.25 | e77ce7c | |
| Nov4.24 | 5cdfa66 | |
| Nov4.22-addHUD | 6b6b70d | |
| Nov4.21 | 22d6522 | |
| Nov4.20-withServer | 434e824 | |
| Nov4.3 | a57f84c | |
| Nov4.2 | 4271030 | |
| Nov4.0 | 25317df | Partial copy (no RESOLUTION_ARCHIVE) |
| Nov3.22 | 7a28232 | |
| Nov3.21 | 6ee22df | |
| Nov3.20 | e826105 | |
| Nov3.14 | fe51ce1 | |
| Nov3.13 | b0bf924 | |
| Nov3.12 | 666528b | |
| Nov3.11 | 4f99df0 | |
| Nov3.1 | cda4ca0 | |
| Nov3.0 | 3b77593 | |
| Nov2.25 | 1012959 | Partial copy (no RESOLUTION_ARCHIVE) |
| Nov2.24 | 59db51c | |
| Nov2.23 | b20e076 | |
| Nov2.21 | c38ee10 | |
| Nov2.1 | ba1e5fb | Partial copy (no RESOLUTION_ARCHIVE) |
| Nov2.0 | f090eff | No copy |
| Nov1.8 | 4829042 | No copy |
| Nov1.7 | 7f55369 | Also tag `REVOLUTION-VERIFIED-NOV1.7` (annotated, on cd0db23) |
| Nov1.6 | 3abe274 | |
| Nov1.5 | 3abe274 | No tag: committed inside Nov1.6. Partial copy (Handover, issues only) |
| Nov1.4 | cd1d07c | Partial copy (issues only) |
| Nov1.3 | 4f9f7c4 | |
| Nov1.2 | dc1f58b | |
| Nov1.1 | 1495b6b | No copy |
| Nov1.0 | 76475ad | |
| Revol31.9 | e3f663e | |
| Revol31.8 | b9b846c | |
| Revol31.2 | d9d2319 | |
