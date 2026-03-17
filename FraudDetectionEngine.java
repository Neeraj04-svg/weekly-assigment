import java.util.*;
import java.util.stream.Collectors;

public class FraudDetectionEngine {

    // Record is perfect for immutable data structures like financial transactions
    public record Transaction(String id, long amount, String merchant, long timestampMs, String accountId) {}

    // Storage and Indexes
    private final List<Transaction> allTransactions = new ArrayList<>();
    
    // Index for fast duplicate/merchant lookups
    // Key: "amount_merchant" -> Value: List of Transactions
    private final Map<String, List<Transaction>> duplicateIndex = new HashMap<>();

    /**
     * Ingests a transaction and updates the necessary indexes.
     */
    public void addTransaction(Transaction t) {
        allTransactions.add(t);
        
        // Build composite key for duplicate detection
        String dupKey = t.amount() + "_" + t.merchant();
        duplicateIndex.computeIfAbsent(dupKey, k -> new ArrayList<>()).add(t);
    }

    /**
     * 1. Classic Two-Sum: Find pairs that sum to target.
     * Time Complexity: O(N) | Space Complexity: O(N)
     */
    public List<List<Transaction>> findTwoSum(long targetAmount) {
        List<List<Transaction>> results = new ArrayList<>();
        Map<Long, Transaction> complements = new HashMap<>();

        for (Transaction t : allTransactions) {
            long complement = targetAmount - t.amount();
            if (complements.containsKey(complement)) {
                results.add(Arrays.asList(complements.get(complement), t));
            }
            complements.put(t.amount(), t);
        }
        return results;
    }

    /**
     * 2. Two-Sum with Time Window (e.g., within 1 hour).
     * Time Complexity: O(N) | Space Complexity: O(N)
     */
    public List<List<Transaction>> findTwoSumWithinTime(long targetAmount, long timeWindowMs) {
        List<List<Transaction>> results = new ArrayList<>();
        // Maps amount to a list of transactions (since multiple transactions can have the same amount)
        Map<Long, List<Transaction>> amountMap = new HashMap<>();

        for (Transaction t : allTransactions) {
            long complement = targetAmount - t.amount();
            
            if (amountMap.containsKey(complement)) {
                for (Transaction compTx : amountMap.get(complement)) {
                    if (Math.abs(t.timestampMs() - compTx.timestampMs()) <= timeWindowMs) {
                        results.add(Arrays.asList(compTx, t));
                    }
                }
            }
            amountMap.computeIfAbsent(t.amount(), k -> new ArrayList<>()).add(t);
        }
        return results;
    }

    /**
     * 3. Duplicate Detection: Same amount, same merchant, different accounts.
     * Time Complexity: O(N) based on index generation | Space Complexity: O(N)
     */
    public List<Map<String, Object>> detectDuplicates() {
        List<Map<String, Object>> suspiciousActivities = new ArrayList<>();

        for (Map.Entry<String, List<Transaction>> entry : duplicateIndex.entrySet()) {
            List<Transaction> txs = entry.getValue();
            if (txs.size() > 1) {
                // Check if they come from different accounts
                Set<String> uniqueAccounts = txs.stream()
                        .map(Transaction::accountId)
                        .collect(Collectors.toSet());

                if (uniqueAccounts.size() > 1) {
                    Map<String, Object> report = new HashMap<>();
                    report.put("amount", txs.get(0).amount());
                    report.put("merchant", txs.get(0).merchant());
                    report.put("accounts", uniqueAccounts);
                    report.put("transactions", txs);
                    suspiciousActivities.add(report);
                }
            }
        }
        return suspiciousActivities;
    }

    /**
     * 4. K-Sum: Find K transactions that sum to target.
     * Warning: Running this on millions of unstructured rows will exceed 100ms.
     * This uses a recursive backtracking approach optimized with sorting.
     */
    public List<List<Transaction>> findKSum(int k, long targetAmount) {
        // Sorting is required to effectively skip duplicates and optimize the search tree
        List<Transaction> sortedTxs = new ArrayList<>(allTransactions);
        sortedTxs.sort(Comparator.comparingLong(Transaction::amount));
        
        return kSumHelper(sortedTxs, targetAmount, k, 0);
    }

    private List<List<Transaction>> kSumHelper(List<Transaction> txs, long target, int k, int index) {
        List<List<Transaction>> res = new ArrayList<>();
        
        if (index >= txs.size() || k < 2) return res;

        // Base case: Reduce to standard Two-Sum using Two Pointers (since array is sorted)
        if (k == 2) {
            int left = index, right = txs.size() - 1;
            while (left < right) {
                long sum = txs.get(left).amount() + txs.get(right).amount();
                if (sum == target) {
                    res.add(new ArrayList<>(Arrays.asList(txs.get(left), txs.get(right))));
                    // Skip duplicate amounts to avoid redundant result sets
                    while (left < right && txs.get(left).amount() == txs.get(left + 1).amount()) left++;
                    while (left < right && txs.get(right).amount() == txs.get(right - 1).amount()) right--;
                    left++;
                    right--;
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
            return res;
        }

        // Recursive case: K > 2
        for (int i = index; i < txs.size() - k + 1; i++) {
            // Pruning: Skip duplicates
            if (i > index && txs.get(i).amount() == txs.get(i - 1).amount()) continue;
            
            // Recurse for k-1
            List<List<Transaction>> subResults = kSumHelper(txs, target - txs.get(i).amount(), k - 1, i + 1);
            for (List<Transaction> list : subResults) {
                list.add(0, txs.get(i)); // Prepend current transaction
                res.add(list);
            }
        }
        return res;
    }

    // --- Testing the Implementation ---
    public static void main(String[] args) {
        FraudDetectionEngine engine = new FraudDetectionEngine();
        
        // Setup sample data
        engine.addTransaction(new Transaction("1", 500, "Store A", 1675890000000L, "acc1"));
        engine.addTransaction(new Transaction("2", 300, "Store B", 1675890900000L, "acc2"));
        engine.addTransaction(new Transaction("3", 200, "Store C", 1675891800000L, "acc3"));
        engine.addTransaction(new Transaction("4", 500, "Store A", 1675892000000L, "acc4")); // Duplicate profile

        System.out.println("--- Two Sum (Target 500) ---");
        engine.findTwoSum(500).forEach(pair -> 
            System.out.println(pair.get(0).id() + " + " + pair.get(1).id()));

        System.out.println("\n--- Duplicate Detection ---");
        System.out.println(engine.detectDuplicates());

        System.out.println("\n--- K-Sum (K=3, Target 1000) ---");
        engine.findKSum(3, 1000).forEach(triplet -> {
            triplet.forEach(t -> System.out.print(t.id() + " "));
            System.out.println();
        });
    }
}
