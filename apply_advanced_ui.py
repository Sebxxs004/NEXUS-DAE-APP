import re

pv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
pvb_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerViewBrown.java"
cbv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\CasesManagementBrownView.java"

# --- 1. PlayerView.java ---
with open(pv_file, "r", encoding="utf-8") as f:
    pv_content = f.read()

findGroups_code = """
    public List<GroupCluster> findGroupsForCase(Caso caso) {
        if (caso == null) {
            return java.util.Collections.emptyList();
        }
        CaseNode targetNode = null;
        for (CaseNode node : nodes) {
            if (node.getCaso().getNombre().equalsIgnoreCase(caso.getNombre())) {
                targetNode = node;
                break;
            }
        }
        if (targetNode == null) return java.util.Collections.emptyList();
        
        List<GroupCluster> result = new ArrayList<>();
        for (GroupCluster cluster : currentClusters) {
            if (cluster.members.contains(targetNode)) {
                result.add(cluster);
            }
        }
        return result;
    }
"""
if "public List<GroupCluster> findGroupsForCase" not in pv_content:
    pv_content = pv_content.replace("public GroupCluster findGroupForCase(Caso caso) {", findGroups_code + "\n    public GroupCluster findGroupForCase(Caso caso) {")

with open(pv_file, "w", encoding="utf-8") as f:
    f.write(pv_content)


# --- 2. PlayerViewBrown.java ---
with open(pvb_file, "r", encoding="utf-8") as f:
    pvb_content = f.read()

findGroups_brown_code = """
    public java.util.List<PlayerView.GroupCluster> findGroupsForCase(com.prisma.models.Caso caso) {
        if (playerView == null) return java.util.Collections.emptyList();
        return playerView.findGroupsForCase(caso);
    }
"""
if "public java.util.List<PlayerView.GroupCluster> findGroupsForCase" not in pvb_content:
    pvb_content = pvb_content.replace("public PlayerView.GroupCluster findGroupForCase(com.prisma.models.Caso caso) {", findGroups_brown_code + "\n    public PlayerView.GroupCluster findGroupForCase(com.prisma.models.Caso caso) {")

with open(pvb_file, "w", encoding="utf-8") as f:
    f.write(pvb_content)


# --- 3. CasesManagementBrownView.java ---
with open(cbv_file, "r", encoding="utf-8") as f:
    cbv_content = f.read()

# buildCaseCard replacement
old_build_card = """        PlayerView.GroupCluster groupCluster = PlayerViewBrown.getInstance(stage).findGroupForCase(caso);
        boolean isGrouped = groupCluster != null;

        // Colors
        String tabColor = isGrouped ? colorToRgb(groupCluster.getColor()) : "#084C8C";
        
        // Folder Tab
        Region folderTab = new Region();
        folderTab.setPrefHeight(20);
        folderTab.setMaxWidth(120);
        folderTab.setStyle("-fx-background-color: " + tabColor + "; -fx-background-radius: 12 12 0 0;");"""

new_build_card = """        java.util.List<PlayerView.GroupCluster> clusters = PlayerViewBrown.getInstance(stage).findGroupsForCase(caso);
        boolean isGrouped = !clusters.isEmpty();

        // Colors
        String tabBgStyle = "-fx-background-color: #084C8C;";
        if (isGrouped) {
            if (clusters.size() == 1) {
                tabBgStyle = "-fx-background-color: " + colorToRgb(clusters.get(0).getColor()) + ";";
            } else {
                StringBuilder sb = new StringBuilder("-fx-background-color: linear-gradient(to right, ");
                int numColors = clusters.size();
                for (int i = 0; i < numColors; i++) {
                    String c = colorToRgb(clusters.get(i).getColor());
                    double start = (double)i / numColors * 100;
                    double end = (double)(i + 1) / numColors * 100;
                    sb.append(c).append(" ").append(start).append("%, ");
                    sb.append(c).append(" ").append(end).append("%");
                    if (i < numColors - 1) sb.append(", ");
                }
                sb.append(");");
                tabBgStyle = sb.toString();
            }
        }
        
        // Folder Tab
        Region folderTab = new Region();
        folderTab.setPrefHeight(20);
        folderTab.setMaxWidth(120);
        folderTab.setStyle(tabBgStyle + " -fx-background-radius: 12 12 0 0;");"""

cbv_content = cbv_content.replace(old_build_card, new_build_card)

# Accordion menu for active groups
old_refresh = """        for (PlayerView.GroupCluster cluster : clusters) {
            VBox groupCard = new VBox(6);
            groupCard.setPadding(new Insets(12));
            groupCard.setStyle("-fx-background-color: #121C3A; -fx-background-radius: 6; -fx-border-color: #1F2A4A; -fx-border-radius: 6;");
            
            Label nameLbl = new Label(cluster.meta.name);
            nameLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            javafx.scene.shape.Circle colorDot = new javafx.scene.shape.Circle(6, cluster.meta.color);
            HBox titleBox = new HBox(8, colorDot, nameLbl);
            titleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Label countLbl = new Label(cluster.members.size() + " casos");
            countLbl.setStyle("-fx-text-fill: #67E8F9; -fx-font-size: 12px;");
            
            groupCard.getChildren().addAll(titleBox, countLbl);
            activeGroupsContainer.getChildren().add(groupCard);
        }"""

new_refresh = """        for (PlayerView.GroupCluster cluster : clusters) {
            VBox groupCard = new VBox(6);
            groupCard.setPadding(new Insets(12));
            groupCard.setStyle("-fx-background-color: #121C3A; -fx-background-radius: 6; -fx-border-color: #1F2A4A; -fx-border-radius: 6;");
            
            Label nameLbl = new Label(cluster.meta.name);
            nameLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            javafx.scene.shape.Circle colorDot = new javafx.scene.shape.Circle(6, cluster.meta.color);
            HBox titleBox = new HBox(8, colorDot, nameLbl);
            titleBox.setAlignment(Pos.CENTER_LEFT);
            
            FontIcon arrowIcon = new FontIcon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHEVRON_DOWN);
            arrowIcon.setIconColor(Color.web("#808D9E"));
            arrowIcon.setIconSize(12);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            HBox headerBox = new HBox(8, titleBox, spacer, arrowIcon);
            headerBox.setAlignment(Pos.CENTER_LEFT);
            headerBox.setStyle("-fx-cursor: hand;");
            
            Label countLbl = new Label(cluster.members.size() + " casos");
            countLbl.setStyle("-fx-text-fill: #67E8F9; -fx-font-size: 12px;");
            
            VBox casesList = new VBox(4);
            casesList.setPadding(new Insets(6, 0, 0, 20));
            casesList.setVisible(false);
            casesList.setManaged(false);
            
            for (PlayerView.CaseNode node : cluster.members) {
                Label caseLbl = new Label("• " + node.getCaso().getNombre());
                caseLbl.setStyle("-fx-text-fill: #A0B0C0; -fx-font-size: 11px;");
                casesList.getChildren().add(caseLbl);
            }
            
            headerBox.setOnMouseClicked(e -> {
                boolean isExpanded = casesList.isVisible();
                casesList.setVisible(!isExpanded);
                casesList.setManaged(!isExpanded);
                arrowIcon.setIconCode(!isExpanded ? org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHEVRON_UP : org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHEVRON_DOWN);
            });
            
            groupCard.getChildren().addAll(headerBox, countLbl, casesList);
            activeGroupsContainer.getChildren().add(groupCard);
        }"""

cbv_content = cbv_content.replace(old_refresh, new_refresh)

with open(cbv_file, "w", encoding="utf-8") as f:
    f.write(cbv_content)

print("Applied advanced UI features!")
