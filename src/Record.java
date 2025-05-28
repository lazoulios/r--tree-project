import java.util.ArrayList;
import java.io.Serializable;

public class Record implements Serializable{
    private long recordID;
    private String name;
    private ArrayList<Double> coor; //coordinates

    // The constructor
    public Record(long recordID, String name, ArrayList<Double> coor){
        this.recordID = recordID;
        this.name = name;
        this.coor = coor;
    }
    public Record(String recordInString) {
        String[] stringArray = recordInString.split(FilesManager.getDelimiter());

        // Expecting: ID + name + coordinates
        if (stringArray.length != FilesManager.getDataDimensions() + 2)
            throw new IllegalArgumentException("Record input string is not correct: " + recordInString);

        recordID = Long.parseLong(stringArray[0]);
        name = stringArray[1];

        coor = new ArrayList<>();
        for (int i = 2; i < stringArray.length; i++) {
            coor.add(Double.parseDouble(stringArray[i]));
        }
    }


    // Getters
    public long getRecordID(){
        return  recordID;
    }

    public String getName(){
        return  name;
    }

    public ArrayList<Double> getCoordinates() { return coor;}

    public double getCoordinateFromDimension(int dimension){
        return  coor.get(dimension);
    }

    // Custom output for the records
    @Override
    public String toString() {
        StringBuilder recordToString = new StringBuilder("ID: " + recordID + ", Name: " + name+ ", Coordinates: ");
        for (int i = 0;  i < coor.size(); i++) {
            if(i > 0)
                recordToString.append(", ");
            recordToString.append(coor.get(i));
        }
        return recordToString.toString();
    }
}
