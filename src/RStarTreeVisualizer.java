import java.io.FileOutputStream;
import java.util.*;

public class RStarTreeVisualizer {

    private static final String OUTPUT_PATH = "rstartree.dot";

    public static void generateGraphviz(Node root) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph RStarTree {\n");
        sb.append("  node [shape=record, fontname=Helvetica];\n");

        Map<Long, Node> visited = new HashMap<>();
        traverseAndBuild(root, sb, visited);

        sb.append("}\n");

        try (FileOutputStream fos = new FileOutputStream(OUTPUT_PATH)) {
            fos.write(sb.toString().getBytes());
            System.out.println("✅ Graphviz DOT file created: " + OUTPUT_PATH);
        } catch (Exception e) {
            System.err.println("❌ Error writing Graphviz file: " + e.getMessage());
        }
    }

    private static void traverseAndBuild(Node node, StringBuilder sb, Map<Long, Node> visited) {
        if (visited.containsKey(node.getNodeBlockId())) return;
        visited.put(node.getNodeBlockId(), node);

        String label = "Node " + node.getNodeBlockId() + "\\nLevel: " + node.getNodeLevelInTree() + "\\nEntries: " + node.getEntries().size();
        sb.append(String.format("  n%d [label=\"%s\"];\n", node.getNodeBlockId(), label));

        if (node.getNodeLevelInTree() > RStarTree.getLeafLevel()) {
            for (Entry entry : node.getEntries()) {
                Long childId = entry.getChildNodeBlockId();
                sb.append(String.format("  n%d -> n%d;\n", node.getNodeBlockId(), childId));
                Node child = FilesManager.readIndexFileBlock(childId);
                if (child != null) {
                    traverseAndBuild(child, sb, visited);
                }
            }
        }
    }

    public static void main(String[] args) {
        RStarTree tree = new RStarTree(false);
        Node root = FilesManager.readIndexFileBlock(RStarTree.getRootNodeBlockId());
        generateGraphviz(root);
    }
}
