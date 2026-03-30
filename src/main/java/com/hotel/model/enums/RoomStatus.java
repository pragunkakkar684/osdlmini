package com.hotel.model.enums;

/**
 * Enum representing the current status of a hotel room.
 *
 * KEY CONCEPT: Enum used across the entire app — in the model, service,
 * and directly in the JavaFX UI for color-coding table rows.
 *
 * Notice: Each status carries a human-readable name AND a hex color string.
 * This is a great example of data travelling WITH the constant, not separately.
 */
public enum RoomStatus {

    AVAILABLE  ("Available",   "#4CAF50"),   // green
    BOOKED     ("Booked",      "#FF9800"),   // orange
    OCCUPIED   ("Occupied",    "#F44336"),   // red
    MAINTENANCE("Maintenance", "#9E9E9E");   // grey

    private final String displayName;
    private final String colorHex;

    RoomStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex    = colorHex;
    }

    public String getDisplayName() { return displayName; }
    public String getColorHex()    { return colorHex; }

    @Override
    public String toString() { return displayName; }
}
