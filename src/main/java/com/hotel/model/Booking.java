package com.hotel.model;

import com.hotel.interfaces.Billable;
import com.hotel.model.enums.PaymentStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Represents a hotel booking — links a Guest to a Room for a date range.
 *
 * KEY CONCEPTS:
 *
 * IMPLEMENTS Billable — Booking can generate its own invoice/bill.
 * Same interface as Room, but different implementation.
 *
 * IMPLEMENTS Comparable — Bookings can be naturally sorted by checkout date.
 * Required by PriorityQueue<Booking> in BookingRepository.
 *
 * SERIALIZABLE — Bookings are saved/loaded from bookings.dat file.
 *
 * WRAPPER CLASSES — Double for totalAmount, Long for numberOfNights.
 *
 * ENCAPSULATION — private fields, public methods only.
 */
public class Booking implements Serializable, Billable, Comparable<Booking> {

    private static final long serialVersionUID = 6L;

    private String bookingId;
    private String guestId;
    private String roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private PaymentStatus paymentStatus;
    private Double totalAmount; // Wrapper class
    private Long numberOfNights; // Wrapper class
    private boolean checkedIn;
    private boolean checkedOut;
    private LocalDate actualCheckInDate;

    public Booking(String bookingId, String guestId, String roomId,
            LocalDate checkInDate, LocalDate checkOutDate) {
        this.bookingId = bookingId;
        this.guestId = guestId;
        this.roomId = roomId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.paymentStatus = PaymentStatus.PENDING;
        this.totalAmount = 0.0;
        this.checkedIn = false;
        this.checkedOut = false;
        this.actualCheckInDate = null;

        // ChronoUnit.DAYS calculates the number of days between two LocalDates
        this.numberOfNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    /** Called when guest physically arrives at the hotel */
    public void doCheckIn() {
        doCheckIn(LocalDate.now());
    }

    /** Called when guest physically arrives at the hotel with an explicit date */
    public void doCheckIn(LocalDate checkInDate) {
        this.checkedIn = true;
        this.actualCheckInDate = checkInDate;
    }

    /** Called at checkout — finalises the amount and marks payment done */
    public void doCheckOut(double finalAmount) {
        this.checkedOut = true;
        this.totalAmount = finalAmount; // autoboxing: double → Double
        this.paymentStatus = PaymentStatus.PAID;
    }

    // -------------------------------------------------------------------
    // Billable interface — this is what makes Booking "Billable"
    // -------------------------------------------------------------------

    @Override
    public double calculateTotalBill() {
        return totalAmount != null ? totalAmount : 0.0; // unboxing: Double → double
    }

    @Override
    public String generateInvoiceSummary() {
        return String.format(
                "Booking: %s | Room: %s | Guest: %s | Nights: %d | Amount: Rs.%.2f | %s",
                bookingId, roomId, guestId, numberOfNights, totalAmount,
                paymentStatus.getDisplayName());
    }

    // -------------------------------------------------------------------
    // Comparable<Booking> — sorts by checkout date (earliest first)
    // Used by PriorityQueue to always surface the next checkout at the top
    // -------------------------------------------------------------------

    @Override
    public int compareTo(Booking other) {
        return this.checkOutDate.compareTo(other.checkOutDate);
    }

    // -------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate d) {
        this.checkInDate = d;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate d) {
        this.checkOutDate = d;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus p) {
        this.paymentStatus = p;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getNumberOfNights() {
        return numberOfNights;
    }

    public void setNumberOfNights(Long n) {
        this.numberOfNights = n;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public boolean isCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

    public LocalDate getActualCheckInDate() {
        return actualCheckInDate;
    }

    public void setActualCheckInDate(LocalDate date) {
        this.actualCheckInDate = date;
    }

    @Override
    public String toString() {
        return generateInvoiceSummary();
    }
}
