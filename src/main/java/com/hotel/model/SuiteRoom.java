package com.hotel.model;

import com.hotel.model.enums.RoomType;

/**
 * Concrete subclass for Suite rooms — the premium tier.
 *
 * INHERITANCE: extends Room — gets everything Room has.
 * POLYMORPHISM: calculateRate() has the most complex pricing of all 3 room types.
 *
 * Suite extras:
 *   - Butler service: Rs.1500/night
 *   - Minibar access: Rs.800/night
 *   - Luxury tax: 18% (like GST on premium services)
 *
 * TWO extra fields (butlerService + minibarAccess) — both absent from Room.
 */
public class SuiteRoom extends Room {

    private static final long serialVersionUID = 4L;

    private static final double LUXURY_TAX_RATE  = 0.18;   // 18%
    private static final double BUTLER_CHARGE    = 1500.0; // per night
    private static final double MINIBAR_CHARGE   = 800.0;  // per night

    // Extra fields unique to SuiteRoom
    private boolean butlerService;
    private boolean minibarAccess;

    public SuiteRoom(String roomId, Integer floorNumber, Integer maxOccupancy,
                     String description, boolean butlerService, boolean minibarAccess) {
        super(roomId, floorNumber, RoomType.SUITE, maxOccupancy, description);
        this.butlerService = butlerService;
        this.minibarAccess = minibarAccess;
    }

    /**
     * POLYMORPHISM — most complex pricing among all 3 room types.
     * Base price + optional butler + optional minibar.
     */
    @Override
    public double calculateRate(int nights) {
        double base    = getPricePerNight() * nights;
        double butler  = butlerService ? BUTLER_CHARGE * nights : 0;
        double minibar = minibarAccess  ? MINIBAR_CHARGE * nights : 0;
        return base + butler + minibar;
    }

    /**
     * Applies 18% luxury tax to the full bill.
     */
    @Override
    public double calculateRate(int nights, boolean includeServiceCharge) {
        double base = calculateRate(nights);
        return includeServiceCharge ? base + (base * LUXURY_TAX_RATE) : base;
    }

    // Getters and setters for Suite-specific fields
    public boolean isButlerService()             { return butlerService; }
    public void    setButlerService(boolean val) { this.butlerService = val; }

    public boolean isMinibarAccess()             { return minibarAccess; }
    public void    setMinibarAccess(boolean val) { this.minibarAccess = val; }
}
