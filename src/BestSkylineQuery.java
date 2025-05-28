import java.util.*;

public class BestSkylineQuery {
     public static ArrayList<Record> computeSkyline(){
         ArrayList<Record> skylineResult = new ArrayList<>();

         long rootBlockID = RStarTree.getRootNodeBlockId();
         Node root = FilesManager.readIndexFileBlock(rootBlockID);

         if (root==null) return skylineResult;

         PriorityQueue<Entry> entriesQueue = new PriorityQueue<>(
                 Comparator.comparingDouble(e -> e.getBoundingBox().minSum())
         );

         entriesQueue.addAll(root.getEntries());

         while (!entriesQueue.isEmpty()){
             Entry e = entriesQueue.poll();

             if (e instanceof LeafEntry){
                 long recordsID = e.getChildNodeBlockId();
                 ArrayList<Record> recordsList = FilesManager.readDataFileBlock(recordsID);
                 for (Record r: recordsList){
                     ArrayList<Double> coordinates = r.getCoordinates();
                     if (!isDominated(coordinates, skylineResult)){
                         skylineResult.removeIf(s -> dominates(coordinates, s.getCoordinates()));

                         skylineResult.add(r);
                     }
                 }
             }
             else {
                 long childNodeBlockId = e.getChildNodeBlockId();
                 Node childNode = FilesManager.readIndexFileBlock(childNodeBlockId);
                 if (childNode==null) continue;
                 entriesQueue.addAll(childNode.getEntries());
             }
         }
         return skylineResult;
     }

    private static boolean dominates(ArrayList<Double> skylinePoint, ArrayList<Double> candidatePoint){
        boolean betterInOne = false;
        for (int i=0; i<skylinePoint.size(); i++){
            if (skylinePoint.get(i)>candidatePoint.get(i)){
                return false;
            }
            else if (skylinePoint.get(i)<candidatePoint.get(i)){
                betterInOne = true;
            }
        }
        return betterInOne;
    }

     private static boolean isDominated(ArrayList<Double> candidatePoint, ArrayList<Record> skyline){
         for (Record s : skyline) {
             if (dominates(s.getCoordinates(), candidatePoint)) return true;
         }
         return false;
     }
}
