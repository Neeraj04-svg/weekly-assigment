import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DNSCacheSystem {

    // Internal class to represent a DNS record
    class DNSRecord {
        String ipAddress;
        long expiryTime; // System time in ms when this expires

        DNSRecord(String ipAddress, int ttlSeconds) {
            this.ipAddress = ipAddress;
            this.expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000L);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private final int MAX_CAPACITY;
    // LRU Cache using LinkedHashMap (accessOrder = true)
    private final Map<String, DNSRecord> cache;
    
    // Metrics tracking
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public DNSCacheSystem(int capacity) {
        this.MAX_CAPACITY = capacity;
        // LinkedHashMap with accessOrder true moves "hit" items to the end
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, DNSRecord>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, DNSRecord> eldest) {
                return size() > MAX_CAPACITY;
            }
        });
    }

    /**
     * Resolves a domain name. 
     * Logic: Check Cache -> Check Expiry -> (If Fail) Query Upstream.
     */
    public String resolve(String domain) {
        long startTime = System.nanoTime();
        DNSRecord record = cache.get(domain);

        if (record != null && !record.isExpired()) {
            hits.incrementAndGet();
            long duration = System.nanoTime() - startTime;
            System.out.printf("Cache HIT for %s -> %s (Time: %.3fms)\n", domain, record.ipAddress, duration / 1_000_000.0);
            return record.ipAddress;
        }

        // Cache MISS or EXPIRED
        misses.incrementAndGet();
        if (record != null && record.isExpired()) {
            System.out.print("Cache EXPIRED... ");
            cache.remove(domain);
        } else {
            System.out.print("Cache MISS... ");
        }

        String ip = queryUpstream(domain);
        cache.put(domain, new DNSRecord(ip, 300)); // Default 300s TTL
        return ip;
    }

    private String queryUpstream(String domain) {
        // Simulating a network delay for upstream DNS
        try { Thread.sleep(50); } catch (InterruptedException e) {}
        String mockIp = "172.217." + (int)(Math.random() * 255) + "." + (int)(Math.random() * 255);
        System.out.println("Queried Upstream -> " + mockIp);
        return mockIp;
    }

    public void getCacheStats() {
        long total = hits.get() + misses.get();
        double hitRate = (total == 0) ? 0 : (hits.get() * 100.0 / total);
        System.out.println("--- Cache Statistics ---");
        System.out.println("Total Requests: " + total);
        System.out.println("Hit Rate: " + String.format("%.2f", hitRate) + "%");
        System.out.println("Current Cache Size: " + cache.size());
    }

    public static void main(String[] args) throws InterruptedException {
        DNSCacheSystem dns = new DNSCacheSystem(100);

        // First lookup (MISS)
        dns.resolve("google.com");
        
        // Second lookup (HIT)
        dns.resolve("google.com");

        // Simulate short TTL expiration logic
        dns.cache.put("expired.com", dns.new DNSRecord("1.1.1.1", 1)); // 1 second TTL
        Thread.sleep(1100); 
        dns.resolve("expired.com"); // Should trigger EXPIRED

        dns.getCacheStats();
    }
}
