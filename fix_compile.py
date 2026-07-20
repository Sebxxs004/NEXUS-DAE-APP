import sys
import re

pv = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
cbv = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\CasesManagementBrownView.java"

with open(pv, "r", encoding="utf-8") as f:
    pv_content = f.read()
with open(cbv, "r", encoding="utf-8") as f:
    cbv_content = f.read()

# 1. Connection constructor missing groupId
# We need to find `new Connection(something, something, reason)` and replace with `new Connection(..., null)`.
# Since there are multiple places, let's just do a regex replace for new Connection(arg1, arg2, arg3) where arg3 is not null, excluding the new 4-arg ones.
# Or just replace the exact lines:
pv_content = pv_content.replace("connections.add(new Connection(sourceNode, targetNode, null));", 'connections.add(new Connection(sourceNode, targetNode, null, "Grupo Legado"));')
pv_content = pv_content.replace("connections.add(new Connection(dragStartNode, targetNode, null));", 'connections.add(new Connection(dragStartNode, targetNode, null, "Grupo Legado"));')
pv_content = pv_content.replace("connections.add(new Connection(fromNode, toNode, null));", 'connections.add(new Connection(fromNode, toNode, null, "Grupo Legado"));')

# 2. #FFFFFF -> Color.web("#FFFFFF")
pv_content = pv_content.replace('GroupMeta(\n                        groupName,\n                        "#FFFFFF",', 'GroupMeta(\n                        groupName,\n                        Color.web("#FFFFFF"),')

# 3. customGroupName not found in PlayerView around 3124
# The addToGroup logic is probably outside `createBatchConnections`, maybe in `addToExistingGroup` which does not have `customGroupName` parameter!
# Let's find addToExistingGroup. It accepts `targetMember` and `casos`. The group is the group of `targetMember`.
# `customGroupName` in my replace was from `addToGroup` old logic but `addToExistingGroup` doesn't have it.
# We should use `targetGroupSignature` instead of `customGroupName`.
old_add = """        boolean alreadyConnected = connections.stream()
                .anyMatch(conn -> ((conn.from == firstNewNode && conn.to == targetMember)
                        || (conn.from == targetMember && conn.to == firstNewNode)) && customGroupName.equals(conn.groupId));
        if (!alreadyConnected) {
            connections.add(new Connection(firstNewNode, targetMember, connectionSummary, customGroupName));
        }"""
new_add = """        
        String targetGroupSignature = null;
        for (GroupCluster cluster : currentClusters) {
            if (cluster.members.contains(targetMember)) {
                targetGroupSignature = cluster.signature;
                break;
            }
        }
        if (targetGroupSignature == null) {
            targetGroupSignature = "Grupo Legado";
        }
        String finalTargetGroupSignature = targetGroupSignature;
        
        boolean alreadyConnected = connections.stream()
                .anyMatch(conn -> ((conn.from == firstNewNode && conn.to == targetMember)
                        || (conn.from == targetMember && conn.to == firstNewNode)) && finalTargetGroupSignature.equals(conn.groupId));
        if (!alreadyConnected) {
            connections.add(new Connection(firstNewNode, targetMember, connectionSummary, finalTargetGroupSignature));
        }"""
pv_content = pv_content.replace(old_add, new_add)

# 4. List not found in CasesManagementBrownView
cbv_content = cbv_content.replace("List<PlayerView.GroupCluster> clusters =", "java.util.List<PlayerView.GroupCluster> clusters =")

# 5. cluster.metadata -> cluster.meta in CasesManagementBrownView
cbv_content = cbv_content.replace("cluster.metadata.name", "cluster.meta.name")

with open(pv, "w", encoding="utf-8") as f:
    f.write(pv_content)

with open(cbv, "w", encoding="utf-8") as f:
    f.write(cbv_content)

print("Fixed compile issues")
