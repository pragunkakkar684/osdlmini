package com.hotel.threads;

import com.hotel.io.LogManager;
import com.hotel.io.RoomFileManager;
import com.hotel.repository.RoomRepository;
import javafx.application.Platform;

/**
 * Watches the RAF file for changes and triggers a UI refresh.
 *
 * ─────────────────────────────────────────────────────────
 * MULTITHREADING: implements Runnable, plain Thread.
 *
 * SYNCHRONIZATION — volatile keyword:
 *
 *   Problem without volatile:
 *     Modern CPUs cache variables in registers or L1/L2 cache.
 *     If the main thread sets running = false, this background thread
 *     might NEVER see the change because it keeps reading its cached copy.
 *     The loop could run FOREVER even after stopGracefully() is called!
 *
 *   Solution — volatile:
 *     'volatile' tells the JVM: "never cache this variable — always read
 *     its value directly from main memory, and write to main memory immediately."
 *     Every thread always sees the LATEST value.
 *
 *   volatile vs synchronized:
 *     volatile  → guarantees VISIBILITY only (every thread sees latest value)
 *     synchronized → guarantees VISIBILITY + ATOMICITY (no partial reads/writes)
 *     Use volatile for simple flags (boolean, reference).
 *     Use synchronized when multiple operations must be atomic together.
 * ─────────────────────────────────────────────────────────
 */
public class RoomStatusUpdaterThread implements Runnable {

    // volatile — main thread writes it, background thread reads it.
    // Without volatile: background thread may cache 'true' and loop forever.
    // With volatile: background thread ALWAYS reads from main memory — sees 'false' immediately.
    private volatile boolean running = true;

    private final RoomFileManager roomFileManager;
    private final RoomRepository  roomRepo;
    private final LogManager      logger;
    private final Runnable        uiRefreshCallback;
    private Thread                thread; // reference for interrupt support
    private volatile int          lastRecordCount = 0;
    private volatile long         lastCheckMillis = -1;

    public RoomStatusUpdaterThread(RoomFileManager roomFileManager,
                                   RoomRepository roomRepo,
                                   LogManager logger,
                                   Runnable uiRefreshCallback) {
        this.roomFileManager   = roomFileManager;
        this.roomRepo          = roomRepo;
        this.logger            = logger;
        this.uiRefreshCallback = uiRefreshCallback;
    }

    @Override
    public void run() {
        thread = Thread.currentThread();
        logger.info("RoomStatusUpdaterThread started. Monitoring RAF file.");

        while (running) {           // ← volatile READ — always sees latest value
            checkForUpdates();
            try {
                Thread.sleep(10_000); // check every 10 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logger.info("RoomStatusUpdaterThread stopped gracefully.");
    }

    /**
     * Called by the MAIN thread to request a graceful stop.
     * volatile WRITE — immediately visible to background thread on next loop iteration.
     * No synchronization needed — single boolean flag, visibility is enough.
     */
    public void stopGracefully() {
        this.running = false;       // ← volatile WRITE
        if (thread != null) thread.interrupt(); // wake from sleep immediately
    }

    private void checkForUpdates() {
        try {
            int records = roomFileManager.getTotalRecords();
            lastRecordCount = records;
            lastCheckMillis = System.currentTimeMillis();
            if (records > 0) {
                // Safe UI refresh — always via Platform.runLater()
                Platform.runLater(uiRefreshCallback);
            }
        } catch (Exception e) {
            if (running) logger.error("RoomStatusUpdater error: " + e.getMessage());
        }
    }

    /** Exposed for ThreadMonitorTab — shows live thread state */
    public Thread.State getThreadState() {
        return thread != null ? thread.getState() : Thread.State.NEW;
    }

    public boolean isRunning() { return running; }

    public int getLastRecordCount() { return lastRecordCount; }

    public long getLastCheckMillis() { return lastCheckMillis; }
}
