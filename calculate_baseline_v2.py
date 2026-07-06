import re

files = [
    r"C:/CCwork/Android Projects/gps-tracker/issues.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/compliance.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/issues_archive.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/requirements_sot.md"
]

issue_set = set()
req_set = set()

# Pattern for IDs like #001, #325-B, #115, #244/245
# We'll extract anything that looks like # followed by alphanumeric/dashes
# then filter out hex colors.
raw_issue_pattern = re.compile(r'#([a-zA-Z0-9\-/]+)')
# Requirement IDs like R014, R810-A15, R799d
req_pattern = re.compile(r'\b(R[0-9]{3}[a-zA-Z0-9-]*)\b')

for path in files:
    try:
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()

            # Extract requirements
            reqs = req_pattern.findall(content)
            for r in reqs:
                req_set.add(r)

            # Extract potential issues
            potential_issues = raw_issue_pattern.findall(content)
            for pi in potential_issues:
                # Handle slashes like 244/245
                parts = pi.replace('/', ' ').split()
                for part in parts:
                    # Filter out hex colors (6 hex digits)
                    if len(part) == 6 and all(c in '0123456789ABCDEFabcdef' for c in part):
                        continue
                    # Filter out purely alphabetical strings that aren't IDs
                    if not any(c.isdigit() for c in part):
                        continue
                    # Filter out other non-issue things (like versions v9.1.2)
                    if part.startswith('v'):
                        continue

                    issue_set.add(part)
    except Exception as e:
        print(f"Error reading {path}: {e}")

# The user mentioned #001 through #464 as examples.
# Let's count them and print total.
print(f"Total Unique Issues: {len(issue_set)}")
print(f"Total Unique Requirements: {len(req_set)}")
print(f"Grand Total (Audit Baseline): {len(issue_set) + len(req_set)}")

# For sanity, list them
# print("Issues:", sorted(list(issue_set)))
# print("Reqs:", sorted(list(req_set)))
