import re

pv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
pvb_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerViewBrown.java"
cbv_file = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\CasesManagementBrownView.java"

# 1. Update PlayerView.java
with open(pv_file, "r", encoding="utf-8") as f:
    pv_content = f.read()

# Add callback field
if "private Runnable onDecisionCallback;" not in pv_content:
    pv_content = pv_content.replace("private String pendingDecisionReason;", "private String pendingDecisionReason;\n    private Runnable onDecisionCallback;")

# Update triggerDecisionForGroup
old_trigger = """    public void triggerDecisionForGroup(String signature) {
        if (investigationFinished) return;
        GroupMeta meta = metadataBySignature.get(signature);
        if (meta == null) return;
        
        pendingDecisionGroupSignature = signature;
        pendingDecisionMeta = meta;
        pendingDecisionGroupName = meta.name;
        pendingDecisionReason = meta.reason;
        pendingDecisionColorButton = null;
        showDecisionOverlay();
    }"""
new_trigger = """    public void triggerDecisionForGroup(String signature, Runnable onDecisionMade) {
        if (investigationFinished) return;
        GroupMeta meta = metadataBySignature.get(signature);
        if (meta == null) return;
        
        pendingDecisionGroupSignature = signature;
        pendingDecisionMeta = meta;
        pendingDecisionGroupName = meta.name;
        pendingDecisionReason = meta.reason;
        pendingDecisionColorButton = null;
        this.onDecisionCallback = onDecisionMade;
        showDecisionOverlay();
    }"""
pv_content = pv_content.replace(old_trigger, new_trigger)

# Update confirmDecisionFromOverlay to call callback
old_confirm_end = """            renderGroupCards();
            updateGroupOverlays();
        }
        hideDecisionOverlay();
    }"""
new_confirm_end = """            renderGroupCards();
            updateGroupOverlays();
        }
        hideDecisionOverlay();
        if (onDecisionCallback != null) {
            onDecisionCallback.run();
            onDecisionCallback = null; // Clear it after running
        }
    }"""
pv_content = pv_content.replace(old_confirm_end, new_confirm_end)

with open(pv_file, "w", encoding="utf-8") as f:
    f.write(pv_content)

# 2. Update PlayerViewBrown.java
with open(pvb_file, "r", encoding="utf-8") as f:
    pvb_content = f.read()

old_brown_trigger = """    public void triggerDecisionForGroup(String signature) {
        if (playerView != null) {
            playerView.triggerDecisionForGroup(signature);
        }
    }"""
new_brown_trigger = """    public void triggerDecisionForGroup(String signature, Runnable callback) {
        if (playerView != null) {
            playerView.triggerDecisionForGroup(signature, callback);
        }
    }"""
pvb_content = pvb_content.replace(old_brown_trigger, new_brown_trigger)

with open(pvb_file, "w", encoding="utf-8") as f:
    f.write(pvb_content)


# 3. Update CasesManagementBrownView.java
with open(cbv_file, "r", encoding="utf-8") as f:
    cbv_content = f.read()

old_btn_logic = """            Button decideBtn = new Button("¿Qué vas a decidir ahora?");
            decideBtn.setStyle("-fx-background-color: #F1C40F; -fx-text-fill: #000000; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 8; -fx-background-radius: 4;");
            decideBtn.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(decideBtn, new Insets(8, 0, 0, 0));
            decideBtn.setOnAction(e -> {
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
            });
            casesList.getChildren().add(decideBtn);"""

new_btn_logic = """            if (cluster.meta != null && cluster.meta.finalized) {
                Label decidedLbl = new Label("Decisión tomada: " + cluster.meta.mode);
                decidedLbl.setStyle("-fx-text-fill: #A0B0C0; -fx-font-size: 11px; -fx-font-style: italic; -fx-padding: 6 0 0 0;");
                casesList.getChildren().add(decidedLbl);
            } else {
                Button decideBtn = new Button("¿Qué vas a decidir ahora?");
                decideBtn.setStyle("-fx-background-color: #F1C40F; -fx-text-fill: #000000; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 8; -fx-background-radius: 4;");
                decideBtn.setMaxWidth(Double.MAX_VALUE);
                VBox.setMargin(decideBtn, new Insets(8, 0, 0, 0));
                decideBtn.setOnAction(e -> {
                    PlayerViewBrown pvb = PlayerViewBrown.getInstance(stage);
                    
                    Runnable onDecisionMade = () -> javafx.application.Platform.runLater(this::refreshActiveGroups);
                    pvb.triggerDecisionForGroup(cluster.signature, onDecisionMade);
                    
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
                });
                casesList.getChildren().add(decideBtn);
            }"""

cbv_content = cbv_content.replace(old_btn_logic, new_btn_logic)

with open(cbv_file, "w", encoding="utf-8") as f:
    f.write(cbv_content)

print("Fixed decision logic")
