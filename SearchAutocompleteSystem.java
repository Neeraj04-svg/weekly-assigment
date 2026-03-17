import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SearchAutocompleteSystem {

    // Global frequency map to track how popular each query is
    private final Map<String, Integer> frequencyMap;
    private final TrieNode root;
    private static final int MAX_RESULTS = 10;

    public SearchAutocompleteSystem() {
        // Using ConcurrentHashMap to allow thread-safe frequency updates
        this.frequencyMap = new ConcurrentHashMap<>();
        this.root = new TrieNode();
    }

    // --- Inner Class: The Trie Node ---
    private class TrieNode {
        Map<Character, TrieNode> children;
        // Caches the top K queries passing through this prefix to guarantee <50ms lookups
        List<String> topQueries;

        public TrieNode() {
            children = new HashMap<>();
            topQueries = new ArrayList<>();
        }
    }

    /**
     * Updates the frequency of a query and re-balances the Top-K caches in the Trie.
     * Time Complexity: O(L) where L is the length of the query.
     */
    public void updateFrequency(String query) {
        frequencyMap.put(query, frequencyMap.getOrDefault(query, 0) + 1);
        TrieNode curr = root;

        for (char c : query.toCharArray()) {
            curr.children.putIfAbsent(c, new TrieNode());
            curr = curr.children.get(c);
            updateTopQueries(curr, query);
        }
    }

    /**
     * Helper to maintain the Top K queries at each node.
     * Since MAX_RESULTS is a small constant (10), sorting this list is O(1).
     */
    private void updateTopQueries(TrieNode node, String query) {
        // If it's not already in the list, add it
        if (!node.topQueries.contains(query)) {
            node.topQueries.add(query);
        }

        // Sort based on frequency (descending), then lexicographically (ascending)
        node.topQueries.sort((a, b) -> {
            int freqA = frequencyMap.get(a);
            int freqB = frequencyMap.get(b);
            if (freqA == freqB) {
                return a.compareTo(b); // Alphabetical tie-breaker
            }
            return Integer.compare(freqB, freqA); // Frequency tie-breaker
        });

        // Eject the 11th element to strictly enforce memory limits
        if (node.topQueries.size() > MAX_RESULTS) {
            node.topQueries.remove(node.topQueries.size() - 1);
        }
    }

    /**
     * Returns the top 10 suggestions for a given prefix.
     * Time Complexity: O(P) where P is the length of the prefix.
     */
    public List<String> search(String prefix) {
        TrieNode curr = root;
        for (char c : prefix.toCharArray()) {
            if (!curr.children.containsKey(c)) {
                return new ArrayList<>(); // Prefix not found, return empty
            }
            curr = curr.children.get(c);
        }
        return curr.topQueries; // Returns the O(1) cached list
    }

    // --- Testing the Implementation ---
    public static void main(String[] args) {
        SearchAutocompleteSystem autocomplete = new SearchAutocompleteSystem();

        // Simulating historical data loading
        autocomplete.updateFrequency("java tutorial");
        autocomplete.updateFrequency("javascript");
        autocomplete.updateFrequency("javascript");
        autocomplete.updateFrequency("java download");
        
        // Simulating a trending search (searched 3 times)
        autocomplete.updateFrequency("java 21 features");
        autocomplete.updateFrequency("java 21 features");
        autocomplete.updateFrequency("java 21 features");

        System.out.println("Search 'jav':");
        List<String> results = autocomplete.search("jav");
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("%d. \"%s\" (Frequency: %d)\n", 
                i + 1, results.get(i), autocomplete.frequencyMap.get(results.get(i)));
        }
        
        System.out.println("\nSearch 'javascript':");
        System.out.println(autocomplete.search("javas"));
    }
}
