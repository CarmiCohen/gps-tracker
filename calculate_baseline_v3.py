import re

files = [
    r"C:/CCwork/Android Projects/gps-tracker/issues.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/compliance.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/issues_archive.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/requirements_sot.md"
]

all_unique_ids = set()

# Pattern for requirements: R followed by 3 digits, then optional characters
req_pattern = re.compile(r'R\d{3}[a-zA-Z0-9-]*')

# Pattern for issues: # followed by 1-3 digits, then optional suffixes
# We need to be careful with colors like #78BE20
issue_pattern = re.compile(r'#([0-9a-zA-Z-]{1,10})')

def is_hex_color(s):
    if len(s) == 6 and all(c in "0123456789ABCDEF" for c in s.upper()):
        return True
    return False

for file_path in files:
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

            # Extract requirements
            reqs = req_pattern.findall(content)
            for r in reqs:
                all_unique_ids.add(r)

            # Extract issues
            # First, handle the slashes like #244/245 by replacing them
            content_processed = content.replace("/", " #")
            issues = issue_pattern.findall(content_processed)
            for i in issues:
                # Filter out colors
                if is_hex_color(i):
                    continue

                # Filter out versions like v9.1.2 if they started with # (unlikely)
                if i.startswith("v") and any(c.isdigit() for c in i):
                    continue

                # Filter out items that don't have digits (likely not an ID)
                if not any(c.isdigit() for c in i):
                    continue

                # Standardize issues: If it's pure digits, pad to 3
                if i.isdigit():
                    all_unique_ids.add(f"#{int(i):03d}")
                else:
                    # If it has a suffix, e.g., 325-B, try to pad the number part
                    match = re.match(r"^(\d+)(-.+)$", i)
                    if match:
                        num = int(match.group(1))
                        suffix = match.group(2)
                        all_unique_ids.add(f"#{num:03d}{suffix}")
                    else:
                        # Case like R325 appearing as #R325 in some places?
                        if i.startswith("R") and re.match(r"^R\d{3}", i):
                            all_unique_ids.add(i)
                        else:
                            all_unique_ids.add(f"#{i}")

    except Exception as e:
        print(f"Error reading {file_path}: {e}")

# The user mentioned Legacy Issue Mapping table specifically.
# Let's make sure we got all IDs from it.
# Row example: | #115 | #322 | Category... |
# My current logic should catch #115 and #322.

# Filter out common non-ID strings that might have slipped through
all_unique_ids = {id for id in all_unique_ids if id not in ["#2024", "#2025"]}

# Split issues and requirements for report
issues = sorted([id for id in all_unique_ids if id.startswith("#")])
requirements = sorted([id for id in all_unique_ids if id.startswith("R")])

print(f"Unique Issues: {len(issues)}")
print(f"Unique Requirements: {len(requirements)}")
print(f"Total Audit Baseline: {len(all_unique_ids)}")

# Let's print the first few to check
# print(f"Sample Issues: {issues[:10]}")
# print(f"Sample Reqs: {requirements[:10]}")
