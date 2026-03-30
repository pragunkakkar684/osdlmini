package com.hotel.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Represents a hotel guest.
 *
 * KEY CONCEPTS:
 *
 * ENCAPSULATION  — all fields private, accessed via getters/setters only.
 *
 * SERIALIZATION  — implements Serializable so Guest objects can be written
 *                  to a .dat file using ObjectOutputStream (byte stream).
 *
 * WRAPPER CLASSES — Integer for age, Double for depositAmount.
 *                   Why not int/double?
 *                   1. Wrapper classes can be null (guest may not have paid deposit yet)
 *                   2. Required for storing in generic Collections like List<Integer>
 *                   3. Provide utility methods: Integer.parseInt(), Double.valueOf() etc.
 *
 * AUTOBOXING     — Java automatically converts int ↔ Integer and double ↔ Double.
 *                  e.g. Integer age = 25;   ← autoboxing   (int → Integer)
 *                       int a = age;        ← unboxing     (Integer → int)
 */
public class Guest implements Serializable {

    private static final long serialVersionUID = 5L;

    private String    guestId;
    private String    name;
    private String    phone;
    private String    email;
    private Integer   age;            // Wrapper class — can be null
    private String    idProofType;    // e.g. "Aadhar", "Passport"
    private String    idProofNumber;
    private Double    depositAmount;  // Wrapper class — can be null (not yet paid)
    private LocalDate registrationDate;

    public Guest(String guestId, String name, String phone, String email,
                 Integer age, String idProofType, String idProofNumber) {
        this.guestId          = guestId;
        this.name             = name;
        this.phone            = phone;
        this.email            = email;
        this.age              = age;            // autoboxing if int literal passed
        this.idProofType      = idProofType;
        this.idProofNumber    = idProofNumber;
        this.depositAmount    = 0.0;            // autoboxing: double 0.0 → Double
        this.registrationDate = LocalDate.now();
    }

    // -------------------------------------------------------------------
    // Getters and Setters — Encapsulation
    // -------------------------------------------------------------------

    public String    getGuestId()                        { return guestId; }
    public void      setGuestId(String guestId)          { this.guestId = guestId; }

    public String    getName()                           { return name; }
    public void      setName(String name)                { this.name = name; }

    public String    getPhone()                          { return phone; }
    public void      setPhone(String phone)              { this.phone = phone; }

    public String    getEmail()                          { return email; }
    public void      setEmail(String email)              { this.email = email; }

    public Integer   getAge()                            { return age; }
    public void      setAge(Integer age)                 { this.age = age; }

    public String    getIdProofType()                    { return idProofType; }
    public void      setIdProofType(String idProofType)  { this.idProofType = idProofType; }

    public String    getIdProofNumber()                       { return idProofNumber; }
    public void      setIdProofNumber(String idProofNumber)   { this.idProofNumber = idProofNumber; }

    public Double    getDepositAmount()                       { return depositAmount; }
    public void      setDepositAmount(Double depositAmount)   { this.depositAmount = depositAmount; }

    public LocalDate getRegistrationDate()                          { return registrationDate; }
    public void      setRegistrationDate(LocalDate registrationDate){ this.registrationDate = registrationDate; }

    @Override
    public String toString() {
        return String.format("[%s] %s | Age: %d | Phone: %s | ID: %s - %s",
                guestId, name, age, phone, idProofType, idProofNumber);
    }
}
