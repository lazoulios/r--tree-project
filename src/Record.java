import java.util.ArrayList;
import java.io.Serializable;

public class Record implements Serializable{
    private long recordID; // unique ID
    private String name; // can be a location name or an empty string
    private ArrayList<Double> coordinates; // record coordinates

    public Record(long recordID, String name, ArrayList<Double> coordinates){
        this.recordID = recordID;
        this.name = name;
        this.coordinates = coordinates;
    }

    public Record(String recordInString) {
        String[] stringArray = recordInString.split(FilesManager.getDelimiter());

        if (stringArray.length != FilesManager.getDataDimensions() + 2)
            throw new IllegalArgumentException("Record input string is not correct: " + recordInString);

        recordID = Long.parseLong(stringArray[0]);
        name = stringArray[1];

        coordinates = new ArrayList<>();
        for (int i = 2; i < stringArray.length; i++) {
            coordinates.add(Double.parseDouble(stringArray[i]));
        }
    }


    // Getters
    public long getRecordID(){
        return  recordID;
    }

    public String getName(){
        return  name;
    }

    public ArrayList<Double> getCoordinates() { return coordinates;}

    public double getCoordinateFromDimension(int dimension){
        return  coordinates.get(dimension);
    }

    @Override
    public String toString() {
        StringBuilder recordToString = new StringBuilder("ID: " + recordID + ", Name: " + name+ ", Coordinates: ");
        for (int i = 0; i < coordinates.size(); i++) {
            if(i > 0)
                recordToString.append(", ");
            recordToString.append(coordinates.get(i));
        }
        return recordToString.toString();
    }
}
