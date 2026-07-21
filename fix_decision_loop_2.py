import re

pv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
cbv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\CasesManagementBrownView.java"

# 1. Update PlayerView.java
with open(pv_file, "r", encoding="utf-8") as f:
    pv_content = f.read()

pv_content = pv_content.replace("private String mode;", "public String mode;")
pv_content = pv_content.replace("private String decisionDetail;", "public String decisionDetail;")
pv_content = pv_content.replace("private boolean finalized;", "public boolean finalized;")

with open(pv_file, "w", encoding="utf-8") as f:
    f.write(pv_content)

# 2. Update CasesManagementBrownView.java
with open(cbv_file, "r", encoding="utf-8") as f:
    cbv_content = f.read()

cbv_content = cbv_content.replace("Runnable onDecisionMade = () -> javafx.application.Platform.runLater(this::refreshActiveGroups);", "Runnable onDecisionMade = () -> javafx.application.Platform.runLater(() -> refreshActiveGroups(stage));")

with open(cbv_file, "w", encoding="utf-8") as f:
    f.write(cbv_content)

print("Fixed compile errors for decision loop")
