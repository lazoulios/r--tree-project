import java.util.*;
import java.util.stream.Collectors;

public class RStarTree {

    private int totalLevels;
    private boolean[] levelsInserted;
    private static final int ROOT_NODE_BLOCK_ID = 1;
    private static final int LEAF_LEVEL = 1;
    private static final int CHOOSE_SUBTREE_LEVEL = 32;
    private static final int REINSERT_TREE_ENTRIES = (int) (0.3 * Node.getMaxEntriesInNode());
    private static final Map<Long, Long> recordToLeafMap = new HashMap<>();

    RStarTree(boolean doBulkLoad) {
        this.totalLevels = FilesManager.getTotalLevelsFile();
        if (doBulkLoad) {
            List<RecordBlockPairID> allRecordsPairs = new ArrayList<>();
            int totalBlocks = FilesManager.getTotalBlocksInDataFile();

            for (int i = 1; i < totalBlocks; i++) {
                ArrayList<Record> blockRecords = FilesManager.readDataFileBlock(i);
                if (blockRecords != null) {
                    for (Record record : blockRecords) {
                        allRecordsPairs.add(new RecordBlockPairID(record, i));
                    }
                } else {
                    throw new IllegalStateException("Error reading records from datafile");
                }
            }

            bulkLoad(allRecordsPairs);
            printTreeStats();
            FilesManager.flushIndexBufferToDisk();
            System.out.println("✅ Total levels after bulk-load: " + totalLevels);
        } else {
            Node root = new Node(1);
            FilesManager.writeNewIndexFileBlock(root);
            for (int i = 1; i < FilesManager.getTotalBlocksInDataFile(); i++) {
                ArrayList<Record> records = FilesManager.readDataFileBlock(i);
                if (records != null) {
                    insertDataBlock(records,i);
                } else {
                    throw new IllegalStateException("Error reading records from datafile");
                }
            }
            printTreeStats();
            FilesManager.flushIndexBufferToDisk();

            System.out.println("✅ Total levels after insertion: " + totalLevels);
        }
    }

    Node getRootNode() {
        return FilesManager.readIndexFileBlock(ROOT_NODE_BLOCK_ID);
    }

    static int getRootNodeBlockId() {
        return ROOT_NODE_BLOCK_ID;
    }

    static int getLeafLevel() {
        return LEAF_LEVEL;
    }

    private void insertDataBlock(ArrayList<Record> records, long datafileBlockId) {
        ArrayList<Bounds> boundsList = Bounds.findMinimumBoundsFromRecords(records);
        BoundingBox blockBoundingBox = new BoundingBox(boundsList);
        LeafEntry entry = new LeafEntry(datafileBlockId, blockBoundingBox);
        this.levelsInserted = new boolean[totalLevels];
        insert(null, null, entry, LEAF_LEVEL);
        for (Record r : records) {
            RStarTree.recordToLeafMap.put(r.getRecordID(), datafileBlockId);
        }
    }


    private Entry insert(Node parentNode, Entry parentEntry, Entry dataEntry, int levelToAdd) {
        long nodeBlockId = (parentEntry == null) ? ROOT_NODE_BLOCK_ID : parentEntry.getChildNodeBlockId();

        if (parentEntry != null) {
            parentEntry.adjustBBToFitEntry(dataEntry);
            FilesManager.updateIndexFileBlock(parentNode, totalLevels);
        }

        Node childNode = FilesManager.readIndexFileBlock(nodeBlockId);
        if (childNode == null) {
            throw new IllegalStateException("Node-block is null");
        }

        if (levelToAdd > totalLevels) {
            totalLevels = levelToAdd;
            boolean[] newLevelsInserted = new boolean[totalLevels];
            if (levelsInserted != null)
                System.arraycopy(levelsInserted, 0, newLevelsInserted, 0, levelsInserted.length);
            levelsInserted = newLevelsInserted;
        }

        if (childNode.getNodeLevelInTree() == levelToAdd) {
            childNode.insertEntry(dataEntry);
            FilesManager.updateIndexFileBlock(childNode, totalLevels);
        } else {
            Entry bestEntry = chooseSubTree(childNode, dataEntry.getBoundingBox(), levelToAdd);
            Entry newEntry = insert(childNode, bestEntry, dataEntry, levelToAdd);

            if (newEntry != null) {
                childNode.insertEntry(newEntry);
            }

            FilesManager.updateIndexFileBlock(childNode, totalLevels);

            if (childNode.getEntries().size() <= Node.getMaxEntriesInNode()) {
                return null;
            }

            return overFlowTreatment(parentNode, parentEntry, childNode);
        }

        if (childNode.getEntries().size() > Node.getMaxEntriesInNode()) {
            return overFlowTreatment(parentNode, parentEntry, childNode);
        }

        return null;
    }

    private Entry chooseSubTree(Node node, BoundingBox BoundingBoxToAdd, int levelToAdd) {
        ArrayList<Entry> entries = node.getEntries();
        if (node.getNodeLevelInTree() == levelToAdd + 1) {
            if (Node.getMaxEntriesInNode() > (CHOOSE_SUBTREE_LEVEL * 2) / 3 && entries.size() > CHOOSE_SUBTREE_LEVEL) {
                ArrayList<Entry> topEntries = getTopAreaEnlargementEntries(entries, BoundingBoxToAdd, CHOOSE_SUBTREE_LEVEL);
                return Collections.min(topEntries, new EntryComparator.EntryOverlapEnlargementComparator(topEntries, BoundingBoxToAdd, entries));
            }
            return Collections.min(entries, new EntryComparator.EntryOverlapEnlargementComparator(entries, BoundingBoxToAdd, entries));
        }
        return getEntryWithMinAreaEnlargement(entries, BoundingBoxToAdd);
    }

    private Entry getEntryWithMinAreaEnlargement(ArrayList<Entry> entries, BoundingBox boundingBox) {
        return Collections.min(
                entries.stream()
                        .map(e -> new EntryAreaEnlargementPair(e, computeAreaEnlargement(e, boundingBox)))
                        .toList(),
                EntryAreaEnlargementPair::compareTo
        ).getEntry();
    }

    private ArrayList<Entry> getTopAreaEnlargementEntries(ArrayList<Entry> entries, BoundingBox BoundingBoxToAdd, int p) {
        return entries.stream()
                .map(e -> new EntryAreaEnlargementPair(e, computeAreaEnlargement(e, BoundingBoxToAdd)))
                .sorted()
                .limit(p)
                .map(EntryAreaEnlargementPair::getEntry)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private double computeAreaEnlargement(Entry entry, BoundingBox toAdd) {
        BoundingBox enlarged = new BoundingBox(Bounds.findMinimumBounds(entry.getBoundingBox(), toAdd));
        return enlarged.getArea() - entry.getBoundingBox().getArea();
    }

    private Entry overFlowTreatment(Node parentNode, Entry parentEntry, Node childNode) {
        int levelIndex = childNode.getNodeLevelInTree() - 1;
        if (levelIndex >= levelsInserted.length) {
            boolean[] newLevelsInserted = new boolean[totalLevels];
            System.arraycopy(levelsInserted, 0, newLevelsInserted, 0, levelsInserted.length);
            levelsInserted = newLevelsInserted;
        }

        if (childNode.getNodeBlockId() != ROOT_NODE_BLOCK_ID && !levelsInserted[levelIndex]) {
            levelsInserted[levelIndex] = true;
            reInsert(parentNode, parentEntry, childNode);
            return null;
        }

        ArrayList<Node> splitNodes = childNode.splitNode();
        if (splitNodes.size() != 2) {
            throw new IllegalStateException("Split must produce exactly two nodes.");
        }

        Node leftNode = splitNodes.get(0);
        Node rightNode = splitNodes.get(1);
        childNode.setEntries(leftNode.getEntries());

        if (childNode.getNodeBlockId() != ROOT_NODE_BLOCK_ID) {
            FilesManager.updateIndexFileBlock(childNode, totalLevels);
            rightNode.setNodeBlockId(FilesManager.getTotalBlocksInIndexFile());
            FilesManager.writeNewIndexFileBlock(rightNode);
            parentEntry.adjustBBToFitEntries(childNode.getEntries());
            FilesManager.updateIndexFileBlock(parentNode, totalLevels);
            return new Entry(rightNode);
        }

        childNode.setNodeBlockId(FilesManager.getTotalBlocksInIndexFile());
        FilesManager.writeNewIndexFileBlock(childNode);

        rightNode.setNodeBlockId(FilesManager.getTotalBlocksInIndexFile());
        FilesManager.writeNewIndexFileBlock(rightNode);

        ArrayList<Entry> newRootEntries = new ArrayList<>();
        newRootEntries.add(new Entry(childNode));
        newRootEntries.add(new Entry(rightNode));

        Node newRoot = new Node(childNode.getNodeLevelInTree()+1, newRootEntries);
        newRoot.setNodeBlockId(ROOT_NODE_BLOCK_ID);
        FilesManager.setLevelsOfTreeIndex(++totalLevels);
        FilesManager.updateIndexFileBlock(newRoot, totalLevels);
        System.out.println("newRootCreated at level: " + totalLevels);

        return null;
    }

    private void reInsert(Node parentNode, Entry parentEntry, Node childNode) {
        int totalEntries = childNode.getEntries().size();
        int expectedEntries = Node.getMaxEntriesInNode() + 1;

        if (totalEntries != expectedEntries) {
            throw new IllegalStateException("Reinsert requires exactly M+1 entries.");
        }

        childNode.getEntries().sort(
                new EntryComparator.EntryDistanceFromCenterComparator(childNode.getEntries(), parentEntry.getBoundingBox())
        );

        int start = totalEntries - REINSERT_TREE_ENTRIES;
        ArrayList<Entry> removedEntries = new ArrayList<>(childNode.getEntries().subList(start, totalEntries));
        childNode.getEntries().subList(start, totalEntries).clear();

        parentEntry.adjustBBToFitEntries(childNode.getEntries());
        FilesManager.updateIndexFileBlock(parentNode, totalLevels);
        FilesManager.updateIndexFileBlock(childNode, totalLevels);

        Queue<Entry> reinsertQueue = new LinkedList<>(removedEntries);
        while (!reinsertQueue.isEmpty()) {
            insert(null, null, reinsertQueue.poll(), childNode.getNodeLevelInTree());
        }
    }

    public void insertSingleRecord(Record record) {
        // Δημιουργούμε προσωρινό block με μόνο αυτή την εγγραφή
        ArrayList<Record> block = new ArrayList<>();
        block.add(record);

        // Γράφουμε νέο block στο τέλος του datafile
        FilesManager.writeDataFileBlock(block);
        long newBlockId = FilesManager.getTotalBlocksInDataFile() - 1;

        // Δημιουργούμε MBR μόνο για το νέο record
        ArrayList<Bounds> boundsList = new ArrayList<>();
        for (int i = 0; i < record.getCoordinates().size(); i++) {
            double value = record.getCoordinateFromDimension(i);
            boundsList.add(new Bounds(value, value));
        }
        BoundingBox boundingBox = new BoundingBox(boundsList);
        LeafEntry entry = new LeafEntry(newBlockId, boundingBox);

        // Εισαγωγή στο R*-Tree στο φύλλο επίπεδο
        insert(null, null, entry, LEAF_LEVEL);

        // Ενημέρωση του map ώστε να μπορεί να διαγραφεί αργότερα
        recordToLeafMap.put(record.getRecordID(), entry.getDataBlockId());

        System.out.println("🟢 Η εγγραφή προστέθηκε στο R*-Tree");
    }




    public void deleteRecord(Record record) {
        // Ανάκτηση του LeafEntry (ή blockID) από το recordToLeafMap
        Long dataBlockId = recordToLeafMap.get(record.getRecordID());
        if (dataBlockId == null) {
            System.out.println("❌ Record not found in index.");
            return;
        }

        // Ανάκτηση του φύλλου που περιέχει το Entry
        Node leafNode = findLeafNodeContainingDataBlock(dataBlockId);
        if (leafNode == null) {
            System.out.println("❌ Leaf node not found.");
            return;
        }

        // Αφαίρεση του LeafEntry από τον κόμβο
        boolean removed = leafNode.getEntries().removeIf(e ->
                e instanceof LeafEntry && ((LeafEntry) e).getDataBlockId() == dataBlockId
        );

        if (removed) {
            System.out.println("✅ LeafEntry removed from index.");
            FilesManager.updateIndexFileBlock(leafNode, FilesManager.getTotalLevelsFile());
        } else {
            System.out.println("⚠️ No matching LeafEntry found in node.");
        }

        // Διαγραφή της εγγραφής από το datafile
        FilesManager.deleteRecordFromDataBlock(record);

        // Optional: Reinsertion of remaining entries from the same block if underflow
        if (leafNode.getEntries().size() < Node.getMinEntriesInNode()) {
            System.out.println("ℹ️ Underflow detected, reinserting remaining entries...");
            ArrayList<Entry> entriesToReinsert = new ArrayList<>(leafNode.getEntries());
            leafNode.getEntries().clear();
            condenseTree(leafNode);
        }

        // Αφαίρεση από τον χάρτη
        recordToLeafMap.remove(record.getRecordID());

        System.out.println("✅ Record deleted successfully.");
    }


    private Node findLeafNodeContainingDataBlock(Long dataBlockId) {
        Node root = getRootNode();
        return searchLeafRecursive(root, dataBlockId);
    }

    private Node searchLeafRecursive(Node node, Long dataBlockId) {
        if (node.getNodeLevelInTree() == getLeafLevel()) {
            for (Entry entry : node.getEntries()) {
                if (entry instanceof LeafEntry && ((LeafEntry) entry).getDataBlockId() == dataBlockId) {
                    return node;
                }
            }
            return null;
        }

        for (Entry entry : node.getEntries()) {
            Node child = FilesManager.readIndexFileBlock(entry.getChildNodeBlockId());
            if (child == null) continue;
            Node result = searchLeafRecursive(child, dataBlockId);
            if (result != null) return result;
        }

        return null;
    }


    public void insertLeafEntry(LeafEntry entry) {
        insert(null, null, entry, RStarTree.getLeafLevel());
    }


    private void condenseTree(Node node) {
        Map<Node, List<Entry>> eliminated = new HashMap<>();

        Node current = node;
        while (current.getNodeBlockId() != ROOT_NODE_BLOCK_ID) {
            Node parent = findParent(current);
            if (parent == null) break;

            Entry parentEntry = findParentEntry(parent, current);
            if (parentEntry == null) break;

            if (current.getEntries().size() < Node.getMinEntriesInNode()) {
                // Αφαίρεση του τρέχοντος κόμβου από τον γονέα
                parent.getEntries().remove(parentEntry);
                eliminated.put(current, new ArrayList<>(current.getEntries()));
            } else {
                // Απλά ενημερώνουμε το MBR του parent
                parentEntry.adjustBBToFitEntries(current.getEntries());
            }

            FilesManager.updateIndexFileBlock(current, totalLevels);
            FilesManager.updateIndexFileBlock(parent, totalLevels);
            current = parent;
        }

        // Αν η ρίζα έχει μόνο ένα entry και δεν είναι φύλλο => συμπίεση ρίζας
        Node root = getRootNode();
        if (root.getEntries().size() == 1 && root.getNodeLevelInTree() > LEAF_LEVEL) {
            Entry onlyEntry = root.getEntries().get(0);
            Node newRoot = FilesManager.readIndexFileBlock(onlyEntry.getChildNodeBlockId());
            newRoot.setNodeBlockId(ROOT_NODE_BLOCK_ID);
            FilesManager.setLevelsOfTreeIndex(--totalLevels);
            FilesManager.updateIndexFileBlock(newRoot, totalLevels);
            System.out.println("🗜️ Συμπίεση ρίζας: Νέο ύψος " + totalLevels);
        }

        // Reinsert τα αποθηκευμένα entries
        for (List<Entry> entryList : eliminated.values()) {
            for (Entry e : entryList) {
                int level;
                if (e instanceof LeafEntry) {
                    level = LEAF_LEVEL;
                } else {
                    Node child = FilesManager.readIndexFileBlock(e.getChildNodeBlockId());
                    if (child == null) {
                        System.out.println("⚠️ Couldn't reinsert entry: child node not found.");
                        continue;
                    }
                    level = child.getNodeLevelInTree();
                }
                insert(null, null, e, level);
            }

        }
    }

    private Node findParent(Node child) {
        Node root = getRootNode();
        return searchParentRecursive(root, child.getNodeBlockId());
    }

    private Node searchParentRecursive(Node current, long childId) {
        if (current.getNodeLevelInTree() == LEAF_LEVEL) return null;

        for (Entry entry : current.getEntries()) {
            if (entry.getChildNodeBlockId() == childId) return current;
            Node next = FilesManager.readIndexFileBlock(entry.getChildNodeBlockId());
            if (next != null) {
                Node result = searchParentRecursive(next, childId);
                if (result != null) return result;
            }
        }
        return null;
    }

    private Entry findParentEntry(Node parent, Node child) {
        for (Entry entry : parent.getEntries()) {
            if (entry.getChildNodeBlockId() == child.getNodeBlockId()) return entry;
        }
        return null;
    }

    public static void printTreeStats() {
        Node root = FilesManager.readIndexFileBlock(RStarTree.getRootNodeBlockId());
        Map<Integer, Integer> levelNodeCounts = new HashMap<>();
        traverseAndCount(root, levelNodeCounts);

        System.out.println("\n📊 R*-Tree Structure:");
        levelNodeCounts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByKey().reversed())
                .forEach(entry -> {
                    int level = entry.getKey();
                    int count = entry.getValue();
                    String label = (level == RStarTree.getLeafLevel()) ? "Leaf" :
                            (level == FilesManager.getTotalLevelsFile()) ? "Root" : "Internal";
                    System.out.printf("Level %d (%s): %d node(s)%n", level, label, count);
                });
    }

    private static void traverseAndCount(Node node, Map<Integer, Integer> levelNodeCounts) {
        int level = node.getNodeLevelInTree();
        levelNodeCounts.put(level, levelNodeCounts.getOrDefault(level, 0) + 1);

        // Αν δεν είναι φύλλο, συνέχισε προς τα κάτω
        if (level > RStarTree.getLeafLevel()) {
            for (Entry entry : node.getEntries()) {
                Node child = FilesManager.readIndexFileBlock(entry.getChildNodeBlockId());
                if (child != null) {
                    traverseAndCount(child, levelNodeCounts);
                }
            }
        }
    }

    private List<Node> recursiveSort(List<RecordBlockPairID> input, int dim, int level) {
        int maxEntries = Node.getMaxEntriesInNode();
        int dims = FilesManager.getDataDimensions();

        if (input.size() <= maxEntries) {
            ArrayList<Entry> entries = new ArrayList<>();
            for (RecordBlockPairID pair : input) {
                BoundingBox boundingBox = new BoundingBox(pair.getRecord());
                entries.add(new Entry(boundingBox));
            }
            Node node = new Node(level, entries);
            FilesManager.writeNewIndexFileBlock(node);
            return List.of(node);
        }

        int sliceSize = (int) Math.ceil(Math.pow(input.size(), 1.0 / dims));
        input.sort(Comparator.comparingDouble(o -> o.getRecord().getCoordinates().get(dim)));
        List<Node> result = new ArrayList<>();
        for (int i = 0; i < input.size(); i += sliceSize) {
            int end = Math.min(i + sliceSize, input.size());
            List<RecordBlockPairID> slice = input.subList(i, end);
            int nextDim = (dim + 1) % dims;
            result.addAll(recursiveSort(slice, nextDim, level));
        }
        return result;
    }

    public void bulkLoad(List<RecordBlockPairID> records) {
        int maxEntries = Node.getMaxEntriesInNode();
        List<Node> leafNodes = recursiveSort(records, 0, 1);

        while (leafNodes.size() > 1) {
            List<RecordBlockPairID> parentLevel = new ArrayList<>();
            for (int i = 0; i < leafNodes.size(); i += maxEntries) {
                int end = Math.min(i + maxEntries, leafNodes.size());
                List<Node> group = leafNodes.subList(i, end);
                ArrayList<Entry> entries = new ArrayList<>();
                for (Node child : group) {
                    Entry entry = new Entry(child.getBoundingBox());
                    entry.setChildNodeBlockId(child.getNodeBlockId());
                    entries.add(entry);
                }
                Node parent = new Node(group.get(0).getNodeLevelInTree() + 1, entries);
                FilesManager.writeNewIndexFileBlock(parent);
                parentLevel.add(new RecordBlockPairID(null, parent.getNodeBlockId()));
            }
            leafNodes = recursiveSort(parentLevel, 0, leafNodes.get(0).getNodeLevelInTree() + 1);
        }

        Node root = leafNodes.get(0);
        this.totalLevels = root.getNodeLevelInTree();
        root.setNodeBlockId(RStarTree.getRootNodeBlockId());
        FilesManager.writeNewIndexFileBlock(root);
    }
}

class EntryAreaEnlargementPair implements Comparable {
    private Entry entry; // The Entry object
    private double areaEnlargement; // It's area enlargement assigned

    EntryAreaEnlargementPair(Entry entry, double areaEnlargement){
        this.entry = entry;
        this.areaEnlargement = areaEnlargement;
    }

    Entry getEntry() {
        return entry;
    }

    private double getAreaEnlargement() {
        return areaEnlargement;
    }

    // Comparing the pairs by area enlargement
    @Override
    public int compareTo(Object obj) {
        EntryAreaEnlargementPair pairB = (EntryAreaEnlargementPair)obj;
        // Resolve ties by choosing the entry with the rectangle of smallest area
        if (this.getAreaEnlargement() == pairB.getAreaEnlargement())
            return Double.compare(this.getEntry().getBoundingBox().getArea(),pairB.getEntry().getBoundingBox().getArea());
        else
            return Double.compare(this.getAreaEnlargement(),pairB.getAreaEnlargement());
    }
}
