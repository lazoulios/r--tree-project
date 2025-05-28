import java.io.Serializable;
import java.util.ArrayList;

// An Entry refers to the address of a child node in the RStarTree and to its BoundingBox (it's covering rectangle),
// which covers all the MBRs in the child node's Entries
class Entry implements Serializable {
    private MBR MBR; // The closed bounded intervals describing the extent of the object along each dimension
    private Long childNodeBlockId; // The address (block ID) of a child node in the RStarTree

    // Constructor which takes parameters the child node of the entry
    Entry(Node childNode) {
        this.childNodeBlockId = childNode.getNodeBlockId();
        adjustBBToFitEntries(childNode.getEntries()); // Adjusting the BoundingBox of the Entry to fit the objects of the childNode
    }

    // Constructor which takes parameters the MBR of the node
    Entry(MBR MBR)
    {
        this.MBR = MBR;
    }

    void setChildNodeBlockId(Long childNodeBlockId) {
        this.childNodeBlockId = childNodeBlockId;
    }

    MBR getBoundingBox() {
        return MBR;
    }

    Long getChildNodeBlockId() {
        return childNodeBlockId;
    }

    // Adjusting the MBR of the entry by replacing it with a new bounding box having the new minimum bounds
    // passed by the array list parameter
    void adjustBBToFitEntries(ArrayList<Entry> entries){
        MBR = new MBR(Bounds.findMinimumBounds(entries));
    }

    // Adjusting the Bounding Box of the entry by replacing it with a new bounding having the extended minimum bounds
    // so that they enclose the entryToInclude
    void adjustBBToFitEntry(Entry entryToInclude){
        MBR = new MBR(Bounds.findMinimumBounds(MBR,entryToInclude.getBoundingBox()));
    }
}
