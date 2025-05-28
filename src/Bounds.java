import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

// Represents the bounds of an interval in a single dimension
class Bounds implements Serializable {
    private double lower; // Representing the lower value of the interval
    private double upper; // Representing the upper value of the interval

    // constructor of the class
    // Since we have to do with bounds of an interval the lower Bound cannot be bigger than upper
    Bounds(double lower, double upper) {
        if (lower <= upper)
        {
            this.lower = lower;
            this.upper = upper;
        }
        else
            throw new IllegalArgumentException( "The lower value of the bounds cannot be bigger than the upper");
    }

    double getLower() {
        return lower;
    }

    double getUpper() {
        return upper;
    }

    // Returns an ArrayList with bounds for each dimension, including the the minimum bounds needed to fit the given entries
    static ArrayList<Bounds> findMinimumBounds(ArrayList<Entry> entries) {
        ArrayList<Bounds> minimumBounds = new ArrayList<>();
        // For each dimension finds the minimum interval needed for the entries to fit
        for (int d = 0; d < FilesManager.getDataDimensions(); d++)
        {
            Entry lowerEntry = Collections.min(entries, new EntryComparator.EntryBoundComparator(entries,d,false));
            Entry upperEntry = Collections.max(entries, new EntryComparator.EntryBoundComparator(entries,d,true));
            minimumBounds.add(new Bounds(lowerEntry.getBoundingBox().getBounds().get(d).getLower(),upperEntry.getBoundingBox().getBounds().get(d).getUpper()));
        }
        return minimumBounds;
    }

    // Returns an ArrayList with bounds for each dimension, including the the minimum bounds needed to merge the given bounding boxes
    static ArrayList<Bounds> findMinimumBounds(MBR MBRA, MBR MBRB) {
        ArrayList<Bounds> minimumBounds = new ArrayList<>();
        // For each dimension finds the minimum interval needed for the entries to fit
        for (int d = 0; d < FilesManager.getDataDimensions(); d++)
        {
            double lower = Math.min(MBRA.getBounds().get(d).getLower(), MBRB.getBounds().get(d).getLower());
            double upper = Math.max(MBRA.getBounds().get(d).getUpper(), MBRB.getBounds().get(d).getUpper());
            minimumBounds.add(new Bounds(lower,upper));
        }
        return minimumBounds;
    }

    public static ArrayList<Bounds> findMinimumBoundsFromRecords(ArrayList<Record> records) {
        int dimensions = FilesManager.getDataDimensions();
        double[] min = new double[dimensions];
        double[] max = new double[dimensions];
        Arrays.fill(min, Double.POSITIVE_INFINITY);
        Arrays.fill(max, Double.NEGATIVE_INFINITY);

        for (Record r : records) {
            for (int i = 0; i < dimensions; i++) {
                double val = r.getCoordinateFromDimension(i);
                if (val < min[i]) min[i] = val;
                if (val > max[i]) max[i] = val;
            }
        }

        ArrayList<Bounds> bounds = new ArrayList<>();
        for (int i = 0; i < dimensions; i++) {
            bounds.add(new Bounds(min[i], max[i]));
        }
        return bounds;
    }

    public static ArrayList<Bounds> findMinimumBoundsFromRecord(Record record) {
        ArrayList<Bounds> boundsList = new ArrayList<>();
        for (int i = 0; i < record.getCoordinates().size(); i++) {
            double coord = record.getCoordinateFromDimension(i);
            boundsList.add(new Bounds(coord, coord));
        }
        return boundsList;
    }


}
