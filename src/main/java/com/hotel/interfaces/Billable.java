package com.hotel.interfaces;

/**
 * Interface representing anything that can generate a bill.
 *
 * KEY CONCEPT: Abstraction via Interface.
 * An interface defines a CONTRACT — it says "any class that implements me
 * MUST provide these methods." It says WHAT to do, not HOW.
 *
 * In this project, BOTH Room and Booking implement Billable.
 * This means you can treat a Room or a Booking the same way
 * when you just want to calculate or display a bill.
 *
 * OOP PRINCIPLE: Abstraction — hide HOW billing works, expose only WHAT is needed.
 */
public interface Billable {

    /**
     * Calculate and return the total bill amount.
     * Every implementing class defines its own billing logic.
     */
    double calculateTotalBill();

    /**
     * Return a human-readable summary for the invoice.
     */
    String generateInvoiceSummary();
}
