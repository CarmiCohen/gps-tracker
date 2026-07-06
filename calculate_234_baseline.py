import re
import os

files = [
    "234/issues.md",
    "234/STATUS/compliance.md",
    "234/STATUS/issues_archive.md",
    "234/STATUS/requirements_sot.md"
]

repo_root = "C:/CCwork/Android Projects/gps-tracker"

unique_issues = set()
unique_reqs = set()

issue_pattern = re.compile(r'#([0-9a-zA-Z-]{1,10})')
req_pattern = re.compile(r'\b(R[0-9]{3}[a-zA-Z0-9-]*)\b')

def is_hex_color(s):
    if len(s) == 6 and all(c in "0123456789ABCDEF" for c in s.upper()):
        return True
    return False

for f_path in files:
    full_path = os.path.join(repo_root, f_path)
    if not os.path.exists(full_path):
        continue

    with open(full_path, 'r', encoding='utf-8') as f:
        content = f.read()

        # Handle slashes like #244/245
        processed_content = content.replace('/', ' #')

        # Extract Issues
        potential_issues = issue_pattern.findall(processed_content)
        for i in potential_issues:
            if is_hex_color(i): continue
            if not any(c.isdigit() for c in i): continue

            if i.isdigit():
                unique_issues.add(f"#{int(i):03d}")
            else:
                match = re.match(r'^(\d+)(-.+)$', i)
                if match:
                    num = int(match.group(1))
                    suffix = match.group(2)
                    unique_issues.add(f"#{num:03d}{suffix}")
                else:
                    if not i.startswith('R'):
                        unique_issues.add(f"#{i}")

        # Extract Requirements
        reqs = req_pattern.findall(content)
        for r in reqs:
            unique_reqs.add(r)

# Filter out common noise
unique_issues = {id for id in unique_issues if not id.lower().startswith('#v') and id not in ["#2024", "#2025", "#367C2B", "#78BE20"]}

print(f"Unique Issues in folder 234: {len(unique_issues)}")
print(f"Unique Requirements in folder 234: {len(unique_reqs)}")
print(f"Total: {len(unique_issues) + len(unique_reqs)}")
