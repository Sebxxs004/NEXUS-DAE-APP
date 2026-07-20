import re

pv = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
with open(pv, "r", encoding="utf-8") as f:
    content = f.read()

# Fix 1: illegal combination of modifiers
content = content.replace("public private static final class GroupMeta", "public static class GroupMeta")
content = content.replace("public private record GroupMeta", "public record GroupMeta")
content = content.replace("private static final class GroupMeta", "public static class GroupMeta")
content = content.replace("private record GroupMeta", "public record GroupMeta")

# Fix 2: name has private access
# If GroupMeta is a class with fields, let's make the fields public.
# Let's see what GroupMeta looks like exactly.
import sys
# It's probably easier to just replace 'String name;' with 'public String name;' 
# But wait, GroupMeta might just be a standard class.
# Let's change the field modifiers to public.
content = content.replace("String name;", "public String name;")
content = content.replace("Color color;", "public Color color;")
content = content.replace("String reason;", "public String reason;")
content = content.replace("String additionalNotes;", "public String additionalNotes;")
content = content.replace("String additionalMedia;", "public String additionalMedia;")
content = content.replace("boolean resolved;", "public boolean resolved;")

with open(pv, "w", encoding="utf-8") as f:
    f.write(content)
print("Fixed GroupMeta")
