import re

pv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
pvb_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerViewBrown.java"
cbv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\CasesManagementBrownView.java"

# 1. Add triggerDecisionForGroup to PlayerView.java
with open(pv_file, "r", encoding="utf-8") as f:
    pv_content = f.read()

trigger_code = """
    public void triggerDecisionForGroup(String signature) {
        if (investigationFinished) return;
        GroupMeta meta = metadataBySignature.get(signature);
        if (meta == null) return;
        
        pendingDecisionGroupSignature = signature;
        pendingDecisionMeta = meta;
        pendingDecisionGroupName = meta.name;
        pendingDecisionReason = meta.reason;
        pendingDecisionColorButton = null;
        showDecisionOverlay();
    }
"""
if "public void triggerDecisionForGroup" not in pv_content:
    pv_content = pv_content.replace("private void showDecisionOverlay() {", trigger_code + "\n    private void showDecisionOverlay() {")
with open(pv_file, "w", encoding="utf-8") as f:
    f.write(pv_content)

# 2. Add triggerDecisionForGroup to PlayerViewBrown.java
with open(pvb_file, "r", encoding="utf-8") as f:
    pvb_content = f.read()

trigger_brown = """
    public void triggerDecisionForGroup(String signature) {
        if (playerView != null) {
            playerView.triggerDecisionForGroup(signature);
        }
    }
"""
if "public void triggerDecisionForGroup" not in pvb_content:
    pvb_content = pvb_content.replace("public java.util.List<PlayerView.GroupCluster> findGroupsForCase", trigger_brown + "\n    public java.util.List<PlayerView.GroupCluster> findGroupsForCase")
with open(pvb_file, "w", encoding="utf-8") as f:
    f.write(pvb_content)

# 3. Add button to CasesManagementBrownView.java
with open(cbv_file, "r", encoding="utf-8") as f:
    cbv_content = f.read()

old_list = """            for (PlayerView.CaseNode node : cluster.members) {
                Label caseLbl = new Label("• " + node.getCaso().getNombre());
                caseLbl.setStyle("-fx-text-fill: #A0B0C0; -fx-font-size: 11px;");
                casesList.getChildren().add(caseLbl);
            }
            
            headerBox.setOnMouseClicked(e -> {"""

new_list = """            for (PlayerView.CaseNode node : cluster.members) {
                Label caseLbl = new Label("• " + node.getCaso().getNombre());
                caseLbl.setStyle("-fx-text-fill: #A0B0C0; -fx-font-size: 11px;");
                casesList.getChildren().add(caseLbl);
            }
            
            Button decideBtn = new Button("¿Qué vas a decidir ahora?");
            decideBtn.setStyle("-fx-background-color: #F1C40F; -fx-text-fill: #000000; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 8; -fx-background-radius: 4;");
            decideBtn.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(decideBtn, new Insets(8, 0, 0, 0));
            decideBtn.setOnAction(e -> {
                PlayerViewBrown pvb = PlayerViewBrown.getInstance(stage);
                javafx.scene.Scene scene = com.prisma.ui.ResponsiveUtils.createResponsiveScene(pvb.getView(), 1500, 900);
                com.prisma.ui.Theme.apply(scene);
                stage.setScene(scene);
                pvb.triggerDecisionForGroup(cluster.signature);
            });
            casesList.getChildren().add(decideBtn);
            
            headerBox.setOnMouseClicked(e -> {"""

cbv_content = cbv_content.replace(old_list, new_list)

with open(cbv_file, "w", encoding="utf-8") as f:
    f.write(cbv_content)

print("Added decision button")
