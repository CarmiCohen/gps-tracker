#!/bin/bash
# Documentation Integrity Validator
# v8.9.91: Automated structural audit to prevent truncation.

EXIT_CODE=0

check_file() {
    FILE=$1
    MIN_LINES=$2
    REQUIRED_HEADER=$3

    if [ ! -f "$FILE" ]; then
        echo "❌ ERROR: $FILE is missing!"
        EXIT_CODE=1
        return
    fi

    # 1. Line Count Audit (Growth-Only Constraint)
    LINE_COUNT=$(wc -l < "$FILE")
    if [ "$LINE_COUNT" -lt "$MIN_LINES" ]; then
        echo "❌ ERROR: $FILE appears truncated! (Current: $LINE_COUNT, Expected Min: $MIN_LINES)"
        EXIT_CODE=1
    fi

    # 2. Structural Header Audit
    if ! grep -q "$REQUIRED_HEADER" "$FILE"; then
        echo "❌ ERROR: $FILE is missing its authoritative header: '$REQUIRED_HEADER'"
        EXIT_CODE=1
    fi

    # 3. Placeholder Audit (Anti-AI Truncation)
    # Checks for the word "brevity" or "omitted" which usually indicate data loss.
    if grep -Ei "omitted for brevity|preserved for brevity|legacy entries preserved" "$FILE"; then
        echo "❌ WARNING: $FILE contains 'brevity' placeholders. Ensure no live data was lost."
    fi
}

echo "🔍 Starting Documentation Integrity Audit..."

# Core Documents
check_file "STATUS/requirements_sot.md" 40 "# System Source of Truth"
check_file "STATUS/compliance.md" 30 "# Compliance & Operational Requirements"
check_file "STATUS/docs_history.md" 30 "# Project History & Versioning"
check_file "issues.md" 50 "# Project Issues & Hardening Tracking"

# Archives (Must be significantly larger and never shrink)
check_file "STATUS/requirements_sot_archive.md" 250 "# System Source of Truth (SoT) - Historical Archive"
check_file "STATUS/issues_archive.md" 100 "# Issues Archive (Historical Resolutions)"
check_file "STATUS/docs_history_archive.md" 50 "Historical Records"
check_file "STATUS/compliance_archive.md" 10 "Historical Compliance"

if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Audit PASS: Documentation structure and growth constraints satisfied."
else
    echo "🛑 Audit FAIL: Truncation or structural corruption detected!"
fi

exit $EXIT_CODE
