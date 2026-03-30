package com.hotel.model.enums;

/**
 * Enum representing the type of hotel room.
 *
 * KEY CONCEPT: Enum with fields and constructor.
 * An enum is NOT just a list of constants — each constant can carry data.
 *
 * VIVA TIP: enums in Java are full classes. They can have:
 *   - Fields
 *   - Constructors (always private)
 *   - Methods
 *   - Implement interfaces
 */
public enum RoomType {

    // Each constant calls the constructor below with its own values
    STANDARD("Standard", 1500.0),
    DELUXE("Deluxe",     3000.0),
    SUITE("Suite",       6000.0);

    // Fields — stored per constant
    private final String displayName;
    private final double baseRate;

    // Constructor — private by default in enums (cannot use 'new RoomType(...)')
    RoomType(String displayName, double baseRate) {
        this.displayName = displayName;
        this.baseRate    = baseRate;
    }

    // Getters
    public String getDisplayName() { return displayName; }
    public double getBaseRate()    { return baseRate; }

    @Override
    public String toString() { return displayName; }
}
