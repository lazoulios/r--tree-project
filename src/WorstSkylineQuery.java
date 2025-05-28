import java.util.ArrayList;

public class WorstSkylineQuery {

    public static ArrayList<Record> computeSkyline() {
        ArrayList<Record> skyline = new ArrayList<>();

        System.out.println("🧮 Calculating Linear Skyline...");
        long startTime = System.currentTimeMillis();

        // Φόρτωσε όλα τα records από το datafile
        ArrayList<Record> allRecords = new ArrayList<>();
        int totalBlocks = FilesManager.getTotalBlocksInDataFile();
        for (int i = 1; i < totalBlocks; i++) { // skip block 0 (metadata)
            ArrayList<Record> blockRecords = FilesManager.readDataFileBlock(i);
            if (blockRecords != null)
                allRecords.addAll(blockRecords);
        }

        int total = allRecords.size();
        System.out.println("🔢 Total records loaded: " + total);
        System.out.println();

        // Υπολόγισε το skyline
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

            // Προβολή προόδου ανά 1000
            if ((i + 1) % 1000 == 0 || i + 1 == total) {
                long now = System.currentTimeMillis();
                long elapsed = now - startTime;
                double progress = (100.0 * (i + 1)) / total;
                System.out.printf("🕒 Checked %d/%d records (%.2f%%) - Elapsed: %d ms%n",
                        i + 1, total, progress, elapsed);
            }
        }

        return skyline;
    }

    // Επιστρέφει true αν το A κυριαρχεί το B
    private static boolean dominates(Record a, Record b) {
        ArrayList<Double> coordsA = a.getCoordinates();
        ArrayList<Double> coordsB = b.getCoordinates();
        boolean strictlyBetterInOne = false;

        for (int i = 0; i < coordsA.size(); i++) {
            if (coordsA.get(i) > coordsB.get(i)) {
                return false;
            } else if (coordsA.get(i) < coordsB.get(i)) {
                strictlyBetterInOne = true;
            }
        }

        return strictlyBetterInOne;
    }
}
