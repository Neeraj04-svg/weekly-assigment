import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class AnalyticsDashboard {
    // 1. Frequency of page views: URL -> Total Count
    private final ConcurrentHashMap<String, AtomicLong> pageViews = new ConcurrentHashMap<>();

    // 2. Unique visitors: URL -> Set of User IDs
    private final ConcurrentHashMap<String, Set<String>> uniqueVisitors = new ConcurrentHashMap<>();

    // 3. Traffic sources: Source Name -> Total Count
    private final ConcurrentHashMap<String, AtomicLong> trafficSources = new ConcurrentHashMap<>();

    /**
     * Processes an incoming event in O(1) time.
     */
    public void processEvent(String url, String userId, String source) {
        // Increment total page views
        pageViews.computeIfAbsent(url, k -> new AtomicLong(0)).incrementAndGet();

        // Track unique visitors (using a thread-safe Set)
        uniqueVisitors.computeIfAbsent(url, k -> ConcurrentHashMap.newKeySet()).add(userId);

        // Track traffic source
        trafficSources.computeIfAbsent(source, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Gets the dashboard data. Top 10 calculation is O(N log 10).
     */
    public void getDashboard() {
        System.out.println("\n--- REAL-TIME DASHBOARD (Updates every 5s) ---");

        // Use a PriorityQueue to find the Top 10 most visited pages
        PriorityQueue<Map.Entry<String, AtomicLong>> topPagesHeap = new PriorityQueue<>(
            Comparator.comparingLong(entry -> entry.getValue().get())
        );

        for (Map.Entry<String, AtomicLong> entry : pageViews.entrySet()) {
            topPagesHeap.offer(entry);
            if (topPagesHeap.size() > 10) {
                topPagesHeap.poll(); // Remove the entry with the smallest count
            }
        }

        // Display results
        List<Map.Entry<String, AtomicLong>> topList = new ArrayList<>(topPagesHeap);
        topList.sort((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()));

        System.out.println("Top Pages:");
        for (int i = 0; i < topList.size(); i++) {
            String url = topList.get(i).getKey();
            long views = topList.get(i).getValue().get();
            int unique = uniqueVisitors.get(url).size();
            System.out.printf("%d. %s - %d views (%d unique)\n", (i + 1), url, views, unique);
        }

        System.out.println("\nTraffic Sources: " + trafficSources);
    }

    public static void main(String[] args) throws InterruptedException {
        AnalyticsDashboard dashboard = new AnalyticsDashboard();
        
        // Simulating high-traffic stream
        Runnable task = () -> {
            String[] urls = {"/home", "/news", "/sports", "/tech", "/weather"};
            String[] sources = {"Google", "Facebook", "Twitter", "Direct"};
            Random rand = new Random();
            
            for (int i = 0; i < 1000; i++) {
                dashboard.processEvent(
                    urls[rand.nextInt(urls.length)], 
                    "user_" + rand.nextInt(500), // Simulate some returning users
                    sources[rand.nextInt(sources.length)]
                );
            }
        };

        // Run multiple threads to simulate 1M+ views
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for(int i=0; i<10; i++) executor.execute(task);
        
        // Dashboard Refresh Loop
        for (int i = 0; i < 3; i++) {
            Thread.sleep(5000);
            dashboard.getDashboard();
        }

        executor.shutdown();
    }
}
