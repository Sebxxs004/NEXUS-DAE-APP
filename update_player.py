import sys
import re

file_path = r"c:\Users\JAAL\Documents\NEXUS-DAE-APP\src\main\java\com\prisma\views\PlayerView.java"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update buildInvestigationJson
old_json = """            json.append("    {\\n");
            json.append("      \\\"from\\\": \\\"").append(escapeJson(connection.from.getCaso().getNombre())).append("\\\",\\n");
            json.append("      \\\"to\\\": \\\"").append(escapeJson(connection.to.getCaso().getNombre())).append("\\\",\\n");
            json.append("      \\\"detail\\\": \\\"").append(escapeJson(connection.reason)).append("\\\"\\n");
            json.append("    }");"""
new_json = """            json.append("    {\\n");
            json.append("      \\\"from\\\": \\\"").append(escapeJson(connection.from.getCaso().getNombre())).append("\\\",\\n");
            json.append("      \\\"to\\\": \\\"").append(escapeJson(connection.to.getCaso().getNombre())).append("\\\",\\n");
            json.append("      \\\"detail\\\": \\\"").append(escapeJson(connection.reason)).append("\\\",\\n");
            json.append("      \\\"groupId\\\": \\\"").append(escapeJson(connection.groupId)).append("\\\"\\n");
            json.append("    }");"""
if old_json in content:
    content = content.replace(old_json, new_json)
else:
    print("Could not find old_json")

# 2. Update loadSessionSnapshot
old_restore = """                        String from = extractJsonValue(obj, "from");
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
new_restore = """                        String from = extractJsonValue(obj, "from");
                        String to = extractJsonValue(obj, "to");
                        String detail = extractJsonValue(obj, "detail");
                        String rawGroupId = extractJsonValue(obj, "groupId");
                        String finalGroupId = rawGroupId.isEmpty() ? "Grupo Legado" : rawGroupId;
                        
                        if (!from.isEmpty() && !to.isEmpty()) {
                            CaseNode nodeFrom = findNodeByName(from);
                            CaseNode nodeTo = findNodeByName(to);
                            if (nodeFrom != null && nodeTo != null) {
                                boolean alreadyConnected = connections.stream()
                                        .anyMatch(c -> (c.from == nodeFrom && c.to == nodeTo && c.groupId.equals(finalGroupId))
                                                || (c.from == nodeTo && c.to == nodeFrom && c.groupId.equals(finalGroupId)));
                                if (!alreadyConnected) {
                                    connections.add(new Connection(nodeFrom, nodeTo, detail, finalGroupId));
                                }
                            }
                        }"""
if old_restore in content:
    content = content.replace(old_restore, new_restore)
else:
    print("Could not find old_restore")

# 3. Update createBatchConnections (first occurrence in addToGroup)
old_add1 = """        if (!alreadyConnected) {
            connections.add(new Connection(firstNewNode, targetMember, connectionSummary));
        }"""
new_add1 = """        if (!alreadyConnected) {
            connections.add(new Connection(firstNewNode, targetMember, connectionSummary, customGroupName));
        }"""
if old_add1 in content:
    content = content.replace(old_add1, new_add1)
else:
    print("Could not find old_add1")

# 4. Update createBatchConnections (main loop)
old_add2 = """                if (!alreadyConnected) {
                    connections.add(new Connection(n1, n2, connectionSummary));
                }"""
new_add2 = """                if (!alreadyConnected) {
                    String actualGroupId = customGroupName != null && !customGroupName.isBlank() ? customGroupName : "Grupo " + (getMaxSequence() + 1);
                    connections.add(new Connection(n1, n2, connectionSummary, actualGroupId));
                }"""
if old_add2 in content:
    content = content.replace(old_add2, new_add2)
else:
    print("Could not find old_add2")

# 5. Helper method getMaxSequence
if "private int getMaxSequence()" not in content:
    max_seq_code = """
    private int getMaxSequence() {
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
"""
    content = content.replace("private List<GroupCluster> detectClusters() {", max_seq_code + "\n    private List<GroupCluster> detectClusters() {")


# 6. Update detectClusters and remove collectGroup
detect_clusters_pattern = re.compile(r"private List<GroupCluster> detectClusters\(\) \{.*?return clusters;\s*\}", re.DOTALL)
collect_group_pattern = re.compile(r"private void collectGroup\(CaseNode node, Set<CaseNode> group\) \{.*?\}\s*\}", re.DOTALL)

new_detect_clusters = """private List<GroupCluster> detectClusters() {
        List<GroupCluster> clusters = new ArrayList<>();
        
        // Group all connections by groupId
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
                        "#FFFFFF", // Color handled in connection, unused here
                        "Asociado por modalidad",
                        "Sin justificación registrada",
                        "",
                        false);
                metadataBySignature.put(signature, meta);
            }
            clusters.add(new GroupCluster(signature, members, meta));
        }

        // Cleanup stale signatures that no longer match any cluster
        Set<String> activeSignatures = clusters.stream()
                .map(c -> c.signature)
                .collect(Collectors.toSet());
        metadataBySignature.keySet().retainAll(activeSignatures);

        return clusters;
    }"""

content = detect_clusters_pattern.sub(new_detect_clusters, content)
content = collect_group_pattern.sub("", content)


with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Updated PlayerView.java successfully.")
