package com.hotel.threads;

import com.hotel.io.LogManager;
import com.hotel.model.Booking;
import com.hotel.repository.BookingRepository;
import javafx.application.Platform;

import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Scans for due checkouts every 60 seconds and alerts the UI.
 *
 * ─────────────────────────────────────────────────────────
 * MULTITHREADING:
 * implements Runnable — separates the task from the thread.
 * Started via: Thread t = new Thread(runnable); t.start();
 * This is preferred over extending Thread directly.
 *
 * SYNCHRONIZATION: synchronized block on PriorityQueue
 * PriorityQueue is NOT thread-safe. If CheckoutReminderThread
 * reads from it while BookingService adds to it, data can corrupt.
 * The synchronized(queue) block acquires the INTRINSIC LOCK on that
 * specific object — only one thread can hold it at a time.
 *
 * THREAD LIFECYCLE:
 * NEW → (start()) → RUNNABLE → (sleep) → TIMED_WAITING → RUNNABLE → ...
 * On interrupt → TERMINATED
 * ─────────────────────────────────────────────────────────
 */
public class CheckoutReminderThread implements Runnable {

    private final BookingRepository bookingRepository;
    private final LogManager logger;
    private final Consumer<String> alertCallback; // sends message to JavaFX UI thread
    private final AtomicInteger scanCount = new AtomicInteger(0);
    private volatile int lastDueTodayCount = 0;
    private volatile long lastScanMillis = -1;

    public CheckoutReminderThread(BookingRepository bookingRepo,
            LogManager logger,
            Consumer<String> alertCallback) {
        this.bookingRepository = bookingRepo;
        this.logger = logger;
        this.alertCallback = alertCallback;
    }

    @Override
    public void run() {
        logger.info("CheckoutReminderThread started.");

        // Runs forever until interrupted — typical pattern for background threads
        while (!Thread.currentThread().isInterrupted()) {
            checkDueCheckouts();
            try {
                Thread.sleep(60_000); // pause 60 seconds — puts thread in TIMED_WAITING
            } catch (InterruptedException e) {
                // Restore interrupted flag — important! sleep() clears it on exception
                Thread.currentThread().interrupt();
                logger.info("CheckoutReminderThread shutting down.");
            }
        }
    }

    private void checkDueCheckouts() {
        PriorityQueue<Booking> queue = bookingRepository.getCheckoutQueue();
        scanCount.incrementAndGet();

        // SYNCHRONIZED BLOCK — intrinsic lock on the shared PriorityQueue object.
        // Any other thread trying to access this same queue object must WAIT
        // until this block finishes and releases the lock.
        synchronized (queue) {
            List<Booking> dueToday = bookingRepository.getDueToday();
            lastDueTodayCount = dueToday.size();
            lastScanMillis = System.currentTimeMillis();
            if (!dueToday.isEmpty()) {
                String msg = "⏰ " + dueToday.size() + " checkout(s) due today!";
                logger.warn(msg);

                // Platform.runLater() — posts task to JavaFX Application Thread.
                // You CANNOT update JavaFX UI from a background thread directly.
                // runLater() queues the update safely.
                Platform.runLater(() -> alertCallback.accept(msg));
            }
        } // lock released here automatically
    }

    public int getScanCount() {
        return scanCount.get();
    }

    public int getLastDueTodayCount() {
        return lastDueTodayCount;
    }

    public long getLastScanMillis() {
        return lastScanMillis;
    }
}
