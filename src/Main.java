import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        boolean filesExist = Files.exists(Paths.get(FilesManager.PATH_TO_DATAFILE));
        boolean resetFiles = false;

        Scanner scanner = new Scanner(System.in);

        if (filesExist) {
            System.out.println("Data-file and index-file already exist");
            System.out.print("Do you want to make new data and index files using the " + FilesManager.getPathToCsv() +  " file? (y/n): ");
            String answer;
            while (true)
            {
                answer = scanner.nextLine().trim().toLowerCase();
                System.out.println();
                if (answer.equals("y")) {
                    resetFiles = true;
                    break;
                } else if (answer.equals("n")) {
                    break;
                } else {
                    System.out.println("Please answer with y/n: ");
                }
            }
        }

        boolean insertRecordsFromDataFile = false;
        int dataDimensions = 0;

        if(!filesExist || resetFiles) {
            insertRecordsFromDataFile = true;
            System.out.print("Give the dimensions of the spacial data (dimensions need to be the same as the data saved in " + FilesManager.getPathToCsv() + "): ");
            dataDimensions = scanner.nextInt();
            scanner.nextLine();
            System.out.println();
        }

        FilesManager.initializeDataFile(dataDimensions, resetFiles);
        FilesManager.initializeIndexFile(dataDimensions, resetFiles);

        double duration_in_ms;
        long innitStartTime;
        long innitEndTime;

        if(insertRecordsFromDataFile) {
            System.out.println("Do you want to bulk load the R*Tree? (y/n): ");
            String answer;
            boolean doBulkLoad;
            while (true)
            {
                answer = scanner.nextLine().trim().toLowerCase();
                System.out.println();
                if (answer.equals("y")) {
                    doBulkLoad = true;
                    break;
                } else if (answer.equals("n")) {
                    doBulkLoad = false;
                    break;
                } else {
                    System.out.println("Please answer with y/n: ");
                }
            }
            System.out.println("Building R*Tree index from datafile...");
            System.out.println();
            innitStartTime = System.nanoTime();
            new RStarTree(doBulkLoad);
            innitEndTime = System.nanoTime();
            duration_in_ms = (innitEndTime - innitStartTime);
            System.out.println();
            System.out.println("R*Tree index built in " +duration_in_ms / 1000000.0 + "ms");
        }
        ArrayList<Integer> dataMetaData = FilesManager.getDataMetaData();
        ArrayList<Integer> indexMetaData = FilesManager.getIndexMetaData();

        System.out.println("Datafile Metadata: [Dimensions: " + dataMetaData.getFirst() +
                ", Block Size: " + dataMetaData.get(1) +
                ", Total Blocks in File: " + dataMetaData.get(2)+"]");
        System.out.println("Index Metadata: [Dimensions: " + indexMetaData.getFirst() +
                ", Block Size: " + indexMetaData.get(1) +
                ", Total Blocks in File: " + indexMetaData.get(2)+
                ", Total Levels in Tree: " + indexMetaData.get(3)+"]");
        System.out.println();

        String selection;
        do {
            System.out.println("Please select a query you would like to execute: \n" +
                    "0) Exit application \n" +
                    "1) Linear Range Query \n" +
                    "2) Range Query using R* Tree index (WIP)\n" +
                    "3) Linear K-Nearest Neighbors Query\n" +
                    "4) K-Nearest Neighbors Query using R* Tree Index\n"+
                    "5) Linear Skyline Query\n" +
                    "6) Skyline Query using R* Tree Index");
            selection = scanner.nextLine().trim();
            System.out.println();

            // Initializing variables for queries
            ArrayList<Record> queryResults;
            ArrayList<Bounds> boundsList;
            int dims;
            BoundingBox queryBoundingBox;

            switch (selection) {

                //      EXIT
                case "0":
                    System.out.println("Exiting application");
                    break;

                //      LINEAR RANGE QUERY
                case "1":
                    System.out.println("Linear Range Query Selected");

                    //Runs for more than 2 dimensions if needed
                    boundsList = new ArrayList<>();
                    dims = FilesManager.getDataDimensions();

                    System.out.println("Give Lower and Upper bounds for the Query MBR for each dimension: ");
                    for (int i = 0; i < dims; i++) {
                        while (true) {
                            System.out.print("Give bounds for dimension " + (i + 1) + " (lower Bound First): ");
                            double lower = scanner.nextDouble();
                            double upper = scanner.nextDouble();

                            if (lower <= upper) {
                                boundsList.add(new Bounds(lower, upper));
                                break;
                            } else {
                                System.out.println("Lower bound must be less than or equal to upper bound. Try again.");
                            }
                        }
                    }

                    queryBoundingBox = new BoundingBox(boundsList);
                    innitStartTime = System.nanoTime();
                    queryResults = WorstRangeQuery.run(queryBoundingBox);
                    innitEndTime = System.nanoTime();
                    duration_in_ms = (innitEndTime - innitStartTime) / 1000000.0;

                    System.out.println("Results:");

                    for (Record record : queryResults) {
                        System.out.println(record.toString());
                    }
                    System.out.println("Linear Range query completed in " + duration_in_ms + " milliseconds");
                    System.out.println("Total points found in range: " + queryResults.size());

                    System.out.println();


                    break;

                //      RANGE QUERY
                case "2":
                    System.out.println("Range Query using R* Tree index Selected(WIP)");
                    boundsList = new ArrayList<>();
                    dims = FilesManager.getDataDimensions();


                    System.out.println("Give Lower and Upper bounds for the Query MBR for each dimension: ");
                    for (int i = 0; i < dims; i++) {
                        while (true) {
                            System.out.print("Give bounds for dimension " + (i + 1) + " (lower Bound First): ");
                            double lower = scanner.nextDouble();
                            double upper = scanner.nextDouble();

                            if (lower <= upper) {
                                boundsList.add(new Bounds(lower, upper));
                                break;
                            } else {
                                System.out.println("Lower bound must be less than or equal to upper bound. Try again.");
                            }
                        }
                    }

                    queryBoundingBox = new BoundingBox(boundsList);
                    innitStartTime = System.nanoTime();
                    queryResults = BestRangeQuery.bestRangeQuery(FilesManager.readIndexFileBlock(RStarTree.getRootNodeBlockId()), queryBoundingBox);
                    innitEndTime = System.nanoTime();
                    duration_in_ms = (innitEndTime - innitStartTime) / 1000000.0;


                    System.out.println("Results:");
                    for (Record record : queryResults) {
                        System.out.println(record.toString());
                    }
                    System.out.println("Range Query completed in " + duration_in_ms + " milliseconds");
                    System.out.println("Total points found in range: " + queryResults.size());
                    System.out.println();


                    break;

                //      LINEAR KNN QUERY
                case "3":
                    System.out.println("Linear K-Nearest Neighbors Query Selected");

                    System.out.print("Enter value for K: ");
                    int k = scanner.nextInt();
                    scanner.nextLine(); // <-- Απαραίτητο!

                    int dimensions = FilesManager.getDataDimensions();
                    ArrayList<Double> queryPoint = new ArrayList<>();
                    System.out.println("Enter coordinates of the query point (you have " + dimensions + " dimensions):");
                    for (int i = 0; i < dimensions; i++) {
                        System.out.print("Dimension " + (i + 1) + ": ");
                        double val = scanner.nextDouble();
                        scanner.nextLine(); // <-- Απαραίτητο σε κάθε αριθμό!
                        queryPoint.add(val);
                    }

                    // Run k-NN query
                    innitStartTime = System.nanoTime();
                    WorstNearestNeighboursQuery query = new WorstNearestNeighboursQuery(queryPoint, k);
                    queryResults = query.getNearestRecords();
                    innitEndTime = System.nanoTime();

                    double duration = (innitEndTime - innitStartTime) / 1_000_000.0;
                    System.out.println("Results:");

                    for (Record record : queryResults) {
                        System.out.println(record.toString());
                    }
                    System.out.println("Linear K-Nearest Neighbors query completed in " + duration + " milliseconds");
                    System.out.println("Total points found in range: " + queryResults.size());

                    System.out.println();   // Καθαρό newline
                    break;


                //      KNN QUERY
                case "4":
                    System.out.println("K-Nearest Neighbors Query using R* Tree Index Selected(WIP)");
                    System.out.println("🔎 Executing Nearest Neighbours Query...");
                    System.out.print("Enter value for K: ");
                    int k2 = scanner.nextInt();
                    scanner.nextLine(); // <-- Απαραίτητο!
                    int dimensions2 = FilesManager.getDataDimensions();
                    ArrayList<Double> queryPoint2 = new ArrayList<>();
                    System.out.println("Enter coordinates of the query point (you have " + dimensions2 + " dimensions):");
                    for (int i = 0; i < dimensions2; i++) {
                        System.out.print("Dimension " + (i + 1) + ": ");
                        double val = scanner.nextDouble();
                        scanner.nextLine(); // <-- Απαραίτητο σε κάθε αριθμό!
                        queryPoint2.add(val);
                    }
                    // Run k-NN query
                    innitStartTime = System.nanoTime();
                    queryResults = BestNearestNeighboursQuery.getNearestNeighbours(queryPoint2, k2);
                    innitEndTime = System.nanoTime();

                    double duration2 = (innitEndTime - innitStartTime) / 1_000_000.0;
                    System.out.println("Results:");

                    for (Record record : queryResults) {
                        System.out.println(record.toString());
                    }
                    ;
                    System.out.println("K-Nearest Neighbors query completed in " + duration2 + " milliseconds");
                    System.out.println("Total points found in range: " + queryResults.size());

                    System.out.println();   // Καθαρό newline


                    break;

                //      LINEAR SKYLINE QUERY
                case "5":
                    System.out.println("Linear Skyline Query Selected");

                    innitStartTime = System.nanoTime();
                    queryResults = WorstSkylineQuery.run();
                    innitEndTime = System.nanoTime();

                    duration_in_ms = (innitEndTime - innitStartTime) / 1_000_000.0;

                    System.out.println("Records in skyline:");

                    for (Record r : queryResults) {
                        System.out.println(r.toString());
                    }
                    System.out.println("Linear Skyline Query completed in " + duration_in_ms + " ms");
                    System.out.println("Total skyline points: " + queryResults.size());
                    System.out.println();

                    break;

                //      SKYLINE QUERY
                case "6":

                    System.out.println("Skyline Query using R* Tree Index (Optimal) Selected");

                    innitStartTime = System.nanoTime();

                    ArrayList<Record> skylineResults = BestSkylineQuery.computeSkyline();

                     innitEndTime = System.nanoTime();
                    double durationInMs = (innitEndTime - innitStartTime) / 1_000_000.0;

                    System.out.println("Skyline Records:");

                    for (Record r : skylineResults) {
                        System.out.println(r);
                    }
                    System.out.println("Skyline Query completed in " + durationInMs + " ms");
                    System.out.println("Total skyline points: " + skylineResults.size());

                    System.out.println();
                    break;



                //      OTHER VALUES
                default:
                    System.out.println("Please select a valid query.");

            }

        } while (!selection.equals("0"));

    }
}