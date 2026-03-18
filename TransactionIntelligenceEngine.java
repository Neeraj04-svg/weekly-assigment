import java.util.*;
import java.time.*;
import java.util.stream.Collectors;

class Transaction {
    int id;
    double amount;
    String merchant;
    String account;
    LocalTime time;

    public Transaction(int id, double amount, String merchant, String account, String time) {
        this.id = id;
        this.amount = amount;
        this.merchant = merchant;
        this.account = account;
        this.time = LocalTime.parse(time);
    }
}

public class TransactionIntelligenceEngine {

    // 1. Classic Two-Sum: Finds pairs summing to target in O(n)
    public List<String> findTwoSum(List<Transaction> txns, double target) {
        Map<Double, Transaction> seen = new HashMap<>();
        List<String> results = new ArrayList<>();

        for (Transaction t : txns) {
            double complement = target - t.amount;
            if (seen.containsKey(complement)) {
                results.add("Pair: ID " + seen.get(complement).id + " & ID " + t.id);
            }
            seen.put(t.amount, t);
        }
        return results;
    }

    // 2. Two-Sum with Time Window (1 Hour)
    public List<String> findTwoSumWithTime(List<Transaction> txns, double target) {
        Map<Double, Transaction> seen = new HashMap<>();
        List<String> results = new ArrayList<>();

        // Assuming transactions are sorted by time; if not, sort first O(n log n)
        for (Transaction t : txns) {
            double complement = target - t.amount;
            if (seen.containsKey(complement)) {
                Transaction prev = seen.get(complement);
                if (Duration.between(prev.time, t.time).toMinutes() <= 60) {
                    results.add("Time-Linked Pair: ID " + prev.id + " & ID " + t.id);
                }
            }
            seen.put(t.amount, t);
        }
        return results;
    }

    // 3. K-Sum: Recursive approach using backtracking (Optimized for small K)
    public void findKSum(List<Transaction> txns, double target, int k, int start, 
                        List<Transaction> current, List<List<Transaction>> results) {
        if (k == 0) {
            if (target == 0) results.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < txns.size(); i++) {
            current.add(txns.get(i));
            findKSum(txns, target - txns.get(i).amount, k - 1, i + 1, current, results);
            current.remove(current.size() - 1);
        }
    }

    // 4. Duplicate Detection: Same amount + merchant, different accounts
    public List<String> detectDuplicates(List<Transaction> txns) {
        // Key: amount|merchant -> List of transactions
        Map<String, List<Transaction>> groups = new HashMap<>();
        List<String> alerts = new ArrayList<>();

        for (Transaction t : txns) {
            String key = t.amount + "|" + t.merchant;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<String, List<Transaction>> entry : groups.entrySet()) {
            List<Transaction> potentialDups = entry.getValue();
            if (potentialDups.size() > 1) {
                Set<String> accounts = potentialDups.stream()
                                                 .map(t -> t.account)
                                                 .collect(Collectors.toSet());
                if (accounts.size() > 1) {
                    alerts.add("Duplicate Alert: " + entry.getKey() + " across accounts: " + accounts);
                }
            }
        }
        return alerts;
    }

    public static void main(String[] args) {
        TransactionIntelligenceEngine engine = new TransactionIntelligenceEngine();
        List<Transaction> txns = Arrays.asList(
            new Transaction(1, 500, "Store A", "Acc_1", "10:00"),
            new Transaction(2, 300, "Store B", "Acc_2", "10:15"),
            new Transaction(3, 200, "Store C", "Acc_3", "10:30"),
            new Transaction(4, 500, "Store A", "Acc_4", "11:00")
        );

        System.out.println(engine.findTwoSum(txns, 500));
        System.out.println(engine.detectDuplicates(txns));
    }
}
