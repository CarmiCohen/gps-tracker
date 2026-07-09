# Issue #015: Coroutine Cancellation
**Status**: Resolved
**Priority**: Medium

## Description
Hardened lifecycle transitions against `CancellationException` to prevent service crashes during rapid activity restarts. (v8.9.72)
