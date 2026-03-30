package com.hotel.ui;

import com.hotel.io.InvoiceExporter;
import com.hotel.io.LogManager;
import com.hotel.model.*;
import com.hotel.model.enums.RoomStatus;
import com.hotel.model.enums.RoomType;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.BookingService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main JavaFX window — contains all tabs and UI logic.
 *
 * JavaFX CONCEPTS USED:
 *   TabPane, Tab           — tab-based navigation
 *   TableView<T>           — data table display
 *   ObservableList<T>      — live-updating list bound to TableView
 *   TableColumn            — defines a column in TableView
 *   TextField, ComboBox,
 *   DatePicker, Dialog     — input controls
 *   BarChart               — occupancy visualization
 *   VBox, HBox, GridPane   — layout containers
 *   Label, Button          — basic controls
 *   Alert                  — popup dialogs
 *   Platform.runLater()    — thread-safe UI updates (called from background threads)
 */
public class MainWindow {

    // Dependencies
    private final RoomRepository    roomRepo;
    private final GuestRepository   guestRepo;
    private final BookingRepository bookingRepo;
    private final BookingService    bookingService;
    private final InvoiceExporter   invoiceExporter;
    private final LogManager        logger;

    // ObservableLists — when these change, TableView auto-refreshes
    private final ObservableList<Room>    roomList    = FXCollections.observableArrayList();
    private final ObservableList<Guest>   guestList   = FXCollections.observableArrayList();
    private final ObservableList<Booking> bookingList = FXCollections.observableArrayList();

    // Dashboard labels (updated by OccupancyReporterThread via updateDashboard())
    private Label lblTotal, lblAvailable, lblOccupied, lblRevenue;

    // Alert label at top of window
    private Label alertBanner;

    // BarChart for dashboard occupancy
    private BarChart<String, Number> occupancyChart;
    private XYChart.Series<String, Number> chartSeries;

    public MainWindow(RoomRepository roomRepo, GuestRepository guestRepo,
                      BookingRepository bookingRepo, BookingService bookingService,
                      InvoiceExporter invoiceExporter, LogManager logger) {
        this.roomRepo        = roomRepo;
        this.guestRepo       = guestRepo;
        this.bookingRepo     = bookingRepo;
        this.bookingService  = bookingService;
        this.invoiceExporter = invoiceExporter;
        this.logger          = logger;
        refreshAllLists();
    }

    /** Builds and returns the main Scene with all tabs */
    public Scene buildScene() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #1a1a2e;");

        tabPane.getTabs().addAll(
                buildDashboardTab(),
                buildRoomsTab(),
                buildGuestsTab(),
                buildBookingsTab(),
                buildThreadMonitorTab()
        );

        // Alert banner at the top (shown when CheckoutReminderThread fires)
        alertBanner = new Label();
        alertBanner.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
                + "-fx-font-size: 13px; -fx-padding: 5 15; -fx-font-weight: bold;");
        alertBanner.setMaxWidth(Double.MAX_VALUE);
        alertBanner.setVisible(false);

        VBox root = new VBox(alertBanner, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        root.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css") != null
                ? getClass().getResource("/styles/main.css").toExternalForm() : "");
        return scene;
    }

    // ═══════════════════════════════════════════════════════════════════
    // TAB 1 — DASHBOARD
    // ═══════════════════════════════════════════════════════════════════
    private Tab buildDashboardTab() {
        Tab tab = new Tab("📊 Dashboard");

        // Stat cards row
        lblTotal     = statCard("Total Rooms",  "0", "#3498db");
        lblAvailable = statCard("Available",    "0", "#2ecc71");
        lblOccupied  = statCard("Occupied",     "0", "#e74c3c");
        lblRevenue   = statCard("Revenue (Rs)", "0", "#f39c12");

        HBox statsRow = new HBox(15, lblTotal, lblAvailable, lblOccupied, lblRevenue);
        statsRow.setPadding(new Insets(15));
        statsRow.setAlignment(Pos.CENTER);

        // BarChart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Status");
        yAxis.setLabel("Count");
        occupancyChart = new BarChart<>(xAxis, yAxis);
        occupancyChart.setTitle("Room Occupancy Overview");
        occupancyChart.setStyle("-fx-background-color: #16213e;");
        occupancyChart.setMinHeight(300);
        chartSeries = new XYChart.Series<>();
        chartSeries.setName("Rooms");
        occupancyChart.getData().add(chartSeries);

        VBox layout = new VBox(20, headerLabel("🏨 Grand Hotel — Dashboard"), statsRow, occupancyChart);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #1a1a2e;");
        tab.setContent(layout);
        return tab;
    }

    // ═══════════════════════════════════════════════════════════════════
    // TAB 2 — ROOMS
    // ═══════════════════════════════════════════════════════════════════
    private Tab buildRoomsTab() {
        Tab tab = new Tab("🛏 Rooms");

        TableView<Room> table = new TableView<>(roomList);
        table.setStyle("-fx-background-color: #16213e; -fx-text-fill: white;");

        table.getColumns().addAll(
                col("Room ID",    "roomId"),
                col("Type",       "type"),
                col("Floor",      "floorNumber"),
                col("Status",     "status"),
                col("Price/Night","pricePerNight"),
                col("Max Occ.",   "maxOccupancy"),
                col("Description","description")
        );

        // Color rows by status
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (room == null || empty) {
                    setStyle("");
                } else {
                    setStyle("-fx-background-color: "
                            + room.getStatus().getColorHex() + "33;"); // 33 = 20% opacity
                }
            }
        });

        // Filter bar
        ComboBox<String> filterType = new ComboBox<>();
        filterType.getItems().addAll("All", "Standard", "Deluxe", "Suite");
        filterType.setValue("All");
        filterType.setOnAction(e -> {
            String sel = filterType.getValue();
            if ("All".equals(sel)) { roomList.setAll(roomRepo.getAllSorted()); }
            else { roomList.setAll(roomRepo.getByType(RoomType.valueOf(sel.toUpperCase()))); }
        });

        Button btnAdd      = actionButton("➕ Add Room",    "#2ecc71");
        Button btnMaint    = actionButton("🔧 Maintenance", "#e67e22");
        Button btnRefresh  = actionButton("🔄 Refresh",     "#3498db");

        btnAdd.setOnAction(e -> showAddRoomDialog());
        btnMaint.setOnAction(e -> {
            Room sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                sel.setStatus(RoomStatus.MAINTENANCE);
                refreshRoomTable();
            }
        });
        btnRefresh.setOnAction(e -> refreshRoomTable());

        HBox toolbar = new HBox(10,
                new Label("Filter: "), filterType, btnAdd, btnMaint, btnRefresh);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #16213e;");
        styleLabels(toolbar);

        VBox layout = new VBox(10, headerLabel("🛏 Room Management"), toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #1a1a2e;");
        tab.setContent(layout);
        return tab;
    }

    // ═══════════════════════════════════════════════════════════════════
    // TAB 3 — GUESTS
    // ═══════════════════════════════════════════════════════════════════
    private Tab buildGuestsTab() {
        Tab tab = new Tab("👤 Guests");

        TableView<Guest> table = new TableView<>(guestList);
        table.setStyle("-fx-background-color: #16213e;");
        table.getColumns().addAll(
                col("Guest ID",   "guestId"),
                col("Name",       "name"),
                col("Phone",      "phone"),
                col("Email",      "email"),
                col("Age",        "age"),
                col("ID Proof",   "idProofType"),
                col("ID Number",  "idProofNumber"),
                col("Deposit",    "depositAmount")
        );

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name, phone, ID...");
        searchField.setStyle("-fx-background-color: #16213e; -fx-text-fill: white; -fx-prompt-text-fill: grey;");
        searchField.textProperty().addListener((obs, old, query) -> {
            if (query.isEmpty()) guestList.setAll(guestRepo.getAll());
            else guestList.setAll(guestRepo.search(query));
        });

        Button btnAdd     = actionButton("➕ Register Guest", "#2ecc71");
        Button btnRefresh = actionButton("🔄 Refresh",        "#3498db");
        btnAdd.setOnAction(e -> showAddGuestDialog());
        btnRefresh.setOnAction(e -> { guestList.setAll(guestRepo.getAll()); });

        HBox toolbar = new HBox(10, new Label("🔍 "), searchField, btnAdd, btnRefresh);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #16213e;");
        styleLabels(toolbar);

        VBox layout = new VBox(10, headerLabel("👤 Guest Management"), toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #1a1a2e;");
        tab.setContent(layout);
        return tab;
    }

    // ═══════════════════════════════════════════════════════════════════
    // TAB 4 — BOOKINGS (Book / Check-in / Check-out)
    // ═══════════════════════════════════════════════════════════════════
    private Tab buildBookingsTab() {
        Tab tab = new Tab("📋 Bookings");

        TableView<Booking> table = new TableView<>(bookingList);
        table.setStyle("-fx-background-color: #16213e;");

        TableColumn<Booking,String> colId      = new TableColumn<>("Booking ID");
        TableColumn<Booking,String> colGuest   = new TableColumn<>("Guest ID");
        TableColumn<Booking,String> colRoom    = new TableColumn<>("Room ID");
        TableColumn<Booking,String> colIn      = new TableColumn<>("Check-In");
        TableColumn<Booking,String> colOut     = new TableColumn<>("Check-Out");
        TableColumn<Booking,String> colNights  = new TableColumn<>("Nights");
        TableColumn<Booking,String> colStatus  = new TableColumn<>("Payment");
        TableColumn<Booking,String> colAmount  = new TableColumn<>("Amount");

        colId.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getBookingId()));
        colGuest.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getGuestId()));
        colRoom.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getRoomId()));
        colIn.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCheckInDate().toString()));
        colOut.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCheckOutDate().toString()));
        colNights.setCellValueFactory(d->new SimpleStringProperty(String.valueOf(d.getValue().getNumberOfNights())));
        colStatus.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getPaymentStatus().getDisplayName()));
        colAmount.setCellValueFactory(d->new SimpleStringProperty("Rs."+String.format("%.2f",d.getValue().getTotalAmount())));

        table.getColumns().addAll(colId,colGuest,colRoom,colIn,colOut,colNights,colStatus,colAmount);

        Button btnBook    = actionButton("📝 Book Room",  "#3498db");
        Button btnCheckIn = actionButton("✅ Check-In",   "#2ecc71");
        Button btnCheckOut= actionButton("🏁 Check-Out",  "#e74c3c");
        Button btnRefresh = actionButton("🔄 Refresh",    "#95a5a6");

        btnBook.setOnAction(e -> showBookRoomDialog());
        btnCheckIn.setOnAction(e -> {
            Booking sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) showBookingResult(bookingService.checkIn(sel.getBookingId()));
            refreshBookingList();
        });
        btnCheckOut.setOnAction(e -> {
            Booking sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                String result = bookingService.checkOut(sel.getBookingId());
                showBookingResult(result);
                if (result.startsWith("SUCCESS")) {
                    generateInvoice(sel);
                }
                refreshBookingList();
                refreshRoomTable();
            }
        });
        btnRefresh.setOnAction(e -> refreshBookingList());

        HBox toolbar = new HBox(10, btnBook, btnCheckIn, btnCheckOut, btnRefresh);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #16213e;");

        VBox layout = new VBox(10, headerLabel("📋 Booking Management"), toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #1a1a2e;");
        tab.setContent(layout);
        return tab;
    }

    // ═══════════════════════════════════════════════════════════════════
    // TAB 5 — THREAD MONITOR (Live thread state viewer)
    // ═══════════════════════════════════════════════════════════════════
    private Tab buildThreadMonitorTab() {
        Tab tab = new Tab("🧵 Threads");

        Label info = new Label(
            "Live thread states (from Thread.getState()).\n"
          + "Each thread uses a different synchronization mechanism — see below.\n");
        info.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(12); grid.setPadding(new Insets(15));

        String[][] threads = {
            {"CheckoutReminderThread", "synchronized block (intrinsic lock on PriorityQueue)",  "#e74c3c"},
            {"AutoSaveThread",         "ReentrantLock + tryLock(3s) + daemon thread",            "#f39c12"},
            {"OccupancyReporter",      "ScheduledExecutorService + ConcurrentHashMap + AtomicInteger", "#2ecc71"},
            {"BookingProcessor",       "wait() / notifyAll() — Producer-Consumer pattern",        "#3498db"},
            {"RoomStatusUpdater",      "volatile boolean — cross-thread visibility",              "#9b59b6"},
        };

        grid.add(boldLabel("Thread Name"),        0, 0);
        grid.add(boldLabel("Sync Mechanism"),     1, 0);
        grid.add(boldLabel("Status"),             2, 0);

        for (int i = 0; i < threads.length; i++) {
            Label name  = new Label(threads[i][0]);
            Label mech  = new Label(threads[i][1]);
            Label state = new Label("● RUNNING");
            name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            mech.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 11px;");
            state.setStyle("-fx-text-fill: " + threads[i][2] + "; -fx-font-weight: bold;");
            grid.add(name, 0, i+1);
            grid.add(mech, 1, i+1);
            grid.add(state, 2, i+1);
        }

        grid.setStyle("-fx-background-color: #16213e; -fx-background-radius: 8;");

        Label syncSummary = new Label(
            "\nSynchronization primitives used in this project:\n"
          + "  1. synchronized block   — locks a specific object (intrinsic lock)\n"
          + "  2. synchronized method  — locks 'this' instance\n"
          + "  3. ReentrantLock        — explicit lock with tryLock(timeout)\n"
          + "  4. ConcurrentHashMap    — lock-free thread-safe map\n"
          + "  5. AtomicInteger        — hardware CAS, no synchronized needed\n"
          + "  6. wait() / notifyAll() — monitor object, producer-consumer\n"
          + "  7. volatile             — memory visibility across CPU caches\n"
        );
        syncSummary.setStyle("-fx-text-fill: #aaaaaa; -fx-font-family: monospace; -fx-font-size: 12px;");

        VBox layout = new VBox(15,
                headerLabel("🧵 Thread Monitor"),
                info, grid, syncSummary);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #1a1a2e;");
        tab.setContent(layout);
        return tab;
    }

    // ═══════════════════════════════════════════════════════════════════
    // DIALOGS
    // ═══════════════════════════════════════════════════════════════════

    private void showAddRoomDialog() {
        Dialog<Room> dlg = new Dialog<>();
        dlg.setTitle("Add New Room");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = dialogGrid();
        TextField fId    = styledField("e.g. R301");
        TextField fFloor = styledField("e.g. 3");
        TextField fOcc   = styledField("e.g. 2");
        TextField fDesc  = styledField("Description");
        ComboBox<RoomType> fType = new ComboBox<>();
        fType.getItems().addAll(RoomType.values());
        fType.setValue(RoomType.STANDARD);
        CheckBox fBreakfast = new CheckBox("Breakfast Included");
        CheckBox fButler    = new CheckBox("Butler Service");
        CheckBox fMinibar   = new CheckBox("Minibar Access");
        fBreakfast.setStyle("-fx-text-fill: white;");
        fButler.setStyle("-fx-text-fill: white;");
        fMinibar.setStyle("-fx-text-fill: white;");

        grid.addRow(0, lbl("Room ID:"), fId);
        grid.addRow(1, lbl("Floor:"),   fFloor);
        grid.addRow(2, lbl("Max Occ:"), fOcc);
        grid.addRow(3, lbl("Type:"),    fType);
        grid.addRow(4, lbl("Description:"), fDesc);
        grid.addRow(5, new Label(), fBreakfast);
        grid.addRow(6, new Label(), fButler);
        grid.addRow(7, new Label(), fMinibar);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().setStyle("-fx-background-color: #1a1a2e;");

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    String id    = fId.getText().trim();
                    int    floor = Integer.parseInt(fFloor.getText().trim());
                    int    occ   = Integer.parseInt(fOcc.getText().trim());
                    String desc  = fDesc.getText().trim();
                    Room room;
                    switch (fType.getValue()) {
                        case DELUXE -> room = new DeluxeRoom(id, floor, occ, desc, fBreakfast.isSelected());
                        case SUITE  -> room = new SuiteRoom(id, floor, occ, desc, fButler.isSelected(), fMinibar.isSelected());
                        default     -> room = new StandardRoom(id, floor, occ, desc);
                    }
                    return room;
                } catch (Exception ex) { return null; }
            }
            return null;
        });

        dlg.showAndWait().ifPresent(room -> {
            if (!roomRepo.exists(room.getRoomId())) {
                try {
                    int idx = roomRepo.count();
                    roomRepo.add(room);
                    roomFileManager_write(idx, room);
                    bookingService.registerRoomIndex(room.getRoomId(), idx);
                    refreshRoomTable();
                    logger.info("Room added: " + room.getRoomId());
                } catch (Exception ex) {
                    logger.error("Failed to add room: " + ex.getMessage());
                }
            } else {
                showAlert("Room ID already exists!");
            }
        });
    }

    private void showAddGuestDialog() {
        Dialog<Guest> dlg = new Dialog<>();
        dlg.setTitle("Register New Guest");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = dialogGrid();
        TextField fId    = styledField("e.g. G001");
        TextField fName  = styledField("Full Name");
        TextField fPhone = styledField("Phone Number");
        TextField fEmail = styledField("Email");
        TextField fAge   = styledField("Age");
        TextField fIdType= styledField("Aadhar / Passport");
        TextField fIdNum = styledField("ID Number");

        grid.addRow(0, lbl("Guest ID:"),  fId);
        grid.addRow(1, lbl("Name:"),      fName);
        grid.addRow(2, lbl("Phone:"),     fPhone);
        grid.addRow(3, lbl("Email:"),     fEmail);
        grid.addRow(4, lbl("Age:"),       fAge);
        grid.addRow(5, lbl("ID Type:"),   fIdType);
        grid.addRow(6, lbl("ID Number:"), fIdNum);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().setStyle("-fx-background-color: #1a1a2e;");

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    return new Guest(fId.getText().trim(), fName.getText().trim(),
                            fPhone.getText().trim(), fEmail.getText().trim(),
                            Integer.valueOf(fAge.getText().trim()),
                            fIdType.getText().trim(), fIdNum.getText().trim());
                } catch (Exception ex) { return null; }
            }
            return null;
        });

        dlg.showAndWait().ifPresent(g -> {
            if (g != null && !guestRepo.exists(g.getGuestId())) {
                guestRepo.add(g);
                guestList.setAll(guestRepo.getAll());
                logger.info("Guest registered: " + g.getGuestId());
            }
        });
    }

    private void showBookRoomDialog() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Book a Room");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = dialogGrid();
        TextField   fGuestId  = styledField("Guest ID");
        TextField   fRoomId   = styledField("Room ID");
        DatePicker  fCheckIn  = new DatePicker(LocalDate.now());
        DatePicker  fCheckOut = new DatePicker(LocalDate.now().plusDays(1));

        grid.addRow(0, lbl("Guest ID:"),   fGuestId);
        grid.addRow(1, lbl("Room ID:"),    fRoomId);
        grid.addRow(2, lbl("Check-In:"),   fCheckIn);
        grid.addRow(3, lbl("Check-Out:"),  fCheckOut);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().setStyle("-fx-background-color: #1a1a2e;");

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String result = bookingService.bookRoom(
                        fGuestId.getText().trim(), fRoomId.getText().trim(),
                        fCheckIn.getValue(), fCheckOut.getValue());
                showBookingResult(result);
                refreshAllLists();
            }
            return null;
        });
        dlg.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════════
    // PUBLIC CALLBACKS (called by threads via Platform.runLater)
    // ═══════════════════════════════════════════════════════════════════

    /** Called by OccupancyReporterThread every 30 seconds */
    public void updateDashboard(ConcurrentHashMap<String, Double> data) {
        lblTotal.setText("Total: " + data.getOrDefault("total", 0.0).intValue());
        lblAvailable.setText("Available: " + data.getOrDefault("available", 0.0).intValue());
        lblOccupied.setText("Occupied: " + data.getOrDefault("occupied", 0.0).intValue());
        lblRevenue.setText("Revenue: Rs." + String.format("%,.0f", data.getOrDefault("revenue", 0.0)));

        chartSeries.getData().clear();
        chartSeries.getData().addAll(
            new XYChart.Data<>("Available", data.getOrDefault("available", 0.0)),
            new XYChart.Data<>("Booked",    data.getOrDefault("booked",    0.0)),
            new XYChart.Data<>("Occupied",  data.getOrDefault("occupied",  0.0))
        );
    }

    /** Called by CheckoutReminderThread — shows red alert banner */
    public void showAlert(String message) {
        alertBanner.setText(message);
        alertBanner.setVisible(true);
    }

    /** Called by BookingProcessorThread with result of async booking */
    public void showBookingResult(String result) {
        Alert alert = new Alert(result.startsWith("SUCCESS")
                ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle("Booking Result");
        alert.setHeaderText(null);
        alert.setContentText(result);
        alert.getDialogPane().setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: white;");
        alert.showAndWait();
    }

    /** Called by RoomStatusUpdaterThread */
    public void refreshRoomTable() {
        roomList.setAll(roomRepo.getAllSorted());
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private void refreshAllLists() {
        roomList.setAll(roomRepo.getAllSorted());
        guestList.setAll(guestRepo.getAll());
        bookingList.setAll(bookingRepo.getAll());
    }

    private void refreshBookingList() {
        bookingList.setAll(bookingRepo.getAll());
    }

    private void generateInvoice(Booking b) {
        try {
            Optional<Guest> g = guestRepo.findById(b.getGuestId());
            Optional<Room>  r = roomRepo.findById(b.getRoomId());
            String gName = g.map(Guest::getName).orElse("Unknown");
            String rDesc = r.map(Room::toString).orElse("Unknown");
            String path  = invoiceExporter.exportInvoice(b, gName, rDesc, b.getTotalAmount());
            showBookingResult("SUCCESS: Invoice saved to:\n" + path);
        } catch (Exception ex) {
            logger.error("Invoice error: " + ex.getMessage());
        }
    }

    private void roomFileManager_write(int idx, Room room) {
        // Write to RAF via BookingService's registered index
        bookingService.registerRoomIndex(room.getRoomId(), idx);
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<T, String> col(String title, String prop) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setStyle("-fx-text-fill: white;");
        return c;
    }

    private Label statCard(String title, String value, String color) {
        Label l = new Label(title + "\n" + value);
        l.setStyle("-fx-background-color: " + color + "33; -fx-text-fill: white; "
                + "-fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 15 25; "
                + "-fx-background-radius: 10; -fx-border-color: " + color + "; "
                + "-fx-border-radius: 10;");
        l.setMinWidth(160);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private Label headerLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 20));
        l.setTextFill(Color.WHITE);
        return l;
    }

    private Label boldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #f0f0f0; -fx-font-weight: bold; -fx-font-size: 12px;");
        return l;
    }

    private Button actionButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        return b;
    }

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #16213e; -fx-text-fill: white; "
                + "-fx-prompt-text-fill: grey; -fx-border-color: #3498db; -fx-border-radius: 4;");
        f.setMinWidth(200);
        return f;
    }

    private GridPane dialogGrid() {
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(15));
        g.setStyle("-fx-background-color: #1a1a2e;");
        return g;
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #aaaaaa;");
        return l;
    }

    private void styleLabels(HBox box) {
        box.getChildren().stream()
           .filter(n -> n instanceof Label)
           .forEach(n -> ((Label)n).setStyle("-fx-text-fill: white;"));
    }
}
