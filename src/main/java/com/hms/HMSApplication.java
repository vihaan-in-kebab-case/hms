package com.hms;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;

/**
 * HMS Main Application
 *
 * Concepts demonstrated per lab week:
 *  Week 2  — RoomType enum (ComboBox populated from enum values)
 *  Week 3  — ServiceThreads.BillGenerationThread (Runnable), AutoSaveThread (Thread)
 *  Week 4  — BookingManager synchronized methods; wait()/notify() on concurrent booking
 *  Week 5  — DataPersistence: FileWriter log append, FileReader log read, byte-stream copy
 *  Week 6  — DataPersistence: ObjectOutputStream/ObjectInputStream for rooms & bookings
 *  Week 7  — Pair<T,U> generic class; PriceCalculator<T extends Number>
 *  Week 8  — ObservableList wraps ArrayList; Collections.sort via BookingManager
 *  Week 9  — JavaFX: TabPane, TableView, GridPane, ComboBox, event handling, CSS
 */
public class HMSApplication extends Application {

    // ── Core shared-resource (Week 4 sync) ──────────────────────────
    private final BookingManager manager = new BookingManager();

    // ── JavaFX observable wrappers of BookingManager collections ─────
    private final ObservableList<Room>     roomObs     = FXCollections.observableArrayList();
    private final ObservableList<Customer> customerObs = FXCollections.observableArrayList();
    private final ObservableList<LogEntry> logObs      = FXCollections.observableArrayList();

    // ── Background threads (Week 3) ──────────────────────────────────
    private ServiceThreads.AutoSaveThread autoSaveThread;

    // ── Dashboard labels ─────────────────────────────────────────────
    private Label statTotal, statOccupied, statAvailable, statRevenue;

    // ── Room selector for booking tab ────────────────────────────────
    private final ComboBox<Integer> roomSelector = new ComboBox<>();

    private boolean showAvailableOnly = false;

    // ════════════════════════════════════════════════════════════════
    //  START
    // ════════════════════════════════════════════════════════════════
    @Override
    public void start(Stage stage) {
        loadPersistedData();   // Week 6: deserialization
        seedDemoIfEmpty();

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().addAll(
                new Tab("⌂  Dashboard",    buildDashboard()),
                new Tab("🚪  Rooms",        buildRoomManagement()),
                new Tab("📋  Booking",      buildBookingSystem()),
                new Tab("📜  Activity Log", buildLogTab())
        );

        Scene scene = new Scene(tabs, 1080, 720);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("HMS — Hotel Management System");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setOnCloseRequest(e -> shutdown());
        stage.show();

        // Week 3: start background threads
        autoSaveThread = new ServiceThreads.AutoSaveThread(manager);
        autoSaveThread.start();

        Label dummy = new Label();
        ServiceThreads.RoomStatusThread.start(manager, dummy);

        refreshObservables();
    }

    // ════════════════════════════════════════════════════════════════
    //  DASHBOARD
    // ════════════════════════════════════════════════════════════════
    private VBox buildDashboard() {
        Label title    = label("Admin Dashboard", "page-title");
        Label subtitle = label("Live overview — data persists across sessions", "page-subtitle");

        statTotal     = new Label("0");
        statOccupied  = new Label("0");
        statAvailable = new Label("0");
        statRevenue   = new Label("₹0");

        HBox cards = new HBox(16,
                statCard("Total Rooms",   statTotal,    "stat-card-blue"),
                statCard("Occupied",      statOccupied, "stat-card-red"),
                statCard("Available",     statAvailable,"stat-card-green"),
                statCard("Revenue (GST)", statRevenue,  "stat-card-gold")
        );

        // Occupancy bar
        ProgressBar occBar = new ProgressBar(0);
        occBar.getStyleClass().add("occ-bar");
        occBar.setMaxWidth(Double.MAX_VALUE);
        occBar.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            if (roomObs.isEmpty()) return 0.0;
            long occ = roomObs.stream().filter(r -> !r.isAvailable()).count();
            return (double) occ / roomObs.size();
        }, roomObs));

        Label occPct = new Label("0%");
        occPct.getStyleClass().add("occ-percent");
        occPct.textProperty().bind(Bindings.createStringBinding(() -> {
            if (roomObs.isEmpty()) return "0%";
            long occ = roomObs.stream().filter(r -> !r.isAvailable()).count();
            return String.format("%.0f%%", (double) occ / roomObs.size() * 100);
        }, roomObs));

        HBox occRow = new HBox(10, occBar, occPct);
        occRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(occBar, Priority.ALWAYS);

        // Recent activity table
        Label recentLbl = label("Recent Activity", "section-label");
        TableView<LogEntry> recentTable = buildLogTable();
        recentTable.setItems(logObs);
        recentTable.setMaxHeight(200);

        VBox layout = new VBox(8,
                title, subtitle, cards,
                label("Occupancy Rate", "section-label"), occRow,
                recentLbl, recentTable);
        layout.setPadding(new Insets(24));
        layout.getStyleClass().add("tab-content");
        VBox.setVgrow(recentTable, Priority.ALWAYS);
        return layout;
    }

    private VBox statCard(String lbl, Label val, String cls) {
        val.getStyleClass().add("stat-value");
        Label l = new Label(lbl); l.getStyleClass().add("stat-label");
        VBox card = new VBox(6, val, l);
        card.getStyleClass().addAll("stat-card", cls);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20, 24, 20, 24));
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // ════════════════════════════════════════════════════════════════
    //  ROOM MANAGEMENT
    // ════════════════════════════════════════════════════════════════
    private VBox buildRoomManagement() {
        Label title = label("Room Management", "page-title");

        TextField roomNum = field("e.g. 101");
        // Week 2: ComboBox populated from RoomType enum values
        ComboBox<RoomType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(RoomType.values());
        typeBox.setPromptText("Room type");
        typeBox.getStyleClass().add("combo");

        // Week 8: sort options
        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll("Sort by Room No", "Sort by Price");
        sortBox.setValue("Sort by Room No");
        sortBox.getStyleClass().add("combo");

        TextField search = field("🔍  Search rooms...");
        search.getStyleClass().add("search-field");

        Button addBtn    = btn("➕  Add Room",      "btn-primary");
        Button deleteBtn = btn("🗑  Delete Selected","btn-danger");
        Button toggleBtn = btn("👁  Available Only", "btn-secondary");

        // Table — bound to roomObs (Week 8: ObservableList)
        TableView<Room> table = new TableView<>();
        table.getStyleClass().add("data-table");
        FilteredList<Room> filtered = new FilteredList<>(roomObs, p -> true);
        table.setItems(filtered);
        table.getColumns().addAll(
                col("Room No","roomNumber",85), col("Type","roomType",110),
                col("₹/Night","price",110),     statusCol());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Live search filter
        search.textProperty().addListener((o, ov, nv) -> applyRoomFilter(filtered, nv, showAvailableOnly));
        sortBox.setOnAction(e -> {
            ArrayList<Room> sorted = sortBox.getValue().contains("Price")
                    ? manager.getRoomsSortedByPrice()
                    : manager.getRoomsSortedByNumber();
            roomObs.setAll(sorted);
        });

        // Week 7: price label showing enum tariff
        Label tariffHint = new Label();
        tariffHint.getStyleClass().add("form-label");
        typeBox.setOnAction(e -> {
            RoomType rt = typeBox.getValue();
            if (rt != null) tariffHint.setText(
                    "Base tariff: ₹" + rt.getBaseTariff()
                    + "/night  |  3-night cost: ₹" + rt.calculateCost(3)
                    + "  |  with GST: ₹" + String.format("%.2f", rt.calculateCostWithTax(3)));
        });

        // Add room
        addBtn.setOnAction(e -> {
            // ── Edge-case validation (Validator) ──────────────────────
            java.util.List<String> errs = Validator.validateNewRoom(roomNum.getText(), typeBox.getValue());
            if (!errs.isEmpty()) { toast("⚠  " + Validator.format(errs)); return; }

            int num = Integer.parseInt(roomNum.getText().trim());

            // Week 2: use enum to create room with correct base tariff
            Room r = new Room(num, typeBox.getValue());
            if (!manager.addRoom(r)) {
                // Duplicate room number
                toast("⚠  Room " + num + " already exists. Choose a different number."); return;
            }

            refreshObservables();
            DataPersistence.saveRooms(manager.getRoomsSortedByNumber()); // Week 6
            roomNum.clear(); typeBox.setValue(null); tariffHint.setText("");
            toast("✅  Room " + num + " (" + r.getRoomType() + ") added successfully.");
        });

        // Delete room
        deleteBtn.setOnAction(e -> {
            Room sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { toast("⚠  Select a room first."); return; }
            if (!manager.removeRoom(sel.getRoomNumber())) {
                toast("⚠  Cannot delete an occupied room."); return;
            }
            refreshObservables();
            DataPersistence.saveRooms(manager.getRoomsSortedByNumber());
            toast("🗑  Room " + sel.getRoomNumber() + " deleted.");
        });

        // Toggle filter
        toggleBtn.setOnAction(e -> {
            showAvailableOnly = !showAvailableOnly;
            toggleBtn.setText(showAvailableOnly ? "👁  Show All Rooms" : "👁  Available Only");
            applyRoomFilter(filtered, search.getText(), showAvailableOnly);
        });

        GridPane form = formGrid("Room Number", roomNum, "Room Type", typeBox, "Tariff Info", tariffHint);
        HBox actions = new HBox(10, addBtn, deleteBtn, toggleBtn, new Spacer(), sortBox, search);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox layout = new VBox(10, title, form, actions, table);
        layout.setPadding(new Insets(24));
        layout.getStyleClass().add("tab-content");
        VBox.setVgrow(table, Priority.ALWAYS);
        return layout;
    }

    // ════════════════════════════════════════════════════════════════
    //  BOOKING & CHECKOUT
    // ════════════════════════════════════════════════════════════════
    private VBox buildBookingSystem() {
        Label title = label("Booking & Checkout", "page-title");

        TextField nameF     = field("Guest full name");
        TextField contactF  = field("Phone / Email");
        TextField discountF = field("Discount % (0–100)");

        // ── Nights spinner ────────────────────────────────────────────
        Spinner<Integer> nightsSpinner = new Spinner<>(1, 365, 1);
        nightsSpinner.setEditable(true);
        nightsSpinner.getStyleClass().add("hms-field");
        nightsSpinner.setPrefWidth(100);

        // ── Amenity checkboxes ────────────────────────────────────────
        CheckBox cbBreakfast = new CheckBox("Breakfast  (+₹350/night)");
        CheckBox cbWifi      = new CheckBox("Wi-Fi  (+₹199 flat)");
        CheckBox cbCab       = new CheckBox("Cab Service  (+₹750/trip)");
        cbBreakfast.getStyleClass().add("hms-check");
        cbWifi.getStyleClass().add("hms-check");
        cbCab.getStyleClass().add("hms-check");
        HBox amenitiesBox = new HBox(20, cbBreakfast, cbWifi, cbCab);
        amenitiesBox.setAlignment(Pos.CENTER_LEFT);
        amenitiesBox.setPadding(new Insets(4, 0, 4, 0));

        roomSelector.getStyleClass().add("combo");
        roomSelector.setPromptText("Select room");

        Button bookBtn     = btn("✔  Book Room",   "btn-primary");
        Button checkoutBtn = btn("↩  Checkout",    "btn-danger");

        // Available rooms mini-table (click to select)
        FilteredList<Room> avail = new FilteredList<>(roomObs, Room::isAvailable);
        TableView<Room> availTable = new TableView<>(avail);
        availTable.getStyleClass().add("data-table");
        availTable.getColumns().addAll(
                col("Room","roomNumber",70), col("Type","roomType",110), col("₹/Night","price",100));
        availTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        availTable.setMinHeight(160);
        availTable.setPrefHeight(220);
        availTable.setOnMouseClicked(e -> {
            Room sel = availTable.getSelectionModel().getSelectedItem();
            if (sel != null) roomSelector.setValue(sel.getRoomNumber());
        });

        // Bookings table
        TableView<Customer> bookTable = new TableView<>(customerObs);
        bookTable.getStyleClass().add("data-table");
        bookTable.getColumns().addAll(
                col("Guest","name",160), col("Contact","contact",140),
                col("Room","roomNumber",70), col("Type","roomType",100),
                col("Check-In","checkInTime",160));
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Book — runs booking through synchronized BookingManager (Week 4)
        bookBtn.setOnAction(e -> {
            // ── Edge-case validation ──────────────────────────────────
            java.util.List<String> errs = Validator.validateBooking(
                    nameF.getText(), contactF.getText(),
                    roomSelector.getValue(), manager.totalRooms());
            if (!errs.isEmpty()) { toast("⚠  " + Validator.format(errs)); return; }

            int roomNo = roomSelector.getValue();

            // Guard: room selector could be stale if another booking just happened
            boolean stillAvailable = manager.getAvailableRooms()
                    .stream().anyMatch(r -> r.getRoomNumber() == roomNo);
            if (!stillAvailable) {
                toast("⚠  Room " + roomNo + " was just booked by someone else.\n"
                    + "Please select another available room.");
                updateRoomDropdown();   // refresh dropdown immediately
                roomSelector.setValue(null);
                return;
            }

            // Find room type/price for Customer object
            String rt = ""; double price = 0;
            for (Room r : manager.getRoomsSortedByNumber()) {
                if (r.getRoomNumber() == roomNo) { rt = r.getRoomType(); price = r.getPrice(); break; }
            }
            Customer c = new Customer(nameF.getText().trim(), contactF.getText().trim(),
                                      roomNo, rt, price);

            // Week 4: synchronized bookRoom (wait/notify inside)
            Pair<Boolean,String> result = manager.bookRoom(c);
            if (result.getFirst()) {
                DataPersistence.appendLog(manager.getLogs().get(manager.getLogs().size()-1)); // Week 5
                DataPersistence.saveBookings(manager.getAllCustomers());                       // Week 6
                refreshObservables();
                nameF.clear(); contactF.clear(); roomSelector.setValue(null);
                toast("✅  " + result.getSecond());
            } else {
                toast("⚠  " + result.getSecond());
            }
        });

        // Checkout — bill generated in background thread (Week 3)
        checkoutBtn.setOnAction(e -> {
            Customer sel = bookTable.getSelectionModel().getSelectedItem();

            // ── Credential validation ─────────────────────────────────
            java.util.List<String> credErrs = Validator.validateCheckout(
                    nameF.getText(), contactF.getText(), sel);
            if (!credErrs.isEmpty()) { toast("⚠  " + Validator.format(credErrs)); return; }

            // ── Discount validation ───────────────────────────────────
            java.util.List<String> discErrs = Validator.validateDiscount(discountF.getText());
            if (!discErrs.isEmpty()) { toast("⚠  " + Validator.format(discErrs)); return; }

            // ── Nights from spinner ───────────────────────────────────
            int nights = nightsSpinner.getValue();

            // ── Collect selected amenities ────────────────────────────
            java.util.List<String> amenities = new java.util.ArrayList<>();
            if (cbBreakfast.isSelected()) amenities.add("Breakfast");
            if (cbWifi.isSelected())      amenities.add("Wi-Fi");
            if (cbCab.isSelected())       amenities.add("Cab Service");

            double disc = 0;
            try { disc = Double.parseDouble(discountF.getText().trim()); } catch (Exception ignored) {}

            // Week 7: PriceCalculator bounded generic
            PriceCalculator<Double> calc = new PriceCalculator<>(sel.getPricePerNight(), disc);
            double roomTotal = calc.discountedPrice(nights);
            double amenityTotal = (cbBreakfast.isSelected() ? BillGenerator.PRICE_BREAKFAST * nights : 0)
                                + (cbWifi.isSelected()      ? BillGenerator.PRICE_WIFI : 0)
                                + (cbCab.isSelected()       ? BillGenerator.PRICE_CAB  : 0);
            double grandTotal = (roomTotal + amenityTotal) * 1.12;

            String amenitySummary = amenities.isEmpty() ? "None" : String.join(", ", amenities);
            String billInfo = String.format("₹%.2f total | %d night(s) | %.0f%% disc | %s",
                    grandTotal, nights, disc, amenitySummary);

            // Week 4: synchronized releaseRoom → notifyAll()
            Pair<Boolean,String> rel = manager.releaseRoom(sel.getRoomNumber(), billInfo);
            if (!rel.getFirst()) { toast("⚠  " + rel.getSecond()); return; }

            // Week 3: BillGenerationThread (Runnable) — background PDF
            String dir = System.getProperty("user.home") + File.separator + "HMS_Bills";
            new File(dir).mkdirs();
            final double fDisc = disc;
            ServiceThreads.BillGenerationThread.start(sel, nights, fDisc, amenities, dir,
                path -> {
                    DataPersistence.appendLog(manager.getLogs().get(manager.getLogs().size()-1));
                    DataPersistence.saveBookings(manager.getAllCustomers());
                    refreshObservables();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Checkout Complete");
                    alert.setHeaderText("Bill saved: " + new File(path).getName());
                    alert.setContentText(billInfo + "\nPath: " + path);
                    try { alert.getDialogPane().getStylesheets().add(
                            getClass().getResource("/styles.css").toExternalForm()); }
                    catch (Exception ignored) {}
                    ButtonType openBtn = new ButtonType("Open Bill");
                    alert.getButtonTypes().add(openBtn);
                    alert.showAndWait().ifPresent(bt -> {
                        if (bt == openBtn) {
                            try { java.awt.Desktop.getDesktop().open(new File(path)); }
                            catch (Exception ex) { toast("Cannot open PDF."); }
                        }
                    });
                },
                err -> toast("⚠  Bill error: " + err)
            );

            nameF.clear(); contactF.clear(); discountF.clear();
            nightsSpinner.getValueFactory().setValue(1);
            cbBreakfast.setSelected(false); cbWifi.setSelected(false); cbCab.setSelected(false);
        });

        GridPane form = formGrid(
                "Guest Name",   nameF,
                "Contact",      contactF,
                "Discount %",   discountF,
                "No. of Nights",nightsSpinner,
                "Room No",      roomSelector);

        Label amenitiesLabel = label("Amenities (charged at checkout)", "section-label");

        Label checkoutHint = new Label(
                "ℹ  To checkout: select a row in the bookings table, then enter the "
              + "exact guest name and contact to confirm identity.");
        checkoutHint.getStyleClass().add("hint-label");
        checkoutHint.setWrapText(true);

        HBox actions = new HBox(12, bookBtn, checkoutBtn);

        VBox layout = new VBox(10,
                title, form, amenitiesLabel, amenitiesBox, actions, checkoutHint,
                label("Available Rooms  (click to select)", "section-label"), availTable,
                label("Current Bookings  (select to checkout)", "section-label"), bookTable);
        layout.setPadding(new Insets(24));
        layout.getStyleClass().add("tab-content");
        VBox.setVgrow(bookTable, Priority.ALWAYS);
        return layout;
    }

    // ════════════════════════════════════════════════════════════════
    //  LOG TAB  (Week 5 character stream export)
    // ════════════════════════════════════════════════════════════════
    private VBox buildLogTab() {
        Label title = label("Activity Log", "page-title");

        TextField search = field("🔍  Search logs...");
        search.getStyleClass().add("search-field");

        Button clearBtn  = btn("🗑  Clear", "btn-danger");
        Button exportBtn = btn("📤  Export Copy", "btn-secondary");

        FilteredList<LogEntry> filtered = new FilteredList<>(logObs, e -> true);
        search.textProperty().addListener((o, ov, nv) -> filtered.setPredicate(log -> {
            if (nv == null || nv.isEmpty()) return true;
            String low = nv.toLowerCase();
            return log.getType().toLowerCase().contains(low)
                    || log.getGuestName().toLowerCase().contains(low)
                    || String.valueOf(log.getRoomNumber()).contains(low)
                    || log.getDetails().toLowerCase().contains(low);
        }));

        clearBtn.setOnAction(e -> {
            logObs.clear();
            toast("Logs cleared from view (file retained).");
        });

        // Week 5: byte-stream copy of the log file
        exportBtn.setOnAction(e -> {
            String dest = System.getProperty("user.home")
                    + File.separator + "HMS_Bills"
                    + File.separator + "activity_export.log";
            new File(System.getProperty("user.home") + File.separator + "HMS_Bills").mkdirs();
            boolean ok = DataPersistence.exportLogCopy(dest);
            toast(ok ? "✅  Log exported to:\n" + dest : "⚠  No log file to export yet.");
        });

        TableView<LogEntry> table = buildLogTable();
        table.setItems(filtered);

        HBox bar = new HBox(10, search, new Spacer(), exportBtn, clearBtn);
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox layout = new VBox(12, title, bar, table);
        layout.setPadding(new Insets(24));
        layout.getStyleClass().add("tab-content");
        VBox.setVgrow(table, Priority.ALWAYS);
        return layout;
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════
    private TableView<LogEntry> buildLogTable() {
        TableView<LogEntry> t = new TableView<>();
        t.getStyleClass().add("data-table");
        TableColumn<LogEntry,String>  ts   = col("Timestamp","timestamp",160);
        TableColumn<LogEntry,String>  type = col("Event","type",110);
        TableColumn<LogEntry,String>  guest= col("Guest","guestName",150);
        TableColumn<LogEntry,Integer> room = col("Room","roomNumber",70);
        TableColumn<LogEntry,String>  det  = col("Details","details",300);
        type.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("log-checkin","log-checkout","log-room","log-default");
                if (empty || item == null) { setText(null); return; }
                setText(item);
                if      (item.equals("CHECK-IN"))  getStyleClass().add("log-checkin");
                else if (item.equals("CHECK-OUT")) getStyleClass().add("log-checkout");
                else if (item.startsWith("ROOM"))  getStyleClass().add("log-room");
                else                               getStyleClass().add("log-default");
            }
        });
        t.getColumns().addAll(ts, type, guest, room, det);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return t;
    }

    private void refreshObservables() {
        Platform.runLater(() -> {
            roomObs.setAll(manager.getRoomsSortedByNumber());   // Week 8: sort
            customerObs.setAll(manager.getAllCustomers());
            logObs.setAll(manager.getLogs());
            updateRoomDropdown();
            refreshStats();
        });
    }

    private void refreshStats() {
        if (statTotal == null) return;
        statTotal.setText(String.valueOf(manager.totalRooms()));
        statOccupied.setText(String.valueOf(manager.occupiedCount()));
        statAvailable.setText(String.valueOf(manager.availableCount()));
        statRevenue.setText(String.format("₹%.0f", manager.computeRevenue()));
    }

    private void updateRoomDropdown() {
        ObservableList<Integer> nums = FXCollections.observableArrayList();
        for (Room r : manager.getAvailableRooms()) nums.add(r.getRoomNumber());
        roomSelector.setItems(nums);
    }

    private void applyRoomFilter(FilteredList<Room> fl, String text, boolean availOnly) {
        fl.setPredicate(r -> {
            boolean avail = !availOnly || r.isAvailable();
            if (text == null || text.isEmpty()) return avail;
            String low = text.toLowerCase();
            return avail && (String.valueOf(r.getRoomNumber()).contains(low)
                    || r.getRoomType().toLowerCase().contains(low)
                    || r.getStatus().toLowerCase().contains(low));
        });
    }

    // Week 6: load serialized data on startup
    private void loadPersistedData() {
        for (Room r : DataPersistence.loadRooms())       manager.loadRoom(r);
        for (Customer c : DataPersistence.loadBookings()) manager.loadBooking(c);
    }

    private void seedDemoIfEmpty() {
        if (manager.totalRooms() > 0) return;
        // Week 2: constructing rooms via RoomType enum
        manager.addRoom(new Room(101, RoomType.SINGLE));
        manager.addRoom(new Room(102, RoomType.DOUBLE));
        manager.addRoom(new Room(201, RoomType.DELUXE));
        manager.addRoom(new Room(202, RoomType.SUITE));
        manager.addRoom(new Room(301, RoomType.PENTHOUSE));
        DataPersistence.saveRooms(manager.getRoomsSortedByNumber());
    }

    private void shutdown() {
        autoSaveThread.save();      // final save on close
        autoSaveThread.stopSaving();
    }

    // ── UI factory helpers ───────────────────────────────────────────
    private Label label(String text, String styleClass) {
        Label l = new Label(text); l.getStyleClass().add(styleClass); return l;
    }
    private TextField field(String prompt) {
        TextField f = new TextField(); f.setPromptText(prompt);
        f.getStyleClass().add("hms-field"); return f;
    }
    private Button btn(String text, String styleClass) {
        Button b = new Button(text); b.getStyleClass().addAll("btn", styleClass); return b;
    }
    @SuppressWarnings("unchecked")
    private <S,T> TableColumn<S,T> col(String title, String prop, double w) {
        TableColumn<S,T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w); return c;
    }
    private TableColumn<Room,String> statusCol() {
        TableColumn<Room,String> c = new TableColumn<>("Status");
        c.setCellValueFactory(new PropertyValueFactory<>("status"));
        c.setPrefWidth(95);
        c.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-available","status-occupied");
                if (empty || item == null) { setText(null); return; }
                setText(item);
                getStyleClass().add(item.equals("Available") ? "status-available" : "status-occupied");
            }
        });
        return c;
    }
    private GridPane formGrid(Object... pairs) {
        GridPane g = new GridPane();
        g.setHgap(16); g.setVgap(10); g.setPadding(new Insets(8, 0, 8, 0));
        for (int i = 0; i < pairs.length; i += 2) {
            Label lbl = new Label(pairs[i].toString()); lbl.getStyleClass().add("form-label");
            javafx.scene.Node nd = (javafx.scene.Node) pairs[i+1];
            g.add(lbl, 0, i/2); g.add(nd, 1, i/2);
            GridPane.setHgrow(nd, Priority.ALWAYS);
        }
        ColumnConstraints c0 = new ColumnConstraints(130);
        ColumnConstraints c1 = new ColumnConstraints(200, 280, Double.MAX_VALUE);
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        return g;
    }
    private void toast(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.setTitle("HMS");
        try { a.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm()); }
        catch (Exception ignored) {}
        a.show();
    }

    static class Spacer extends Region {
        Spacer() { HBox.setHgrow(this, Priority.ALWAYS); }
    }

    public static void main(String[] args) { launch(); }
}
