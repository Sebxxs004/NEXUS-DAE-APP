import sys

file_path = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

def do_replace(old_str, new_str, label):
    global content
    if old_str in content:
        content = content.replace(old_str, new_str)
        print(f"Replaced {label}")
    else:
        print(f"FAILED to find {label}")

# 1. Connection
old1 = """    private static final class Connection {

        private final CaseNode from;
        private final CaseNode to;
        private final String reason;
        private final Line line;

        private Connection(CaseNode from, CaseNode to, String reason) {
            this.from = from;
            this.to = to;
            this.reason = reason;
            this.line = new Line();
            this.line.setStroke(Color.web("#67e8f9", 0.78));
            this.line.setStrokeWidth(2.2);
            this.line.setMouseTransparent(true);
        }
    }"""
new1 = """    private static final class Connection {

        private final CaseNode from;
        private final CaseNode to;
        private final String reason;
        private final String groupId;
        private final Line line;

        private Connection(CaseNode from, CaseNode to, String reason, String groupId) {
            this.from = from;
            this.to = to;
            this.reason = reason;
            this.groupId = groupId;
            this.line = new Line();
            
            String colorHex = "#67e8f9"; // default
            if (groupId != null && !groupId.isBlank()) {
                String[] colors = {"#ef4444", "#3b82f6", "#10b981", "#f59e0b", "#8b5cf6", "#ec4899", "#14b8a6", "#f97316"};
                int idx = Math.abs(groupId.hashCode()) % colors.length;
                colorHex = colors[idx];
            }
            this.line.setStroke(Color.web(colorHex, 0.78));
            this.line.setStrokeWidth(2.2);
            this.line.setMouseTransparent(true);
        }
    }"""
do_replace(old1, new1, "Connection")

# 2. buildInvestigationJson
old2 = """            json.append("    {\\n");
            json.append("      \\\"from\\\": \\\"").append(escapeJson(connection.from.getCaso().getNombre())).append("\\\",\\n");
            json.append("      \\\"to\\\": \\\"").append(escapeJson(connection.to.getCaso().getNombre())).append("\\\",\\n");
            json.append("      \\\"detail\\\": \\\"").append(escapeJson(connection.reason)).append("\\\"\\n");
            json.append("    }");"""
new2 = """            json.append("    {\\n");
            json.append("      \\\"from\\\": \\\"").append(escapeJson(connection.from.getCaso().getNombre())).append("\\\",\\n");
            json.append("      \\\"to\\\": \\\"").append(escapeJson(connection.to.getCaso().getNombre())).append("\\\",\\n");
            json.append("      \\\"detail\\\": \\\"").append(escapeJson(connection.reason)).append("\\\",\\n");
            json.append("      \\\"groupId\\\": \\\"").append(escapeJson(connection.groupId != null ? connection.groupId : "Grupo Legado")).append("\\\"\\n");
            json.append("    }");"""
do_replace(old2, new2, "buildInvestigationJson")

# 3. loadSessionSnapshot
old3 = """                        String from = extractJsonValue(obj, "from");
                        String to = extractJsonValue(obj, "to");
                        String detail = extractJsonValue(obj, "detail");
                        if (!from.isEmpty() && !to.isEmpty()) {
                            CaseNode nodeFrom = findNodeByName(from);
                            CaseNode nodeTo = findNodeByName(to);
                            if (nodeFrom != null && nodeTo != null) {
                                boolean alreadyConnected = connections.stream()
                                        .anyMatch(c -> (c.from == nodeFrom && c.to == nodeTo)
                                                || (c.from == nodeTo && c.to == nodeFrom));
                                if (!alreadyConnected) {
                                    connections.add(new Connection(nodeFrom, nodeTo, detail));
                                }
                            }
                        }"""
new3 = """                        String from = extractJsonValue(obj, "from");
                        String to = extractJsonValue(obj, "to");
                        String detail = extractJsonValue(obj, "detail");
                        String rawGroupId = extractJsonValue(obj, "groupId");
                        String finalGroupId = rawGroupId.isEmpty() ? "Grupo Legado" : rawGroupId;
                        if (!from.isEmpty() && !to.isEmpty()) {
                            CaseNode nodeFrom = findNodeByName(from);
                            CaseNode nodeTo = findNodeByName(to);
                            if (nodeFrom != null && nodeTo != null) {
                                boolean alreadyConnected = connections.stream()
                                        .anyMatch(c -> ((c.from == nodeFrom && c.to == nodeTo) || (c.from == nodeTo && c.to == nodeFrom)) && finalGroupId.equals(c.groupId));
                                if (!alreadyConnected) {
                                    connections.add(new Connection(nodeFrom, nodeTo, detail, finalGroupId));
                                }
                            }
                        }"""
do_replace(old3, new3, "loadSessionSnapshot")

# 4. addToGroup logic
old4 = """        boolean alreadyConnected = connections.stream()
                .anyMatch(conn -> (conn.from == firstNewNode && conn.to == targetMember)
                        || (conn.from == targetMember && conn.to == firstNewNode));
        if (!alreadyConnected) {
            connections.add(new Connection(firstNewNode, targetMember, connectionSummary));
        }"""
new4 = """        boolean alreadyConnected = connections.stream()
                .anyMatch(conn -> ((conn.from == firstNewNode && conn.to == targetMember)
                        || (conn.from == targetMember && conn.to == firstNewNode)) && customGroupName.equals(conn.groupId));
        if (!alreadyConnected) {
            connections.add(new Connection(firstNewNode, targetMember, connectionSummary, customGroupName));
        }"""
do_replace(old4, new4, "addToGroup")

# 5. createBatchConnections loop
old5 = """        for (int i = 0; i < targetNodes.size() - 1; i++) {
            CaseNode from = targetNodes.get(i);
            CaseNode to = targetNodes.get(i + 1);

            boolean alreadyConnected = connections.stream()
                    .anyMatch(conn -> (conn.from == from && conn.to == to) || (conn.from == to && conn.to == from));
            if (!alreadyConnected) {
                connections.add(new Connection(from, to, connectionSummary));
            }
        }"""
new5 = """        for (int i = 0; i < targetNodes.size() - 1; i++) {
            CaseNode from = targetNodes.get(i);
            CaseNode to = targetNodes.get(i + 1);

            String actualGroupId = customGroupName != null && !customGroupName.isBlank() ? customGroupName : "Grupo " + (getMaxSequence() + 1);
            boolean alreadyConnected = connections.stream()
                    .anyMatch(conn -> ((conn.from == from && conn.to == to) || (conn.from == to && conn.to == from)) && actualGroupId.equals(conn.groupId));
            if (!alreadyConnected) {
                connections.add(new Connection(from, to, connectionSummary, actualGroupId));
            }
        }"""
do_replace(old5, new5, "createBatchConnections")


# 6. detectClusters and collectGroup
import re
dc_pat = re.compile(r"private List<GroupCluster> detectClusters\(\) \{.*?return clusters;\s*\}", re.DOTALL)
cg_pat = re.compile(r"private void collectGroup\(CaseNode node, Set<CaseNode> group\) \{.*?\}\s*\}", re.DOTALL)

new_dc = """private int getMaxSequence() {
        int maxSequence = 0;
        for (GroupMeta m : metadataBySignature.values()) {
            if (m.name != null && m.name.startsWith("Grupo ")) {
                try {
                    int num = Integer.parseInt(m.name.substring(6).trim());
                    if (num > maxSequence) maxSequence = num;
                } catch (Exception e) {}
            }
        }
        return maxSequence;
    }

    private List<GroupCluster> detectClusters() {
        List<GroupCluster> clusters = new ArrayList<>();
        
        java.util.Map<String, java.util.List<Connection>> connsByGroup = connections.stream()
            .filter(c -> c.groupId != null && !c.groupId.isEmpty())
            .collect(Collectors.groupingBy(c -> c.groupId));

        for (java.util.Map.Entry<String, java.util.List<Connection>> entry : connsByGroup.entrySet()) {
            String groupName = entry.getKey();
            Set<CaseNode> component = new HashSet<>();
            for (Connection c : entry.getValue()) {
                component.add(c.from);
                component.add(c.to);
            }
            
            if (component.size() < 2) continue;
            
            List<CaseNode> members = component.stream()
                    .sorted(Comparator.comparing(caseNode -> caseNode.getCaso().getNombre()))
                    .collect(Collectors.toList());
                    
            String signature = groupName;
            GroupMeta meta = metadataBySignature.get(signature);
            if (meta == null) {
                meta = new GroupMeta(
                        groupName,
                        "#FFFFFF",
                        "Asociado por modalidad",
                        "Sin justificación registrada",
                        "",
                        false);
                metadataBySignature.put(signature, meta);
            }
            clusters.add(new GroupCluster(signature, members, meta));
        }

        Set<String> activeSignatures = clusters.stream()
                .map(c -> c.signature)
                .collect(Collectors.toSet());
        metadataBySignature.keySet().retainAll(activeSignatures);

        return clusters;
    }"""

content = dc_pat.sub(new_dc, content)
content = cg_pat.sub("", content)
print("Replaced detectClusters and collectGroup")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Saved PlayerView.java")
