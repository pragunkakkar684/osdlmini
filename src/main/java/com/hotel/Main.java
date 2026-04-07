package com.hotel;

import com.hotel.io.DataManager;
import com.hotel.io.InvoiceExporter;
import com.hotel.io.LogManager;
import com.hotel.io.RoomFileManager;
import com.hotel.model.Booking;
import com.hotel.model.Guest;
import com.hotel.model.Room;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.BookingService;
import com.hotel.threads.*;
import com.hotel.ui.MainWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Application entry point — extends javafx.application.Application.
 *
 * KEY CONCEPTS HERE:
 *   - JavaFX lifecycle: init() → start() → stop()
 *   - All objects created and wired together (Dependency Injection manually)
 *   - All 5 threads started here with correct daemon settings
 *   - Data loaded from .dat files on startup (deserialization)
 *   - Data saved on shutdown (serialization)
 *   - ReentrantLock shared across AutoSaveThread for file coordination
 */
public class Main extends Application {

    // ── Shared infrastructure ──────────────────────────────────────────
    private final LogManager        logger          = new LogManager("data/hotel.log");
    private final ReentrantLock     fileLock        = new ReentrantLock();

    // ── Repositories ───────────────────────────────────────────────────
    private final RoomRepository    roomRepo        = new RoomRepository();
    private final GuestRepository   guestRepo       = new GuestRepository();
    private final BookingRepository bookingRepo     = new BookingRepository();

    // ── I/O Managers ───────────────────────────────────────────────────
    private final DataManager<Room>    roomDM       = new DataManager<>("data/rooms.dat");
    private final DataManager<Guest>   guestDM      = new DataManager<>("data/guests.dat");
    private final DataManager<Booking> bookingDM    = new DataManager<>("data/bookings.dat");
    private final RoomFileManager   roomFileMgr     = new RoomFileManager("data/rooms.raf");
    private final InvoiceExporter   invoiceExporter = new InvoiceExporter("data/invoices");

    // ── Service ────────────────────────────────────────────────────────
    private final BookingService    bookingService  = new BookingService(
            roomRepo, bookingRepo, roomFileMgr, logger);

    // ── Thread references (kept for stop() cleanup) ────────────────────
    private CheckoutReminderThread  reminderTask;
    private Thread                      reminderThread;
    private AutoSaveThread          autoSaveTask;
    private Thread                      autoSaveThread;
    private OccupancyReporterThread     occupancyReporter;
    private BookingProcessorThread   bookingProcessorTask;
    private BookingProcessorThread      bookingProcessor;
    private Thread                      bookingProcessorThread;
    private RoomStatusUpdaterThread  statusUpdaterTask;
    private RoomStatusUpdaterThread     statusUpdater;
    private Thread                      statusUpdaterThread;

    // ── JavaFX lifecycle ───────────────────────────────────────────────

    /**
     * start() — called by JavaFX after init().
     * Builds and shows the primary Stage (window).
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        // 1. Load persisted data from .dat files (deserialization)
        loadAllData();

        // 2. Build the main window, passing all dependencies
        MainWindow mainWindow = new MainWindow(
                roomRepo, guestRepo, bookingRepo,
                bookingService, invoiceExporter, logger);

        // 3. Launch all background threads
        startAllThreads(mainWindow);
        mainWindow.setThreadMonitorContext(
                reminderThread, autoSaveThread, bookingProcessorThread, statusUpdaterThread,
                reminderTask, autoSaveTask, occupancyReporter, bookingProcessorTask, statusUpdaterTask);

        // 4. Show the JavaFX window
        primaryStage.setTitle("Grand Hotel Management System");
        primaryStage.setScene(mainWindow.buildScene());
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.show();

        logger.info("Application started successfully.");
    }

    /**
     * stop() — called by JavaFX when window is closed.
     * Gracefully shuts down threads and saves all data.
     */
    @Override
    public void stop() throws Exception {
        logger.info("Application shutting down.");

        // Stop threads gracefully
        if (reminderThread   != null) reminderThread.interrupt();
        if (statusUpdater    != null) statusUpdater.stopGracefully();
        if (bookingProcessor != null) bookingProcessor.stop();
        if (occupancyReporter != null) occupancyReporter.stop();
        // autoSaveThread is daemon — JVM terminates it automatically

        // Final save on exit
        AutoSaveThread saver = new AutoSaveThread(
                roomRepo, guestRepo, bookingRepo,
                roomDM, guestDM, bookingDM, logger, fileLock);
        saver.performSave();

        logger.info("Shutdown complete.");
    }

    // ── Private helpers ────────────────────────────────────────────────

    private void loadAllData() {
        try {
            List<Room>    rooms    = roomDM.load();
            List<Guest>   guests  = guestDM.load();
            List<Booking> bookings = bookingDM.load();

            rooms.forEach(roomRepo::add);
            guests.forEach(guestRepo::add);
            bookings.forEach(bookingRepo::add);

            // Register room indices in RAF for in-place status updates
            List<Room> allRooms = roomRepo.getAll();
            for (int i = 0; i < allRooms.size(); i++) {
                bookingService.registerRoomIndex(allRooms.get(i).getRoomId(), i);
            }

            logger.info("Data loaded: " + rooms.size() + " rooms, "
                    + guests.size() + " guests, " + bookings.size() + " bookings.");
        } catch (Exception e) {
            logger.warn("No existing data found — starting fresh. (" + e.getMessage() + ")");
        }
    }

    private void startAllThreads(MainWindow mainWindow) {

        // Thread 1: CheckoutReminderThread — Runnable + synchronized block
        reminderTask = new CheckoutReminderThread(bookingRepo, logger,
                        msg -> Platform.runLater(() -> mainWindow.showAlert(msg)));
        reminderThread = new Thread(
                reminderTask,
                "CheckoutReminderThread");
        reminderThread.setDaemon(true);
        reminderThread.start();

        // Thread 2: AutoSaveThread — Daemon + ReentrantLock
        autoSaveTask = new AutoSaveThread(
                roomRepo, guestRepo, bookingRepo,
                roomDM, guestDM, bookingDM, logger, fileLock);
        autoSaveThread = new Thread(autoSaveTask, "AutoSaveThread");
        autoSaveThread.setDaemon(true);
        autoSaveThread.start();

        // Thread 3: OccupancyReporterThread — ScheduledExecutorService + ConcurrentHashMap + AtomicInteger
        occupancyReporter = new OccupancyReporterThread(
                roomRepo, bookingRepo, logger,
                data -> Platform.runLater(() -> mainWindow.updateDashboard(data)));
        occupancyReporter.start();

        // Thread 4: BookingProcessorThread — wait()/notifyAll() producer-consumer
        bookingProcessorTask = new BookingProcessorThread(
                bookingService, logger,
                result -> Platform.runLater(() -> mainWindow.showBookingResult(result)));
        bookingProcessor = bookingProcessorTask;
        bookingProcessorThread = new Thread(bookingProcessor, "BookingProcessorThread");
        bookingProcessorThread.setDaemon(true);
        bookingProcessorThread.start();

        // Thread 5: RoomStatusUpdaterThread — volatile boolean
        statusUpdaterTask = new RoomStatusUpdaterThread(
                roomFileMgr, roomRepo, logger,
                () -> Platform.runLater(mainWindow::refreshRoomTable));
        statusUpdater = statusUpdaterTask;
        statusUpdaterThread = new Thread(statusUpdater, "RoomStatusUpdaterThread");
        statusUpdaterThread.setDaemon(true);
        statusUpdaterThread.start();

        logger.info("All 5 background threads started.");
    }

    /** Standard Java entry point — launches JavaFX */
    public static void main(String[] args) {
        launch(args);
    }
}
