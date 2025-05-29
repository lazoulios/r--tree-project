import java.util.ArrayList;

public class WorstSkylineQuery {
    // Simple helper method to check if one record X dominates a second record Y
    private static boolean dominates(Record x, Record y) {
        boolean flag = false;
        ArrayList<Double> coordsX = x.getCoordinates();
        ArrayList<Double> coordsY = y.getCoordinates();

        for (int i = 0; i < coordsX.size(); i++) {
            if (coordsX.get(i) > coordsY.get(i)) {
                return false;
            } else if (coordsX.get(i) < coordsY.get(i)) {
                flag = true;
            }
        }
        return flag;
    }

    public static ArrayList<Record> run() {
        ArrayList<Record> skyline = new ArrayList<>();

        System.out.println("[🧮] Calculating Linear Skyline...");
        long startTime = System.currentTimeMillis();
        // datafile loading
        ArrayList<Record> allRecords = new ArrayList<>();
        int totalBlocks = FilesManager.getTotalBlocksInDataFile();
        for (int i = 1; i < totalBlocks; i++) {
            ArrayList<Record> blockRecords = FilesManager.readDataFileBlock(i);
            if (blockRecords != null)
                allRecords.addAll(blockRecords);
        }

        int total = allRecords.size();
        System.out.println("[📚] Total records loaded: " + total);
        System.out.println();
        // Skyline calculation
        for (int i = 0; i < total; i++) {
            Record candidate = allRecords.get(i);
            boolean dominated = false;
            for (Record other : allRecords) {
                if (dominates(other, candidate)) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) {
                skyline.add(candidate);
            }
            // Progress Report
            if ((i+1) % 50000 == 0 || i+1 == total) {
                long endTime = System.currentTimeMillis();
                double progress = (100.0 * (i+1)) / total;
                System.out.printf("[🔎] Checked %d/%d records (%.2f%%) - Elapsed: %d ms%n",
                        i+1, total, progress, (endTime - startTime));
            }
        }
        return skyline;
    }
}
