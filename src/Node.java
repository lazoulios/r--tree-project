import java.io.Serializable;
import java.util.ArrayList;

// Class representing a Node of the RStarTree
class Node implements Serializable {
    private static final int MAX_ENTRIES = 4; // Maximum amount of entries inside a node (M)
    private static final int MIN_ENTRIES = (int)(0.5 * MAX_ENTRIES); // Setting m to 50% of M (therefore 2<=entries<=4)
    private int level; // In which level the node is positioned
    private long blockId; // The unique ID of the file block that this Node points to
    private ArrayList<Entry> entries; // List keeping track if a node's entries

    // Root constructor with its level as a parameter
    Node(int level) {
        this.level = level;
        this.entries = new ArrayList<>();
        this.blockId = RStarTree.getRootNodeBlockId();
    }

    // Node constructor with level and entries parameters
    Node(int level, ArrayList<Entry> entries) {
        this.level = level;
        this.entries = entries;
    }

    static int getMaxEntriesInNode() {
        return MAX_ENTRIES;
    }

    static int getMinEntriesInNode() {return MIN_ENTRIES;}

    void setNodeBlockId(int blockId) {
        this.blockId = blockId;
    }

    void setEntries(ArrayList<Entry> entries) {
        this.entries = entries;
    }

    ArrayList<Entry> getEntries() {
        return entries;
    }

    long getNodeBlockId() {
        return blockId;
    }

    int getNodeLevelInTree() {
        return level;
    }

    public BoundingBox getBoundingBox() {
        if (entries == null || entries.isEmpty()) return null;

        ArrayList<Bounds> combinedBounds = new ArrayList<>();
        int dimensions = FilesManager.getDataDimensions();

        BoundingBox firstBoundingBox = entries.get(0).getBoundingBox();
        for (int d = 0; d < dimensions; d++) {
            double lower = firstBoundingBox.getBounds().get(d).getLower();
            double upper = firstBoundingBox.getBounds().get(d).getUpper();
            combinedBounds.add(new Bounds(lower, upper));
        }

        for (int i = 1; i < entries.size(); i++) {
            BoundingBox current = entries.get(i).getBoundingBox();
            for (int d = 0; d < dimensions; d++) {
                Bounds existing = combinedBounds.get(d);
                double lower = Math.min(existing.getLower(), current.getBounds().get(d).getLower());
                double upper = Math.max(existing.getUpper(), current.getBounds().get(d).getUpper());
                combinedBounds.set(d, new Bounds(lower, upper));
            }
        }

        return new BoundingBox(combinedBounds);
    }

    void insertEntry(Entry entry)
    {
        entries.add(entry);
    }

    ArrayList<Node> splitNode() {
        ArrayList<Distribution> splitAxisDistributions = chooseSplitAxis();
        return chooseSplitIndex(splitAxisDistributions);
    }

    private ArrayList<Distribution> chooseSplitAxis() {

        ArrayList<Distribution> splitAxisDistributions = new ArrayList<>();
        double splitAxisMarginsSum = Double.MAX_VALUE;
        for (int d = 0; d < FilesManager.getDataDimensions(); d++)
        {
            ArrayList<Entry> entriesSortedByUpper = new ArrayList<>();
            ArrayList<Entry> entriesSortedByLower = new ArrayList<>();

            for (Entry entry : entries)
            {
                entriesSortedByLower.add(entry);
                entriesSortedByUpper.add(entry);
            }

            entriesSortedByLower.sort(new EntryComparator.EntryBoundComparator(entriesSortedByLower,d,false));
            entriesSortedByUpper.sort(new EntryComparator.EntryBoundComparator(entriesSortedByUpper,d,true));

            ArrayList<ArrayList<Entry>> sortedEntries = new ArrayList<>();
            sortedEntries.add(entriesSortedByLower);
            sortedEntries.add(entriesSortedByUpper);

            double sumOfMargins = 0;
            ArrayList<Distribution>  distributions = new ArrayList<>();
            for (ArrayList<Entry> sortedEntryList: sortedEntries)
            {
                for (int k = 1; k <= MAX_ENTRIES - 2* MIN_ENTRIES +2; k++)
                {
                    ArrayList<Entry> firstGroup = new ArrayList<>();
                    ArrayList<Entry> secondGroup = new ArrayList<>();
                    // The first group contains the first (m-l)+k entries, the second group contains the remaining entries
                    for (int j = 0; j < (MIN_ENTRIES -1)+k; j++)
                        firstGroup.add(sortedEntryList.get(j));
                    for (int j = (MIN_ENTRIES -1)+k; j < entries.size(); j++)
                        secondGroup.add(sortedEntryList.get(j));

                    BoundingBox bbFirstGroup = new BoundingBox(Bounds.findMinimumBounds(firstGroup));
                    BoundingBox bbSecondGroup = new BoundingBox(Bounds.findMinimumBounds(secondGroup));

                    Distribution distribution = new Distribution(new DistributionGroup(firstGroup,bbFirstGroup), new DistributionGroup(secondGroup,bbSecondGroup));
                    distributions.add(distribution);
                    sumOfMargins += bbFirstGroup.getMargin() + bbSecondGroup.getMargin();
                }
                if (splitAxisMarginsSum > sumOfMargins)
                {
                    splitAxisMarginsSum = sumOfMargins;
                    splitAxisDistributions = distributions;
                }
            }
        }
        return splitAxisDistributions;
    }

    private ArrayList<Node> chooseSplitIndex(ArrayList<Distribution> splitAxisDistributions) {

        if (splitAxisDistributions.size() == 0)
            throw new IllegalArgumentException("Wrong distributions group size. Given 0");

        double minOverlapValue = Double.MAX_VALUE;
        double minAreaValue = Double.MAX_VALUE;
        int bestDistributionIndex = 0;
        for (int i = 0; i < splitAxisDistributions.size(); i++)
        {
            DistributionGroup distributionFirstGroup = splitAxisDistributions.get(i).getFirstGroup();
            DistributionGroup distributionSecondGroup = splitAxisDistributions.get(i).getSecondGroup();

            double overlap = BoundingBox.calculateOverlapValue(distributionFirstGroup.getBoundingBox(), distributionSecondGroup.getBoundingBox());
            if(minOverlapValue > overlap)
            {
                minOverlapValue = overlap;
                minAreaValue = distributionFirstGroup.getBoundingBox().getArea() + distributionSecondGroup.getBoundingBox().getArea();
                bestDistributionIndex = i;
            }

            else if (minOverlapValue == overlap)
            {
                double area = distributionFirstGroup.getBoundingBox().getArea() + distributionSecondGroup.getBoundingBox().getArea() ;
                if(minAreaValue > area)
                {
                    minAreaValue = area;
                    bestDistributionIndex = i;
                }
            }
        }
        ArrayList<Node> splitNodes = new ArrayList<>();
        DistributionGroup firstGroup = splitAxisDistributions.get(bestDistributionIndex).getFirstGroup();
        DistributionGroup secondGroup = splitAxisDistributions.get(bestDistributionIndex).getSecondGroup();
        splitNodes.add(new Node(level,firstGroup.getEntries()));
        splitNodes.add(new Node(level,secondGroup.getEntries()));
        return splitNodes;
    }


}




class Distribution {
    private DistributionGroup firstGroup;
    private DistributionGroup secondGroup;

    Distribution(DistributionGroup firstGroup, DistributionGroup secondGroup) {
        this.firstGroup = firstGroup;
        this.secondGroup = secondGroup;
    }

    DistributionGroup getFirstGroup(){
        return firstGroup;
    }

    DistributionGroup getSecondGroup(){
        return secondGroup;
    }
}

class DistributionGroup {
    private ArrayList<Entry> entries;
    private BoundingBox BoundingBox;

    DistributionGroup(ArrayList<Entry> entries, BoundingBox BoundingBox) {
        this.entries = entries;
        this.BoundingBox = BoundingBox;
    }

    ArrayList<Entry> getEntries() {
        return entries;
    }

    BoundingBox getBoundingBox(){
        return BoundingBox;
    }
}