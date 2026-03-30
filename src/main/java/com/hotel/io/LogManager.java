package com.hotel.io;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Appends structured log entries to hotel.log using PrintWriter.
 *
 * KEY CONCEPT: Character Streams — PrintWriter in APPEND mode.
 *
 * Two things make this file distinct from InvoiceExporter:
 *
 * 1. APPEND MODE — FileWriter(path, true) opens in append mode.
 *    Every new log line is added to the END of the file.
 *    InvoiceExporter uses FileWriter(path) — creates/overwrites the file each time.
 *
 * 2. PrintWriter — provides convenient println() and printf() for formatted text.
 *    BufferedWriter only gives write() and newLine().
 *    PrintWriter is easier for line-by-line logging.
 *
 * Stream chain:
 *   String  →  PrintWriter  →  FileWriter(append=true)  →  hotel.log
 *
 * SYNCHRONIZED — The log() method is synchronized because multiple threads
 *   (CheckoutReminderThread, AutoSaveThread etc.) all call logger.info() at the
 *   same time. Without synchronization, log lines could interleave/corrupt.
 */
public class LogManager {

    private final String logFilePath;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogManager(String logFilePath) {
        this.logFilePath = logFilePath;
        ensureDirectoryExists(logFilePath);
    }

    /**
     * Appends one log line to the file.
     *
     * synchronized → only one thread writes at a time.
     * Without this, two threads could write simultaneously and produce:
     *   "[INFO] Booking[WARN] AutoSave done"  ← garbled, interleaved output
     */
    public synchronized void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(FMT);
        String entry = String.format("[%s] [%-5s] %s", timestamp, level, message);

        // PrintWriter wraps FileWriter in APPEND MODE (true = append, not overwrite)
        // Auto-flush is disabled here — PrintWriter closes and flushes at end of try block
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFilePath, true))) {
            pw.println(entry); // println = print + newline — cleaner than write() + newLine()
        } catch (IOException e) {
            System.err.println("LogManager error: " + e.getMessage());
        }
    }

    // Convenience methods — callers don't need to know the level string
    public void info(String message)  { log("INFO",  message); }
    public void warn(String message)  { log("WARN",  message); }
    public void error(String message) { log("ERROR", message); }

    private void ensureDirectoryExists(String path) {
        File parent = new File(path).getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
    }
}
