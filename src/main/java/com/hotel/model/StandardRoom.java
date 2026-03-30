package com.hotel.model;

import com.hotel.model.enums.RoomType;

/**
 * Concrete subclass for Standard rooms.
 *
 * KEY CONCEPT: Inheritance — extends Room and inherits ALL its fields and methods.
 * KEY CONCEPT: Polymorphism — overrides calculateRate() with Standard-specific logic.
 *
 * StandardRoom does NOT redeclare roomId, status, floorNumber etc.
 * It gets them for FREE from Room via inheritance.
 *
 * Service charge: 8% (lowest — standard tier)
 */
public class StandardRoom extends Room {

    private static final long serialVersionUID = 2L;

    // Standard rooms charge 8% service charge on top of base price
    private static final double SERVICE_CHARGE_RATE = 0.08;

    /**
     * Constructor calls super() — passes common data up to Room's constructor.
     * RoomType.STANDARD is hardcoded — a StandardRoom is always of type STANDARD.
     */
    public StandardRoom(String roomId, Integer floorNumber,
                        Integer maxOccupancy, String description) {
        super(roomId, floorNumber, RoomType.STANDARD, maxOccupancy, description);
    }

    /**
     * POLYMORPHISM — overrides abstract method from Room.
     * Simple calculation: just base price × nights. No extras.
     */
    @Override
    public double calculateRate(int nights) {
        return getPricePerNight() * nights;
    }

    /**
     * POLYMORPHISM — overrides the overloaded abstract method from Room.
     * METHOD OVERLOADING + OVERRIDING together:
     *   - Overloading  = same name, different parameters (defined in Room)
     *   - Overriding   = subclass provides its own body (done here)
     */
    @Override
    public double calculateRate(int nights, boolean includeServiceCharge) {
        double base = calculateRate(nights);               // reuse the method above
        if (includeServiceCharge) {
            return base + (base * SERVICE_CHARGE_RATE);   // add 8%
        }
        return base;
    }
}
