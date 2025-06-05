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
            System.out.print("Enter the spatial data dimensions (they have to be the same as the ones in the data saved at " + FilesManager.getPathToCsv() + "): ");
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
                ", Total Tree Levels: " + indexMetaData.get(3)+"]");
        System.out.println();

        String selection;
        do {
            System.out.println("Select the query you would like to execute: \n" +
                    "1) Linear (Basic) Range Query \n" +
                    "2) Range Query using R* Tree index (Optimal)\n" +
                    "3) Linear (Basic) K-Nearest Neighbors Query\n" +
                    "4) K-Nearest Neighbors Query using R* Tree Index (Optimal)\n"+
                    "5) Linear (Basic) Skyline Query\n" +
                    "6) Skyline Query using R* Tree Index (Optimal)\n" +
                    "7) Exit");
            selection = scanner.nextLine().trim();
            System.out.println();
            
            ArrayList<Record> results;
            ArrayList<Bounds> bounds;
            int dims;
            BoundingBox queryBoundingBox;

            switch (selection) {
                case "1":
                    System.out.println("Worst Range Query Selected");
                    
                    bounds = new ArrayList<>();
                    dims = FilesManager.getDataDimensions();

                    System.out.println("Enter the Lower and Upper bounds for the Query MBR for each dimension: ");
                    for (int i = 0; i < dims; i++) {
                        while (true) {
                            System.out.print("Enter the bounds for dimension " + (i + 1) + " (lower Bound First): ");
                            double lower = scanner.nextDouble();
                            double upper = scanner.nextDouble();

                            if (lower <= upper) {
                                bounds.add(new Bounds(lower, upper));
                                break;
                            } else {
                                System.out.println("Lower bound must be less than or equal to upper bound.");
                            }
                        }
                    }

                    queryBoundingBox = new BoundingBox(bounds);
                    innitStartTime = System.nanoTime();
                    results = WorstRangeQuery.run(queryBoundingBox);
                    innitEndTime = System.nanoTime();
                    duration_in_ms = (innitEndTime - innitStartTime) / 1000000.0;

                    System.out.println("Results:");

                    for (Record record : results) {
                        System.out.println(record.toString());
                    }
                    System.out.println("Worst Range query completed in " + duration_in_ms + " ms");
                    System.out.println("Total points found in the given range: " + results.size());

                    System.out.println();


                    break;

                case "2":
                    System.out.println("Best Range Query selected");
                    bounds = new ArrayList<>();
                    dims = FilesManager.getDataDimensions();


                    System.out.println("Enter the Lower and Upper bounds for the Query MBR for each dimension: ");
                    for (int i = 0; i < dims; i++) {
                        while (true) {
                            System.out.print("Enter the bounds for dimension " + (i + 1) + " (lower Bound First): ");
                            double lower = scanner.nextDouble();
                            double upper = scanner.nextDouble();

                            if (lower <= upper) {
                                bounds.add(new Bounds(lower, upper));
                                break;
                            } else {
                                System.out.println("Lower bound must be less than or equal to upper bound.");
                            }
                        }
                    }

                    queryBoundingBox = new BoundingBox(bounds);
                    innitStartTime = System.nanoTime();
                    results = BestRangeQuery.bestRangeQuery(FilesManager.readIndexFileBlock(RStarTree.getRootNodeBlockId()), queryBoundingBox);
                    innitEndTime = System.nanoTime();
                    duration_in_ms = (innitEndTime - innitStartTime) / 1000000.0;


                    System.out.println("Results:");
                    for (Record record : results) {
                        System.out.println(record.toString());
                    }
                    System.out.println("Best Range Query completed in " + duration_in_ms + " ms");
                    System.out.println("Total points found in the given range: " + results.size());
                    System.out.println();


                    break;
                    
                case "3":
                    System.out.println("Worst K-Nearest Neighbors Query Selected");

                    System.out.print("Enter the amount of neighbors requested (K)");
                    int k = scanner.nextInt();
                    scanner.nextLine();

                    int dimensions = FilesManager.getDataDimensions();
                    ArrayList<Double> queryPoint = new ArrayList<>();
                    System.out.println("Enter the coordinates of the query point (you have " + dimensions + " dimensions):");
                    for (int i = 0; i < dimensions; i++) {
                        System.out.print("Dimension no." + (i + 1) + ": ");
                        double val = scanner.nextDouble();
                        scanner.nextLine();
                        queryPoint.add(val);
                    }
                    
                    innitStartTime = System.nanoTime();
                    WorstNearestNeighboursQuery query = new WorstNearestNeighboursQuery(queryPoint, k);
                    results = query.getNearestRecords();
                    innitEndTime = System.nanoTime();

                    double duration = (innitEndTime - innitStartTime) / 1_000_000.0;
                    System.out.println("Results:");

                    for (Record record : results) {
                        System.out.println(record.toString());
                    }
                    System.out.println("Worst K-Nearest Neighbors query completed in " + duration + " milliseconds");
                    System.out.println(k + " nearest records: " + results.size());

                    System.out.println();
                    break;

                case "4":
                    System.out.println("Best K-Nearest Neighbors Query Selected");
                    System.out.println("Executing K-Nearest Neighbours Query...");
                    System.out.print("Enter the amount of neighbors requested (K)");
                    int k2 = scanner.nextInt();
                    scanner.nextLine();
                    int dimensions2 = FilesManager.getDataDimensions();
                    ArrayList<Double> queryPoint2 = new ArrayList<>();
                    System.out.println("Enter the coordinates of the query point (you have " + dimensions2 + " dimensions):");
                    for (int i = 0; i < dimensions2; i++) {
                        System.out.print("Dimension no." + (i + 1) + ": ");
                        double val = scanner.nextDouble();
                        scanner.nextLine();
                        queryPoint2.add(val);
                    }
                    // Run k-NN query
                    innitStartTime = System.nanoTime();
                    results = BestNearestNeighboursQuery.getNearestNeighbours(queryPoint2, k2);
                    innitEndTime = System.nanoTime();

                    double duration2 = (innitEndTime - innitStartTime) / 1_000_000.0;
                    System.out.println("Results:");

                    for (Record record : results) {
                        System.out.println(record.toString());
                    }
                    ;
                    System.out.println("Best K-Nearest Neighbors query completed in " + duration2 + " milliseconds");
                    System.out.println(k2 + " nearest records: " + results.size());

                    System.out.println();


                    break;
                case "5":
                    System.out.println("Worst Skyline Query Selected");

                    innitStartTime = System.nanoTime();
                    results = WorstSkylineQuery.run();
                    innitEndTime = System.nanoTime();

                    duration_in_ms = (innitEndTime - innitStartTime) / 1_000_000.0;

                    System.out.println("Total skyline records:");

                    for (Record r : results) {
                        System.out.println(r.toString());
                    }
                    System.out.println("Worst Skyline Query completed in " + duration_in_ms + " ms");
                    System.out.println("Total records in skyline: " + results.size());
                    System.out.println();
                    break;

                case "6":

                    System.out.println("Best Skyline Query Selected");

                    innitStartTime = System.nanoTime();

                    ArrayList<Record> skylineResults = BestSkylineQuery.computeSkyline();

                    innitEndTime = System.nanoTime();
                    double durationInMS = (innitEndTime - innitStartTime) / 1_000_000.0;

                    System.out.println("Total skyline records:");

                    for (Record r : skylineResults) {
                        System.out.println(r);
                    }
                    System.out.println("Best Skyline Query completed in " + durationInMS + " ms");
                    System.out.println("Total records in skyline: " + skylineResults.size());

                    System.out.println();
                    break;

                case "7":
                    System.out.println("Exiting app...");
                    break;

                default:
                    System.out.println("Invalid input. Try again.");

            }

        } while (!selection.equals("7"));

    }
}