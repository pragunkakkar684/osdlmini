package com.hotel.ui;

import com.hotel.io.InvoiceExporter;
import com.hotel.io.LogManager;
import com.hotel.model.*;
import com.hotel.model.RoomIdValidator;
import com.hotel.model.enums.RoomStatus;
import com.hotel.model.enums.RoomType;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.BookingService;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MainWindow {
    private final RoomRepository roomRepo;
    private final GuestRepository guestRepo;
    private final BookingRepository bookingRepo;
    private final BookingService bookingService;
    private final InvoiceExporter invoiceExporter;
    private final LogManager logger;

    private final ObservableList<Room> roomList = FXCollections.observableArrayList();
    private final ObservableList<Guest> guestList = FXCollections.observableArrayList();
    private final ObservableList<Booking> bookingList = FXCollections.observableArrayList();

    private Room selectedRoom;
    private VBox selectedRoomCard;
    private Guest selectedGuest;
    private VBox selectedGuestCard;
    private Booking selectedBooking;
    private HBox selectedBookingCard;

    private Label lblTotal, lblAvailable, lblOccupied, lblRevenue;
    private HBox alertBanner;
    private Label alertBannerText;
    private PauseTransition alertHideTransition;

    public MainWindow(RoomRepository roomRepo, GuestRepository guestRepo,
            BookingRepository bookingRepo, BookingService bookingService,
            InvoiceExporter invoiceExporter, LogManager logger) {
        this.roomRepo = roomRepo;
        this.guestRepo = guestRepo;
        this.bookingRepo = bookingRepo;
        this.bookingService = bookingService;
        this.invoiceExporter = invoiceExporter;
        this.logger = logger;
        refreshAllLists();
    }

    public Scene buildScene() {
        BorderPane mainContainer = new BorderPane();

        // --- Sidebar ---
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(260);

        Label brand = new Label("The Fern Hotel");
        brand.getStyleClass().add("sidebar-brand");
        VBox.setMargin(brand, new Insets(40, 0, 40, 30));

        ToggleGroup navGroup = new ToggleGroup();
        ToggleButton navDash = navButton("Dashboard", navGroup);
        ToggleButton navRooms = navButton("Rooms", navGroup);
        ToggleButton navGuests = navButton("Guests", navGroup);
        ToggleButton navBooks = navButton("Bookings", navGroup);
        ToggleButton navThreads = navButton("Threads", navGroup);

        sidebar.getChildren().addAll(brand, navDash, navRooms, navGuests, navBooks, navThreads);

        // --- Content Area ---
        StackPane contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        Region viewDashboard = buildDashboardView();
        Region viewRooms = buildRoomsView();
        Region viewGuests = buildGuestsView();
        Region viewBookings = buildBookingsView();
        Region viewThreads = buildThreadMonitorView();

        navDash.setOnAction(e -> {
            if (navDash.isSelected())
                setView(contentArea, viewDashboard);
        });
        navRooms.setOnAction(e -> {
            if (navRooms.isSelected())
                setView(contentArea, viewRooms);
        });
        navGuests.setOnAction(e -> {
            if (navGuests.isSelected())
                setView(contentArea, viewGuests);
        });
        navBooks.setOnAction(e -> {
            if (navBooks.isSelected())
                setView(contentArea, viewBookings);
        });
        navThreads.setOnAction(e -> {
            if (navThreads.isSelected())
                setView(contentArea, viewThreads);
        });

        // Default active
        navDash.setSelected(true);
        setView(contentArea, viewDashboard);

        alertBannerText = new Label();
        alertBannerText.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        Button alertClose = new Button("X");
        alertClose.setFocusTraversable(false);
        alertClose.setOnAction(e -> hideAlert());
        alertClose.setStyle("-fx-background-color: transparent; -fx-text-fill: white; "
                + "-fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");

        Region alertSpacer = new Region();
        HBox.setHgrow(alertSpacer, Priority.ALWAYS);

        alertBanner = new HBox(12, alertBannerText, alertSpacer, alertClose);
        alertBanner.setAlignment(Pos.CENTER_LEFT);
        alertBanner.setStyle("-fx-background-color: #E74C3C; -fx-padding: 10 20;");
        alertBanner.setMaxWidth(Double.MAX_VALUE);
        alertBanner.setVisible(false);
        alertBanner.managedProperty().bind(alertBanner.visibleProperty());

        alertHideTransition = new PauseTransition(Duration.seconds(4));
        alertHideTransition.setOnFinished(e -> hideAlert());

        mainContainer.setTop(alertBanner);
        mainContainer.setLeft(sidebar);
        mainContainer.setCenter(contentArea);

        Scene scene = new Scene(mainContainer, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css") != null
                ? getClass().getResource("/styles/main.css").toExternalForm()
                : "");
        return scene;
    }

    private void setView(StackPane container, Region view) {
        container.getChildren().setAll(view);
    }

    private ToggleButton navButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.getStyleClass().add("sidebar-btn");
        btn.setToggleGroup(group);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        return btn;
    }

    // DASHBOARD VIEW
    private Region buildDashboardView() {
        lblTotal = statCard("Total Rooms", "0");
        lblAvailable = statCard("Available", "0");
        lblOccupied = statCard("Occupied", "0");
        lblRevenue = statCard("Revenue", "Rs. 0");

        HBox statsRow = new HBox(15, lblTotal, lblAvailable, lblOccupied, lblRevenue);
        statsRow.setPadding(new Insets(15, 0, 15, 0));
        statsRow.setAlignment(Pos.CENTER_LEFT);

        VBox summaryCard = new VBox(8,
                boldLabel("Operational Summary"),
                new Label("Track room availability, occupancy, and total revenue in real time."),
                new Label("Use the Rooms and Bookings sections for day-to-day operations."));
        summaryCard.getStyleClass().add("dashboard-card");
        summaryCard.setPadding(new Insets(20));

        VBox layout = new VBox(20, headerLabel("Dashboard"), statsRow, summaryCard);
        layout.setPadding(new Insets(40));

        ScrollPane sp = new ScrollPane(layout);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #FAFAFA;");
        return sp;
    }

    // ROOMS VIEW
    private Region buildRoomsView() {
        FlowPane grid = new FlowPane(15, 15);
        grid.setPadding(new Insets(15, 0, 15, 0));

        ComboBox<String> filterType = new ComboBox<>();
        filterType.getItems().addAll("All", "Standard", "Deluxe", "Suite");
        filterType.setValue("All");
        filterType.setOnAction(e -> {
            String sel = filterType.getValue();
            if ("All".equals(sel)) {
                roomList.setAll(roomRepo.getAllSorted());
            } else {
                roomList.setAll(roomRepo.getByType(RoomType.valueOf(sel.toUpperCase())));
            }
        });

        Button btnAdd = actionButton("Add Room", null);
        Button btnMaint = actionButton("Maintenance", "#D4AF37");
        Button btnRefresh = actionButton("Refresh", null);

        for (Room r : roomList) {
            grid.getChildren().add(createRoomCard(r, btnMaint));
        }
        roomList.addListener((ListChangeListener.Change<? extends Room> c) -> {
            grid.getChildren().clear();
            for (Room r : roomList) {
                grid.getChildren().add(createRoomCard(r, btnMaint));
            }
        });

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #FAFAFA;");

        btnAdd.setOnAction(e -> showAddRoomDialog());
        btnMaint.setOnAction(e -> {
            if (selectedRoom != null) {
                if (selectedRoom.getStatus() == RoomStatus.MAINTENANCE) {
                    selectedRoom.setStatus(RoomStatus.AVAILABLE);
                } else if (selectedRoom.getStatus() == RoomStatus.AVAILABLE) {
                    selectedRoom.setStatus(RoomStatus.MAINTENANCE);
                } else {
                    showAlert("Cannot modify a booked or occupied room!");
                    return;
                }
                refreshRoomTable();
                selectedRoom = null;
                selectedRoomCard = null;
                btnMaint.setText("Maintenance");
                btnMaint.setStyle("-fx-background-color: #D4AF37; -fx-text-fill: white;");
            } else {
                showAlert("Please select a room card first.");
            }
        });
        btnRefresh.setOnAction(e -> refreshRoomTable());

        HBox toolbar = new HBox(10, boldLabel("Filter: "), filterType, btnAdd, btnMaint, btnRefresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        VBox layout = new VBox(15, headerLabel("Room Management"), toolbar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        layout.setPadding(new Insets(40));
        return layout;
    }

    private VBox createRoomCard(Room r, Button btnMaint) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dashboard-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(220);

        Label lblId = new Label("Room " + r.getRoomId());
        lblId.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblId.setTextFill(Color.web("#1A3626"));

        Label lblType = new Label(r.getType().toString());
        lblType.setStyle("-fx-text-fill: #9CAEA5; -fx-font-weight: bold;");

        Label lblStatus = new Label(r.getStatus().toString());
        lblStatus.setStyle("-fx-background-color: " + r.getStatus().getColorHex()
                + "; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label lblPrice = new Label("Rs. " + String.format("%.0f", r.getPricePerNight()) + " / night");
        lblPrice.setStyle("-fx-font-weight: bold; -fx-text-fill: #1A3626; -fx-font-size: 14px;");

        Label lblDesc = new Label(r.getDescription());
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CAEA5;");
        lblDesc.setMaxHeight(45);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(lblId, lblType, lblStatus, spacer, lblPrice, lblDesc);

        card.setOnMouseClicked(e -> {
            if (selectedRoomCard != null) {
                selectedRoomCard.setStyle("");
            }
            selectedRoomCard = card;
            selectedRoom = r;
            card.setStyle("-fx-border-color: #1A3626; -fx-border-width: 2;");

            if (r.getStatus() == RoomStatus.MAINTENANCE) {
                btnMaint.setText("End Maintenance");
                btnMaint.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white;");
            } else {
                btnMaint.setText("Maintenance");
                btnMaint.setStyle("-fx-background-color: #D4AF37; -fx-text-fill: white;");
            }
        });
        return card;
    }

    // GUESTS VIEW
    private Region buildGuestsView() {
        FlowPane grid = new FlowPane(15, 15);
        grid.setPadding(new Insets(15, 0, 15, 0));

        for (Guest g : guestList) {
            grid.getChildren().add(createGuestCard(g));
        }
        guestList.addListener((ListChangeListener.Change<? extends Guest> c) -> {
            grid.getChildren().clear();
            for (Guest g : guestList) {
                grid.getChildren().add(createGuestCard(g));
            }
        });

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #FAFAFA;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name, phone, or ID");
        searchField.textProperty().addListener((obs, old, query) -> {
            if (query.isEmpty())
                guestList.setAll(guestRepo.getAll());
            else
                guestList.setAll(guestRepo.search(query));
        });

        Button btnAdd = actionButton("Register Guest", null);
        Button btnRefresh = actionButton("Refresh", null);
        btnAdd.setOnAction(e -> showAddGuestDialog());
        btnRefresh.setOnAction(e -> {
            guestList.setAll(guestRepo.getAll());
        });

        HBox toolbar = new HBox(10, boldLabel("Search:"), searchField, btnAdd, btnRefresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        VBox layout = new VBox(15, headerLabel("Guest Management"), toolbar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        layout.setPadding(new Insets(40));
        return layout;
    }

    private VBox createGuestCard(Guest g) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dashboard-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(240);

        Label lblName = new Label(g.getName());
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblName.setTextFill(Color.web("#1A3626"));

        Label lblId = new Label("ID: " + g.getGuestId());
        lblId.setStyle("-fx-text-fill: #9CAEA5; -fx-font-weight: bold;");

        Label lblContact = new Label("📞 " + g.getPhone() + "\n📧 " + g.getEmail());
        lblContact.setStyle("-fx-text-fill: #1A3626; -fx-font-size: 13px;");

        Label lblDocs = new Label(
                "Proof: " + g.getIdProofType() + " (" + g.getIdProofNumber() + ")\nAge: " + g.getAge());
        lblDocs.setStyle("-fx-text-fill: #9CAEA5; -fx-font-size: 12px;");

        card.getChildren().addAll(lblName, lblId, lblContact, lblDocs);

        card.setOnMouseClicked(e -> {
            if (selectedGuestCard != null)
                selectedGuestCard.setStyle("");
            selectedGuestCard = card;
            selectedGuest = g;
            card.setStyle("-fx-border-color: #1A3626; -fx-border-width: 2;");
        });
        return card;
    }

    // BOOKINGS VIEW
    private Region buildBookingsView() {
        VBox list = new VBox(10);
        list.setPadding(new Insets(10, 0, 15, 0));

        for (Booking b : bookingList) {
            list.getChildren().add(createBookingCard(b));
        }
        bookingList.addListener((ListChangeListener.Change<? extends Booking> c) -> {
            list.getChildren().clear();
            for (Booking b : bookingList) {
                list.getChildren().add(createBookingCard(b));
            }
        });

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #FAFAFA;");

        Button btnBook = actionButton("Book Room", null);
        Button btnCheckIn = actionButton("Check-In", null);
        Button btnCheckOut = actionButton("Check-Out", "#E74C3C");
        Button btnRefresh = actionButton("Refresh", null);

        btnBook.setOnAction(e -> showBookRoomDialog());
        btnCheckIn.setOnAction(e -> {
            if (selectedBooking != null) {
                showBookingResult(bookingService.checkIn(selectedBooking.getBookingId()));
                refreshBookingList();
            } else {
                showAlert("Please select a booking card first.");
            }
        });
        btnCheckOut.setOnAction(e -> {
            if (selectedBooking != null) {
                String result = bookingService.checkOut(selectedBooking.getBookingId());
                showBookingResult(result);
                if (result.startsWith("SUCCESS")) {
                    generateInvoice(selectedBooking);
                }
                refreshBookingList();
                refreshRoomTable();
                selectedBooking = null;
                selectedBookingCard = null;
            } else {
                showAlert("Please select a booking card first.");
            }
        });
        btnRefresh.setOnAction(e -> refreshBookingList());

        HBox toolbar = new HBox(10, btnBook, btnCheckIn, btnCheckOut, btnRefresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        VBox layout = new VBox(15, headerLabel("Booking Management"), toolbar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        layout.setPadding(new Insets(40));
        return layout;
    }

    private HBox createBookingCard(Booking b) {
        HBox card = new HBox(20);
        card.getStyleClass().add("dashboard-card");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);

        VBox col1 = new VBox(5,
                new Label("Booking ID: " + b.getBookingId()) {
                    {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #1A3626;");
                    }
                },
                new Label("Guest ID: " + b.getGuestId()) {
                    {
                        setStyle("-fx-text-fill: #9CAEA5;");
                    }
                },
                new Label("Room ID: " + b.getRoomId()) {
                    {
                        setStyle("-fx-text-fill: #9CAEA5;");
                    }
                });
        col1.setPrefWidth(180);

        VBox col2 = new VBox(5,
                new Label("Check-In: " + b.getCheckInDate()) {
                    {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #1A3626;");
                    }
                },
                new Label("Check-Out: " + b.getCheckOutDate()) {
                    {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #1A3626;");
                    }
                },
                new Label(b.getNumberOfNights() + " Nights") {
                    {
                        setStyle("-fx-text-fill: #9CAEA5;");
                    }
                });
        col2.setPrefWidth(180);

        Label lblStatus = new Label(b.getPaymentStatus().getDisplayName());
        String statusColor = b.getPaymentStatus().name().equals("PAID") ? "#2ECC71" : "#E74C3C";
        lblStatus.setStyle("-fx-background-color: " + statusColor
                + "; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");

        Label lblAmount = new Label("Rs. " + String.format("%.2f", b.getTotalAmount()));
        lblAmount.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #D4AF37;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(col1, col2, spacer, lblStatus, lblAmount);

        card.setOnMouseClicked(e -> {
            if (selectedBookingCard != null)
                selectedBookingCard.setStyle("");
            selectedBookingCard = card;
            selectedBooking = b;
            card.setStyle("-fx-border-color: #1A3626; -fx-border-width: 2;");
        });
        return card;
    }

    // THREAD MONITOR VIEW
    private Region buildThreadMonitorView() {
        Label info = new Label(
                "Live thread states (from Thread.getState()).\n"
                        + "Each thread uses a different synchronization mechanism — see below.\n");
        info.setStyle("-fx-text-fill: #9CAEA5; -fx-font-size: 13px;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.getStyleClass().add("dashboard-card");

        String[][] threads = {
                { "CheckoutReminderThread", "synchronized block (intrinsic lock on PriorityQueue)", "#E74C3C" },
                { "AutoSaveThread", "ReentrantLock + tryLock(3s) + daemon thread", "#D4AF37" },
                { "OccupancyReporter", "ScheduledExecutorService + ConcurrentHashMap + AtomicInteger", "#2ECC71" },
                { "BookingProcessor", "wait() / notifyAll() — Producer-Consumer pattern", "#1A3626" },
                { "RoomStatusUpdater", "volatile boolean — cross-thread visibility", "#9B59B6" },
        };

        grid.add(boldLabel("Thread Name"), 0, 0);
        grid.add(boldLabel("Sync Mechanism"), 1, 0);
        grid.add(boldLabel("Status"), 2, 0);

        for (int i = 0; i < threads.length; i++) {
            Label name = new Label(threads[i][0]);
            Label mech = new Label(threads[i][1]);
            Label state = new Label("● RUNNING");
            name.setStyle("-fx-text-fill: #1A3626; -fx-font-weight: bold;");
            mech.setStyle("-fx-text-fill: #9CAEA5; -fx-font-size: 12px;");
            state.setStyle("-fx-text-fill: " + threads[i][2] + "; -fx-font-weight: bold;");
            grid.add(name, 0, i + 1);
            grid.add(mech, 1, i + 1);
            grid.add(state, 2, i + 1);
        }

        Label syncSummary = new Label(
                "\nSynchronization primitives used in this project:\n"
                        + "  1. synchronized block   — locks a specific object (intrinsic lock)\n"
                        + "  2. synchronized method  — locks 'this' instance\n"
                        + "  3. ReentrantLock        — explicit lock with tryLock(timeout)\n"
                        + "  4. ConcurrentHashMap    — lock-free thread-safe map\n"
                        + "  5. AtomicInteger        — hardware CAS, no synchronized needed\n"
                        + "  6. wait() / notifyAll() — monitor object, producer-consumer\n"
                        + "  7. volatile             — memory visibility across CPU caches\n");
        syncSummary.setStyle("-fx-text-fill: #9CAEA5; -fx-font-family: monospace; -fx-font-size: 13px;");

        VBox layout = new VBox(15, headerLabel("Thread Monitor"), info, grid, syncSummary);
        layout.setPadding(new Insets(40));

        ScrollPane sp = new ScrollPane(layout);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: #FAFAFA;");
        return sp;
    }

    // DIALOGS
    private void showAddRoomDialog() {
        Dialog<Room> dlg = new Dialog<>();
        dlg.setTitle("Add New Room");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().getStyleClass().add("dialog-pane");

        GridPane grid = dialogGrid();
        TextField fId = styledField("e.g. R301");
        TextField fFloor = styledField("e.g. 3");
        TextField fOcc = styledField("e.g. 2");
        TextField fDesc = styledField("Description");
        ComboBox<RoomType> fType = new ComboBox<>();
        fType.getItems().addAll(RoomType.values());
        fType.setValue(RoomType.STANDARD);
        CheckBox fBreakfast = new CheckBox("Breakfast Included");
        CheckBox fButler = new CheckBox("Butler Service");
        CheckBox fMinibar = new CheckBox("Minibar Access");
        fBreakfast.setStyle("-fx-text-fill: #1A3626;");
        fButler.setStyle("-fx-text-fill: #1A3626;");
        fMinibar.setStyle("-fx-text-fill: #1A3626;");

        grid.addRow(0, lbl("Room ID:"), fId);
        grid.addRow(1, lbl("Floor:"), fFloor);
        grid.addRow(2, lbl("Max Occ:"), fOcc);
        grid.addRow(3, lbl("Type:"), fType);
        grid.addRow(4, lbl("Description:"), fDesc);
        grid.addRow(5, new Label(), fBreakfast);
        grid.addRow(6, new Label(), fButler);
        grid.addRow(7, new Label(), fMinibar);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    String id = RoomIdValidator.validate(fId.getText());
                    int floor = Integer.parseInt(fFloor.getText().trim());
                    int occ = Integer.parseInt(fOcc.getText().trim());
                    String desc = fDesc.getText().trim();
                    if (floor <= 0) {
                        throw new IllegalArgumentException("Floor must be greater than 0.");
                    }
                    if (occ <= 0) {
                        throw new IllegalArgumentException("Max occupancy must be greater than 0.");
                    }
                    if (desc.isEmpty()) {
                        throw new IllegalArgumentException("Description cannot be blank.");
                    }
                    Room room;
                    switch (fType.getValue()) {
                        case DELUXE -> room = new DeluxeRoom(id, floor, occ, desc, fBreakfast.isSelected());
                        case SUITE ->
                            room = new SuiteRoom(id, floor, occ, desc, fButler.isSelected(), fMinibar.isSelected());
                        default -> room = new StandardRoom(id, floor, occ, desc);
                    }
                    return room;
                } catch (Exception ex) {
                    showAlert(ex.getMessage());
                    return null;
                }
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
        dlg.getDialogPane().getStyleClass().add("dialog-pane");

        GridPane grid = dialogGrid();
        TextField fId = styledField("e.g. G001");
        TextField fName = styledField("Full Name");
        TextField fPhone = styledField("Phone Number");
        TextField fEmail = styledField("Email");
        TextField fAge = styledField("Age");
        TextField fIdType = styledField("Aadhar / Passport");
        TextField fIdNum = styledField("ID Number");

        grid.addRow(0, lbl("Guest ID:"), fId);
        grid.addRow(1, lbl("Name:"), fName);
        grid.addRow(2, lbl("Phone:"), fPhone);
        grid.addRow(3, lbl("Email:"), fEmail);
        grid.addRow(4, lbl("Age:"), fAge);
        grid.addRow(5, lbl("ID Type:"), fIdType);
        grid.addRow(6, lbl("ID Number:"), fIdNum);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    return new Guest(fId.getText().trim(), fName.getText().trim(),
                            fPhone.getText().trim(), fEmail.getText().trim(),
                            Integer.valueOf(fAge.getText().trim()),
                            fIdType.getText().trim(), fIdNum.getText().trim());
                } catch (Exception ex) {
                    return null;
                }
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
        dlg.getDialogPane().getStyleClass().add("dialog-pane");

        GridPane grid = dialogGrid();
        TextField fGuestId = styledField("Guest ID");
        TextField fRoomId = styledField("Room ID");
        DatePicker fCheckIn = new DatePicker(LocalDate.now());
        DatePicker fCheckOut = new DatePicker(LocalDate.now().plusDays(1));

        grid.addRow(0, lbl("Guest ID:"), fGuestId);
        grid.addRow(1, lbl("Room ID:"), fRoomId);
        grid.addRow(2, lbl("Check-In:"), fCheckIn);
        grid.addRow(3, lbl("Check-Out:"), fCheckOut);
        dlg.getDialogPane().setContent(grid);

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

    // PUBLIC CALLBACKS
    public void updateDashboard(ConcurrentHashMap<String, Double> data) {
        lblTotal.setText("Total Rooms\n" + data.getOrDefault("total", 0.0).intValue());
        lblAvailable.setText("Available\n" + data.getOrDefault("available", 0.0).intValue());
        lblOccupied.setText("Occupied\n" + data.getOrDefault("occupied", 0.0).intValue());
        lblRevenue.setText("Revenue\nRs. " + String.format("%,.0f", data.getOrDefault("revenue", 0.0)));
    }

    public void showAlert(String message) {
        alertBannerText.setText(message == null || message.trim().isEmpty()
                ? "An unexpected error occurred."
                : message);
        alertBanner.setVisible(true);
        alertHideTransition.stop();
        alertHideTransition.playFromStart();
    }

    private void hideAlert() {
        alertHideTransition.stop();
        alertBanner.setVisible(false);
        alertBannerText.setText("");
    }

    public void showBookingResult(String result) {
        Alert alert = new Alert(result.startsWith("SUCCESS")
                ? Alert.AlertType.INFORMATION
                : Alert.AlertType.ERROR);
        alert.setTitle("Booking Result");
        alert.setHeaderText(null);
        alert.setContentText(result);
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    public void refreshRoomTable() {
        roomList.setAll(roomRepo.getAllSorted());
    }

    // HELPERS
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
            Optional<Room> r = roomRepo.findById(b.getRoomId());
            String gName = g.map(Guest::getName).orElse("Unknown");
            String rDesc = r.map(Room::toString).orElse("Unknown");
            String path = invoiceExporter.exportInvoice(b, gName, rDesc, b.getTotalAmount());
            showBookingResult("SUCCESS: Invoice saved to:\n" + path);
            showInvoicePreview(path);
        } catch (Exception ex) {
            logger.error("Invoice error: " + ex.getMessage());
        }
    }

    private void showInvoicePreview(String invoicePath) {
        try {
            String invoiceText = Files.readString(Path.of(invoicePath), StandardCharsets.UTF_8);

            Dialog<Void> dlg = new Dialog<>();
            dlg.setTitle("Invoice Preview");
            dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dlg.getDialogPane().getStyleClass().add("dialog-pane");

            TextArea invoiceArea = new TextArea(invoiceText);
            invoiceArea.setEditable(false);
            invoiceArea.setWrapText(false);
            invoiceArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
            invoiceArea.setPrefSize(700, 420);

            VBox content = new VBox(10,
                    boldLabel("Generated Invoice"),
                    invoiceArea,
                    new Label("File: " + invoicePath));
            content.setPadding(new Insets(16));

            dlg.getDialogPane().setContent(content);
            dlg.showAndWait();
        } catch (Exception ex) {
            logger.error("Invoice preview error: " + ex.getMessage());
            showBookingResult("ERROR: Invoice created but preview could not be opened.");
        }
    }

    private void roomFileManager_write(int idx, Room room) {
        bookingService.registerRoomIndex(room.getRoomId(), idx);
    }

    private Label statCard(String title, String value) {
        Label l = new Label(title + "\n" + value);
        l.getStyleClass().add("stat-card");
        l.setStyle("-fx-text-fill: #1A3626; -fx-font-size: 18px; -fx-font-weight: bold;");
        l.setMinWidth(180);
        l.setAlignment(Pos.CENTER_LEFT);
        return l;
    }

    private Label headerLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("header-label");
        return l;
    }

    private Label boldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #1A3626; -fx-font-weight: bold; -fx-font-size: 14px;");
        return l;
    }

    private Button actionButton(String text, String colorHex) {
        Button b = new Button(text);
        b.getStyleClass().add("button");
        if ("#D4AF37".equals(colorHex)) {
            b.getStyleClass().add("button-gold");
        } else if ("#E74C3C".equals(colorHex)) {
            b.setStyle("-fx-background-color: #8B0000; -fx-text-fill: white;");
        }
        return b;
    }

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setPrefWidth(220);
        return f;
    }

    private GridPane dialogGrid() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(12);
        g.setPadding(new Insets(20));
        return g;
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #2C3E50; -fx-font-weight: bold;");
        return l;
    }
}
