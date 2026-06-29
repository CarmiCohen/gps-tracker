#!/bin/bash
# Issue Tracking Collision Check
# Ensures that no new issue IDs in issues.md already exist in issues_archive.md

ISSUES_FILE="STATUS/issues.md"
ARCHIVE_FILE="STATUS/issues_archive.md"

# 1. Extract IDs from the "Open Technical Issues" and "Pending Validation" sections of issues.md
# We look for the pattern "Issue #XXX"
OPEN_IDS=$(sed -n '/## 🔴 Open Technical Issues/,/## 🟢 Resolved/p' "$ISSUES_FILE" | grep -o "Issue #[0-9]\+" | sort -u)

if [ -z "$OPEN_IDS" ]; then
    echo "✅ No active issues to check in $ISSUES_FILE."
    exit 0
fi

# 2. Check each ID against the archive
COLLISIONS=""
for ID in $OPEN_IDS; do
    if grep -q "$ID" "$ARCHIVE_FILE"; then
        COLLISIONS="$COLLISIONS $ID"
    fi
done

# 3. Report results
if [ -n "$COLLISIONS" ]; then
    echo "❌ ERROR: Duplicate Issue IDs detected!"
    echo "The following IDs are already present in $ARCHIVE_FILE:"
    echo "$COLLISIONS"
    echo "Please renumber these issues before committing."
    exit 1
else
    echo "✅ No collisions detected between $ISSUES_FILE and $ARCHIVE_FILE."
    exit 0
fi
