import re
import os

files = [
    "issues.md",
    "STATUS/compliance.md",
    "STATUS/issues_archive.md",
    "STATUS/requirements_sot.md"
]

repo_root = "C:/CCwork/Android Projects/gps-tracker"

all_ids = set()

# Regex for Issue IDs: # followed by digits and optional alphanumeric/dash suffixes
# Excluding hex colors which are usually 6 chars and all hex.
issue_pattern = re.compile(r'#([0-9a-zA-Z-]{1,10})')
# Regex for Requirement IDs: R followed by 3 digits and optional suffixes
req_pattern = re.compile(r'R\d{3}[a-zA-Z0-9-]*')

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
            # Filter colors
            if is_hex_color(i):
                continue
            # Filter non-IDs (must have at least one digit)
            if not any(c.isdigit() for c in i):
                continue
            # Pad simple numeric IDs to 3 digits (e.g., #5 -> #005)
            if i.isdigit():
                all_ids.add(f"#{int(i):03d}")
            else:
                # Handle suffixes like 325-B
                match = re.match(r'^(\d+)(-.+)$', i)
                if match:
                    num = int(match.group(1))
                    suffix = match.group(2)
                    all_ids.add(f"#{num:03d}{suffix}")
                else:
                    # Catch things like R799d if they appeared as #R799d (unlikely but safe)
                    if i.startswith('R'):
                        all_ids.add(i)
                    else:
                        all_ids.add(f"#{i}")

        # Extract Requirements
        reqs = req_pattern.findall(content)
        for r in reqs:
            all_ids.add(r)

# Final cleanup: exclude items that are definitely not IDs
filtered_ids = {id for id in all_ids if not id.lower().startswith('#v') and id not in ["#2024", "#2025"]}

issues = sorted([id for id in filtered_ids if id.startswith('#')])
reqs = sorted([id for id in filtered_ids if id.startswith('R')])

print(f"Unique Issues Found: {len(issues)}")
print(f"Unique Requirements Found: {len(reqs)}")
print(f"Total Audit Baseline: {len(filtered_ids)}")
