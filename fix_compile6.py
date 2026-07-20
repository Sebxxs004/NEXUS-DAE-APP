import re

pv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"

with open(pv_file, "r", encoding="utf-8") as f:
    pv_content = f.read()

pv_content = pv_content.replace("private Caso getCaso()", "public Caso getCaso()")

with open(pv_file, "w", encoding="utf-8") as f:
    f.write(pv_content)

print("Fixed getCaso access")
