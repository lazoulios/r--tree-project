import  java.util.ArrayList;


public class WorstRangeQuery {

    //Checks if the coordinates are in between the min and max in any dimensions
    private static boolean inRange(ArrayList<Double> coords, double[] minCoor, double[] maxCoor) {
        for (int i = 0; i < coords.size(); i++) {
            if (coords.get(i) < minCoor[i] || coords.get(i) > maxCoor[i]) {
                return false;
            }
        }
        return true;
    }

    //Performs a linear scan of the entire data file
    public static ArrayList<Record> runLinearRangeQuery(MBR queryMBR){
        ArrayList<Record> results = new ArrayList<>();

        int totalBlocks = FilesManager.getTotalBlocksInDataFile();
        ArrayList<Bounds> boundsList = queryMBR.getBounds();

        int dimensions = FilesManager.getDataDimensions();
        double[] minCoor = new double[dimensions];
        double[] maxCoor = new double[dimensions];

        for (int i = 0; i < dimensions; i++) {
            Bounds b = boundsList.get(i);
            minCoor[i]= b.getLower();
            maxCoor[i]= b.getUpper();
        }

        for(int blockId=1; blockId<totalBlocks; blockId++){
            ArrayList<Record> records = FilesManager.readDataFileBlock(blockId);
            if(records == null) continue;

            for(Record rec : records){
                if(inRange(rec.getCoordinates(), minCoor, maxCoor)){
                    results.add(rec);
                }
            }
        }
        return results;
    }
}
