import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

class WorstNearestNeighboursQuery {
    private ArrayList<Double> target;
    private int k;
    private PriorityQueue<RecordDistancePair> nearestNeighbours;

    WorstNearestNeighboursQuery(ArrayList<Double> target, int k) {
        if (k < 0)
            throw new IllegalArgumentException("The number of nearest neighbours must be a positive integer.");
        this.target = target;
        this.k = k;
        this.nearestNeighbours = new PriorityQueue<>(k, new Comparator<RecordDistancePair>() {
            @Override
            public int compare(RecordDistancePair x, RecordDistancePair y) {
                return Double.compare(y.getDistance(), x.getDistance());
            }
        });
    }

    private double findEuclideanDistance(ArrayList<Double> x, ArrayList<Double> y) {
        double sum = 0;
        for (int i = 0; i < x.size(); i++) {
            double diff = x.get(i) - y.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private void searchNeighbours() {
        int totalBlocks = FilesManager.getTotalBlocksInDataFile();
        for (int blockId = 1; blockId < totalBlocks; blockId++) {
            ArrayList<Record> recordsInBlock = FilesManager.readDataFileBlock(blockId);
            if (recordsInBlock == null) continue;

            for (Record record : recordsInBlock) {
                double distance = findEuclideanDistance(record.getCoordinates(), target);

                if (nearestNeighbours.size() < k) {
                    nearestNeighbours.add(new RecordDistancePair(record, distance));
                } else if (distance < nearestNeighbours.peek().getDistance()) {
                    nearestNeighbours.poll();
                    nearestNeighbours.add(new RecordDistancePair(record, distance));
                }
            }
        }
    }

    ArrayList<Record> getNearestRecords() {
        searchNeighbours();
        ArrayList<Record> result = new ArrayList<>();
        while (!nearestNeighbours.isEmpty()) {
            result.add(nearestNeighbours.poll().getRecord());
        }
        Collections.reverse(result);
        return result;
    }
}

class RecordDistancePair {
    private final Record record;
    private final double distance;

    public RecordDistancePair(Record record, double distance) {
        this.record = record;
        this.distance = distance;
    }

    public Record getRecord() {
            return record;
        }

    public double getDistance() {
            return distance;
        }
}
