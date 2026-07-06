import re
import os

repo_root = "C:/CCwork/Android Projects/gps-tracker"

files_gps = [
    "issues.md",
    "STATUS/compliance.md",
    "STATUS/issues_archive.md",
    "STATUS/requirements_sot.md",
    "STATUS/compliance_archive.md"
]

files_234 = [
    "234/issues.md",
    "234/STATUS/compliance.md",
    "234/STATUS/issues_archive.md",
    "234/STATUS/requirements_sot.md",
    "234/STATUS/compliance_archive.md"
]

issue_pattern = re.compile(r'#([0-9]{1,3}(?:-[A-Z0-9]+)?)')
req_pattern = re.compile(r'\b(R[0-9]{3}[a-zA-Z0-9-]*)\b')

def count_in_file(rel_path):
    full_path = os.path.join(repo_root, rel_path)
    if not os.path.exists(full_path):
        return 0, 0
    with open(full_path, 'r', encoding='utf-8') as f:
        content = f.read().replace('/', ' #')
        issues = set()
        found_issues = issue_pattern.findall(content)
        for i in found_issues:
            if len(i) == 6 and all(c in '0123456789ABCDEFabcdef' for c in i): continue
            if i.isdigit(): issues.add(f"#{int(i):03d}")
            else: issues.add(f"#{i}")

        reqs = set(req_pattern.findall(content))
        return len(issues), len(reqs)

print("GPS-Tracker Project:")
for f in files_gps:
    i, r = count_in_file(f)
    print(f"{f}: Issues={i}, Reqs={r}")

print("\n234 Project Snapshot:")
for f in files_234:
    i, r = count_in_file(f)
    print(f"{f}: Issues={i}, Reqs={r}")
