package com.hotel.io;

import java.io.*;

/**
 * Manages room records in a flat binary file using RandomAccessFile.
 *
 * KEY CONCEPT: RandomAccessFile
 *   Unlike FileInputStream (reads sequentially from start to end),
 *   RandomAccessFile can SEEK to any byte position and read/write there.
 *   Think of it like a USB drive — you can jump to track 5 without playing 1-4.
 *
 * RECORD FORMAT: Each room occupies exactly 128 bytes in the file.
 *   roomId  : 20 chars × 2 bytes/char = 40 bytes
 *   status  : 20 chars × 2 bytes/char = 40 bytes
 *   price   : 1 double                =  8 bytes
 *   padding : filler bytes            = 40 bytes
 *                                    ─────────
 *   TOTAL                            = 128 bytes per record
 *
 * Because every record is the same size, we can jump directly to record N:
 *   position = N × 128
 * No need to read all previous records — true random access!
 */
public class RoomFileManager {

    private static final int RECORD_SIZE  = 128;
    private static final int ROOM_ID_LEN  = 20; // chars (40 bytes)
    private static final int STATUS_LEN   = 20; // chars (40 bytes)

    private final String filePath;

    public RoomFileManager(String filePath) {
        this.filePath = filePath;
        ensureDirectoryExists(filePath);
    }

    /**
     * Write a full room record at position [index] in the file.
     * Uses seek() to jump directly to the correct byte offset.
     *
     * Mode "rw" = open for reading AND writing (creates file if not exists).
     */
    public void writeRecord(int index, String roomId, String status, double price)
            throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            long position = (long) index * RECORD_SIZE;
            raf.seek(position);                              // JUMP to byte position

            raf.writeChars(pad(roomId, ROOM_ID_LEN));       // 40 bytes
            raf.writeChars(pad(status,  STATUS_LEN));        // 40 bytes
            raf.writeDouble(price);                          //  8 bytes
            raf.write(new byte[40]);                         // 40 bytes padding
        }
    }

    /**
     * Read a room record at position [index].
     * Seeks directly — does NOT read through all previous records.
     */
    public String[] readRecord(int index) throws IOException {
        File f = new File(filePath);
        if (!f.exists()) return null;

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            long position = (long) index * RECORD_SIZE;
            if (position >= raf.length()) return null;

            raf.seek(position);
            String roomId = readChars(raf, ROOM_ID_LEN).trim();
            String status = readChars(raf, STATUS_LEN).trim();
            double price  = raf.readDouble();

            return new String[]{roomId, status, String.valueOf(price)};
        }
    }

    /**
     * Update ONLY the status field of a record — in-place, without rewriting anything else.
     *
     * The offset to the status field = (index × 128) + (20 chars × 2 bytes = 40 bytes for roomId)
     * We seek directly there and overwrite only those 40 bytes.
     * The roomId and price bytes are untouched — true random access!
     */
    public void updateStatus(int index, String newStatus) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            // Skip past roomId field (20 chars × 2 bytes each = 40 bytes)
            long statusOffset = (long) index * RECORD_SIZE + (ROOM_ID_LEN * 2L);
            raf.seek(statusOffset);
            raf.writeChars(pad(newStatus, STATUS_LEN));      // overwrite only status
        }
    }

    /** Total number of records currently in the file */
    public int getTotalRecords() throws IOException {
        File f = new File(filePath);
        if (!f.exists()) return 0;
        return (int) (f.length() / RECORD_SIZE);
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    /** Pad or truncate string to exactly [length] characters */
    private String pad(String s, int length) {
        if (s.length() >= length) return s.substring(0, length);
        return s + " ".repeat(length - s.length());
    }

    /** Read [count] chars from RandomAccessFile (each char = 2 bytes) */
    private String readChars(RandomAccessFile raf, int count) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(raf.readChar()); // readChar reads 2 bytes → 1 char
        }
        return sb.toString();
    }

    private void ensureDirectoryExists(String path) {
        File parent = new File(path).getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
    }

    public String getFilePath() { return filePath; }
}
