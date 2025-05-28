import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

// Class used for executing a k-nearest neighbours query of a specific search point without any use of an index
// Finds the k closest records of that search point


class WorstNearestNeighboursQuery {
    private ArrayList<Double> searchPoint;
    private int k;
    private PriorityQueue<RecordDistancePair> nearestNeighbours;

    WorstNearestNeighboursQuery(ArrayList<Double> searchPoint, int k) {
        if (k < 0)
            throw new IllegalArgumentException("Parameter 'k' for the nearest neighbours must be a positive integer.");
        this.searchPoint = searchPoint;
        this.k = k;

        // Μετατρέπει το PriorityQueue σε max-heap (με βάση απόσταση)
        this.nearestNeighbours = new PriorityQueue<>(k, new Comparator<RecordDistancePair>() {
            @Override
            public int compare(RecordDistancePair a, RecordDistancePair b) {
                return Double.compare(b.getDistance(), a.getDistance());
            }
        });
    }

    // ✅ Νέα έκδοση που επιστρέφει τα Record αντικείμενα
    ArrayList<Record> getNearestRecords() {
        findNeighbours();
        ArrayList<Record> result = new ArrayList<>();
        while (!nearestNeighbours.isEmpty()) {
            result.add(nearestNeighbours.poll().getRecord());
        }
        Collections.reverse(result); // Να είναι τα πιο κοντινά πρώτα
        return result;
    }

    private void findNeighbours() {
        int totalBlocks = FilesManager.getTotalBlocksInDataFile();
        for (int blockId = 1; blockId < totalBlocks; blockId++) {
            ArrayList<Record> recordsInBlock = FilesManager.readDataFileBlock(blockId);
            if (recordsInBlock == null) continue;

            for (Record record : recordsInBlock) {
                double distance = calculateEuclideanDistance(record.getCoordinates(), searchPoint);

                if (nearestNeighbours.size() < k) {
                    nearestNeighbours.add(new RecordDistancePair(record, distance));
                } else if (distance < nearestNeighbours.peek().getDistance()) {
                    nearestNeighbours.poll();
                    nearestNeighbours.add(new RecordDistancePair(record, distance));
                }
            }
        }
    }

    // Υπολογισμός ευκλείδειας απόστασης
    private double calculateEuclideanDistance(ArrayList<Double> a, ArrayList<Double> b) {
        double sum = 0;
        for (int i = 0; i < a.size(); i++) {
            double diff = a.get(i) - b.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
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
