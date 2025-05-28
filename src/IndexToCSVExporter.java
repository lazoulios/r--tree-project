import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

public class IndexToCSVExporter {

    public static void exportMBRsToCSV(String outputFilePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            writer.println("nodeBlockId,isLeaf,dataBlockId,boundingBox");

            Node root = FilesManager.readIndexFileBlock(RStarTree.getRootNodeBlockId());
            traverseAndExport(root, writer);
            System.out.println("✅ Export complete: " + outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void traverseAndExport(Node node, PrintWriter writer) {
        if (node == null) return;

        boolean isLeaf = node.getNodeLevelInTree() == RStarTree.getLeafLevel();

        for (Entry entry : node.getEntries()) {
            StringBuilder line = new StringBuilder();
            line.append(node.getNodeBlockId()).append(","); // node id
            line.append(isLeaf).append(",");

            if (entry instanceof LeafEntry leafEntry) {
                line.append(leafEntry.getDataBlockId()); // datafile block id
            } else {
                line.append(""); // empty for non-leaf
            }
            line.append(",");

            BoundingBox boundingBox = entry.getBoundingBox();
            List<Bounds> bounds = boundingBox.getBounds();
            for (int i = 0; i < bounds.size(); i++) {
                Bounds b = bounds.get(i);
                line.append(b.getLower()).append("-").append(b.getUpper());
                if (i < bounds.size() - 1) {
                    line.append(";");
                }
            }
            writer.println(line);
            writer.flush();

            // recursive traversal
            if (!isLeaf && entry.getChildNodeBlockId() != -1) {
                Node child = FilesManager.readIndexFileBlock(entry.getChildNodeBlockId());
                traverseAndExport(child, writer);
            }
        }
    }

    public static void main(String[] args) {
        exportMBRsToCSV("index_mbrs.csv");
    }
}
