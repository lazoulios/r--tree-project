import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

class FilesManager {
    private static final String DELIMITER = ",";
    private static final String PATH_TO_CSV = "src/resources/data.csv";
    static final String PATH_TO_DATAFILE = "src/resources/datafile.dat";
    static final String PATH_TO_INDEXFILE = "src/resources/indexfile.dat";
    private static final int BLOCK_SIZE = 32 * 1024;
    private static int dataDimensions;
    private static int totalBlocksInDataFile;
    private static int totalBlocksInIndexFile;
    private static int totalLevelsOfTreeIndex;
    private static long nextAvailableIndexBlockId = 1;
    private static final Map<Long, Node> indexBuffer = new LinkedHashMap<>();


    static String getPathToCsv() {
        return PATH_TO_CSV;
    }

    static String getDelimiter() {
        return DELIMITER;
    }

    static int getDataDimensions() {
        return dataDimensions;
    }

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        return baos.toByteArray();
    }

    static ArrayList<Integer> getIndexMetaData() {
        return readMetaDataBlock(PATH_TO_INDEXFILE);
    }

    static ArrayList<Integer> getDataMetaData() {
        return readMetaDataBlock(PATH_TO_DATAFILE);
    }

    private static ArrayList<Integer> readMetaDataBlock(String pathToFile) {
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(pathToFile), "r");
            byte[] block = new byte[BLOCK_SIZE];
            raf.seek(0);
            int bytesRead = raf.read(block);
            if (bytesRead != BLOCK_SIZE) {
                throw new IOException("Could not read full metadata block (expected " + BLOCK_SIZE + ", got " + bytesRead + ")");
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(block);
            ObjectInputStream ois = new ObjectInputStream(bais);
            int metaDataSize = (Integer) ois.readObject();
            byte[] metadataBytes = new byte[metaDataSize];
            int actuallyRead = bais.read(metadataBytes);
            if (actuallyRead != metaDataSize) {
                throw new IOException("Could not read full metadata content");
            }
            ObjectInputStream metadataStream = new ObjectInputStream(new ByteArrayInputStream(metadataBytes));
            return (ArrayList<Integer>) metadataStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void updateMetaDataBlock(String pathToFile) {
        try {
            ArrayList<Integer> fileMetaData = new ArrayList<>();
            fileMetaData.add(dataDimensions);
            fileMetaData.add(BLOCK_SIZE);
            if (pathToFile.equals(PATH_TO_DATAFILE)) {
                fileMetaData.add(totalBlocksInDataFile);
            } else if (pathToFile.equals(PATH_TO_INDEXFILE)) {
                fileMetaData.add(totalBlocksInIndexFile);
                fileMetaData.add(totalLevelsOfTreeIndex);
            }
            byte[] metaDataInBytes = serialize(fileMetaData);
            byte[] metaDataSizeBytes = serialize(metaDataInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(metaDataSizeBytes, 0, block, 0, metaDataSizeBytes.length);
            System.arraycopy(metaDataInBytes, 0, block, metaDataSizeBytes.length, metaDataInBytes.length);
            RandomAccessFile raf = new RandomAccessFile(new File(pathToFile), "rw");
            raf.write(block);
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int getTotalBlocksInDataFile() {
        return totalBlocksInDataFile;
    }

    private static int calculateMaxRecordsInBlock() {
        ArrayList<Record> blockRecords = new ArrayList<>();
        int i;
        for (i = 0; i < 10000; i++) {
            ArrayList<Double> coords = new ArrayList<>();
            for (int d = 0; d < dataDimensions; d++)
                coords.add(0.0);
            Record record = new Record(0, "default_name", coords);
            blockRecords.add(record);
            byte[] recordInBytes = serializeOrEmpty(blockRecords);
            byte[] lengthInBytes = serializeOrEmpty(recordInBytes.length);
            if (lengthInBytes.length + recordInBytes.length > BLOCK_SIZE)
                break;
        }
        System.out.println("Max records in a block: " + (i-1));
        return i - 1;
    }

    private static byte[] serializeOrEmpty(Object obj) {
        try {
            return serialize(obj);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    public static void writeDataFileBlock(ArrayList<Record> records) {
        try {
            byte[] recordInBytes = serialize(records);
            byte[] metaDataLengthInBytes = serialize(recordInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];
            if (metaDataLengthInBytes.length + recordInBytes.length > BLOCK_SIZE) {
                throw new IllegalStateException("Block too large to fit in one data block");
            }
            System.arraycopy(metaDataLengthInBytes, 0, block, 0, metaDataLengthInBytes.length);
            System.arraycopy(recordInBytes, 0, block, metaDataLengthInBytes.length, recordInBytes.length);
            FileOutputStream fos = new FileOutputStream(PATH_TO_DATAFILE, true);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(block);
            totalBlocksInDataFile++;
            updateMetaDataBlock(PATH_TO_DATAFILE);
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static ArrayList<Record> readDataFileBlock(long blockID) {
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(PATH_TO_DATAFILE), "r");
            raf.seek(blockID * BLOCK_SIZE);
            byte[] block = new byte[BLOCK_SIZE];
            int bytesRead = raf.read(block);
            if (bytesRead != BLOCK_SIZE)
                throw new IOException("Block size read was not " + BLOCK_SIZE + " bytes");
            ByteArrayInputStream bais = new ByteArrayInputStream(block);
            ObjectInputStream ois = new ObjectInputStream(bais);
            int recordDataLength = (Integer) ois.readObject();
            byte[] recordBytes = new byte[recordDataLength];
            int actuallyRead = bais.read(recordBytes);
            if (actuallyRead != recordDataLength)
                throw new IOException("Could not read full record data");
            ObjectInputStream recordOis = new ObjectInputStream(new ByteArrayInputStream(recordBytes));
            return (ArrayList<Record>) recordOis.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static void initializeDataFile(int dataDimensions, boolean newDataFile) {
        try {
            if (!newDataFile && Files.exists(Paths.get(PATH_TO_DATAFILE))) {
                ArrayList<Integer> dataFileMetaData = readMetaDataBlock(PATH_TO_DATAFILE);
                if (dataFileMetaData == null)
                    throw new Exception("Could not read datafile's MetaData block");
                FilesManager.dataDimensions = dataFileMetaData.get(0);
                totalBlocksInDataFile = dataFileMetaData.get(2);
            } else {
                Files.deleteIfExists(Paths.get(PATH_TO_DATAFILE));
                FilesManager.dataDimensions = dataDimensions;
                totalBlocksInDataFile = 1;
                updateMetaDataBlock(PATH_TO_DATAFILE);
                ArrayList<Record> blockRecords = new ArrayList<>();
                BufferedReader csvReader = new BufferedReader(new FileReader(PATH_TO_CSV));
                csvReader.readLine();
                int maxRecordsInBlock = calculateMaxRecordsInBlock();
                String line;
                while ((line = csvReader.readLine()) != null) {
                    if (blockRecords.size() == maxRecordsInBlock) {
                        writeDataFileBlock(blockRecords);
                        blockRecords = new ArrayList<>();
                    }
                    blockRecords.add(new Record(line));
                }
                csvReader.close();
                if (!blockRecords.isEmpty())
                    writeDataFileBlock(blockRecords);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int getTotalBlocksInIndexFile() {
        return totalBlocksInIndexFile;
    }

    static int getTotalLevelsFile() {
        return totalLevelsOfTreeIndex;
    }


    static void initializeIndexFile(int dataDimensions, boolean newFile) {
        try {
            if (!newFile && Files.exists(Paths.get(PATH_TO_INDEXFILE))) {
                ArrayList<Integer> indexFileMetaData = readMetaDataBlock(PATH_TO_INDEXFILE);
                FilesManager.dataDimensions = indexFileMetaData.get(0);
                totalBlocksInIndexFile = indexFileMetaData.get(2);
                totalLevelsOfTreeIndex = indexFileMetaData.get(3);
            } else {
                Files.deleteIfExists(Paths.get(PATH_TO_INDEXFILE));
                FilesManager.dataDimensions = dataDimensions;
                totalLevelsOfTreeIndex = 1;
                totalBlocksInIndexFile = 1;
                updateMetaDataBlock(PATH_TO_INDEXFILE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void writeNewIndexFileBlock(Node node) {
        indexBuffer.put(node.getNodeBlockId(), node);
        totalBlocksInIndexFile++;
        updateMetaDataBlock(PATH_TO_INDEXFILE);
    }

    static void updateIndexFileBlock(Node node, int totalLevelsOfTreeIndex) {
        indexBuffer.put(node.getNodeBlockId(), node);
        FilesManager.totalLevelsOfTreeIndex = totalLevelsOfTreeIndex;
    }

    static Node readIndexFileBlock(long blockId) {
        if (indexBuffer.containsKey(blockId)) return indexBuffer.get(blockId);
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(PATH_TO_INDEXFILE), "r");
            raf.seek(blockId * BLOCK_SIZE);
            byte[] block = new byte[BLOCK_SIZE];
            if (raf.read(block) != BLOCK_SIZE) throw new IOException();

            ByteArrayInputStream bais = new ByteArrayInputStream(block);

            byte[] lenBytes = new byte[4];
            if (bais.read(lenBytes) != 4) throw new IOException("Couldn't read length bytes");
            int nodeDataLength = ByteBuffer.wrap(lenBytes).getInt();

            byte[] nodeBytes = new byte[nodeDataLength];
            if (bais.read(nodeBytes) != nodeDataLength) throw new IOException("Couldn't read full node data");

            ObjectInputStream nodeOis = new ObjectInputStream(new ByteArrayInputStream(nodeBytes));
            return (Node) nodeOis.readObject();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    static void flushIndexBufferToDisk() {
        try (RandomAccessFile raf = new RandomAccessFile(PATH_TO_INDEXFILE, "rw")) {
            for (Map.Entry<Long, Node> entry : indexBuffer.entrySet()) {
                long blockId = entry.getKey();
                Node node = entry.getValue();
                byte[] nodeInBytes = serialize(node);
                byte[] lenBytes = ByteBuffer.allocate(4).putInt(nodeInBytes.length).array();
                byte[] block = new byte[BLOCK_SIZE];
                System.arraycopy(lenBytes, 0, block, 0, 4);
                System.arraycopy(nodeInBytes, 0, block, 4, nodeInBytes.length);

                // Μετακίνηση στο σωστό offset
                long offset = blockId * BLOCK_SIZE;
                raf.seek(offset);
                raf.write(block);
            }

            updateMetaDataBlock(PATH_TO_INDEXFILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        indexBuffer.clear();
    }


    static void setLevelsOfTreeIndex(int totalLevelsOfTreeIndex) {
        FilesManager.totalLevelsOfTreeIndex = totalLevelsOfTreeIndex;
        updateMetaDataBlock(PATH_TO_INDEXFILE);
    }

    static long appendRecordToDataBlock(Record record) {
        try {
            int maxRecords = calculateMaxRecordsInBlock();
            for (long blockId = 1; blockId < totalBlocksInDataFile; blockId++) {
                ArrayList<Record> records = readDataFileBlock(blockId);
                if (records != null && records.size() < maxRecords) {
                    records.add(record);
                    overwriteDataFileBlock(blockId, records);
                    return blockId;
                }
            }
            ArrayList<Record> newBlock = new ArrayList<>();
            newBlock.add(record);
            writeDataFileBlock(newBlock);
            return totalBlocksInDataFile - 1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    static boolean deleteRecordFromDataBlock(Record record) {
        try {
            for (long blockId = 1; blockId < totalBlocksInDataFile; blockId++) {
                ArrayList<Record> records = readDataFileBlock(blockId);
                if (records != null && records.removeIf(r -> r.getRecordID() == record.getRecordID())) {
                    overwriteDataFileBlock(blockId, records);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void overwriteDataFileBlock(long blockId, ArrayList<Record> records) throws IOException {
        byte[] recordInBytes = serialize(records);
        byte[] metaDataLengthInBytes = serialize(recordInBytes.length);
        byte[] block = new byte[BLOCK_SIZE];

        if (metaDataLengthInBytes.length + recordInBytes.length > BLOCK_SIZE)
            throw new IllegalStateException("Block too large to overwrite");

        System.arraycopy(metaDataLengthInBytes, 0, block, 0, metaDataLengthInBytes.length);
        System.arraycopy(recordInBytes, 0, block, metaDataLengthInBytes.length, recordInBytes.length);

        try (RandomAccessFile raf = new RandomAccessFile(PATH_TO_DATAFILE, "rw")) {
            raf.seek(blockId * BLOCK_SIZE);
            raf.write(block);
        }
    }

    public static Map<Node, Integer> writeNewIndexFileBlocks(List<Node> nodes) {
        Map<Node, Integer> result = new HashMap<>();

        for (Node node : nodes) {
            long blockID = getNextIndexBlockId();
            node.setNodeBlockId((int) blockID);
            indexBuffer.put(blockID, node);
            result.put(node,(int) blockID);
        }
        return result;
    }

    public static long getNextIndexBlockId() {
        return ++nextAvailableIndexBlockId;
    }
}
