import java.util.ArrayList;

public class RangeQuery {

    /**
     * Executes a range query using the R*-Tree structure recursively.
     * Traverses only branches whose MBRs overlap with the query MBR.
     */
    public static ArrayList<Record> rangeQuery(Node node, MBR queryMBR) {
        ArrayList<Record> results = new ArrayList<>();

        int dimensions = FilesManager.getDataDimensions();
        double[] minCoor = new double[dimensions];
        double[] maxCoor = new double[dimensions];

        // Extract min and max coordinates from query MBR
        ArrayList<Bounds> boundsList = queryMBR.getBounds();
        for (int i = 0; i < dimensions; i++) {
            Bounds b = boundsList.get(i);
            minCoor[i] = b.getLower();
            maxCoor[i] = b.getUpper();
        }

        for (Entry entry : node.getEntries()) {
            MBR entryMBR = entry.getBoundingBox();

            // If the entry MBR overlaps the query MBR
            if (MBR.checkOverlap(entryMBR, queryMBR)) {
                if (node.getNodeLevelInTree() == RStarTree.getLeafLevel()) {
                    // Leaf node → entry points to a data block
                    ArrayList<Record> records = FilesManager.readDataFileBlock(entry.getChildNodeBlockId());
                    if (records != null) {
                        for (Record record : records) {
                            if (isRecordInRange(record, minCoor, maxCoor)) {
                                results.add(record);
                            }
                        }
                    }
                } else {
                    // Internal node → go deeper
                    Node childNode = FilesManager.readIndexFileBlock(entry.getChildNodeBlockId());
                    if (childNode != null) {
                        results.addAll(rangeQuery(childNode, queryMBR));
                    }
                }
            }
        }

        return results;
    }

    /**
     * Checks whether a record lies within the query MBR (using min/max arrays).
     */
    private static boolean isRecordInRange(Record record, double[] minCoor, double[] maxCoor) {
        ArrayList<Double> coords = record.getCoordinates();
        for (int i = 0; i < coords.size(); i++) {
            double val = coords.get(i);
            if (val < minCoor[i] || val > maxCoor[i]) {
                return false;
            }
        }
        return true;
    }
}
