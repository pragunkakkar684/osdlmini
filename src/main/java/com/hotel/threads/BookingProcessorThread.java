package com.hotel.threads;

import com.hotel.io.LogManager;
import com.hotel.service.BookingService;
import javafx.application.Platform;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Processes booking requests asynchronously using the Producer-Consumer pattern.
 *
 * ─────────────────────────────────────────────────────────
 * DESIGN: Producer-Consumer
 *   Producer = JavaFX UI thread → calls submitRequest() → adds to queue
 *   Consumer = this thread      → picks from queue → processes booking
 *
 *   The queue decouples the two sides:
 *   - UI doesn't freeze waiting for booking logic to finish
 *   - BookingService processes at its own pace
 *
 * SYNCHRONIZATION — wait() / notifyAll() on a shared object:
 *   This is the classic Java MONITOR OBJECT pattern.
 *   wait()      → consumer releases the lock AND sleeps until notified
 *   notifyAll() → producer wakes up ALL threads waiting on this object
 *
 *   Both wait() and notifyAll() MUST be called inside a synchronized block
 *   on the same object — otherwise IllegalMonitorStateException is thrown.
 *
 * This is different from the other sync tools:
 *   synchronized(queue)  → just locks for brief critical section
 *   wait()/notifyAll()   → thread SLEEPS inside the lock, wakes on signal
 * ─────────────────────────────────────────────────────────
 */
public class BookingProcessorThread implements Runnable {

    /** Simple value object to carry booking request data through the queue */
    private static class BookingRequest {
        final String guestId, roomId, checkIn, checkOut;
        BookingRequest(String guestId, String roomId, String checkIn, String checkOut) {
            this.guestId  = guestId;  this.roomId  = roomId;
            this.checkIn  = checkIn;  this.checkOut = checkOut;
        }
    }

    // Shared queue — protected by intrinsic lock for wait()/notifyAll()
    private final Queue<BookingRequest> taskQueue = new LinkedList<>();

    private final BookingService   bookingService;
    private final LogManager       logger;
    private final Consumer<String> resultCallback; // posts result back to UI
    private volatile boolean       running = true;

    public BookingProcessorThread(BookingService bookingService,
                                  LogManager logger,
                                  Consumer<String> resultCallback) {
        this.bookingService = bookingService;
        this.logger         = logger;
        this.resultCallback = resultCallback;
    }

    // -----------------------------------------------------------------------
    // PRODUCER side — called by the JavaFX UI thread
    // -----------------------------------------------------------------------

    public void submitRequest(String guestId, String roomId,
                              String checkIn, String checkOut) {
        synchronized (taskQueue) {           // acquire lock on taskQueue
            taskQueue.add(new BookingRequest(guestId, roomId, checkIn, checkOut));
            taskQueue.notifyAll();           // wake up consumer — it's been wait()-ing
        }                                    // lock released
    }

    // -----------------------------------------------------------------------
    // CONSUMER side — runs on this background thread
    // -----------------------------------------------------------------------

    @Override
    public void run() {
        logger.info("BookingProcessorThread started (Producer-Consumer).");

        while (running) {
            BookingRequest request;

            synchronized (taskQueue) {       // acquire lock to check/wait on queue
                // while loop (not if!) — guards against spurious wakeups
                while (taskQueue.isEmpty() && running) {
                    try {
                        taskQueue.wait();    // release lock + sleep until notifyAll()
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (!running) break;
                request = taskQueue.poll();  // take one item
            }                                // lock released — process OUTSIDE the lock

            if (request != null) {
                process(request);            // process without holding the lock
            }
        }
    }

    private void process(BookingRequest req) {
        String result = bookingService.bookRoomAsync(
                req.guestId, req.roomId, req.checkIn, req.checkOut);
        logger.info("BookingProcessor result: " + result);
        Platform.runLater(() -> resultCallback.accept(result));
    }

    /** Gracefully stop the consumer thread */
    public void stop() {
        running = false;
        synchronized (taskQueue) {
            taskQueue.notifyAll(); // wake thread so it can see running=false and exit
        }
    }
}
