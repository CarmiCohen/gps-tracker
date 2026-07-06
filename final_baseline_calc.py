import re

files = [
    r"C:/CCwork/Android Projects/gps-tracker/issues.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/compliance.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/issues_archive.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/requirements_sot.md"
]

all_traceability_items = set()

# Regex patterns
issue_pattern = re.compile(r'#([0-9]{1,3}(?:-[A-Z0-9]+)?)')
req_pattern = re.compile(r'\b(R[0-9]{3}[a-zA-Z0-9-]*)\b')

for file_path in files:
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

            # Pre-process content to handle slashes in IDs like #244/245 or #339/348
            processed_content = content.replace('/', ' #')

            # Find Issues
            issues = issue_pattern.findall(processed_content)
            for issue in issues:
                # Normalize to #NNN format if it's just numbers
                if re.match(r'^[0-9]+$', issue):
                    val = int(issue)
                    # Exclude things that are clearly not IDs (like years 2024 or versions if they were #)
                    # The prompt says #001 to #464.
                    if 1 <= val <= 500:
                        all_traceability_items.add(f"#{val:03d}")
                else:
                    # Keep suffix versions as is (e.g. 325-B)
                    # But if it's like 214-A, ensure the number part is 3 digits?
                    match = re.match(r'^([0-9]+)(-[A-Z0-9]+)$', issue)
                    if match:
                        num = int(match.group(1))
                        suffix = match.group(2)
                        all_traceability_items.add(f"#{num:03d}{suffix}")
                    else:
                        all_traceability_items.add(f"#{issue}")

            # Find Requirements
            reqs = req_pattern.findall(content)
            for req in reqs:
                all_traceability_items.add(req)

    except Exception as e:
        print(f"Error reading {file_path}: {e}")

# Filter out hex colors if any leaked through (shouldn't with the patterns above)
# and filter out any other noise.
final_set = {item for item in all_traceability_items if not re.match(r'^#[0-9A-F]{6}$', item)}

# Let's count and sort
issues = sorted([i for i in final_set if i.startswith('#')])
reqs = sorted([r for r in final_set if r.startswith('R')])

print(f"Unique Issues: {len(issues)}")
# print(issues)
print(f"Unique Requirements: {len(reqs)}")
# print(reqs)
print(f"Total Audit Baseline: {len(final_set)}")
