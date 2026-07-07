import os
import re

SHARDS_DIR = "STATUS/issue_shards"
COMPLIANCE_ARCHIVE = "STATUS/compliance_archive.md"
ISSUES_ARCHIVE = "STATUS/issues_archive.md"
ISSUES_MD = "issues.md"

def create_shard(issue_id, title, status, description="Recovered from historical archive."):
    if not os.path.exists(SHARDS_DIR):
        os.makedirs(SHARDS_DIR)

    file_name = f"issue_{issue_id.replace('#', '')}.md"
    path = os.path.join(SHARDS_DIR, file_name)

    # Don't overwrite if it exists (manual ones are better)
    if os.path.exists(path):
        return

    content = f"# Issue {issue_id}: {title}\n"
    content += f"**Status**: {status}\n\n"
    content += "## Description\n"
    content += f"{description}\n"

    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

def recover_from_compliance():
    if not os.path.exists(COMPLIANCE_ARCHIVE): return
    with open(COMPLIANCE_ARCHIVE, "r", encoding="utf-8") as f:
        content = f.read()

    # Matches "* **FIXED Issue #ID: Title** - Resolution: Description"
    matches = re.findall(r"\*   \*\*FIXED Issue (#?\d+): (.*?)\*\* - Resolution: (.*)", content)
    for issue_id, title, desc in matches:
        create_shard(issue_id, title, "Resolved (Historical)", desc)

    # Matches "* **FIXED Title (#ID)** - Resolution: Description"
    matches_alt = re.findall(r"\*   \*\*FIXED (.*?) \(#?(\d+)\)\*\* - Resolution: (.*)", content)
    for title, issue_id, desc in matches_alt:
        create_shard(issue_id, title, "Resolved (Historical)", desc)

def recover_from_issues_archive():
    if not os.path.exists(ISSUES_ARCHIVE): return
    with open(ISSUES_ARCHIVE, "r", encoding="utf-8") as f:
        content = f.read()

    # Matches "*   **Issue #ID**: Title. Description"
    matches = re.findall(r"\*\s+\*\*Issue (#?\d+)\*\*:\s+(.*?)\.\s+(.*)", content)
    for issue_id, title, desc in matches:
        create_shard(issue_id, title, "Resolved (Historical)", desc)

def recover_from_issues_md():
    if not os.path.exists(ISSUES_MD): return
    with open(ISSUES_MD, "r", encoding="utf-8") as f:
        content = f.read()

    # Matches active/pending/recently resolved in tables
    matches = re.findall(r"\|\s+\*\*#?(\d+)\*\*\s+\|\s+(.*?)\s+\|\s+(.*?)\s+\|", content)
    for issue_id, title, detail in matches:
        if "Resolved" in detail or "FIXED" in detail:
            status = detail
        elif "Pending" in detail:
            status = "Pending Validation"
        else:
            status = "Open"
        create_shard(issue_id, title, status, detail)

if __name__ == "__main__":
    print("🚀 Starting Forensic Issue Recovery...")
    recover_from_compliance()
    recover_from_issues_archive()
    recover_from_issues_md()
    print("✅ Recovery Complete. Individual shards created in STATUS/issue_shards/")
