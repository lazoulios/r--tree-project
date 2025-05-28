import java.io.Serializable;
import java.util.ArrayList;

// Class representing a Node of the RStarTree
class Node implements Serializable {
    private static final int MAX_ENTRIES = 4; // The maximum entries that a Node can fit based on the file parameters
    private static final int MIN_ENTRIES = (int)(0.5 * MAX_ENTRIES); // Setting m to 50%
    private int level; // The level of the tree that this Node is located
    private long blockId; // The unique ID of the file block that this Node refers to
    private ArrayList<Entry> entries; // The ArrayList with the Entries of the Node

    // Root constructor with it's level as a parameter which makes a new empty ArrayList for the Node
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

    void setNodeBlockId(int blockId) {
        this.blockId = blockId;
    }

    void setEntries(ArrayList<Entry> entries) {
        this.entries = entries;
    }

    static int getMaxEntriesInNode() {
        return MAX_ENTRIES;
    }

    static int getMinEntriesInNode() {return MIN_ENTRIES;}

    long getNodeBlockId() {
        return blockId;
    }

    int getNodeLevelInTree() {
        return level;
    }

    ArrayList<Entry> getEntries() {
        return entries;
    }

    public MBR getMBR() {
        if (entries == null || entries.isEmpty()) return null;

        ArrayList<Bounds> combinedBounds = new ArrayList<>();
        int dimensions = FilesManager.getDataDimensions();

        MBR firstMBR = entries.get(0).getBoundingBox();
        for (int d = 0; d < dimensions; d++) {
            double lower = firstMBR.getBounds().get(d).getLower();
            double upper = firstMBR.getBounds().get(d).getUpper();
            combinedBounds.add(new Bounds(lower, upper));
        }

        for (int i = 1; i < entries.size(); i++) {
            MBR current = entries.get(i).getBoundingBox();
            for (int d = 0; d < dimensions; d++) {
                Bounds existing = combinedBounds.get(d);
                double lower = Math.min(existing.getLower(), current.getBounds().get(d).getLower());
                double upper = Math.max(existing.getUpper(), current.getBounds().get(d).getUpper());
                combinedBounds.set(d, new Bounds(lower, upper));
            }
        }

        return new MBR(combinedBounds);
    }

    // Adds the given entry to the entries ArrayList of the Node
    void insertEntry(Entry entry)
    {
        entries.add(entry);
    }

    // Splits the entries of the Node and divides them to two new Nodes
    // Returns an ArrayList which
    ArrayList<Node> splitNode() {
        ArrayList<Distribution> splitAxisDistributions = chooseSplitAxis();
        return chooseSplitIndex(splitAxisDistributions);
    }

    // Returns the distributions of the best Axis
    private ArrayList<Distribution> chooseSplitAxis() {
        // For each axis sort the entries by the lower then by the upper
        // value of their rectangles and determine all distributions as described above Compute S which is the
        // sum of all margin-values of the different distributions

        ArrayList<Distribution> splitAxisDistributions = new ArrayList<>(); // for the different distributions
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
            // Determining distributions
            // Total number of different distributions = M-2*m+2 for each sorted vector
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

                    MBR bbFirstGroup = new MBR(Bounds.findMinimumBounds(firstGroup));
                    MBR bbSecondGroup = new MBR(Bounds.findMinimumBounds(secondGroup));

                    Distribution distribution = new Distribution(new DistributionGroup(firstGroup,bbFirstGroup), new DistributionGroup(secondGroup,bbSecondGroup));
                    distributions.add(distribution);
                    sumOfMargins += bbFirstGroup.getMargin() + bbSecondGroup.getMargin();
                }

                // Choose the axis with the minimum sum as split axis
                if (splitAxisMarginsSum > sumOfMargins)
                {
                    // bestSplitAxis = d;
                    splitAxisMarginsSum = sumOfMargins;
                    splitAxisDistributions = distributions;
                }
            }
        }
        return splitAxisDistributions;
    }

    // Returns a vector of Nodes, containing the two nodes that occurred from the split
    private ArrayList<Node> chooseSplitIndex(ArrayList<Distribution> splitAxisDistributions) {

        if (splitAxisDistributions.size() == 0)
            throw new IllegalArgumentException("Wrong distributions group size. Given 0");

        double minOverlapValue = Double.MAX_VALUE;
        double minAreaValue = Double.MAX_VALUE;
        int bestDistributionIndex = 0;
        // Along the chosen split axis, choose the
        // distribution with the minimum overlap value
        for (int i = 0; i < splitAxisDistributions.size(); i++)
        {
            DistributionGroup distributionFirstGroup = splitAxisDistributions.get(i).getFirstGroup();
            DistributionGroup distributionSecondGroup = splitAxisDistributions.get(i).getSecondGroup();

            double overlap = MBR.calculateOverlapValue(distributionFirstGroup.getMBR(), distributionSecondGroup.getMBR());
            if(minOverlapValue > overlap)
            {
                minOverlapValue = overlap;
                minAreaValue = distributionFirstGroup.getMBR().getArea() + distributionSecondGroup.getMBR().getArea();
                bestDistributionIndex = i;
            }
            // Resolve ties by choosing the distribution with minimum area-value
            else if (minOverlapValue == overlap)
            {
                double area = distributionFirstGroup.getMBR().getArea() + distributionSecondGroup.getMBR().getArea() ;
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
    private MBR MBR;

    DistributionGroup(ArrayList<Entry> entries, MBR MBR) {
        this.entries = entries;
        this.MBR = MBR;
    }

    ArrayList<Entry> getEntries() {
        return entries;
    }

    MBR getMBR(){
        return MBR;
    }
}