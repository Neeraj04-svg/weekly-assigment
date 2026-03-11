import java.util.concurrent.ConcurrentHashMap;

public class DistributedRate {

    // Internal class to represent the state of a specific client's bucket
    private static class TokenBucket {
        final long maxTokens;
        final double refillRatePerMs;

        double currentTokens;
        long lastRefillTimestamp;

        TokenBucket(long maxTokens, long tokensPerHour) {
            this.maxTokens = maxTokens;
            // Calculate how many tokens are added per millisecond
            this.refillRatePerMs = (double) tokensPerHour / (60 * 60 * 1000);
            this.currentTokens = maxTokens;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        // Synchronized to ensure atomic refill and consumption per client
        public synchronized boolean tryConsume() {
            refill();
            if (currentTokens >= 1.0) {
                currentTokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTimestamp;

            // Add tokens based on elapsed time
            double tokensToAdd = timePassed * refillRatePerMs;
            currentTokens = Math.min(maxTokens, currentTokens + tokensToAdd);
            lastRefillTimestamp = now;
        }

        public long getResetTimeSeconds() {
            // Estimate seconds until bucket is full again
            double missingTokens = maxTokens - currentTokens;
            return (long) (missingTokens / (refillRatePerMs * 1000));
        }
    }

    // Map: ClientID -> Their specific token bucket
    private final ConcurrentHashMap<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();
    private final long CAPACITY = 1000;
    private final long HOURLY_LIMIT = 1000;

    public String checkRateLimit(String clientId) {
        TokenBucket bucket = clientBuckets.computeIfAbsent(clientId,
                k -> new TokenBucket(CAPACITY, HOURLY_LIMIT));

        if (bucket.tryConsume()) {
            return "Allowed (" + (int)Math.floor(bucket.currentTokens) + " requests remaining)";
        } else {
            return "Denied (0 requests remaining, retry after " + bucket.getResetTimeSeconds() + "s)";
        }
    }

    public static void main(String[] args) {
        DistributedRate limiter = new DistributedRate();
        String testClient = "abc123";

        // Simulate rapid requests
        for (int i = 0; i < 5; i++) {
            System.out.println("Request " + (i + 1) + ": " + limiter.checkRateLimit(testClient));
        }

        // Simulate a client hitting the limit
        System.out.println("\n--- Simulating Limit Hit ---");
        TokenBucket clientData = limiter.clientBuckets.get(testClient);
        clientData.currentTokens = 0; // Force empty for demonstration

        System.out.println(limiter.checkRateLimit(testClient));
    }
}