import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutocompleteSystem {

    class TrieNode {
        // Map of character to next node
        Map<Character, TrieNode> children = new HashMap<>();
        // Stores the top 10 search queries passing through/ending at this node
        List<String> topSuggestions = new ArrayList<>();
    }

    private final TrieNode root = new TrieNode();
    // Global frequency map: Query -> Search Count
    private final Map<String, Integer> frequencies = new ConcurrentHashMap<>();
    private final int MAX_SUGGESTIONS = 10;

    /**
     * Updates the frequency of a query and re-optimizes the Trie path.
     * Time Complexity: O(L log K) where L is query length and K is 10.
     */
    public void updateFrequency(String query) {
        query = query.toLowerCase();
        int newFreq = frequencies.getOrDefault(query, 0) + 1;
        frequencies.put(query, newFreq);

        TrieNode current = root;
        for (char c : query.toCharArray()) {
            current.children.putIfAbsent(c, new TrieNode());
            current = current.children.get(c);
            updateTopSuggestions(current, query);
        }
    }

    /**
     * Refreshes the top suggestions list for a specific node.
     */
    private void updateTopSuggestions(TrieNode node, String query) {
        if (!node.topSuggestions.contains(query)) {
            node.topSuggestions.add(query);
        }

        // Sort based on global frequency
        node.topSuggestions.sort((a, b) -> {
            int freqA = frequencies.getOrDefault(a, 0);
            int freqB = frequencies.getOrDefault(b, 0);
            return freqB != freqA ? freqB - freqA : a.compareTo(b);
        });

        // Keep only top 10
        if (node.topSuggestions.size() > MAX_SUGGESTIONS) {
            node.topSuggestions.remove(MAX_SUGGESTIONS);
        }
    }

    /**
     * Returns top 10 suggestions for a prefix in O(L) time.
     */
    public List<String> search(String prefix) {
        prefix = prefix.toLowerCase();
        TrieNode current = root;
        for (char c : prefix.toCharArray()) {
            if (!current.children.containsKey(c)) {
                return Collections.emptyList();
            }
            current = current.children.get(c);
        }
        return current.topSuggestions;
    }

    public static void main(String[] args) {
        AutocompleteSystem engine = new AutocompleteSystem();

        // Simulate popular searches
        engine.updateFrequency("java tutorial");
        engine.updateFrequency("java tutorial"); // Extra hits for popularity
        engine.updateFrequency("javascript");
        engine.updateFrequency("java download");
        engine.updateFrequency("java 21 features");

        // Search test
        System.out.println("Suggestions for 'jav':");
        List<String> results = engine.search("jav");
        for (int i = 0; i < results.size(); i++) {
            String query = results.get(i);
            System.out.printf("%d. %s (%d searches)\n",
                    i + 1, query, engine.frequencies.get(query));
        }
    }
}