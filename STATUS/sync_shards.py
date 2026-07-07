import os
import re

SHARDS_DIR = "STATUS/issue_shards"
ISSUES_MD = "issues.md"
ARCHIVE_MD = "STATUS/issues_archive.md"

def parse_shard(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Extract ID and Title from the first line: "# Issue #ID: Title"
    header_match = re.search(r"# Issue #(\d+R?[\w-]*): (.*)", content)
    if not header_match:
        return None

    issue_id = header_match.group(1)
    title = header_match.group(2).strip()

    # Extract Status and Requirement
    status_match = re.search(r"\*\*Status\*\*: (.*)", content)
    status = status_match.group(1).strip() if status_match else "Unknown"

    req_match = re.search(r"\*\*Requirement\*\*: (.*)", content)
    requirement = req_match.group(1).strip() if req_match else None

    # Extract Priority (for open issues)
    priority_match = re.search(r"\*\*Priority\*\*: (.*)", content)
    priority = priority_match.group(1).strip() if priority_match else "Low"

    return {
        "id": issue_id,
        "status": status,
        "title": title,
        "requirement": requirement,
        "priority": priority,
        "content": content,
        "is_resolved": "Resolved" in status or "FIXED" in status,
        "is_pending": "Pending" in status
    }

def sync():
    if not os.path.exists(SHARDS_DIR):
        os.makedirs(SHARDS_DIR)
        return

    shards = []
    for filename in os.listdir(SHARDS_DIR):
        if filename.endswith(".md"):
            data = parse_shard(os.path.join(SHARDS_DIR, filename))
            if data:
                shards.append(data)

    # Sort shards by ID (numeric)
    def sort_key(s):
        numeric = re.search(r"(\d+)", s["id"])
        return int(numeric.group(1)) if numeric else 999

    shards.sort(key=sort_key, reverse=True)

    active = [s for s in shards if not s["is_resolved"] and not s["is_pending"]]
    pending = [s for s in shards if s["is_pending"]]
    resolved = [s for s in shards if s["is_resolved"]]

    # 1. Update issues.md
    with open(ISSUES_MD, "w", encoding="utf-8") as f:
        f.write("# Project Issues & Hardening Tracking\n\n")
        f.write("## 📊 Hardening Progress Dashboard\n")
        f.write("| Category | Status | Count |\n| :--- | :--- | :--- |\n")
        f.write(f"| **Open Technical Issues** | 🔴 High | {len(active)} |\n")
        f.write(f"| **Validation Tasks** | 🟡 Pending | {len(pending)} |\n")
        f.write(f"| **Resolved (Total)** | 🟢 Progress | {len(resolved)} |\n\n")

        f.write("## 🔴 Open Issues\n| ID | Issue | Priority |\n| :--- | :--- | :--- |\n")
        for s in active:
            f.write(f"| **#{s['id']}** | {s['title']} | {s['priority']} |\n")

        f.write("\n## 🟡 Pending Validation\n| ID | Task | Requirement |\n| :--- | :--- | :--- |\n")
        for s in pending:
            f.write(f"| **#{s['id']}** | {s['title']} | {s['requirement'] or 'N/A'} |\n")

        f.write("\n## 🟢 Recently Resolved\n| ID | Issue | Resolution |\n| :--- | :--- | :--- |\n")
        # Only show the last 5 resolved in issues.md
        for s in resolved[:5]:
            f.write(f"| **#{s['id']}** | {s['title']} | {s['status']} |\n")

    # 2. Update STATUS/issues_archive.md
    with open(ARCHIVE_MD, "w", encoding="utf-8") as f:
        f.write("# Issues Archive (Historical Resolutions)\n\n")
        f.write(f"**Total Unique Resolutions: {len(resolved)}**\n\n")
        for s in resolved:
            f.write(f"### Issue #{s['id']}: {s['title']}\n- **Status**: {s['status']}\n\n")

if __name__ == "__main__":
    sync()
