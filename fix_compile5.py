import re

pv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
cbv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\CasesManagementBrownView.java"

# 1. Fix CaseNode access
with open(pv_file, "r", encoding="utf-8") as f:
    pv_content = f.read()

pv_content = pv_content.replace("private static class CaseNode", "public static class CaseNode")
pv_content = pv_content.replace("private static final class CaseNode", "public static final class CaseNode")
pv_content = pv_content.replace("private class CaseNode", "public class CaseNode")

with open(pv_file, "w", encoding="utf-8") as f:
    f.write(pv_content)

# 2. Fix groupCluster usage in badge
with open(cbv_file, "r", encoding="utf-8") as f:
    cbv_content = f.read()

old_badge = """        if (isGrouped) {
            Label groupBadge = new Label(groupCluster.getName());
            groupBadge.setStyle("-fx-background-color: " + colorToRgb(groupCluster.getColor()) + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8 4 8; -fx-background-radius: 4;");
            StackPane.setAlignment(groupBadge, Pos.TOP_LEFT);
            imageArea.getChildren().add(groupBadge);
        }"""

new_badge = """        if (isGrouped) {
            HBox badgesBox = new HBox(4);
            for (PlayerView.GroupCluster cluster : clusters) {
                Label groupBadge = new Label(cluster.getName());
                groupBadge.setStyle("-fx-background-color: " + colorToRgb(cluster.getColor()) + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8 4 8; -fx-background-radius: 4;");
                badgesBox.getChildren().add(groupBadge);
            }
            StackPane.setAlignment(badgesBox, Pos.TOP_LEFT);
            imageArea.getChildren().add(badgesBox);
        }"""

cbv_content = cbv_content.replace(old_badge, new_badge)

with open(cbv_file, "w", encoding="utf-8") as f:
    f.write(cbv_content)

print("Fixed access and badge")
