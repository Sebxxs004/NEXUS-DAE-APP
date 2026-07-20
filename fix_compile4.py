import re

pv = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
with open(pv, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("private public ", "public ")
content = content.replace("public private ", "public ")

with open(pv, "w", encoding="utf-8") as f:
    f.write(content)
print("Fixed modifiers")
