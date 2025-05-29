import  java.util.ArrayList;

public class WorstRangeQuery {

    //Simple helper method that shows if the coords provided exist between the minCoords and maxCoords provided
    private static boolean inRange(ArrayList<Double> coords, double[] minCoords, double[] maxCoords) {
        for (int i = 0; i < coords.size(); i++) {
            if (coords.get(i) < minCoords[i] || coords.get(i) > maxCoords[i]) {
                return false;
            }
        }
        return true;
    }

    //Performs a linear scan of the entire data file
    public static ArrayList<Record> run(BoundingBox queryBB){
        ArrayList<Record> results = new ArrayList<>();
        int totalBlocks = FilesManager.getTotalBlocksInDataFile();
        ArrayList<Bounds> boundsList = queryBB.getBounds();

        int dims = FilesManager.getDataDimensions();
        double[] minCoord = new double[dims];
        double[] maxCoord = new double[dims];

        for (int i = 0; i < dims; i++) {
            Bounds bounds = boundsList.get(i);
            minCoord[i]= bounds.getLower();
            maxCoord[i]= bounds.getUpper();
        }
        for(int blockId=1; blockId<totalBlocks; blockId++){
            ArrayList<Record> records = FilesManager.readDataFileBlock(blockId);
            if(records == null)
                continue;
            for(Record record : records){
                if(inRange(record.getCoordinates(), minCoord, maxCoord)){
                    results.add(record);
                }
            }
        }
        return results;
    }
}
