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
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(stream);
        oos.writeObject(obj);
        return stream.toByteArray();
    }

    static ArrayList<Integer> getIndexMetaData() {
        return readMetaDataBlock(PATH_TO_INDEXFILE);
    }

    static ArrayList<Integer> getDataMetaData() {
        return readMetaDataBlock(PATH_TO_DATAFILE);
    }

    private static ArrayList<Integer> readMetaDataBlock(String pathToFile) {
        try {
            RandomAccessFile accessFile = new RandomAccessFile(new File(pathToFile), "r");
            byte[] block = new byte[BLOCK_SIZE];
            accessFile.seek(0);
            int bytesRead = accessFile.read(block);
            if (bytesRead != BLOCK_SIZE) {
                throw new IOException("Could not read full metadata block (expected " + BLOCK_SIZE + ", got " + bytesRead + ")");
            }
            ByteArrayInputStream byte_input_stream = new ByteArrayInputStream(block);
            ObjectInputStream obj_input_stream = new ObjectInputStream(byte_input_stream);
            int metaDataSize = (Integer) obj_input_stream.readObject();
            byte[] metadataBytes = new byte[metaDataSize];
            int read_content = byte_input_stream.read(metadataBytes);
            if (read_content != metaDataSize) {
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
            byte[] blockInBytes = new byte[BLOCK_SIZE];
            System.arraycopy(metaDataSizeBytes, 0, blockInBytes, 0, metaDataSizeBytes.length);
            System.arraycopy(metaDataInBytes, 0, blockInBytes, metaDataSizeBytes.length, metaDataInBytes.length);
            RandomAccessFile accessFile = new RandomAccessFile(new File(pathToFile), "rw");
            accessFile.write(blockInBytes);
            accessFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int getTotalBlocksInDataFile() {
        return totalBlocksInDataFile;
    }

    private static int calculateMaxRecordsInBlock() {
        ArrayList<Record> recordsInBlock = new ArrayList<>();
        int i;
        for (i = 0; i < 10000; i++) {
            ArrayList<Double> coordinates = new ArrayList<>();
            for (int d = 0; d < dataDimensions; d++)
                coordinates.add(0.0);
            Record record = new Record(0, "default_name", coordinates);
            recordsInBlock.add(record);
            byte[] recordInBytes = serializeOrEmpty(recordsInBlock);
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
            byte[] recordSerialized = serialize(records);
            byte[] metaDataLengthSerialized = serialize(recordSerialized.length);
            byte[] block = new byte[BLOCK_SIZE];
            if (metaDataLengthSerialized.length + recordSerialized.length > BLOCK_SIZE) {
                throw new IllegalStateException("Block too large to fit in one data block");
            }
            System.arraycopy(metaDataLengthSerialized, 0, block, 0, metaDataLengthSerialized.length);
            System.arraycopy(recordSerialized, 0, block, metaDataLengthSerialized.length, recordSerialized.length);
            FileOutputStream fileOutStream = new FileOutputStream(PATH_TO_DATAFILE, true);
            BufferedOutputStream byteOutStream = new BufferedOutputStream(fileOutStream);
            byteOutStream.write(block);
            totalBlocksInDataFile++;
            updateMetaDataBlock(PATH_TO_DATAFILE);
            byteOutStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static ArrayList<Record> readDataFileBlock(long blockID) {
        try {
            RandomAccessFile accessFile = new RandomAccessFile(new File(PATH_TO_DATAFILE), "r");
            accessFile.seek(blockID * BLOCK_SIZE);
            byte[] block = new byte[BLOCK_SIZE];
            int bytesRead = accessFile.read(block);
            if (bytesRead != BLOCK_SIZE)
                throw new IOException("The block size read was not exactly" + BLOCK_SIZE + " bytes");
            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(block);
            ObjectInputStream objInputStream = new ObjectInputStream(arrayInputStream);
            int recordDataLength = (Integer) objInputStream.readObject();
            byte[] recordBytes = new byte[recordDataLength];
            int readData = arrayInputStream.read(recordBytes);
            if (readData != recordDataLength)
                throw new IOException("Could not read the full record data");
            ObjectInputStream recordOutStream = new ObjectInputStream(new ByteArrayInputStream(recordBytes));
            return (ArrayList<Record>) recordOutStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static void initializeDataFile(int dataDims, boolean newDataFile) {
        try {
            if (!newDataFile && Files.exists(Paths.get(PATH_TO_DATAFILE))) {
                ArrayList<Integer> dataFileMetaData = readMetaDataBlock(PATH_TO_DATAFILE);
                if (dataFileMetaData == null)
                    throw new Exception("Could not read MetaData block from DataFile");
                FilesManager.dataDimensions = dataFileMetaData.get(0);
                totalBlocksInDataFile = dataFileMetaData.get(2);
            } else {
                Files.deleteIfExists(Paths.get(PATH_TO_DATAFILE));
                FilesManager.dataDimensions = dataDims;
                totalBlocksInDataFile = 1;
                updateMetaDataBlock(PATH_TO_DATAFILE);
                ArrayList<Record> recordsInBlock = new ArrayList<>();
                BufferedReader csvReader = new BufferedReader(new FileReader(PATH_TO_CSV));
                csvReader.readLine();
                int maxRecordsInBlock = calculateMaxRecordsInBlock();
                String line;
                while ((line = csvReader.readLine()) != null) {
                    if (recordsInBlock.size() == maxRecordsInBlock) {
                        writeDataFileBlock(recordsInBlock);
                        recordsInBlock = new ArrayList<>();
                    }
                    recordsInBlock.add(new Record(line));
                }
                csvReader.close();
                if (!recordsInBlock.isEmpty())
                    writeDataFileBlock(recordsInBlock);
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

            byte[] bytesLength = new byte[4];
            if (bais.read(bytesLength) != 4) throw new IOException("Unable to read bytes length");
            int nodeDataLength = ByteBuffer.wrap(bytesLength).getInt();

            byte[] nodeBytes = new byte[nodeDataLength];
            if (bais.read(nodeBytes) != nodeDataLength) throw new IOException("Unable to read full node data");

            ObjectInputStream nodeObjInStream = new ObjectInputStream(new ByteArrayInputStream(nodeBytes));
            return (Node) nodeObjInStream.readObject();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    static void flushIndexBufferToDisk() {
        try (RandomAccessFile accessFile = new RandomAccessFile(PATH_TO_INDEXFILE, "rw")) {
            for (Map.Entry<Long, Node> entry : indexBuffer.entrySet()) {
                long entryKey = entry.getKey();
                Node node = entry.getValue();
                byte[] nodeInBytes = serialize(node);
                byte[] bytesLength = ByteBuffer.allocate(4).putInt(nodeInBytes.length).array();
                byte[] block = new byte[BLOCK_SIZE];
                System.arraycopy(bytesLength, 0, block, 0, 4);
                System.arraycopy(nodeInBytes, 0, block, 4, nodeInBytes.length);

                // Μετακίνηση στο σωστό offset
                long offset = entryKey * BLOCK_SIZE;
                accessFile.seek(offset);
                accessFile.write(block);
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
        byte[] recordSerialized = serialize(records);
        byte[] metaDataLengthSerialized = serialize(recordSerialized.length);
        byte[] block = new byte[BLOCK_SIZE];

        if (metaDataLengthSerialized.length + recordSerialized.length > BLOCK_SIZE)
            throw new IllegalStateException("Block too large to overwrite");

        System.arraycopy(metaDataLengthSerialized, 0, block, 0, metaDataLengthSerialized.length);
        System.arraycopy(recordSerialized, 0, block, metaDataLengthSerialized.length, recordSerialized.length);

        try (RandomAccessFile accessFile = new RandomAccessFile(PATH_TO_DATAFILE, "rw")) {
            accessFile.seek(blockId * BLOCK_SIZE);
            accessFile.write(block);
        }
    }

    public static Map<Node, Integer> writeNewIndexFileBlocks(List<Node> nodes) {
        Map<Node, Integer> result = new HashMap<>();

        for (Node node : nodes) {
            long nextIndexBlockId = getNextIndexBlockId();
            node.setNodeBlockId((int) nextIndexBlockId);
            indexBuffer.put(nextIndexBlockId, node);
            result.put(node,(int) nextIndexBlockId);
        }
        return result;
    }

    public static long getNextIndexBlockId() {
        return ++nextAvailableIndexBlockId;
    }
}
