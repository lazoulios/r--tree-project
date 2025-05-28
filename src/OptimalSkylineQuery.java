import java.util.*;

public class OptimalSkylineQuery {
     public static ArrayList<Record> computeSkyline(){
         ArrayList<Record> skyline = new ArrayList<>();

         long rootBlockID = RStarTree.getRootNodeBlockId();
         Node root = FilesManager.readIndexFileBlock(rootBlockID);

         if (root==null) return skyline;

         PriorityQueue<Entry> queue = new PriorityQueue<>(
                 Comparator.comparingDouble(e -> e.getBoundingBox().minSum())
         );

         queue.addAll(root.getEntries());

         while (!queue.isEmpty()){
             Entry e = queue.poll();

             if (e instanceof LeafEntry){
                 long recordsID = e.getChildNodeBlockId();
                 ArrayList<Record> records = FilesManager.readDataFileBlock(recordsID);
                 for (Record r: records){
                     ArrayList<Double> coords = r.getCoordinates();
                     if (!isDominated(coords, skyline)){
                         skyline.removeIf(s -> dominates(coords, s.getCoordinates()));

                         skyline.add(r);
                     }
                 }
             }
             else {
                 long childBlockID = e.getChildNodeBlockId();
                 Node childNode = FilesManager.readIndexFileBlock(childBlockID);
                 if (childNode==null) continue;
                 for (Entry child_entry: childNode.getEntries()){
                     queue.add(child_entry);
                 }
             }
         }
         return skyline;
     }

     private static boolean isDominated(ArrayList<Double> candidate, ArrayList<Record> skyline){
         for (Record s : skyline) {
             if (dominates(s.getCoordinates(), candidate)) return true;
         }
         return false;
     }

     private static boolean dominates(ArrayList<Double> skylinePoint, ArrayList<Double> candidate){
         boolean strictlyBetterInOne = false;
         for (int i=0; i<skylinePoint.size(); i++){
             if (skylinePoint.get(i)>candidate.get(i)){
                 return false;
             }
             else if (skylinePoint.get(i)<candidate.get(i)){
                 strictlyBetterInOne = true;
             }
         }
         return strictlyBetterInOne;
     }
}
