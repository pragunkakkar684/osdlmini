# Hotel Management System (osdlmini)

A comprehensive JavaFX desktop application for hotel management that demonstrates advanced Java programming concepts in a real-world scenario. The system provides complete room management, guest management, booking operations, billing, and reporting capabilities—all with file-based persistence (no database required).

---

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Java Concepts Demonstrated](#java-concepts-demonstrated)
4. [Architecture](#architecture)
5. [Project Structure](#project-structure)
6. [Setup & Build](#setup--build)
7. [Running the Application](#running-the-application)
8. [Key Components](#key-components)
9. [Threading Model](#threading-model)
10. [Data Persistence](#data-persistence)
11. [Usage Guide](#usage-guide)
12. [Technology Stack](#technology-stack)

---

## Overview

The Hotel Management System is a full-featured desktop application designed to manage all aspects of hotel operations. It serves as a practical learning tool demonstrating enterprise-level Java patterns and best practices, including object-oriented design, multi-threading, file I/O, serialization, and GUI development with JavaFX.

**Key Characteristics:**
- **Standalone Application**: No external database—all data stored in local files
- **Multi-threaded**: Features background threads for auto-save, checkout reminders, and reporting
- **Type-safe**: Leverages Java generics and enums throughout
- **Full Persistence**: Complete serialization/deserialization of all entities
- **GUI-First**: Rich JavaFX interface with tabbed navigation and real-time updates

---

## Features

| Feature | Description |
|---------|-------------|
| **Room Management** | Create, update, delete, and manage hotel rooms with different types (Standard, Deluxe, Suite) |
| **Guest Management** | Register new guests, search by name/ID, maintain guest profiles |
| **Room Booking** | Book rooms for date ranges, check real-time availability |
| **Check-in/Checkout** | Process guest arrivals and departures with status tracking |
| **Billing & Invoicing** | Automatic bill calculation, invoice generation, and export to `.txt` format |
| **Reports Dashboard** | View occupancy statistics, revenue summaries, and booking history |
| **Checkout Reminders** | Background thread sends notifications for upcoming checkouts |
| **Auto-Save** | Periodic background saving of all data to prevent loss |
| **Search Functionality** | Find guests and bookings by various criteria |
| **Data Persistence** | All data automatically saved and restored on application startup |

---

## Java Concepts Demonstrated

### Object-Oriented Programming

| Concept | Implementation | Location |
|---------|---|----------|
| **Encapsulation** | Private fields with public getters/setters; data hiding and controlled access | `Room.java`, `Guest.java`, `Booking.java` |
| **Inheritance** | `Room` abstract base class with three concrete subclasses: `StandardRoom`, `DeluxeRoom`, `SuiteRoom` | `model/` directory |
| **Polymorphism** | Overridden `calculateRate()` methods, method overloading in service layer | `Room.java` (abstract), subclasses |
| **Abstraction** | Abstract class `Room` and interfaces `Billable`, `Searchable<T>` define contracts | `model/`, `interfaces/` |

### Collections & Data Structures

| Concept | Usage |
|---------|-------|
| **Generics** | `Repository<T>`, `DataManager<T extends Serializable>`, generic collections |
| **HashMap** | `roomId → Room` mappings for fast O(1) lookups |
| **ArrayList** | Dynamic lists for guests, bookings, and search results |
| **TreeMap** | Sorted reports and date-based queries |
| **PriorityQueue** | Sorted checkout reminders by date |
| **ConcurrentHashMap** | Thread-safe collections in multi-threaded contexts |

### Enumerations

| Enum | Values | Purpose |
|------|--------|---------|
| `RoomStatus` | AVAILABLE, BOOKED, OCCUPIED, MAINTENANCE | Tracks room state |
| `RoomType` | STANDARD, DELUXE, SUITE | Room classification with base rates |
| `PaymentStatus` | PENDING, PAID, REFUNDED | Booking payment tracking |

### Wrapper Classes

- `Integer` for floor numbers and occupancy (supports null and collections)
- `Double` for prices, amounts, and calculations (autoboxing/unboxing demonstrated)
- `Long` for duration calculations in bookings

### I/O Streams

| Stream Type | Purpose | Location |
|-------------|---------|----------|
| **Byte Streams** | Binary serialization of objects (ObjectOutputStream/ObjectInputStream) | `DataManager.java` |
| **Character Streams** | Invoice export (BufferedWriter/FileWriter) and logging (PrintWriter) | `InvoiceExporter.java`, `LogManager.java` |
| **Random Access File** | Direct read/write to specific room records | `RoomFileManager.java` |

### Serialization

- All model classes (`Room`, `Guest`, `Booking`) implement `Serializable`
- Objects saved to `.dat` files and restored on application startup
- Serialization handles complex object graphs with proper versioning

### Concurrency & Threading

| Feature | Details |
|---------|---------|
| **Thread Creation** | `Thread`, `Runnable` interface implementations |
| **Synchronization** | `synchronized` methods, `synchronized` blocks for thread safety |
| **Locks** | `ReentrantLock` for file coordinate access across threads |
| **Thread Safety** | `volatile` keyword, `AtomicInteger` for counters, `ConcurrentHashMap` |
| **Thread Control** | `wait()`, `notify()` for inter-thread communication; daemon threads |
| **ExecutorService** | Potential for thread pooling in future enhancements |

### Multithreading Threads

1. **CheckoutReminderThread**: Monitors bookings for checkout reminders
2. **AutoSaveThread**: Periodically saves all data to prevent loss (daemon thread)
3. **BookingProcessorThread**: Processes booking workflows asynchronously
4. **RoomStatusUpdaterThread**: Updates room states based on bookings
5. **OccupancyReporterThread**: Generates occupancy statistics

---

## Architecture

### Layered Design Pattern

```
┌─────────────────────────┐
│     UI Layer            │  ← JavaFX GUI (MainWindow, Tabs)
│   (ui package)          │
└────────────┬────────────┘
             │
┌────────────▼────────────┐
│   Service Layer         │  ← Business logic (BookingService, etc.)
│   (service package)     │
└────────────┬────────────┘
             │
┌────────────▼────────────┐
│   Repository Layer      │  ← Data access abstraction
│   (repository package)  │
└────────────┬────────────┘
             │
┌────────────▼────────────┐
│   Model Layer           │  ← Domain objects
│   (model package)       │
└────────────┬────────────┘
             │
┌────────────▼────────────┐
│   I/O Layer             │  ← File handling & persistence
│   (io package)          │
└─────────────────────────┘
```

### Key Design Patterns

- **Repository Pattern**: Abstract data access through `Repository<T>` base class
- **Dependency Injection**: Manual wiring in `Main.java`
- **Observer Pattern**: JavaFX Observable lists trigger UI updates
- **Factory Pattern**: `Room` subclass creation based on `RoomType` enum
- **Template Method**: Base service classes define workflow steps

---

## Project Structure

```
osdlmini/
├── README.md                              ← This file
├── implementation_plan.md                 ← Detailed feature specification
├── pom.xml                               ← Maven configuration
│
├── src/main/java/com/hotel/
│   ├── Main.java                         ← JavaFX Application entry point
│   │
│   ├── model/                            ← Domain entities (Serializable)
│   │   ├── Room.java                     ← Abstract base class
│   │   ├── StandardRoom.java             ← Budget room (extends Room)
│   │   ├── DeluxeRoom.java               ← Premium room (extends Room)
│   │   ├── SuiteRoom.java                ← Luxury room (extends Room)
│   │   ├── Guest.java                    ← Guest profile
│   │   ├── Booking.java                  ← Room reservation
│   │   └── enums/
│   │       ├── RoomStatus.java           ← AVAILABLE, BOOKED, OCCUPIED, MAINTENANCE
│   │       ├── RoomType.java             ← STANDARD, DELUXE, SUITE (with base rates)
│   │       └── PaymentStatus.java        ← PENDING, PAID, REFUNDED
│   │
│   ├── interfaces/                       ← Contracts for polymorphism
│   │   ├── Billable.java                 ← Provides calculateTotalBill() contract
│   │   └── Searchable.java               ← Generic search contract
│   │
│   ├── repository/                       ← Data access abstraction
│   │   ├── Repository.java               ← Generic base repository
│   │   ├── RoomRepository.java           ← Room CRUD operations
│   │   ├── GuestRepository.java          ← Guest CRUD operations
│   │   └── BookingRepository.java        ← Booking CRUD operations
│   │
│   ├── service/                          ← Business logic layer
│   │   └── BookingService.java           ← Booking workflows (check-in, checkout, billing)
│   │
│   ├── threads/                          ← Background processing
│   │   ├── CheckoutReminderThread.java   ← Monitors upcoming checkouts
│   │   ├── AutoSaveThread.java           ← Periodic data persistence (daemon)
│   │   ├── BookingProcessorThread.java   ← Asynchronous booking processing
│   │   ├── RoomStatusUpdaterThread.java  ← Updates room states
│   │   └── OccupancyReporterThread.java  ← Generates occupancy stats
│   │
│   ├── io/                               ← File I/O and persistence
│   │   ├── DataManager.java              ← Generic serialization/deserialization
│   │   ├── RoomFileManager.java          ← RandomAccessFile for room data
│   │   ├── InvoiceExporter.java          ← Export invoices as text files
│   │   └── LogManager.java              ← Application logging
│   │
│   └── ui/                               ← JavaFX GUI components
│       └── MainWindow.java               ← Main application window (TabPane-based)
│
├── src/main/resources/
│   └── styles/main.css                   ← JavaFX CSS styling
│
├── data/                                 ← Runtime data files (auto-created)
│   ├── rooms.dat                         ← Serialized Room objects
│   ├── guests.dat                        ← Serialized Guest objects
│   ├── bookings.dat                      ← Serialized Booking objects
│   ├── rooms.raf                         ← RandomAccessFile for room updates
│   ├── invoices/                         ← Exported invoice text files
│   └── hotel.log                         ← Application log file
│
└── target/                               ← Maven build output (compiled classes)
```

---

## Setup & Build

### Prerequisites

- **Java 17 or higher** (Maven compiler target)
- **Maven 3.6+** (build tool)
- **JavaFX 17.0.2 SDK** (automatically managed by Maven)

### Build Steps

1. **Clone or download the project:**
   ```bash
   git clone <repository-url>
   cd osdlmini
   ```

2. **Compile the project:**
   ```bash
   mvn clean compile
   ```

3. **Create the executable JAR:**
   ```bash
   mvn package
   ```

4. **Verify build success:**
   - Look for `target/hotel-management-1.0.0.jar`
   - Check that no compilation errors occurred

---

## Running the Application

### Method 1: Maven (Recommended)

```bash
mvn javafx:run
```

This command compiles and launches the application using the JavaFX Maven plugin.

### Method 2: Direct Java Execution

After building:
```bash
java -jar target/hotel-management-1.0.0.jar
```

### Method 3: IDE Execution

- In IntelliJ IDEA: Right-click `Main.java` → Run
- In Eclipse: Right-click `Main.java` → Run As → Java Application

### First Run

On first execution, the application will:
1. Create the `data/` directory if it doesn't exist
2. Initialize empty data files (`rooms.dat`, `guests.dat`, `bookings.dat`)
3. Start all background threads
4. Display the main GUI window

---

## Key Components

### Model Layer

#### Room.java (Abstract Base Class)
**Demonstrates:** Abstraction, Inheritance, Polymorphism, Encapsulation, Serialization

```java
public abstract class Room implements Serializable, Billable {
    private String roomId;
    private Integer floorNumber;      // Wrapper class
    private Integer maxOccupancy;     // Wrapper class
    private Double pricePerNight;     // Wrapper class
    private RoomStatus status;        // Enum
    private RoomType type;            // Enum
    
    // Abstract method — subclasses MUST implement
    public abstract double calculateRate(int nights);
    public abstract double calculateRate(int nights, boolean includeServiceCharge);
}
```

- `StandardRoom`: Base rate, no premium
- `DeluxeRoom`: 30% premium on base rate
- `SuiteRoom`: 60% premium on base rate

#### Guest.java
Represents a hotel guest with contact information. Serializable for persistence.

#### Booking.java
Links a guest to a room for a specific date range. Implements:
- `Billable`: Can calculate total bill
- `Comparable<Booking>`: Sortable by checkout date for priority queues
- `Serializable`: Can be persisted to disk

**Key Features:**
- Automatic night calculation using `ChronoUnit.DAYS`
- Check-in/checkout state tracking
- Payment status management

### Repository Layer

All repositories extend the generic `Repository<T>` base class:

```java
public abstract class Repository<T> {
    protected Map<String, T> data = new HashMap<>();
    
    public void add(T item);
    public void delete(String id);
    public T findById(String id);
    public List<T> findAll();
}
```

- **RoomRepository**: Manages room CRUD; synchronized methods for thread safety
- **GuestRepository**: Manages guest data; supports searching by name/ID
- **BookingRepository**: Manages bookings; maintains `PriorityQueue` for checkout reminders

### Service Layer

#### BookingService.java
Orchestrates booking workflows with synchronized methods:

```java
public synchronized void checkIn(String bookingId)
public synchronized void checkOut(String bookingId)
public synchronized double calculateBill(String bookingId)
```

Synchronization ensures thread-safe operations when multiple threads access bookings.

### I/O Layer

#### DataManager<T extends Serializable>
Generic class for serialization/deserialization:

```java
public class DataManager<T extends Serializable> {
    public void saveData(List<T> data, String filename) // ObjectOutputStream
    public List<T> loadData(String filename)            // ObjectInputStream
}
```

#### RoomFileManager
Uses `RandomAccessFile` for direct record access:
- Read/write individual room records without loading entire file
- Efficient for large datasets

#### InvoiceExporter
Exports bills as formatted text files using `BufferedWriter`:

```
═══════════════════════════════════
        HOTEL INVOICE
═══════════════════════════════════
Guest: John Doe
Room: 101
Check-in: 2024-03-31
Check-out: 2024-04-05
Nights: 5
Total: $500.00
═══════════════════════════════════
```

#### LogManager
Application logging with `PrintWriter` for audit trails.

### Threading Model

#### Thread Lifecycle

```
Main.java start() {
  → CheckoutReminderThread.start()      (user thread)
  → AutoSaveThread.setDaemon(true)      (daemon thread)
  → BookingProcessorThread.start()      (user thread)
  → RoomStatusUpdaterThread.start()     (user thread)
  → OccupancyReporterThread.start()     (user thread)
}
```

#### Thread Safety Mechanisms

1. **Synchronized Methods:**
   ```java
   public synchronized void addBooking(Booking b) { ... }
   ```

2. **Synchronized Blocks:**
   ```java
   synchronized(this) {
       // Thread-safe section
   }
   ```

3. **ReentrantLock:**
   ```java
   ReentrantLock fileLock = new ReentrantLock();
   fileLock.lock();
   try {
       // Critical section
   } finally {
       fileLock.unlock();
   }
   ```

4. **Volatile Variables:**
   ```java
   private volatile boolean running = true;
   ```

5. **Thread Coordination:**
   ```java
   synchronized(this) {
       wait();        // Thread waits
       notify();      // Notify waiting threads
   }
   ```

6. **Concurrent Collections:**
   ```java
   ConcurrentHashMap<String, Room> safeMap = new ConcurrentHashMap<>();
   AtomicInteger bookingCounter = new AtomicInteger(0);
   ```

#### Daemon Threads

- **AutoSaveThread**: Set as daemon so JVM can exit even if saving is in progress
- Other threads are user threads, ensuring proper shutdown

---

## Data Persistence

### Serialization Strategy

**Byte Stream Approach (Binary):**
```
Room/Guest/Booking objects
        ↓
ObjectOutputStream → Java serialization
        ↓
.dat file (binary format)
        ↓
ObjectInputStream → Restore objects
        ↓
Reconstructed objects in memory
```

**Character Stream Approach (Text):**
```
Booking data
        ↓
BufferedWriter → Format as text
        ↓
Invoice .txt file (human-readable)
        ↓
LogManager → Application logs
```

**RandomAccessFile Approach:**
```
Room records
        ↓
RandomAccessFile → Direct access by position
        ↓
Update individual records without rewriting file
```

### Startup Loading

```java
Main.start() {
    // Load from .dat files using ObjectInputStream
    List<Room> rooms = roomDM.loadData("data/rooms.dat");
    List<Guest> guests = guestDM.loadData("data/guests.dat");
    List<Booking> bookings = bookingDM.loadData("data/bookings.dat");
    
    // Populate repositories
    roomRepo.addAll(rooms);
    guestRepo.addAll(guests);
    bookingRepo.addAll(bookings);
}
```

### Shutdown Saving

```java
Main.stop() {
    // Save all data on application exit
    roomDM.saveData(roomRepo.findAll(), "data/rooms.dat");
    guestDM.saveData(guestRepo.findAll(), "data/guests.dat");
    bookingDM.saveData(bookingRepo.findAll(), "data/bookings.dat");
}
```

### Auto-Save Thread

The AutoSaveThread periodically saves all data to prevent loss from unexpected shutdown:

```java
while (running) {
    Thread.sleep(30000); // Every 30 seconds
    fileLock.lock();
    try {
        saveAllData();
    } finally {
        fileLock.unlock();
    }
}
```

---

## Usage Guide

### 1. Managing Rooms

**Add a Room:**
1. Navigate to the **Rooms** tab
2. Click "Add Room"
3. Enter Room ID (e.g., "101"), Floor, Type (Standard/Deluxe/Suite), and descriptions
4. Click "Save"

**Room Types & Pricing:**
- **Standard**: Base rate (e.g., $100/night)
- **Deluxe**: 30% premium ($130/night)
- **Suite**: 60% premium ($160/night)

### 2. Managing Guests

**Register a Guest:**
1. Navigate to the **Guests** tab
2. Click "Register Guest"
3. Enter Name, Email, Phone, ID type (Passport/Driver's License/ID Card)
4. Click "Register"

**Search Guest:**
- Use the search bar to find by name or ID

### 3. Making Bookings

**Book a Room:**
1. Navigate to the **Bookings** tab
2. Click "New Booking"
3. Select Guest, Room, and date range
4. System automatically calculates number of nights
5. Click "Confirm Booking"

**Check Availability:**
- System prevents double-booking
- Displays available rooms for selected date range

### 4. Check-in/Checkout

**Check-in a Guest:**
1. Find the booking in the Bookings tab
2. Click "Check-in"
3. Confirm guest arrival
4. System updates room status to OCCUPIED

**Checkout a Guest:**
1. Click "Checkout" on the booking
2. System calculates final bill
3. Generates and exports invoice
4. Updates room status to AVAILABLE

### 5. Billing & Invoices

**Generate Invoice:**
- Automatically created during checkout
- Saved to `data/invoices/` as `.txt` file
- Contains guest info, room charges, dates, and total amount

**View Invoice:**
- Open `data/invoices/` folder in file explorer
- Invoice filename format: `INVOICE_<BookingID>.txt`

### 6. Reports & Dashboard

**View Statistics:**
1. Navigate to the **Dashboard** tab
2. See real-time metrics:
   - Total rooms in hotel
   - Current occupancy rate
   - Revenue this month
   - Average booking duration
3. Charts show trends over time

### 7. Background Operations

**Auto-Save:**
- Runs every 30 seconds (configurable)
- Automatically saves all changes
- Runs in daemon thread (won't prevent exit)

**Checkout Reminders:**
- Background thread monitors upcoming checkouts
- Sends notification for guests checking out today
- Visible in notifications panel

---

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 17+ |
| **Build Tool** | Maven | 3.6+ |
| **GUI Framework** | JavaFX | 17.0.2 |
| **Java Edition** | Java SE | 17 |
| **Concurrency** | Java Threads, Locks | Built-in |
| **Serialization** | Java Object Serialization | Built-in |
| **File I/O** | Java NIO & Legacy I/O | Built-in |

### Maven Plugins

- **javafx-maven-plugin**: Enables `mvn javafx:run` command
- **maven-compiler-plugin**: Compiles source code to Java 17 bytecode

---

## Code Examples

### Example 1: Creating and Saving a Room

```java
// Create a new deluxe room
Room room = new DeluxeRoom("R202", 2, 2, "Modern room with city view");

// Save through repository
roomRepository.add(room);

// Data automatically serialized to rooms.dat by AutoSaveThread
```

### Example 2: Processing a Booking

```java
// Create booking
Booking booking = new Booking("BK001", "G123", "R101", 
                              LocalDate.of(2024, 3, 31),
                              LocalDate.of(2024, 4, 5));

// Check-in guest
bookingService.checkIn("BK001");

// Calculate and process bill
double total = bookingService.calculateBill("BK001");

// Checkout with bill amount
bookingService.checkOut("BK001", total);

// Invoice automatically exported to data/invoices/
```

### Example 3: Thread-Safe Operations

```java
// In BookingRepository
public synchronized void add(Booking booking) {
    bookings.put(booking.getId(), booking);
    // Thread-safe: only one thread can execute at a time
}

// In AutoSaveThread
synchronized(fileLock) {
    // Safely coordinate with other threads accessing files
    saveAllData();
}
```

### Example 4: Generic Repository Usage

```java
public class Repository<T> {
    protected Map<String, T> data = new HashMap<>();
    
    public void add(T item) {
        data.put(item.getId(), item);
    }
    
    public T findById(String id) {
        return data.get(id);
    }
}

// Usage
RoomRepository roomRepo = new RoomRepository();
GuestRepository guestRepo = new GuestRepository();
// Both use same generic logic
```

### Example 5: Serialization Usage

```java
// Save objects
DataManager<Room> roomManager = new DataManager<>("data/rooms.dat");
List<Room> allRooms = roomRepository.findAll();
roomManager.saveData(allRooms, "data/rooms.dat");

// Load objects
List<Room> restoredRooms = roomManager.loadData("data/rooms.dat");
forEach(room -> roomRepository.add(room));
```

---

## Troubleshooting

### Build Issues

**Error: "Maven is not recognized"**
- Install Maven 3.6+ or use IDE's embedded Maven

**Error: "Java 17 not found"**
- Install Java 17 JDK from Oracle or OpenJDK

**Error: "JavaFX modules not found"**
- Maven downloads JavaFX automatically; check internet connection
- Delete `~/.m2/repository` and rebuild to re-download dependencies

### Runtime Issues

**No data persists after restart**
- Check that `data/` directory exists and is writable
- Look for exceptions in `data/hotel.log`
- Ensure sufficient disk space

**UI doesn't update after operations**
- JavaFX ObservableLists may need refresh
- Try restarting the application

**Threads not shutting down cleanly**
- Check for infinite loops in thread run methods
- Ensure daemon threads are properly marked

---

## Future Enhancements

- [ ] Database integration (MySQL, PostgreSQL)
- [ ] Network multi-user support
- [ ] Advanced reporting with export to PDF/Excel
- [ ] Guest loyalty program
- [ ] Payment gateway integration
- [ ] Email notifications
- [ ] Mobile app companion
- [ ] REST API for third-party integrations

---

## Contributing

To extend this project:

1. Follow the existing layered architecture
2. Add new features in appropriate packages (model, service, ui)
3. Use `synchronized` for thread-safe operations
4. Implement `Serializable` for persistent entities
5. Write to `LogManager` for audit trails
6. Update this README with new features

---

## License

This project is provided as-is for educational purposes.

---

## Contact & Support

For issues, questions, or suggestions, please refer to the implementation plan (`implementation_plan.md`) for detailed architectural information.