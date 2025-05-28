// Entries at the bottom of the tree (leaf - level)
// Holds the record ID as well as the block ID to the corresponding datafile block
public class LeafEntry extends Entry {
    private long datafileBlockId; // points to the corresponding block in the datafile
    public LeafEntry(long datafileBlockId, BoundingBox boundingBox) {
        super(boundingBox);  // sets bounding box BlockId = datafileBlockId;
        this.setChildNodeBlockId(datafileBlockId);
    }

    public long getDataBlockId() {
        return datafileBlockId;
    }
}
