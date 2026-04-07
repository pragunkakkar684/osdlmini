package com.hotel.threads;

import com.hotel.io.LogManager;
import com.hotel.model.enums.RoomStatus;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.RoomRepository;
import javafx.application.Platform;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Periodically recalculates occupancy and revenue, then pushes results to the UI.
 *
 * ─────────────────────────────────────────────────────────
 * MULTITHREADING — ScheduledExecutorService:
 *   Instead of a manual Thread + sleep loop, we use a thread pool executor.
 *   scheduleAtFixedRate() runs compute() every 30 seconds automatically.
 *   The executor manages thread creation, lifecycle, and error handling.
 *
 * SYNCHRONIZATION 1 — ConcurrentHashMap:
 *   The reportData map is shared between the executor thread (writer)
 *   and the JavaFX thread (reader for dashboard display).
 *   ConcurrentHashMap handles concurrent access without any explicit lock.
 *   It uses fine-grained bucket-level locking — much better than a
 *   fully synchronized HashMap which locks the entire map for every operation.
 *
 * SYNCHRONIZATION 2 — AtomicInteger:
 *   totalBookingsProcessed is a shared counter updated by this thread.
 *   AtomicInteger uses hardware-level Compare-And-Swap (CAS) operations.
 *   incrementAndGet() is atomic — no synchronized block needed at all.
 *   Much lighter than a full lock for simple integer increments.
 * ─────────────────────────────────────────────────────────
 */
public class OccupancyReporterThread {

    // ConcurrentHashMap — thread-safe, no synchronized needed for reads/writes
    private final ConcurrentHashMap<String, Double> reportData = new ConcurrentHashMap<>();

    // AtomicInteger — lock-free thread-safe counter
    private final AtomicInteger totalBookingsProcessed = new AtomicInteger(0);

    private final RoomRepository    roomRepo;
    private final BookingRepository bookingRepo;
    private final LogManager        logger;

    // Callback to push updated data to the JavaFX Dashboard UI
    private final Consumer<ConcurrentHashMap<String, Double>> uiCallback;

    private ScheduledExecutorService executor;
    private volatile Thread workerThread;
    private final AtomicInteger reportCount = new AtomicInteger(0);
    private volatile long lastReportMillis = -1;

    public OccupancyReporterThread(RoomRepository roomRepo,
                                   BookingRepository bookingRepo,
                                   LogManager logger,
                                   Consumer<ConcurrentHashMap<String, Double>> uiCallback) {
        this.roomRepo   = roomRepo;
        this.bookingRepo = bookingRepo;
        this.logger     = logger;
        this.uiCallback = uiCallback;
    }

    /**
     * Starts the scheduled executor.
     * The thread factory gives the thread a readable name — visible in Thread Monitor UI.
     */
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "OccupancyReporterThread");
            workerThread = t;
            t.setDaemon(true); // daemon — won't prevent app shutdown
            return t;
        });

        // Run compute() immediately (delay=0), then every 30 seconds
        executor.scheduleAtFixedRate(this::compute, 0, 30, TimeUnit.SECONDS);
        logger.info("OccupancyReporterThread (ScheduledExecutorService) started.");
    }

    /** All stats calculations happen here — runs on the executor thread */
    private void compute() {
        int    total       = roomRepo.count();
        long   occupied    = roomRepo.countByStatus(RoomStatus.OCCUPIED);
        long   booked      = roomRepo.countByStatus(RoomStatus.BOOKED);
        long   available   = roomRepo.countByStatus(RoomStatus.AVAILABLE);
        double revenue     = bookingRepo.getTotalRevenue();
        double occupancyPct = total > 0 ? ((double)(occupied + booked) / total) * 100 : 0;

        // AtomicInteger.incrementAndGet() — atomic, no synchronized block needed
        totalBookingsProcessed.incrementAndGet();
        reportCount.incrementAndGet();
        lastReportMillis = System.currentTimeMillis();

        // ConcurrentHashMap.put() — thread-safe without any lock
        reportData.put("total",      (double) total);
        reportData.put("occupied",   (double) occupied);
        reportData.put("booked",     (double) booked);
        reportData.put("available",  (double) available);
        reportData.put("revenue",    revenue);
        reportData.put("occupancyPct", occupancyPct);

        logger.info(String.format("Report: Occupancy=%.1f%%, Revenue=Rs.%.2f",
                occupancyPct, revenue));

        // Push to JavaFX thread — UI update must happen on Application Thread
        Platform.runLater(() -> uiCallback.accept(reportData));
    }

    public void stop() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    public boolean isActive() {
        return executor != null && !executor.isShutdown();
    }

    public Thread.State getThreadState() {
        return workerThread != null ? workerThread.getState() : Thread.State.NEW;
    }

    // Getters — used by ThreadMonitorTab
    public ConcurrentHashMap<String, Double> getReportData()       { return reportData; }
    public int getTotalBookingsProcessed()  { return totalBookingsProcessed.get(); }
    public int getReportCount()            { return reportCount.get(); }
    public long getLastReportMillis()      { return lastReportMillis; }
}
