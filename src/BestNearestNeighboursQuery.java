import javax.management.Query;
import java.util.*;


class BestNearestNeighboursQuery extends Query {
    private ArrayList<Double> searchPoint; // List of coordinates for the query search point
    private double searchPointRadius; // Reference radius representing a bound
    private int k; // The number of nearest neighbours requested
    private PriorityQueue<RecordDistancePair> nearestNeighbours; // Priority queue tracking the nearest neighbors

    BestNearestNeighboursQuery(ArrayList<Double> searchPoint, int k) {
        if (k < 0)
            throw new IllegalArgumentException("k must be an integer greater than zero");
        this.searchPoint = searchPoint;
        this.k = k;
        this.searchPointRadius = Double.MAX_VALUE;
        this.nearestNeighbours = new PriorityQueue<>(k, (recordDistancePairA, recordDistancePairB) -> Double.compare(recordDistancePairB.getDistance(), recordDistancePairA.getDistance()));
    }

    ArrayList<Record> getQueryRecord(Node node) {
        ArrayList<Record> qualifyingRecord = new ArrayList<>();
        findNeighbours(node);
        while (nearestNeighbours.size() != 0)
        {
            RecordDistancePair recordDistancePair = nearestNeighbours.poll();
            qualifyingRecord.add(recordDistancePair.getRecord());
        }
        Collections.reverse(qualifyingRecord);
        return qualifyingRecord;
    }

    static ArrayList<Record> getNearestNeighbours(ArrayList<Double> searchPoint, int k){
        BestNearestNeighboursQuery nn_query = new BestNearestNeighboursQuery(searchPoint,k);
        return nn_query.getQueryRecord(FilesManager.readIndexFileBlock(RStarTree.getRootNodeBlockId()));
    }

    
    private void findNeighbours(Node node) {
        PriorityQueue<NodeEntryPair> queue = new PriorityQueue<>(
                Comparator.comparingDouble(p -> p.entry.getBoundingBox().findMinDistanceFromPoint(searchPoint))
        );

        for (Entry e : node.getEntries()) {
            queue.add(new NodeEntryPair(node, e));
        }

        while (!queue.isEmpty()) {
            NodeEntryPair entryPair = queue.poll();
            Entry entry = entryPair.entry;

            double mindist = entry.getBoundingBox().findMinDistanceFromPoint(searchPoint);

            if (nearestNeighbours.size() == k && mindist >= searchPointRadius) continue;


            Node childNode = FilesManager.readIndexFileBlock(entry.getChildNodeBlockId());
            if (childNode == null) continue;

            if (childNode.getNodeLevelInTree() == RStarTree.getLeafLevel()){
                ArrayList<Record> records = FilesManager.readDataFileBlock(entry.getChildNodeBlockId());
                if (records != null){
                    for (Record record : records) {
                        double dist = calculateEuclideanDistance(record.getCoordinates(), searchPoint);
                        if (nearestNeighbours.size() < k){
                            nearestNeighbours.add(new RecordDistancePair(record, dist));
                        } else if (dist < nearestNeighbours.peek().getDistance()){
                            nearestNeighbours.poll();
                            nearestNeighbours.add(new RecordDistancePair(record, dist));
                            searchPointRadius = nearestNeighbours.peek().getDistance();
                        }

                        if (nearestNeighbours.size() == k) {
                            searchPointRadius = nearestNeighbours.peek().getDistance();
                        }
                    }
                }
            } else {
                for(Entry childEntry : childNode.getEntries()){
                    queue.add(new NodeEntryPair(childNode, childEntry));
                }
            }
        }
    }

    private double calculateEuclideanDistance(ArrayList<Double> p, ArrayList<Double> q) {
        double sum = 0;
        for (int i = 0; i < p.size(); i++) {
            double diff = p.get(i) - q.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private static class NodeEntryPair {
        Node node;
        Entry entry;
        NodeEntryPair(Node node, Entry entry) {
            this.node = node;
            this.entry = entry;
        }
    }
    }



