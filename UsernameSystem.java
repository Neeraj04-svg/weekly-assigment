import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class UsernameSystem {
    // Stores: Username -> UserID (O(1) lookup)
    private final ConcurrentHashMap<String, String> registeredUsers = new ConcurrentHashMap<>();
    
    // Stores: Username -> Count of attempts (O(1) update)
    private final ConcurrentHashMap<String, AtomicInteger> attemptTracker = new ConcurrentHashMap<>();

    /**
     * Checks if a username is available in O(1) time.
     * Also tracks the popularity of the attempt.
     */
    public boolean checkAvailability(String username) {
        // Track the attempt frequency
        attemptTracker.computeIfAbsent(username, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Check existence
        return !registeredUsers.containsKey(username.toLowerCase());
    }

    /**
     * Registers a user if the name is available.
     */
    public void register(String username, String userId) {
        registeredUsers.putIfAbsent(username.toLowerCase(), userId);
    }

    /**
     * Generates similar usernames by appending numbers or symbols.
     */
    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();
        int suffix = 1;

        while (suggestions.size() < 3) {
            String candidate = username + suffix;
            if (!registeredUsers.containsKey(candidate)) {
                suggestions.add(candidate);
            }
            suffix++;
        }
        
        // Additional variation: adding a period
        String dotted = username.replace("_", ".");
        if (!registeredUsers.containsKey(dotted) && !dotted.equals(username)) {
            suggestions.add(0, dotted); // Prioritize this variation
        }

        return suggestions.stream().limit(3).collect(Collectors.toList());
    }

    /**
     * Returns the username that was checked most frequently.
     */
    public String getMostAttempted() {
        return attemptTracker.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().get()))
                .map(Map.Entry::getKey)
                .orElse("No attempts yet");
    }

    public static void main(String[] args) {
        UsernameSystem sys = new UsernameSystem();

        // Setup existing users
        sys.register("john_doe", "USR101");

        // 1. Check Availability
        System.out.println("Is 'john_doe' available? " + sys.checkAvailability("john_doe"));
        System.out.println("Is 'jane_smith' available? " + sys.checkAvailability("jane_smith"));

        // 2. Get Suggestions
        if (!sys.checkAvailability("john_doe")) {
            System.out.println("Suggestions: " + sys.suggestAlternatives("john_doe"));
        }

        // 3. Track Popularity
        sys.checkAvailability("admin"); // Simulate multiple checks
        sys.checkAvailability("admin");
        System.out.println("Most attempted: " + sys.getMostAttempted());
    }
}
