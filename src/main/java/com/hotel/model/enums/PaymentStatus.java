package com.hotel.model.enums;

/**
 * Enum representing the payment state of a booking.
 *
 * KEY CONCEPT: This enum is SIMPLER — no extra fields, just constants.
 * This is the most basic form of an enum, shown here for contrast
 * with RoomType and RoomStatus which carry data.
 *
 * Used in: Booking.java to track whether a bill has been paid.
 */
public enum PaymentStatus {

    PENDING,    
    PAID,       
    REFUNDED,   
    CANCELLED;  

    /**
     * Returns a user-friendly version of the constant name.
     * e.g. PENDING → "Pending"  (capitalize first letter, lowercase rest)
     */
    public String getDisplayName() {
        String name = this.name(); // "PENDING"
        return name.charAt(0) + name.substring(1).toLowerCase(); // "Pending"
    }

    @Override
    public String toString() { return getDisplayName(); }
}
