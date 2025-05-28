import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

// Class used for comparing Entries based on different criteria
class EntryComparator {
    // Class used to compare entries by their lower or upper bounds
    static class EntryBoundComparator implements Comparator<Entry>
    {
        // Hash-map  used for mapping the comparison value of the Entries during the compare method
        // Key: the given entry and Value: the given entry's bounds
        private HashMap<Entry,Double> entryComparisonMap;

        EntryBoundComparator(List<Entry> entriesToCompare, int dimension, boolean compareByUpper)
        {
            this.entryComparisonMap = new HashMap<>();
            if (compareByUpper)
            {
                for (Entry entry : entriesToCompare)
                    entryComparisonMap.put(entry,entry.getBoundingBox().getBounds().get(dimension).getUpper());
            }
            else
            {
                for (Entry entry : entriesToCompare)
                    entryComparisonMap.put(entry,entry.getBoundingBox().getBounds().get(dimension).getLower());
            }
        }

        public int compare(Entry entryA, Entry entryB)
        {
            return Double.compare(entryComparisonMap.get(entryA),entryComparisonMap.get(entryB));
        }
    }

    // Class used to compare entries by their area enlargement of including a new BoundingBox
    static class EntryAreaEnlargementComparator implements Comparator<Entry>
    {
        // Hash-map used for mapping the comparison value of the Entries during the compare method
        private HashMap<Entry,ArrayList<Double>> entryComparisonMap;

        EntryAreaEnlargementComparator(List<Entry> entriesToCompare, BoundingBox BoundingBoxToAdd)
        {
            // Initialising Hash-map
            this.entryComparisonMap = new HashMap<>();
            for (Entry entry : entriesToCompare)
            {
                BoundingBox entryNewBB = new BoundingBox(Bounds.findMinimumBounds(entry.getBoundingBox(), BoundingBoxToAdd));
                ArrayList<Double> values = new ArrayList<>();
                values.add(entry.getBoundingBox().getArea()); // First value of the ArrayList is the area of the bounding box
                double areaEnlargement = entryNewBB.getArea() - entry.getBoundingBox().getArea();
                if (areaEnlargement < 0)
                    throw new IllegalStateException("The enlargement cannot be a negative number");
                values.add(areaEnlargement); // Second value of the ArrayList is the area enlargement of the specific Entry
                entryComparisonMap.put(entry,values);
            }

        }

        @Override
        public int compare(Entry entryA, Entry entryB) {
            double areaEnlargementA = entryComparisonMap.get(entryA).get(1);
            double areaEnlargementB = entryComparisonMap.get(entryB).get(1);
            // Resolve ties by choosing the entry with the rectangle of smallest area
            if (areaEnlargementA == areaEnlargementB)
                return Double.compare(entryComparisonMap.get(entryA).get(0),entryComparisonMap.get(entryB).get(0));
            else
                return Double.compare(areaEnlargementA,areaEnlargementB);
        }
    }

    // Class used to compare entries by their overlap enlargement of including a new "rectangle" item
    static class EntryOverlapEnlargementComparator implements Comparator<Entry>
    {
        private BoundingBox BoundingBoxToAdd; // The bounding box to add
        private ArrayList<Entry> nodeEntries; // All the entries of the Node

        // Hash-map used for mapping the comparison value of the Entries during the compare method
        // Key: given Entry
        // Value: the given Entry's overlap Enlargement
        private HashMap<Entry,Double> entryComparisonMap;
        EntryOverlapEnlargementComparator(List<Entry> entriesToCompare, BoundingBox BoundingBoxToAdd, ArrayList<Entry> nodeEntries)
        {
            this.BoundingBoxToAdd = BoundingBoxToAdd;
            this.nodeEntries = nodeEntries;

            this.entryComparisonMap = new HashMap<>();
            for (Entry entry : entriesToCompare)
            {
                double overlapEntry = calculateEntryOverlapValue(entry, entry.getBoundingBox());
                Entry newEntry = new Entry(new BoundingBox(Bounds.findMinimumBounds(entry.getBoundingBox(), BoundingBoxToAdd)));
                double overlapNewEntry = calculateEntryOverlapValue(entry, newEntry.getBoundingBox());
                double overlapEnlargementEntry = overlapNewEntry - overlapEntry ;

                if (overlapEnlargementEntry < 0)
                    throw new IllegalStateException("The enlargement cannot be a negative number");

                entryComparisonMap.put(entry,overlapEnlargementEntry);
            }
        }

        @Override
        public int compare(Entry entryA, Entry entryB) {
            double overlapEnlargementEntryA = entryComparisonMap.get(entryA);
            double overlapEnlargementEntryB = entryComparisonMap.get(entryB);
            // Resolve ties by choosing the entry whose rectangle needs the least area enlargement, then
            // the entry with the rectangle of smallest area (which is included in the EntryAreaEnlargementComparator)
            if (overlapEnlargementEntryA == overlapEnlargementEntryB)
            {   ArrayList<Entry> entriesToCompare = new ArrayList<>();
                entriesToCompare.add(entryA);
                entriesToCompare.add(entryB);
                return new EntryAreaEnlargementComparator(entriesToCompare, BoundingBoxToAdd).compare(entryA,entryB);
            }
            else
                return Double.compare(overlapEnlargementEntryA,overlapEnlargementEntryB);
        }

        double calculateEntryOverlapValue(Entry entry, BoundingBox BoundingBox){
            double sum = 0;
            for (Entry nodeEntry : nodeEntries)
            {
                if (nodeEntry != entry)
                    sum += BoundingBox.calculateOverlapValue(BoundingBox,nodeEntry.getBoundingBox());
            }
            return sum;
        }
    }

    // Class used to compare entries by their distance from their overall's bouncing box's center
    static class EntryDistanceFromCenterComparator implements Comparator<Entry>
    {
        // Hash-map  used for mapping the comparison value of the Entries during the compare method
        // Key: the given Entry
        // Value: the given Entry's BoundingBox distance from the given BoundingBox
        private HashMap<Entry,Double> entryComparisonMap;

        EntryDistanceFromCenterComparator(List<Entry>entriesToCompare, ArrayList<Double> point) {
            this.entryComparisonMap = new HashMap<>();

            for (Entry entry : entriesToCompare)
                entryComparisonMap.put(entry,entry.getBoundingBox().findMinDistanceFromPoint(point));
        }

        EntryDistanceFromCenterComparator(List<Entry>entriesToCompare, BoundingBox BoundingBox) {
            this.entryComparisonMap = new HashMap<>();

            for (Entry entry : entriesToCompare)
                entryComparisonMap.put(entry, BoundingBox.findDistanceBetweenBoundingBoxes(entry.getBoundingBox(), BoundingBox));
        }

        public int compare(Entry entryA, Entry entryB)
        {
            return Double.compare(entryComparisonMap.get(entryA),entryComparisonMap.get(entryB));
        }
    }


}