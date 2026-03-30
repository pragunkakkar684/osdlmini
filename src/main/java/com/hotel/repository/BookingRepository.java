package com.hotel.repository;

import com.hotel.model.Booking;

import java.time.LocalDate;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Repository for Booking entities.
 *
 * KEY CONCEPTS:
 *
 * COLLECTIONS — PriorityQueue<Booking> is the star here.
 *   PriorityQueue always keeps the smallest element at the head.
 *   Since Booking implements Comparable (sorted by checkOutDate),
 *   the booking with the EARLIEST checkout is always at the front.
 *   The CheckoutReminderThread peeks at this queue every 60 seconds.
 *
 * WHY PriorityQueue HERE?
 *   We need to quickly find "what's the next checkout?" without scanning
 *   all bookings every time. PriorityQueue gives us the answer in O(1) via peek().
 *
 * GENERICS — Extends Repository<Booking> — T is fixed as Booking.
 */
public class BookingRepository extends Repository<Booking> {

    /**
     * PriorityQueue — ordered by Booking.compareTo() (earliest checkOut at head).
     * NOT thread-safe on its own — the CheckoutReminderThread uses
     * a synchronized block when accessing this queue.
     */
    private final PriorityQueue<Booking> checkoutQueue = new PriorityQueue<>();

    /** Override to also add to the PriorityQueue */
    @Override
    public void add(Booking booking) {
        super.add(booking);                // adds to HashMap
        checkoutQueue.offer(booking);      // adds to PriorityQueue (auto-sorted)
    }

    @Override
    protected String getId(Booking booking) {
        return booking.getBookingId();
    }

    /** Search by booking ID, guest ID, or room ID */
    @Override
    public List<Booking> search(String query) {
        String lq = query.toLowerCase();
        return store.values().stream()
                .filter(b -> b.getBookingId().toLowerCase().contains(lq)
                        || b.getGuestId().toLowerCase().contains(lq)
                        || b.getRoomId().toLowerCase().contains(lq))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------
    // Booking-specific queries
    // -------------------------------------------------------------------

    /** All bookings for a specific guest */
    public List<Booking> getByGuestId(String guestId) {
        return store.values().stream()
                .filter(b -> b.getGuestId().equals(guestId))
                .collect(Collectors.toList());
    }

    /** All bookings for a specific room */
    public List<Booking> getByRoomId(String roomId) {
        return store.values().stream()
                .filter(b -> b.getRoomId().equals(roomId))
                .collect(Collectors.toList());
    }

    /** Bookings whose checkout date is TODAY and haven't checked out yet */
    public List<Booking> getDueToday() {
        LocalDate today = LocalDate.now();
        return store.values().stream()
                .filter(b -> b.getCheckOutDate().equals(today) && !b.isCheckedOut())
                .collect(Collectors.toList());
    }

    /** All bookings not yet completed (active guests) */
    public List<Booking> getActiveBookings() {
        return store.values().stream()
                .filter(b -> !b.isCheckedOut())
                .collect(Collectors.toList());
    }

    /**
     * Expose the PriorityQueue to the CheckoutReminderThread.
     * The thread will synchronize on this object before accessing it.
     */
    public PriorityQueue<Booking> getCheckoutQueue() {
        return checkoutQueue;
    }

    /** Sum of all completed booking amounts — used in reports/dashboard */
    public double getTotalRevenue() {
        return store.values().stream()
                .filter(Booking::isCheckedOut)
                .mapToDouble(Booking::getTotalAmount)  // Double → double (unboxing)
                .sum();
    }
}
