package com.hotel.io;

import com.hotel.model.Booking;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exports billing invoices as human-readable .txt files.
 *
 * KEY CONCEPT: Character Streams
 *   BufferedWriter wraps FileWriter to write TEXT to a file efficiently.
 *   Unlike byte streams (ObjectOutputStream), character streams handle
 *   encoding automatically — text is written as UTF-8 characters.
 *
 * Stream chain:
 *   String text  →  BufferedWriter  →  FileWriter  →  .txt file
 *
 *   FileWriter    = character stream that writes chars to a file
 *   BufferedWriter = wraps FileWriter, buffers writes in memory
 *                   so it doesn't hit the disk for every single character.
 *                   Flush happens when buffer is full or writer is closed.
 *
 * COMPARISON with byte streams used in DataManager:
 *   DataManager (byte)  → ObjectOutputStream → binary .dat file (not human-readable)
 *   InvoiceExporter (char) → BufferedWriter  → text .txt file (human-readable)
 */
public class InvoiceExporter {

    private final String invoiceDir;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public InvoiceExporter(String invoiceDir) {
        this.invoiceDir = invoiceDir;
        new File(invoiceDir).mkdirs(); // create directory if it doesn't exist
    }

    /**
     * Generates and writes a formatted invoice .txt file.
     *
     * @param booking      the booking being checked out
     * @param guestName    guest's full name
     * @param roomDetails  room summary string
     * @param amount       final calculated amount
     * @return             absolute path of the created invoice file
     */
    public String exportInvoice(Booking booking, String guestName,
                                String roomDetails, double amount) throws IOException {

        String fileName = invoiceDir + File.separator
                + "INV_" + booking.getBookingId() + ".txt";

        // CHARACTER STREAM: BufferedWriter wraps FileWriter
        // FileWriter(fileName) creates/overwrites the file
        // BufferedWriter adds an in-memory buffer for efficiency
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {

            writeLine(writer, "=".repeat(52));
            writeLine(writer, "        GRAND HOTEL — INVOICE");
            writeLine(writer, "=".repeat(52));
            writeLine(writer, "Invoice No  : INV_" + booking.getBookingId());
            writeLine(writer, "Date        : " + LocalDateTime.now().format(FMT));
            writeLine(writer, "-".repeat(52));
            writeLine(writer, "Guest Name  : " + guestName);
            writeLine(writer, "Booking ID  : " + booking.getBookingId());
            writeLine(writer, "Room        : " + roomDetails);
            writeLine(writer, "Check-In    : " + booking.getCheckInDate());
            writeLine(writer, "Check-Out   : " + booking.getCheckOutDate());
            writeLine(writer, "No. Nights  : " + booking.getNumberOfNights());
            writeLine(writer, "-".repeat(52));
            writeLine(writer, String.format("%-35s Rs. %,10.2f", "TOTAL AMOUNT:", amount));
            writeLine(writer, "=".repeat(52));
            writeLine(writer, "  Thank you for staying at Grand Hotel!");
            writeLine(writer, "=".repeat(52));

        } // BufferedWriter.close() auto-called here → flushes buffer to disk

        return fileName;
    }

    /** Writes one line + newline using writer.write() + writer.newLine() */
    private void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);    // write the text
        writer.newLine();      // write OS-appropriate newline (\n or \r\n)
    }
}
