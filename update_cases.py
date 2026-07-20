import sys
import re

file_path = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\CasesManagementBrownView.java"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add VBox activeGroupsContainer field
if "private VBox activeGroupsContainer;" not in content:
    content = content.replace("private ScrollPane gridScroll;", "private ScrollPane gridScroll;\n    private VBox activeGroupsContainer;")

# 2. Update setPrefColumns
old_pref = "casesGrid.setPrefColumns(3);"
new_pref = "casesGrid.setPrefColumns(4);"
if old_pref in content:
    content = content.replace(old_pref, new_pref)

# 3. Modify layout in constructor
old_layout = """        contentShell.setTop(new VBox(0, topRow, searchRow));
        contentShell.setBottom(batchButtonsContainer);
        contentShell.setCenter(new VBox(0, subtitle, gridScroll));
        BorderPane.setMargin(subtitle, new Insets(8, 16, 4, 16));
        BorderPane.setMargin(gridScroll, new Insets(0, 16, 16, 16));
        VBox.setVgrow(gridScroll, Priority.ALWAYS);"""

new_layout = """        contentShell.setTop(new VBox(0, topRow, searchRow));
        contentShell.setBottom(batchButtonsContainer);
        
        VBox centerContent = new VBox(0, subtitle, gridScroll);
        VBox.setVgrow(gridScroll, Priority.ALWAYS);
        HBox.setHgrow(centerContent, Priority.ALWAYS);
        
        activeGroupsContainer = new VBox(12);
        activeGroupsContainer.setPadding(new Insets(16));
        activeGroupsContainer.setPrefWidth(300);
        activeGroupsContainer.setMinWidth(300);
        activeGroupsContainer.setStyle("-fx-background-color: #04091A; -fx-background-radius: 8;");
        
        Label groupsTitle = new Label("Grupos Activos");
        groupsTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: " + FONT + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        activeGroupsContainer.getChildren().add(groupsTitle);
        
        ScrollPane groupsScroll = new ScrollPane(activeGroupsContainer);
        groupsScroll.setFitToWidth(true);
        groupsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        groupsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        groupsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        groupsScroll.setPrefWidth(320);
        groupsScroll.setMinWidth(320);
        
        HBox centerLayout = new HBox(16, centerContent, groupsScroll);
        centerLayout.setPadding(new Insets(0, 16, 16, 16));
        
        contentShell.setCenter(centerLayout);
        
        BorderPane.setMargin(subtitle, new Insets(8, 16, 4, 0));
        BorderPane.setMargin(gridScroll, new Insets(0, 0, 16, 0));"""

if old_layout in content:
    content = content.replace(old_layout, new_layout)
else:
    print("Could not find old layout")

# 4. Add refreshActiveGroups
if "private void refreshActiveGroups(Stage stage)" not in content:
    refresh_method = """
    private void refreshActiveGroups(Stage stage) {
        if (activeGroupsContainer == null) return;
        activeGroupsContainer.getChildren().clear();
        Label groupsTitle = new Label("Grupos Activos");
        groupsTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-family: " + FONT + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        activeGroupsContainer.getChildren().add(groupsTitle);
        
        List<PlayerView.GroupCluster> clusters = PlayerViewBrown.getInstance(stage).getCurrentClusters();
        if (clusters.isEmpty()) {
            Label emptyLbl = new Label("No hay grupos activos");
            emptyLbl.setStyle("-fx-text-fill: #808D9E; -fx-font-family: " + FONT + "; -fx-font-size: 14px;");
            activeGroupsContainer.getChildren().add(emptyLbl);
            return;
        }
        
        for (PlayerView.GroupCluster cluster : clusters) {
            VBox groupCard = new VBox(6);
            groupCard.setPadding(new Insets(12));
            groupCard.setStyle("-fx-background-color: #121C3A; -fx-background-radius: 6; -fx-border-color: #1F2A4A; -fx-border-radius: 6;");
            
            Label nameLbl = new Label(cluster.metadata.name);
            nameLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            Label countLbl = new Label(cluster.members.size() + " casos");
            countLbl.setStyle("-fx-text-fill: #67E8F9; -fx-font-size: 12px;");
            
            groupCard.getChildren().addAll(nameLbl, countLbl);
            activeGroupsContainer.getChildren().add(groupCard);
        }
    }
"""
    content = content.replace("private void refreshGrid(Stage stage) {", refresh_method + "\n    private void refreshGrid(Stage stage) {\n        refreshActiveGroups(stage);")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Updated CasesManagementBrownView.java successfully.")
