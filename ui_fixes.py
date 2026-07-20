import re

cbv = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\CasesManagementBrownView.java"
with open(cbv, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Resize cards for 4 columns
content = content.replace("casesGrid.setPrefTileWidth(290);", "casesGrid.setPrefTileWidth(225);")
content = content.replace("fullCard.setPrefWidth(290);", "fullCard.setPrefWidth(225);")
content = content.replace("fullCard.setMinWidth(290);", "fullCard.setMinWidth(225);")
content = content.replace("fullCard.setMaxWidth(290);", "fullCard.setMaxWidth(225);")
content = content.replace("cardWrapper.setPrefWidth(290);", "cardWrapper.setPrefWidth(225);")
content = content.replace("cardWrapper.setMinWidth(290);", "cardWrapper.setMinWidth(225);")
content = content.replace("cardWrapper.setMaxWidth(290);", "cardWrapper.setMaxWidth(225);")

# 2. Add color dot to Active Groups list
old_group_content = """            Label nameLbl = new Label(cluster.meta.name);
            nameLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            Label countLbl = new Label(cluster.members.size() + " casos");
            countLbl.setStyle("-fx-text-fill: #67E8F9; -fx-font-size: 12px;");
            
            groupCard.getChildren().addAll(nameLbl, countLbl);"""
new_group_content = """            Label nameLbl = new Label(cluster.meta.name);
            nameLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            javafx.scene.shape.Circle colorDot = new javafx.scene.shape.Circle(6, cluster.meta.color);
            HBox titleBox = new HBox(8, colorDot, nameLbl);
            titleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Label countLbl = new Label(cluster.members.size() + " casos");
            countLbl.setStyle("-fx-text-fill: #67E8F9; -fx-font-size: 12px;");
            
            groupCard.getChildren().addAll(titleBox, countLbl);"""

content = content.replace(old_group_content, new_group_content)

with open(cbv, "w", encoding="utf-8") as f:
    f.write(content)
print("Applied UI fixes")
