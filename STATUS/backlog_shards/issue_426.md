# Issue #426: Logic Regression - GPS Staleness Coupling
**Status**: Resolved (v8.9.43)
**Requirement**: R989

## Description
Detected a regression where GPS health was incorrectly coupled with heartbeat status.

## Resolution
Decoupled GPS health from heartbeat status in the UI logic, ensuring the TRK badge accurately reflects local fix freshness independently of relay connection.
