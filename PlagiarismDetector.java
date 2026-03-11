import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlagiarismDetector {
    // N-gram size (5 is standard for academic text)
    private static final int N = 5;
    
    // Map: N-gram -> Set of Document IDs that contain it
    private final Map<String, Set<String>> ngramIndex = new ConcurrentHashMap<>();
    
    // Map: Document ID -> Total count of unique n-grams in that doc
    private final Map<String, Integer> documentSizes = new ConcurrentHashMap<>();

    /**
     * Pre-processes a document and adds it to the reference database.
     */
    public void addReferenceDocument(String docId, String content) {
        Set<String> ngrams = extractNGrams(content);
        documentSizes.put(docId, ngrams.size());
        
        for (String gram : ngrams) {
            ngramIndex.computeIfAbsent(gram, k -> ConcurrentHashMap.newKeySet()).add(docId);
        }
    }

    /**
     * Analyzes a new submission against the database.
     */
    public void analyzeDocument(String newDocId, String content) {
        Set<String> newNgrams = extractNGrams(content);
        int totalGrams = newNgrams.size();
        
        // Count matches per reference document
        Map<String, Integer> matchCounts = new HashMap<>();
        
        for (String gram : newNgrams) {
            if (ngramIndex.containsKey(gram)) {
                for (String refDocId : ngramIndex.get(gram)) {
                    matchCounts.put(refDocId, matchCounts.getOrDefault(refDocId, 0) + 1);
                }
            }
        }

        System.out.println("Analyzing " + newDocId + "...");
        System.out.println("Extracted " + totalGrams + " n-grams.");

        // Calculate and report similarity
        matchCounts.forEach((refId, matches) -> {
            double similarity = (matches * 100.0) / totalGrams;
            String status = getStatus(similarity);
            System.out.printf("-> Found %d matching n-grams with %s\n", matches, refId);
            System.out.printf("   Similarity: %.1f%% (%s)\n", similarity, status);
        });
    }

    private String getStatus(double percentage) {
        if (percentage > 50) return "PLAGIARISM DETECTED";
        if (percentage > 15) return "SUSPICIOUS";
        return "CLEAR";
    }

    /**
     * Helper to break text into N-word sequences.
     */
    private Set<String> extractNGrams(String text) {
        String[] words = text.toLowerCase().replaceAll("[^a-zA-Z ]", "").split("\\s+");
        Set<String> ngrams = new HashSet<>();
        
        for (int i = 0; i <= words.length - N; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < N; j++) {
                sb.append(words[i + j]).append(" ");
            }
            ngrams.add(sb.toString().trim());
        }
        return ngrams;
    }

    public static void main(String[] args) {
        PlagiarismDetector detector = new PlagiarismDetector();

        // 1. Build the database
        detector.addReferenceDocument("essay_089.txt", "The quick brown fox jumps over the lazy dog repeatedly.");
        detector.addReferenceDocument("essay_092.txt", "Artificial intelligence is the simulation of human intelligence by machines.");

        // 2. Test a suspicious document
        String newSubmission = "Artificial intelligence is the simulation of human intelligence processes by computers.";
        detector.analyzeDocument("submission_101.txt", newSubmission);
    }
}
