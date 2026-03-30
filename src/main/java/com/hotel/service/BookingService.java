package com.hotel.service;

import com.hotel.io.LogManager;
import com.hotel.io.RoomFileManager;
import com.hotel.model.Booking;
import com.hotel.model.Room;
import com.hotel.model.enums.RoomStatus;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core business logic for all booking operations.
 *
 * ─────────────────────────────────────────────────────────
 * SYNCHRONIZATION — synchronized methods:
 *
 *   bookRoom(), checkIn(), checkOut() are ALL synchronized.
 *   This means only ONE thread can execute any of these methods at a time
 *   on the SAME BookingService instance.
 *
 *   WHY? The "check-then-act" problem:
 *     Thread A: checks room R101 → AVAILABLE ✅
 *     Thread B: checks room R101 → AVAILABLE ✅   (same moment!)
 *     Thread A: books R101 ✅
 *     Thread B: books R101 ✅   ← DOUBLE BOOKING! ❌ Bug without sync.
 *
 *   With synchronized: Thread B WAITS at the method entry until Thread A
 *   finishes. By then, R101 is BOOKED and Thread B's check returns false.
 *
 *   synchronized method = synchronized(this) — the lock is on the
 *   BookingService INSTANCE itself.
 * ─────────────────────────────────────────────────────────
 */
public class BookingService {

    private final RoomRepository    roomRepo;
    private final BookingRepository bookingRepo;
    private final RoomFileManager   roomFileManager;
    private final LogManager        logger;

    // Maps roomId → its index in the RAF file, for in-place status updates
    private final ConcurrentHashMap<String, Integer> roomIndexMap = new ConcurrentHashMap<>();

    public BookingService(RoomRepository roomRepo, BookingRepository bookingRepo,
                          RoomFileManager roomFileManager, LogManager logger) {
        this.roomRepo        = roomRepo;
        this.bookingRepo     = bookingRepo;
        this.roomFileManager = roomFileManager;
        this.logger          = logger;
    }

    /**
     * Book a room — synchronized to prevent double-booking.
     * Check (is room available?) and act (mark as booked) are ATOMIC.
     */
    public synchronized String bookRoom(String guestId, String roomId,
                                        LocalDate checkIn, LocalDate checkOut) {
        Optional<Room> roomOpt = roomRepo.findById(roomId);
        if (roomOpt.isEmpty()) return "ERROR: Room not found.";

        Room room = roomOpt.get();

        // Critical check — if another thread booked between our call and here,
        // synchronized ensures we always see the up-to-date status
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            return "ERROR: Room " + roomId + " is not available ("
                    + room.getStatus().getDisplayName() + ").";
        }

        // Generate unique booking ID
        String bookingId = "BK" + UUID.randomUUID().toString()
                                      .substring(0, 6).toUpperCase();

        Booking booking = new Booking(bookingId, guestId, roomId, checkIn, checkOut);
        bookingRepo.add(booking);

        room.setStatus(RoomStatus.BOOKED);
        updateRoomInRAF(roomId, RoomStatus.BOOKED);

        logger.info("Booked: Room=" + roomId + " Guest=" + guestId
                + " BookingID=" + bookingId);
        return "SUCCESS: Booking " + bookingId + " confirmed.";
    }

    /** Async version called by BookingProcessorThread */
    public String bookRoomAsync(String guestId, String roomId,
                                String checkIn, String checkOut) {
        return bookRoom(guestId, roomId,
                LocalDate.parse(checkIn), LocalDate.parse(checkOut));
    }

    /** Synchronized — marks room OCCUPIED atomically */
    public synchronized String checkIn(String bookingId) {
        Optional<Booking> bOpt = bookingRepo.findById(bookingId);
        if (bOpt.isEmpty())        return "ERROR: Booking not found.";
        if (bOpt.get().isCheckedIn()) return "ERROR: Already checked in.";

        Booking booking = bOpt.get();
        booking.doCheckIn();

        roomRepo.findById(booking.getRoomId()).ifPresent(r -> {
            r.setStatus(RoomStatus.OCCUPIED);
            updateRoomInRAF(r.getRoomId(), RoomStatus.OCCUPIED);
        });

        logger.info("Check-in complete: Booking=" + bookingId);
        return "SUCCESS: Checked in for booking " + bookingId + ".";
    }

    /** Synchronized — finalises bill and releases room atomically */
    public synchronized String checkOut(String bookingId) {
        Optional<Booking> bOpt = bookingRepo.findById(bookingId);
        if (bOpt.isEmpty())          return "ERROR: Booking not found.";
        if (bOpt.get().isCheckedOut()) return "ERROR: Already checked out.";

        Booking booking = bOpt.get();
        Optional<Room> roomOpt = roomRepo.findById(booking.getRoomId());
        if (roomOpt.isEmpty()) return "ERROR: Room data missing.";

        Room room = roomOpt.get();
        // Polymorphism in action — calculateRate() dispatched to correct subclass
        double total = room.calculateRate(booking.getNumberOfNights().intValue(), true);

        booking.doCheckOut(total);
        room.setStatus(RoomStatus.AVAILABLE);
        updateRoomInRAF(room.getRoomId(), RoomStatus.AVAILABLE);

        logger.info("Checkout: Booking=" + bookingId
                + " Amount=Rs." + String.format("%.2f", total));
        return "SUCCESS: Checkout complete. Total: Rs." + String.format("%.2f", total);
    }

    /** Update room status in the RandomAccessFile in-place */
    private void updateRoomInRAF(String roomId, RoomStatus status) {
        Integer idx = roomIndexMap.get(roomId);
        if (idx == null) return;
        try {
            roomFileManager.updateStatus(idx, status.getDisplayName());
        } catch (Exception e) {
            logger.error("RAF update error for " + roomId + ": " + e.getMessage());
        }
    }

    public void registerRoomIndex(String roomId, int index) {
        roomIndexMap.put(roomId, index);
    }

    public List<Booking> getAllBookings() { return bookingRepo.getAll(); }
    public BookingRepository getBookingRepository() { return bookingRepo; }
}
