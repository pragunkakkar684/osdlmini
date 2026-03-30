package com.hotel.model;

import com.hotel.model.enums.RoomType;

/**
 * Concrete subclass for Deluxe rooms.
 *
 * INHERITANCE: extends Room — gets all fields/methods from Room + StandardRoom differences.
 * POLYMORPHISM: overrides calculateRate() with Deluxe-specific pricing.
 *
 * Deluxe rooms have:
 *   - Optional breakfast (Rs.500 per night extra)
 *   - 12% service charge (higher than Standard's 8%)
 *   - An extra field: breakfastIncluded (not in Room — unique to Deluxe)
 */
public class DeluxeRoom extends Room {

    private static final long serialVersionUID = 3L;

    private static final double SERVICE_CHARGE_RATE = 0.12; // 12%
    private static final double BREAKFAST_CHARGE    = 500.0; // per night

    // Extra field specific to DeluxeRoom — not in the parent Room class
    private boolean breakfastIncluded;

    public DeluxeRoom(String roomId, Integer floorNumber,
                      Integer maxOccupancy, String description,
                      boolean breakfastIncluded) {
        super(roomId, floorNumber, RoomType.DELUXE, maxOccupancy, description);
        this.breakfastIncluded = breakfastIncluded;
    }

    /**
     * POLYMORPHISM — different pricing from StandardRoom.
     * Adds breakfast cost on top of base price if breakfast is included.
     */
    @Override
    public double calculateRate(int nights) {
        double base      = getPricePerNight() * nights;
        double breakfast = breakfastIncluded ? BREAKFAST_CHARGE * nights : 0;
        return base + breakfast;
    }

    /**
     * POLYMORPHISM — overloaded version with service charge.
     * Calls calculateRate(nights) first, then applies 12% on top.
     */
    @Override
    public double calculateRate(int nights, boolean includeServiceCharge) {
        double base = calculateRate(nights); // reuses method above
        return includeServiceCharge ? base + (base * SERVICE_CHARGE_RATE) : base;
    }

    // Extra getter/setter for the Deluxe-specific field
    public boolean isBreakfastIncluded()                        { return breakfastIncluded; }
    public void    setBreakfastIncluded(boolean val)            { this.breakfastIncluded = val; }
}
