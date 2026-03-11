import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FlashSaleManager {
    // Inventory: SKU -> Current Stock (Atomic for thread safety)
    private final Map<String, AtomicInteger> inventory = new ConcurrentHashMap<>();
    
    // Waiting List: SKU -> Queue of User IDs (FIFO)
    private final Map<String, Queue<Long>> waitingLists = new ConcurrentHashMap<>();

    /**
     * Initializes a product in the system.
     */
    public void addProduct(String sku, int initialStock) {
        inventory.put(sku, new AtomicInteger(initialStock));
        waitingLists.put(sku, new ConcurrentLinkedQueue<>());
    }

    /**
     * Checks stock in O(1) time.
     */
    public int checkStock(String sku) {
        AtomicInteger stock = inventory.get(sku);
        return (stock != null) ? stock.get() : 0;
    }

    /**
     * Attempts to purchase an item. 
     * Uses atomic decrements to prevent overselling.
     */
    public String purchaseItem(String sku, long userId) {
        AtomicInteger stock = inventory.get(sku);
        
        if (stock == null) return "Product not found";

        // Try to decrement stock only if it's > 0
        // updateAndGet ensures the operation is atomic
        int remaining = stock.getAndUpdate(current -> current > 0 ? current - 1 : 0);

        if (remaining > 0) {
            return "Success! Order placed. Remaining: " + (remaining - 1);
        } else {
            // Stock is 0, add to waiting list
            Queue<Long> queue = waitingLists.get(sku);
            queue.offer(userId);
            
            // Calculate position in queue (Approximation in high-concurrency)
            int position = queue.size(); 
            return "Sold out! Added to waiting list at position #" + position;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        FlashSaleManager sale = new FlashSaleManager();
        String product = "IPHONE15_PRO";
        sale.addProduct(product, 100);

        // Simulate 50,000 concurrent purchase attempts using an ExecutorService
        ExecutorService executor = Executors.newFixedThreadPool(100);
        for (int i = 1; i <= 150; i++) {
            final long userId = i;
            executor.execute(() -> {
                String result = sale.purchaseItem(product, userId);
                if (userId % 50 == 0 || userId > 100) { // Log a sample of attempts
                    System.out.println("User " + userId + ": " + result);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
