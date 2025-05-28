import java.io.Serializable;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

// Represents a bounding box in the n-dimensional space
class MBR implements Serializable {
    private ArrayList<Bounds> bounds; // The bounds for each dimension (for each axis)
    private Double area; // The total area that the BoundingBox covers
    private Double margin; // The total margin (perimeter) that the BoundingBox takes
    private ArrayList<Double> center; // Represents te coordinates of the point that represents the center of the bounding box

    public MBR(ArrayList<Bounds> bounds) {
        this.bounds = bounds;
        this.area = calculateArea();
        this.margin = calculateMargin();
        this.center = getCenter();
    }

    public MBR(Record record) {
        ArrayList<Double> coords = record.getCoordinates();
        ArrayList<Bounds> boundsList = new ArrayList<>();
        for (double coord : coords) {
            boundsList.add(new Bounds(coord, coord));
        }
        this.bounds = boundsList;
        this.area = calculateArea();
        this.margin = calculateArea();
        this.center = getCenter();
    }

    ArrayList<Bounds> getBounds() {
        return bounds;
    }

    double getArea() {
        // If area is not yet initialized, find the area
        if (area == null)
            area = calculateArea();

        return area;
    }

    double getMargin() {
        // If margin is not yet initialized, find the margin
        if (margin == null)
            margin = calculateMargin();

        return margin;
    }

    // Returns true if the given's point radius overlaps with the bounding box
    boolean checkOverLapWithPoint(ArrayList<Double> point, double radius){
        // If the minimum distance from the point is less or equal the point's radius then the bounding box is in the range
        return findMinDistanceFromPoint(point) <= radius;
    }

    // Returns the minimum distance between the bounding box and the point
    double findMinDistanceFromPoint(ArrayList<Double> point){
        double minDistance = 0;
        // For every dimension find the minimum distance
        double rd;
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
        {
            if(getBounds().get(d).getLower() > point.get(d))
                rd = getBounds().get(d).getLower();
            else if (getBounds().get(d).getUpper() < point.get(d))
                rd = getBounds().get(d).getUpper();
            else
                rd = point.get(d);

            minDistance += Math.pow(point.get(d) - rd,2);
        }
        return sqrt(minDistance);
    }

    public ArrayList<Double> getCenter() {
        // If center is not yet initialized, find the center and return it
        if (center == null)
        {
            center = new ArrayList<>();

            for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
                center.add((bounds.get(d).getUpper()+bounds.get(d).getLower())/2);
        }
        return center;
    }
    // Calculates and returns the margin (perimeter) of this BoundingBox
    private double calculateMargin() {
        double sum = 0;
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
            sum += abs(bounds.get(d).getUpper() - bounds.get(d).getLower());
        return sum;
    }

    // Calculates and returns the area of this BoundingBox
    private double calculateArea() {
        double productOfEdges = 1;
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
            productOfEdges = productOfEdges * (bounds.get(d).getUpper() - bounds.get(d).getLower());
        return abs(productOfEdges);
    }

    // Returns true if the two bounding boxes overlap
    static boolean checkOverlap(MBR MBRA, MBR MBRB) {
        // For every dimension find the intersection point
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
        {
            double overlapD = Math.min(MBRA.getBounds().get(d).getUpper(), MBRB.getBounds().get(d).getUpper())
                    - Math.max(MBRA.getBounds().get(d).getLower(), MBRB.getBounds().get(d).getLower());

            if (overlapD < 0)
                return false;
        }
        return true;
    }

    // Calculates and returns the overlap value between two bounding boxes
    static double calculateOverlapValue(MBR MBRA, MBR MBRB) {
        double overlapValue = 1;
        // For every dimension find the intersection point
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
        {
            double overlapD = Math.min(MBRA.getBounds().get(d).getUpper(), MBRB.getBounds().get(d).getUpper())
                    - Math.max(MBRA.getBounds().get(d).getLower(), MBRB.getBounds().get(d).getLower());

            if (overlapD <= 0)
                return 0; // No overlap, return 0
            else
                overlapValue = overlapD*overlapValue;
        }
        return overlapValue;
    }

    // Calculates and returns the euclidean distance value between two bounding boxes's centers
    static double findDistanceBetweenBoundingBoxes(MBR MBRA, MBR MBRB) {
        double distance = 0;
        // For every dimension find the intersection point
        for (int d = 0; d < FilesHandler.getDataDimensions(); d++)
        {
            distance += Math.pow(MBRA.getCenter().get(d) - MBRB.getCenter().get(d),2);
        }
        return sqrt(distance);
    }

    // Calculates and returns the sum of the lower bounds in each dimension
    // Priority metric for the optimal skyline query algorithm
    public double minSum(){
        double sum = 0.0;
        for (Bounds b: this.bounds){
            sum += b.getLower();
        }
        return sum;
    }
}