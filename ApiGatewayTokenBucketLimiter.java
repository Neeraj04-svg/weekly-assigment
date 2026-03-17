import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ApiGatewayTokenBucketLimiter {

    // Simulates the shared datastore. ConcurrentHashMap handles client collisions safely.
    private final Map<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();
    
    private final long maxRequests;
    private final long windowMillis;

    public ApiGatewayTokenBucketLimiter(long maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000;
    }

    /**
     * Checks if a client is allowed to make a request.
     */
    public String checkRateLimit(String clientId) {
        // computeIfAbsent is thread-safe and ensures we only create one bucket per client
        TokenBucket bucket = clientBuckets.computeIfAbsent(
            clientId, 
            k -> new TokenBucket(maxRequests, windowMillis)
        );

        return bucket.tryConsume();
    }

    /**
     * Retrieves the current status for a given client.
     */
    public String getRateLimitStatus(String clientId) {
        TokenBucket bucket = clientBuckets.get(clientId);
        if (bucket == null) {
            return String.format("{used: 0, limit: %d, reset: %d}", 
                    maxRequests, System.currentTimeMillis() + windowMillis);
        }
        return bucket.getStatus();
    }

    // --- Inner Class: The Token Bucket ---
    
    private static class TokenBucket {
        private final long maxTokens;
        private final long refillPeriodMs;
        
        // Using double for fractional token accumulation over time
        private double availableTokens;
        private long lastRefillTime;

        public TokenBucket(long maxTokens, long refillPeriodMs) {
            this.maxTokens = maxTokens;
            this.refillPeriodMs = refillPeriodMs;
            this.availableTokens = maxTokens;
            this.lastRefillTime = System.currentTimeMillis();
        }

        /**
         * Synchronized to prevent race conditions if the same client 
         * sends concurrent requests. Since locking is per-client, 
         * it meets the <1ms latency goal globally.
         */
        public synchronized String tryConsume() {
            refill();

            if (availableTokens >= 1.0) {
                availableTokens -= 1.0;
                return String.format("Allowed (%d requests remaining)", (long) availableTokens);
            } else {
                // Calculate time until at least 1 token is available
                long timePerTokenMs = refillPeriodMs / maxTokens;
                long retryAfterSec = (timePerTokenMs - (System.currentTimeMillis() - lastRefillTime)) / 1000;
                
                // Ensure we don't return 0 or negative due to rounding
                retryAfterSec = Math.max(1, retryAfterSec); 
                
                return String.format("Denied (0 requests remaining, retry after %ds)", retryAfterSec);
            }
        }

        public synchronized String getStatus() {
            refill();
            long used = maxTokens - (long) availableTokens;
            // Approximate reset time based on when the bucket would be completely full again
            long timeToFullMs = (long) (((maxTokens - availableTokens) / maxTokens) * refillPeriodMs);
            long resetTimestamp = (System.currentTimeMillis() + timeToFullMs) / 1000; // Unix epoch
            
            return String.format("{used: %d, limit: %d, reset: %d}", used, maxTokens, resetTimestamp);
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long timePassedMs = now - lastRefillTime;

            // How many tokens were generated in the elapsed time?
            double tokensToAdd = ((double) timePassedMs / refillPeriodMs) * maxTokens;

            if (tokensToAdd > 0) {
                availableTokens = Math.min(maxTokens, availableTokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }

    // --- Testing the Implementation ---
    public static void main(String[] args) throws InterruptedException {
        // 1000 requests per 3600 seconds (1 hour)
        ApiGatewayTokenBucketLimiter limiter = new ApiGatewayTokenBucketLimiter(1000, 3600);
        String clientId = "abc123";

        System.out.println(limiter.checkRateLimit(clientId)); // Allowed (999...)
        System.out.println(limiter.checkRateLimit(clientId)); // Allowed (998...)
        
        // Simulating immediate consumption to trigger a deny
        TokenBucket bucket = limiter.clientBuckets.get(clientId);
        synchronized(bucket) {
             // Force bucket empty for testing
             bucket.availableTokens = 0.5;
        }
        
        System.out.println(limiter.checkRateLimit(clientId)); // Denied
        System.out.println(limiter.getRateLimitStatus(clientId)); // Status JSON
    }
}
