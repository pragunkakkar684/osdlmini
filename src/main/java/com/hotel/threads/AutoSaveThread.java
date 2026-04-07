package com.hotel.threads;

import com.hotel.io.DataManager;
import com.hotel.io.LogManager;
import com.hotel.model.Booking;
import com.hotel.model.Guest;
import com.hotel.model.Room;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.RoomRepository;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Daemon thread that saves all data to .dat files every 5 minutes.
 *
 * ─────────────────────────────────────────────────────────
 * MULTITHREADING — Daemon Thread:
 *   A daemon thread is a "background service" thread.
 *   When ALL non-daemon threads finish, the JVM exits immediately
 *   — it does NOT wait for daemon threads to finish.
 *   Set via: thread.setDaemon(true) BEFORE thread.start()
 *   Ideal for AutoSave — the app shouldn't stay alive just to finish a save.
 *
 * SYNCHRONIZATION — ReentrantLock (Explicit Lock):
 *   ReentrantLock is a more powerful alternative to synchronized.
 *   Key advantage used here: tryLock(timeout) — tries to acquire the lock
 *   but gives up after N seconds instead of waiting forever.
 *   This prevents AutoSave from hanging the app if a file is stuck.
 *
 * Why ReentrantLock over synchronized here?
 *   synchronized → waits FOREVER for the lock (can cause deadlock)
 *   ReentrantLock → tryLock(3, SECONDS) → gives up gracefully if disk is slow
 * ─────────────────────────────────────────────────────────
 */
public class AutoSaveThread implements Runnable {

    private final RoomRepository    roomRepo;
    private final GuestRepository   guestRepo;
    private final BookingRepository bookingRepo;
    private final DataManager<Room>    roomDM;
    private final DataManager<Guest>   guestDM;
    private final DataManager<Booking> bookingDM;
    private final LogManager        logger;

    // Shared ReentrantLock — also used by other threads that write files
    // Ensures only one thread writes to files at a time
    private final ReentrantLock fileLock;
    private final AtomicInteger successfulSaveCount = new AtomicInteger(0);
    private volatile long lastSuccessfulSaveMillis = -1;
    private volatile String lastSaveMessage = "Waiting for first autosave.";

    public AutoSaveThread(RoomRepository roomRepo, GuestRepository guestRepo,
                          BookingRepository bookingRepo,
                          DataManager<Room> roomDM, DataManager<Guest> guestDM,
                          DataManager<Booking> bookingDM,
                          LogManager logger, ReentrantLock fileLock) {
        this.roomRepo   = roomRepo;   this.guestRepo   = guestRepo;
        this.bookingRepo = bookingRepo;
        this.roomDM     = roomDM;     this.guestDM     = guestDM;
        this.bookingDM  = bookingDM;
        this.logger     = logger;     this.fileLock    = fileLock;
    }

    @Override
    public void run() {
        logger.info("AutoSaveThread started (daemon=true).");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(5 * 60 * 1000); // wait 5 minutes
                performSave();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("AutoSaveThread interrupted — performing final save.");
                performSave(); // save once more before exiting
            }
        }
    }

    /**
     * Attempts to acquire lock and save all data.
     * tryLock(3, SECONDS): waits at most 3 seconds for the lock.
     *   If acquired → saves → unlocks (always in finally).
     *   If timeout  → skips this save cycle, logs a warning.
     */
    public void performSave() {
        try {
            // REENTRANTLOCK: tryLock with timeout — won't block forever
            if (fileLock.tryLock(3, TimeUnit.SECONDS)) {
                try {
                    // Critical section — only this thread writes files now
                    roomDM.save(roomRepo.getAll());
                    guestDM.save(guestRepo.getAll());
                    bookingDM.save(bookingRepo.getAll());
                    successfulSaveCount.incrementAndGet();
                    lastSuccessfulSaveMillis = System.currentTimeMillis();
                    lastSaveMessage = "Autosave completed successfully.";
                    logger.info("AutoSave completed successfully.");
                } catch (Exception e) {
                    lastSaveMessage = "Autosave failed: " + e.getMessage();
                    logger.error("AutoSave failed: " + e.getMessage());
                } finally {
                    fileLock.unlock(); // ALWAYS unlock in finally — even if exception thrown
                }
            } else {
                logger.warn("AutoSave skipped — could not acquire file lock in 3s.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getSuccessfulSaveCount() {
        return successfulSaveCount.get();
    }

    public long getLastSuccessfulSaveMillis() {
        return lastSuccessfulSaveMillis;
    }

    public String getLastSaveMessage() {
        return lastSaveMessage;
    }
}
