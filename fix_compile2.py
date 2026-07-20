import re

pv = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
with open(pv, "r", encoding="utf-8") as f:
    content = f.read()

# Fix 1: remaining new Connection(...) without groupId
content = re.sub(r'new Connection\(([^,]+),\s*([^,]+),\s*([^,)]+)\)', r'new Connection(\1, \2, \3, "Grupo Legado")', content)

# Fix 2: GroupMeta visibility
# Let's search for `static final class GroupMeta` or similar and replace with `public static final class GroupMeta`
content = content.replace('static final class GroupMeta', 'public static final class GroupMeta')
# Also the record if it is a record
content = content.replace('record GroupMeta', 'public record GroupMeta')

with open(pv, "w", encoding="utf-8") as f:
    f.write(content)
print("Fixed remaining compile errors")
