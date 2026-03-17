public class LinearProbingParkingLot {

    private final String[] spots;
    private final int capacity;
    private int currentlyParked;

    // The Tombstone marker: crucial for Open Addressing deletions!
    private static final String LEAVED_MARKER = "<AVAILABLE_AGAIN>";

    public LinearProbingParkingLot(int capacity) {
        this.capacity = capacity;
        this.spots = new String[capacity];
        this.currentlyParked = 0;
    }

    /**
     * Hash function to map a license plate to an ideal parking spot.
     */
    private int getHash(String licensePlate) {
        // Math.abs handles negative hash codes
        return Math.abs(licensePlate.hashCode()) % capacity;
    }

    /**
     * Parks a car using Linear Probing for collision resolution.
     */
    public String park(String licensePlate) {
        if (currentlyParked == capacity) {
            return "Denied: Parking lot is full.";
        }

        int index = getHash(licensePlate);
        int startIndex = index;

        // Probe for an empty spot or a tombstone
        while (spots[index] != null && !spots[index].equals(LEAVED_MARKER)) {
            // If the car is already parked, ignore
            if (spots[index].equals(licensePlate)) {
                return String.format("Car %s is already parked at spot %d", licensePlate, index);
            }
            
            // Linear probing: move to the next spot, wrapping around if necessary
            index = (index + 1) % capacity;
            
            // Safety break (shouldn't happen due to capacity check, but good practice)
            if (index == startIndex) break; 
        }

        spots[index] = licensePlate;
        currentlyParked++;
        return String.format("Parked %s at spot %d", licensePlate, index);
    }

    /**
     * Finds a parked car.
     */
    public int findSpot(String licensePlate) {
        int index = getHash(licensePlate);
        int startIndex = index;

        while (spots[index] != null) {
            if (spots[index].equals(licensePlate)) {
                return index; // Found it
            }
            index = (index + 1) % capacity;
            
            // If we've looped entirely around, it's not here
            if (index == startIndex) {
                break;
            }
        }
        return -1; // Not found
    }

    /**
     * Removes a car, leaving a Tombstone marker behind.
     */
    public String leave(String licensePlate) {
        int index = findSpot(licensePlate);
        
        if (index == -1) {
            return String.format("Car %s not found in the lot.", licensePlate);
        }

        // Leave a tombstone instead of null to preserve the probe sequence
        spots[index] = LEAVED_MARKER;
        currentlyParked--;
        return String.format("Car %s has left spot %d", licensePlate, index);
    }
    
    // --- Testing the Implementation ---
    public static void main(String[] args) {
        // Small lot to force collisions
        LinearProbingParkingLot lot = new LinearProbingParkingLot(5);
        
        System.out.println(lot.park("XYZ-123")); // Spot depends on hash
        System.out.println(lot.park("ABC-999")); 
        System.out.println(lot.park("LMN-456"));
        
        System.out.println("\nLocating ABC-999: Spot " + lot.findSpot("ABC-999"));
        
        System.out.println("\n" + lot.leave("XYZ-123"));
        
        // Even though XYZ-123 left, we can still find LMN-456 because of the Tombstone
        System.out.println("Locating LMN-456 after XYZ-123 left: Spot " + lot.findSpot("LMN-456"));
        
        // Parking a new car will reuse the tombstone spot or find a new empty one
        System.out.println(lot.park("NEW-000"));
    }
}
