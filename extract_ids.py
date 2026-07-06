import re

files = [
    r"C:/CCwork/Android Projects/gps-tracker/issues.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/compliance.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/issues_archive.md",
    r"C:/CCwork/Android Projects/gps-tracker/STATUS/requirements_sot.md"
]

# regex to find # followed by digits and optional suffixes
issue_pattern = re.compile(r'#([0-9]{3}(?:-[A-Z0-9]+)?)')
# legacy/other issues like #115 or #244/245
legacy_pattern = re.compile(r'#([0-9]{1,3}(?:-[A-Z0-9]+)?)')
# requirement pattern
req_pattern = re.compile(r'(R[0-9]{3}[a-zA-Z0-9-]*)')

all_found_issues = set()
all_found_reqs = set()

for file_path in files:
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            # Handle the slash in #244/245
            content = content.replace('/', ' #')

            issues = legacy_pattern.findall(content)
            for i in issues:
                # Filter out hex colors (usually 6 chars, all hex)
                if len(i) == 6 and all(c in '0123456789ABCDEF' for c in i.upper()):
                    continue
                # Filter out common false positives
                if i in ['000', '123']: continue # placeholder

                all_found_issues.add(i)

            reqs = req_pattern.findall(content)
            for r in reqs:
                all_found_reqs.add(r)
    except:
        pass

# The prompt says include "Authoritative Issue IDs (#001-#464)" and "Requirement IDs (R014-R972)"
# Let's count them.
print(f"Issues: {len(all_found_issues)}")
print(f"Reqs: {len(all_found_reqs)}")
print(f"Total: {len(all_found_issues) + len(all_found_reqs)}")

# Print unique counts by categories mentioned in prompt if possible
# Authoritative issues: #001-#464
auth_issues = [i for i in all_found_issues if re.match(r'^[0-9]{3}$', i) or '-' in i]
# Legacy issues: anything else starting with #
legacy_issues = [i for i in all_found_issues if i not in auth_issues]

print(f"Auth Issues: {len(auth_issues)}")
print(f"Legacy Issues: {len(legacy_issues)}")
