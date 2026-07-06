import re

files = [
    r"C:/CCwork/Android Projects/gps-tracker/issues.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/compliance.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/issues_archive.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/requirements_sot.md"
]

# Pattern for # followed by numbers, potentially with suffixes like -B, -A, etc.
# Also handling ranges or lists like #244/245
issue_id_pattern = re.compile(r'#([0-9]{3}(?:-[A-Z0-9]+)?)')
# Special legacy IDs that might not be 3 digits (though the prompt says #001 through #464)
# Let's also look for # followed by 1-3 digits.
legacy_issue_id_pattern = re.compile(r'#([0-9]{1,3}(?:-[A-Z0-9]+)?)')

# Requirement ID pattern: R followed by digits and optional suffixes
req_id_pattern = re.compile(r'(R[0-9]{3}[a-zA-Z0-9-]*)')

unique_issues = set()
unique_requirements = set()

for file_path in files:
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

            # Find issues
            # We want to catch things like #244/245 as two issues.
            # So we can replace / with # and then search.
            search_content = content.replace('/', ' #')

            issues = legacy_issue_id_pattern.findall(search_content)
            for issue in issues:
                # Exclude hex colors like #78BE20 or #06B6D4 (checked by length or content)
                if len(issue) <= 6 and all(c in '0123456789ABCDEF- ' for c in issue.upper()):
                    # Most hex colors won't have - except maybe in weird cases.
                    # But issue IDs in this project seem to be 3 digits or have a suffix.
                    # Colors are typically 6 hex digits.
                    if len(issue) == 6 and not '-' in issue:
                        continue

                # Check if it's actually an ID.
                # 3 digits is standard. 1-2 digits are legacy.
                unique_issues.add(issue)

            # Find requirements
            reqs = req_id_pattern.findall(content)
            for req in reqs:
                unique_requirements.add(req)

    except Exception as e:
        print(f"Error reading {file_path}: {e}")

# Manual cleaning of false positives (like colors if any leaked through)
# JD Vivid Green (#78BE20), Cyan (#06B6D4), Orange (#FF8C00 - not seen but possible)
colors = {"78BE20", "06B6D4", "367C2B"}
unique_issues = {i for i in unique_issues if i not in colors}

# The prompt mentions #001 through #464.
# Let's see what we found.
print(f"Unique Issues Count: {len(unique_issues)}")
print(f"Unique Requirements Count: {len(unique_requirements)}")
print(f"Total Audit Baseline: {len(unique_issues) + len(unique_requirements)}")

# Sort and print for debugging if needed (internal)
# print(sorted(list(unique_issues)))
# print(sorted(list(unique_requirements)))
