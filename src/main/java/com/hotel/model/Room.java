package com.hotel.model;

import com.hotel.interfaces.Billable;
import com.hotel.model.enums.RoomStatus;
import com.hotel.model.enums.RoomType;

import java.io.Serializable;

/**
 * Abstract base class for all hotel rooms.
 *
 * OOP PRINCIPLES DEMONSTRATED HERE:
 *
 * 1. ABSTRACTION — 'abstract class' cannot be instantiated directly.
 * Abstract methods (calculateRate) define WHAT subclasses must do.
 *
 * 2. ENCAPSULATION — All fields are private. Outside code MUST use
 * getters/setters — cannot touch fields directly.
 *
 * 3. INHERITANCE — StandardRoom, DeluxeRoom, SuiteRoom all extend this class
 * and inherit its fields + concrete methods.
 *
 * 4. POLYMORPHISM — calculateRate() is abstract here. Each subclass overrides
 * it differently. Same method name, different behaviour.
 *
 * SERIALIZATION — implements Serializable so Room objects can be saved to
 * a .dat file using ObjectOutputStream.
 *
 * WRAPPER CLASSES — Integer and Double used instead of int/double so they can
 * be stored in Collections (e.g., List, Map) and support null.
 */
public abstract class Room implements Serializable, Billable {

    // serialVersionUID is required for Serialization — identifies the class version
    private static final long serialVersionUID = 1L;

    // ENCAPSULATION: all fields private — only accessible via getters/setters
    private String roomId;
    private Integer floorNumber; // Wrapper class: Integer (not int)
    private String description;
    private Integer maxOccupancy; // Wrapper class: Integer (not int)
    private RoomStatus status; // uses our enum
    private RoomType type; // uses our enum
    private Double pricePerNight; // Wrapper class: Double (not double)

    // Constructor — called by subclasses using super(...)
    public Room(String roomId, Integer floorNumber, RoomType type, Integer maxOccupancy, String description) {
        this.roomId = RoomIdValidator.validate(roomId);
        this.floorNumber = requirePositive(floorNumber, "Floor number");
        this.type = type;
        this.maxOccupancy = requirePositive(maxOccupancy, "Max occupancy");
        this.description = requireDescription(description);
        this.status = RoomStatus.AVAILABLE; // default status
        this.pricePerNight = type.getBaseRate(); // pulled from enum!
    }

    // -------------------------------------------------------------------------
    // ABSTRACT METHODS — subclasses MUST override these (Abstraction +
    // Polymorphism)
    // -------------------------------------------------------------------------

    /** Calculate total charge for 'nights' nights (no service charge). */
    public abstract double calculateRate(int nights);

    /**
     * Overloaded version — includes optional service charge.
     * METHOD OVERLOADING: same name, different parameters = compile-time
     * polymorphism.
     */
    public abstract double calculateRate(int nights, boolean includeServiceCharge);

    // -------------------------------------------------------------------------
    // CONCRETE METHOD — shared by all subclasses (Inheritance benefit)
    // -------------------------------------------------------------------------

    /** Billable interface implementation — one night's charge */
    @Override
    public double calculateTotalBill() {
        return pricePerNight; // subclasses override calculateRate for multi-night
    }

    @Override
    public String generateInvoiceSummary() {
        return String.format("Room %s (%s) - Floor %d - %.2f/night",
                roomId, type.getDisplayName(), floorNumber, pricePerNight);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | Floor %d | %s | Rs.%.2f/night",
                roomId, type.getDisplayName(), floorNumber,
                status.getDisplayName(), pricePerNight);
    }

    // -------------------------------------------------------------------------
    // GETTERS AND SETTERS — Encapsulation
    // -------------------------------------------------------------------------

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = RoomIdValidator.validate(roomId);
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = requirePositive(floorNumber, "Floor number");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = requireDescription(description);
    }

    public Integer getMaxOccupancy() {
        return maxOccupancy;
    }

    public void setMaxOccupancy(Integer maxOccupancy) {
        this.maxOccupancy = requirePositive(maxOccupancy, "Max occupancy");
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public Double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    private static Integer requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0.");
        }
        return value;
    }

    private static String requireDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be blank.");
        }
        return description.trim();
    }
}
