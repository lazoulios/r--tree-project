import javax.management.Query;
import java.util.*;


// Class used for executing a k-nearest neighbours query of a specific search point with the use of the RStarTree
// Finds the k closest records of that search point
class NearestNeighboursQuery extends Query {
    private ArrayList<Double> searchPoint; // The coordinates of point used for radius queries
    private double searchPointRadius; // The reference radius that is used as a bound
    private int k; // The number of nearest neighbours to be found
    private PriorityQueue<RecordDistancePair> nearestNeighbours; // Using a max heap for the nearest neighbours

    NearestNeighboursQuery(ArrayList<Double> searchPoint, int k) {
        if (k < 0)
            throw new IllegalArgumentException("Parameter 'k' for the nearest neighbours must be a positive integer.");
        this.searchPoint = searchPoint;
        this.k = k;
        this.searchPointRadius = Double.MAX_VALUE;
        this.nearestNeighbours = new PriorityQueue<>(k, (recordDistancePairA, recordDistancePairB) -> {
            return Double.compare(recordDistancePairB.getDistance(), recordDistancePairA.getDistance()); // In order to make a MAX heap
        });
    }

    // Returns the ids of the query's records
    ArrayList<Record> getQueryRecord(Node node) {
        ArrayList<Record> qualifyingRecord = new ArrayList<>();
        findNeighbours(node);
        while (nearestNeighbours.size() != 0)
        {
            RecordDistancePair recordDistancePair = nearestNeighbours.poll();
            qualifyingRecord.add(recordDistancePair.getRecord());
        }
        Collections.reverse(qualifyingRecord); // In order to return closest neighbours first instead of farthest
        return qualifyingRecord;
    }

    static ArrayList<Record> getNearestNeighbours(ArrayList<Double> searchPoint, int k){
        NearestNeighboursQuery query = new NearestNeighboursQuery(searchPoint,k);
        return query.getQueryRecord(FilesManager.readIndexFileBlock(RStarTree.getRootNodeBlockId()));
    }


    // Finds the nearest neighbours by using a branch and bound algorithm
    // with the help of the RStarTree
    private void findNeighbours(Node node) {
        PriorityQueue<NodeEntryPair> queue = new PriorityQueue<>(
                Comparator.comparingDouble(p -> p.entry.getBoundingBox().findMinDistanceFromPoint(searchPoint))
        );

        for (Entry e : node.getEntries()) {
            queue.add(new NodeEntryPair(node, e));
        }

        while (!queue.isEmpty()) {
            NodeEntryPair pair = queue.poll();
            Entry entry = pair.entry;

            double minDistance = entry.getBoundingBox().findMinDistanceFromPoint(searchPoint);

            if (nearestNeighbours.size() == k && minDistance >= searchPointRadius) continue;


            Node childNode = FilesManager.readIndexFileBlock(entry.getChildNodeBlockId());
            if (childNode == null) continue;

            if (childNode.getNodeLevelInTree() == RStarTree.getLeafLevel()){
                ArrayList<Record> records = FilesManager.readDataFileBlock(entry.getChildNodeBlockId());
                if (records != null){
                    for (Record record : records) {
                        double distance = calculateEuclideanDistance(record.getCoordinates(), searchPoint);
                        if (nearestNeighbours.size() < k){
                            nearestNeighbours.add(new RecordDistancePair(record, distance));
                        } else if (distance < nearestNeighbours.peek().getDistance()){
                            nearestNeighbours.poll();
                            nearestNeighbours.add(new RecordDistancePair(record, distance));
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
                /*if (nearestNeighbours.size() >= k)
                    nearestNeighbours.poll();
                double minDistance = leafEntry.getBoundingBox().findMinDistanceFromPoint(searchPoint);
                nearestNeighbours.add(new RecordDistancePair(FilesManager.readDataFileBlock(leafEntry.getDataBlockId()), minDistance));
                searchPointRadius = nearestNeighbours.peek().getDistance();
                i++;*/
            }

    private static class NodeEntryPair {
        Node node;
        Entry entry;
        NodeEntryPair(Node node, Entry entry) {
            this.node = node;
            this.entry = entry;
        }
    }

    //}
        private double calculateEuclideanDistance(ArrayList<Double> a, ArrayList<Double> b) {
            double sum = 0;
            for (int i = 0; i < a.size(); i++) {
                double diff = a.get(i) - b.get(i);
                sum += diff * diff;
            }
            return Math.sqrt(sum);
        }
    }



