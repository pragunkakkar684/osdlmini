# Hotel Management System — Implementation Plan

A standalone JavaFX desktop application for hotel management, designed to demonstrate every required Java concept in depth. The system handles room management, customer booking, check-in/checkout, billing, and reporting — all without a database (file-based persistence only).

---

## Feature Set

| # | Feature | Description |
|---|---------|-------------|
| 1 | **Room Management** | Add, edit, view, delete rooms with type/floor/amenities |
| 2 | **Guest Management** | Register guests, search by name/ID |
| 3 | **Booking System** | Book a room, check availability by date range |
| 4 | **Check-in / Checkout** | Check-in workflow, auto-bill generation on checkout |
| 5 | **Billing & Invoice** | Auto-calculate charges, export invoice to `.txt` file |
| 6 | **Reports Dashboard** | Occupancy stats, revenue summary, booking history |
| 7 | **Notifications** | Background auto-reminder for checkouts due today |
| 8 | **Data Persistence** | All data saved to files; loaded on startup |

---

## Java Concepts Coverage Map

| Concept | Where Used |
|---------|-----------|
| **Encapsulation** | `Room`, `Guest`, `Booking` model classes with private fields + getters/setters |
| **Inheritance** | `Room` → `StandardRoom`, `DeluxeRoom`, `SuiteRoom` |
| **Polymorphism** | `Room.calculateRate()` overridden per subclass; method overloading in service layer |
| **Abstraction** | `abstract class Room` + `interface Billable`, `interface Searchable<T>` |
| **Wrapper Classes** | `Integer`, `Double` used in collections, autoboxing in calculations |
| **Enum** | `RoomStatus` (AVAILABLE, BOOKED, OCCUPIED, MAINTENANCE), `RoomType` (STANDARD, DELUXE, SUITE), `PaymentStatus` |
| **Multithreading** | 5 distinct threads: CheckoutReminder, AutoSave, OccupancyReporter, BookingProcessor, RoomStatusUpdater — using `Thread`, `Runnable`, `ExecutorService`, daemon threads |
| **Synchronization** | `synchronized` methods, `synchronized` blocks, `ReentrantLock`, `wait()`/`notify()`, `volatile`, `AtomicInteger`, `ConcurrentHashMap` — all used in appropriate, distinct scenarios |
| **Byte Streams** | Serialize binary booking data using `ObjectOutputStream` / `ObjectInputStream` |
| **Character Streams** | Invoice export using `BufferedWriter` / `FileWriter`; log files with `PrintWriter` |
| **Random Access File** | `RandomAccessFile` to update individual room records in a flat binary file |
| **Serialization** | `Guest`, `Booking`, `Room` implement `Serializable`; saved to `.dat` files |
| **Deserialization** | Load `.dat` files on startup to restore full state |
| **Generics** | `Repository<T>`, `SearchResult<T>`, `Pair<K,V>`, generic `DataManager<T extends Serializable>` |
| **Collections** | `HashMap<String,Room>`, `ArrayList<Booking>`, `TreeMap` for sorted reports, `PriorityQueue` for checkout reminders |
| **JavaFX** | Full tabbed GUI: Dashboard, Rooms, Guests, Bookings, Reports tabs; `ObservableList`, `TableView`, `Charts` |

---

## Project Structure

```
hotel-management/
├── src/main/java/com/hotel/
│   ├── Main.java                         ← JavaFX Application entry point
│   ├── model/
│   │   ├── Room.java                     ← abstract class, Serializable
│   │   ├── StandardRoom.java             ← extends Room
│   │   ├── DeluxeRoom.java               ← extends Room
│   │   ├── SuiteRoom.java                ← extends Room
│   │   ├── Guest.java                    ← Serializable
│   │   ├── Booking.java                  ← Serializable
│   │   └── enums/
│   │       ├── RoomStatus.java
│   │       ├── RoomType.java
│   │       └── PaymentStatus.java
│   ├── interfaces/
│   │   ├── Billable.java
│   │   └── Searchable.java               ← Generic interface
│   ├── repository/
│   │   ├── Repository.java               ← Generic base repository
│   │   ├── RoomRepository.java
│   │   ├── GuestRepository.java
│   │   └── BookingRepository.java
│   ├── service/
│   │   ├── RoomService.java
│   │   ├── BookingService.java           ← synchronized methods
│   │   ├── BillingService.java
│   │   └── ReportService.java
│   ├── io/
│   │   ├── DataManager.java              ← Generic serialization/deserialization
│   │   ├── RoomFileManager.java          ← RandomAccessFile for room records
│   │   ├── InvoiceExporter.java          ← Character streams (BufferedWriter)
│   │   └── LogManager.java              ← Character streams (PrintWriter)
│   ├── threads/
│   │   ├── CheckoutReminderThread.java   ← Runnable, background thread
│   │   └── AutoSaveThread.java           ← Daemon thread for periodic save
│   └── ui/
│       ├── MainWindow.java               ← TabPane root
│       ├── DashboardTab.java             ← Stats + charts
│       ├── RoomTab.java                  ← TableView + CRUD
│       ├── GuestTab.java                 ← Guest registration + search
│       ├── BookingTab.java               ← Book / check-in / checkout
│       └── ReportsTab.java               ← Revenue + occupancy charts
├── src/main/resources/
│   ├── styles/main.css
│   └── images/
└── data/                                 ← Persisted .dat and .raf files
```

---

## Proposed Changes

### Component 1 — Model Layer

#### [NEW] Room.java (abstract, Serializable, Billable)
- Abstract class with `roomId`, `floorNumber`, `status (RoomStatus enum)`, `type (RoomType enum)`
- Abstract method `calculateRate(int nights)`
- Concrete methods: `toString()`, `getStatusDisplay()`

#### [NEW] StandardRoom.java, DeluxeRoom.java, SuiteRoom.java
- Each overrides `calculateRate(int nights)` with different pricing logic
- Demonstrates **polymorphism** and **inheritance**

#### [NEW] Guest.java
- Fields: `guestId (String)`, `name`, `phone`, `email`, `checkInDate`
- Uses `Integer` / `Double` wrapper classes for age/deposit amounts

#### [NEW] Booking.java
- Links `Guest` + `Room` + date range
- Has `PaymentStatus` enum field
- Implements `Billable` interface

#### [NEW] Enums: RoomStatus, RoomType, PaymentStatus
- Each enum has display-name field and constructor → demonstrates enum with fields/methods

---

### Component 2 — Generics & Collections

#### [NEW] Repository\<T\> (generic base)
- `add(T item)`, `remove(String id)`, `findById(String id)`, `getAll()` → returns `List<T>`
- Backed by `HashMap<String, T>`

#### [NEW] RoomRepository, GuestRepository, BookingRepository
- Extend `Repository<T>` with type-specific search methods
- Use `TreeMap` for sorted room listings, `ArrayList` for bookings, `PriorityQueue` for checkout queue

#### [NEW] Searchable\<T\> interface
- Generic `search(String query): List<T>`
- Implemented by each repository

---

### Component 3 — I/O & Persistence

#### [NEW] DataManager\<T extends Serializable\>
- Generic class handling `ObjectOutputStream` (byte stream) serialization to `.dat` files
- `save(List<T>)` and `load(): List<T>` methods

#### [NEW] RoomFileManager.java
- Uses `RandomAccessFile` with fixed-size room records
- Supports seeking to a record by index to update room status in-place

#### [NEW] InvoiceExporter.java
- Uses `BufferedWriter` / `FileWriter` (character streams) to write invoice `.txt` file
- Formats bill with room charges, taxes, total

#### [NEW] LogManager.java
- `PrintWriter` appending to `hotel.log` (character stream)
- Logs every booking, checkout, and error

---

### Component 4 — Multithreading & Synchronization ⭐ (Core Focus)

This is the most technically rich component. Five dedicated threads serve distinct roles, and every major Java synchronization primitive is used *in the right context* — not as demonstration padding, but as the correct tool for each problem.

---

#### Thread Architecture Overview

```
Main (JavaFX Application Thread)
├── CheckoutReminderThread   [Runnable + Thread]      — scans due checkouts every 60s
├── AutoSaveThread           [Runnable + daemon]       — persists data every 5 min
├── OccupancyReporterThread  [Runnable + Executor]     — generates stats in background
├── BookingProcessorThread   [Producer-Consumer]       — async processes booking queue
└── RoomStatusUpdaterThread  [volatile flag + notify]  — pushes status changes to UI
```

---

#### [NEW] CheckoutReminderThread.java
**How**: Implements `Runnable`, started with `new Thread(runnable).start()`
**What it does**: Runs an infinite loop with `Thread.sleep(60_000)`. Checks `PriorityQueue<Booking>` for any bookings due today. If found, pushes an alert to the JavaFX UI via `Platform.runLater()`.

**Synchronization used**: `synchronized (bookingQueue) { ... }` block — because `PriorityQueue` is not thread-safe. This demonstrates the classic **intrinsic lock / monitor** pattern.

```java
@Override
public void run() {
    while (!Thread.currentThread().isInterrupted()) {
        synchronized (bookingQueue) {
            // scan for due checkouts
        }
        try { Thread.sleep(60_000); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

---

#### [NEW] AutoSaveThread.java
**How**: Implements `Runnable`, set as a **daemon thread** (`thread.setDaemon(true)`) so it never prevents JVM shutdown
**What it does**: Wakes every 5 minutes and triggers serialization of all data to `.dat` files

**Synchronization used**: `ReentrantLock` with `lock.tryLock(timeout)` — because `ReentrantLock` gives finer control than `synchronized`: it supports timeout acquisition (avoids deadlock) and `lockInterruptibly()`. Demonstrates **explicit lock** vs intrinsic lock trade-offs.

```java
if (fileLock.tryLock(2, TimeUnit.SECONDS)) {
    try { dataManager.saveAll(); }
    finally { fileLock.unlock(); }
}
```

---

#### [NEW] OccupancyReporterThread.java
**How**: Managed by a `ScheduledExecutorService` (thread pool), submitted via `executor.scheduleAtFixedRate(...)`
**What it does**: Periodically recalculates occupancy %, revenue, and updates the Dashboard's `ObservableList`

**Synchronization used**: The shared `reportData` map uses `ConcurrentHashMap<String, Double>` — demonstrating **lock-free thread-safe collections** from `java.util.concurrent`. Also uses `AtomicInteger` for the live room-booked counter shown on the dashboard ticker.

```java
private final ConcurrentHashMap<String, Double> reportData = new ConcurrentHashMap<>();
private final AtomicInteger totalBookingsToday = new AtomicInteger(0);
// thread-safe increment without synchronized:
totalBookingsToday.incrementAndGet();
```

---

#### [NEW] BookingProcessorThread.java — Producer-Consumer Pattern
**How**: Uses `BlockingQueue<BookingRequest>` (`LinkedBlockingQueue`) to decouple UI from booking logic
**What it does**: The JavaFX UI thread (producer) submits booking requests to the queue. This thread (consumer) processes them one by one — validates room availability, updates status, logs — without blocking the UI.

**Synchronization used**: `wait()` / `notify()` on a custom `BookingQueue` class (wraps `LinkedList<BookingRequest>`) — explicitly demonstrating Java's **Object Monitor** protocol (`wait`, `notifyAll`). This is kept separate from the `BlockingQueue` version to show both approaches.

```java
// Producer (UI thread)
synchronized (bookingQueue) {
    bookingQueue.add(request);
    bookingQueue.notifyAll();   // wake consumer
}

// Consumer (BookingProcessorThread)
synchronized (bookingQueue) {
    while (bookingQueue.isEmpty()) {
        bookingQueue.wait();    // release lock and sleep
    }
    BookingRequest req = bookingQueue.poll();
}
```

---

#### [NEW] RoomStatusUpdaterThread.java
**How**: Implements `Runnable`, uses a `volatile boolean running` flag for clean shutdown
**What it does**: Monitors external file changes (e.g., if another admin edits rooms.dat manually) and refreshes room statuses.

**Synchronization used**: `volatile` keyword on the `running` flag — demonstrates that `volatile` guarantees **visibility** across threads (without mutual exclusion). Without `volatile`, the main thread setting `running = false` might not be seen by the background thread due to CPU cache.

```java
private volatile boolean running = true;

public void stopGracefully() { this.running = false; }  // seen immediately by thread

@Override
public void run() {
    while (running) { /* check for file changes */ }
}
```

---

#### [MODIFY] BookingService.java — synchronized methods
All booking state mutations are `synchronized` at the method level, creating a clear **critical section** boundary. This prevents race conditions if two users click "Book" simultaneously (realistic in multi-admin scenarios).

```java
public synchronized boolean bookRoom(String roomId, String guestId, LocalDate from, LocalDate to) {
    // atomic check-then-act: no other thread can enter while this runs
    if (roomRepository.findById(roomId).getStatus() != RoomStatus.AVAILABLE) return false;
    // ... proceed with booking
}
```

---

#### [NEW] ThreadMonitorTab.java (Bonus UI tab)
A **live Thread Monitor** tab in the GUI showing:
- Each thread's name, state (`RUNNABLE`, `TIMED_WAITING`, etc.) via `thread.getState()`
- Live counter from `AtomicInteger`
- A "Stop Thread" button that sets the `volatile` flag to false

This directly visualizes multithreading concepts in the UI itself — excellent for a demo/presentation.

---

#### Updated Project Structure for Threads

```
threads/
├── CheckoutReminderThread.java   ← Runnable + synchronized block
├── AutoSaveThread.java           ← Daemon thread + ReentrantLock
├── OccupancyReporterThread.java  ← ScheduledExecutorService + ConcurrentHashMap + AtomicInteger
├── BookingProcessorThread.java   ← wait() / notify() producer-consumer
└── RoomStatusUpdaterThread.java  ← volatile flag for graceful shutdown
```

| Sync Primitive | Thread | Why chosen here |
|---------------|--------|-----------------|
| `synchronized` block | CheckoutReminder | Simple, short critical section on non-thread-safe `PriorityQueue` |
| `ReentrantLock` | AutoSave | Needs timeout to avoid blocking on slow disk I/O |
| `ConcurrentHashMap` | OccupancyReporter | High-read, low-write map; lock-free reads outperform synchronized HashMap |
| `AtomicInteger` | OccupancyReporter | Single counter increment — no need for full lock overhead |
| `wait()` / `notify()` | BookingProcessor | Classic producer-consumer; teaches Monitor Object pattern |
| `volatile` | RoomStatusUpdater | Flag visibility only, no atomicity needed — simplest correct tool |
| `synchronized` method | BookingService | Entire check-then-act must be atomic to prevent double-booking |

---

### Component 5 — JavaFX GUI

#### [NEW] MainWindow.java
- `TabPane` with 5 tabs: Dashboard, Rooms, Guests, Bookings, Reports
- `MenuBar` with File → Save, Exit; Help → About

#### [NEW] DashboardTab.java
- Summary cards: Total Rooms, Available, Occupied, Today's Checkouts
- `BarChart` for weekly occupancy using JavaFX Charts

#### [NEW] RoomTab.java
- `TableView<Room>` with `ObservableList`
- Add/Edit/Delete rooms via modal `Dialog`
- Filter by `RoomType` using `ComboBox`

#### [NEW] GuestTab.java
- `TableView<Guest>` with search field (`TextField` with live filter)
- Register new guest form

#### [NEW] BookingTab.java
- Book room: select guest + room + date range with `DatePicker`
- Check-in / Checkout buttons with confirmation dialogs
- Auto-shows generated invoice on checkout

#### [NEW] ReportsTab.java
- `PieChart` for room type distribution
- Revenue table using `TreeMap` (automatically sorted by date)

---

## Verification Plan

### Manual Testing Steps (in the running app)
1. **Launch** the app → data directory auto-created, app loads with empty state
2. **Room tab**: Add a Standard, Deluxe, and Suite room → verify they appear in table
3. **Guest tab**: Register 2 guests → verify they appear searchable
4. **Booking tab**: Book a room for Guest 1 → room status changes to BOOKED
5. **Checkout**: Check-in then checkout Guest 1 → invoice `.txt` generated in `data/invoices/`
6. **Restart app** → verify all rooms and guests reloaded from `.dat` files (serialization test)
7. **Reports tab**: Verify occupancy chart and revenue table reflect bookings
8. **Log file**: Check `data/hotel.log` has entries for all operations

### Automated Checks
- Run `javac` / `mvn compile` to verify no compilation errors
- Background thread visible in system: `CheckoutReminderThread` prints to log every minute
