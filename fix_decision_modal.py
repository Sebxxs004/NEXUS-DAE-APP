import re

pv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
pvb_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerViewBrown.java"
cbv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\CasesManagementBrownView.java"

# 1. Update PlayerView.java
with open(pv_file, "r", encoding="utf-8") as f:
    pv_content = f.read()

getter_code = """
    public javafx.scene.layout.StackPane getDecisionOverlay() {
        return decisionOverlay;
    }
"""
if "public javafx.scene.layout.StackPane getDecisionOverlay()" not in pv_content:
    pv_content = pv_content.replace("private void showDecisionOverlay() {", getter_code + "\n    private void showDecisionOverlay() {")

show_code_old = """    private void showDecisionOverlay() {
        decisionOptionCheckboxes.values().forEach(check -> check.setSelected(false));"""

show_code_new = """    private void showDecisionOverlay() {
        if (!moduleHost.getChildren().contains(decisionOverlay)) {
            if (decisionOverlay.getParent() instanceof javafx.scene.layout.Pane) {
                ((javafx.scene.layout.Pane) decisionOverlay.getParent()).getChildren().remove(decisionOverlay);
            }
            moduleHost.getChildren().add(decisionOverlay);
        }
        decisionOptionCheckboxes.values().forEach(check -> check.setSelected(false));"""

pv_content = pv_content.replace(show_code_old, show_code_new)

with open(pv_file, "w", encoding="utf-8") as f:
    f.write(pv_content)


# 2. Update PlayerViewBrown.java
with open(pvb_file, "r", encoding="utf-8") as f:
    pvb_content = f.read()

brown_getter = """
    public javafx.scene.layout.StackPane getDecisionOverlay() {
        return playerView != null ? playerView.getDecisionOverlay() : null;
    }
"""
if "public javafx.scene.layout.StackPane getDecisionOverlay()" not in pvb_content:
    pvb_content = pvb_content.replace("public void triggerDecisionForGroup", brown_getter + "\n    public void triggerDecisionForGroup")

with open(pvb_file, "w", encoding="utf-8") as f:
    f.write(pvb_content)


# 3. Update CasesManagementBrownView.java
with open(cbv_file, "r", encoding="utf-8") as f:
    cbv_content = f.read()

old_action = """            decideBtn.setOnAction(e -> {
                PlayerViewBrown pvb = PlayerViewBrown.getInstance(stage);
                javafx.scene.Scene dummyScene = com.prisma.ui.ResponsiveUtils.createResponsiveScene(pvb.getView(), 1500, 900);
                com.prisma.ui.Theme.apply(dummyScene);
                
                javafx.scene.Scene currentScene = stage.getScene();
                if (currentScene != null) {
                    javafx.scene.Parent viewRoot = dummyScene.getRoot();
                    dummyScene.setRoot(new javafx.scene.layout.Region());
                    currentScene.setRoot(viewRoot);
                } else {
                    stage.setScene(dummyScene);
                    stage.setMaximized(true);
                    stage.setFullScreen(true);
                }
                
                pvb.triggerDecisionForGroup(cluster.signature);
            });"""

new_action = """            decideBtn.setOnAction(e -> {
                PlayerViewBrown pvb = PlayerViewBrown.getInstance(stage);
                pvb.triggerDecisionForGroup(cluster.signature);
                
                javafx.scene.layout.StackPane overlay = pvb.getDecisionOverlay();
                if (overlay != null) {
                    if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                        ((javafx.scene.layout.Pane) overlay.getParent()).getChildren().remove(overlay);
                    }
                    if (!view.getChildren().contains(overlay)) {
                        view.getChildren().add(overlay);
                    }
                    overlay.toFront();
                }
            });"""

cbv_content = cbv_content.replace(old_action, new_action)

# Also there was a previous attempt where I didn't change it, let's catch it if it exists
old_action2 = """            decideBtn.setOnAction(e -> {
                PlayerViewBrown pvb = PlayerViewBrown.getInstance(stage);
                javafx.scene.Scene scene = com.prisma.ui.ResponsiveUtils.createResponsiveScene(pvb.getView(), 1500, 900);
                com.prisma.ui.Theme.apply(scene);
                stage.setScene(scene);
                pvb.triggerDecisionForGroup(cluster.signature);
            });"""
cbv_content = cbv_content.replace(old_action2, new_action)


with open(cbv_file, "w", encoding="utf-8") as f:
    f.write(cbv_content)

print("Fixed decision modal reparenting")
