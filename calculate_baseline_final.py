import re

files = [
    r"C:/CCwork/Android Projects/gps-tracker/issues.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/compliance.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/issues_archive.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/requirements_sot.md"
]

issue_ids = set()
req_ids = set()

# Regex for issues: # followed by 1-3 digits, with optional suffixes like -A, -B, -A15, etc.
# Also handle #244/245 style.
# And ensure we don't catch hex colors (#78BE20).
issue_pattern = re.compile(r'#([0-9]{1,3}(?:-[A-Z0-9]+)?)')

# Regex for requirements: R followed by 3 digits and optional alphanumeric suffixes.
req_pattern = re.compile(r'\b(R[0-9]{3}[a-zA-Z0-9-]*)\b')

for path in files:
    try:
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
            # Split slashes to catch both IDs in #244/245
            content = content.replace('/', ' #')

            # Find all potential issue IDs
            # Note: The mapping table has many IDs.
            found_issues = issue_pattern.findall(content)
            for i in found_issues:
                # Filter out hex colors (usually 6 chars, all hex)
                if len(i) == 6 and all(c in '0123456789ABCDEFabcdef' for c in i):
                    continue
                # Pad numeric IDs to 3 digits for uniqueness (e.g., #5 -> #005)
                if i.isdigit():
                    issue_ids.add(f"#{int(i):03d}")
                else:
                    # For suffixes, try to pad the number part
                    match = re.match(r'^([0-9]+)(-.+)$', i)
                    if match:
                        num = int(match.group(1))
                        suffix = match.group(2)
                        issue_ids.add(f"#{num:03d}{suffix}")
                    else:
                        issue_ids.add(f"#{i}")

            # Find all requirement IDs
            found_reqs = req_pattern.findall(content)
            for r in found_reqs:
                req_ids.add(r)

    except Exception as e:
        print(f"Error reading {path}: {e}")

# The prompt mentioned #001 through #464.
# Let's count and sum.
print(f"Unique Issue IDs: {len(issue_ids)}")
print(f"Unique Requirement IDs: {len(req_ids)}")
print(f"Total Audit Baseline: {len(issue_ids) + len(req_ids)}")
