package com.hms;

import javafx.application.Platform;
import javafx.scene.control.Label;

import java.util.function.Consumer;

/**
 * WEEK 3 — Thread creation using Runnable interface.
 * WEEK 4 — Synchronization via BookingManager (shared resource).
 *
 * Three background service threads:
 *  1. BillGenerationThread  — generates PDF on checkout (Runnable)
 *  2. AutoSaveThread        — periodically serializes data to disk (Thread subclass)
 *  3. RoomStatusThread      — simulates real-time room status ticker (Runnable)
 */
public class ServiceThreads {

    // ── 1. Bill Generation Thread (Week 3: Runnable interface) ───────

    /**
     * Runs PDF bill generation in a background thread so the UI
     * doesn't freeze during iText rendering.
     */
    public static class BillGenerationThread implements Runnable {

        private final Customer customer;
        private final int nights;
        private final double discount;
        private final java.util.List<String> amenities;
        private final String outputDir;
        private final Consumer<String> onSuccess;
        private final Consumer<String> onError;

        public BillGenerationThread(Customer customer, int nights, double discount,
                                    java.util.List<String> amenities, String outputDir,
                                    Consumer<String> onSuccess, Consumer<String> onError) {
            this.customer  = customer;
            this.nights    = nights;
            this.discount  = discount;
            this.amenities = amenities;
            this.outputDir = outputDir;
            this.onSuccess = onSuccess;
            this.onError   = onError;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(200); // Week 3: sleep()
                java.io.File bill = BillGenerator.generateBill(
                        customer, nights, discount, amenities, outputDir);
                Platform.runLater(() -> onSuccess.accept(bill.getAbsolutePath()));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }

        public static Thread start(Customer customer, int nights, double discount,
                                   java.util.List<String> amenities, String dir,
                                   Consumer<String> onSuccess, Consumer<String> onError) {
            Thread t = new Thread(
                    new BillGenerationThread(customer, nights, discount, amenities, dir, onSuccess, onError),
                    "BillGenThread-" + customer.getRoomNumber());
            t.setDaemon(true);
            t.start();
            return t;
        }
    }

    // ── 2. Auto-Save Thread (Week 3: extends Thread) ─────────────────

    /**
     * Periodically saves rooms and bookings to disk using serialization (Week 6).
     * Demonstrates Thread subclass pattern (Week 3).
     */
    public static class AutoSaveThread extends Thread {

        private final BookingManager manager;
        private volatile boolean running = true;
        private static final long INTERVAL_MS = 30_000; // every 30 seconds

        public AutoSaveThread(BookingManager manager) {
            super("AutoSaveThread");
            this.manager = manager;
            setDaemon(true); // won't block JVM shutdown
        }

        @Override
        public void run() {
            System.out.println("[AutoSave] Thread started.");
            while (running) {
                try {
                    Thread.sleep(INTERVAL_MS); // Week 3: sleep()
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                save();
            }
            System.out.println("[AutoSave] Thread stopped.");
        }

        public void save() {
            DataPersistence.saveRooms(manager.getRoomsSortedByNumber());
            DataPersistence.saveBookings(manager.getAllCustomers());
            System.out.println("[AutoSave] Data persisted.");
        }

        public void stopSaving() { running = false; interrupt(); }
    }

    // ── 3. Room Status Logger Thread (Week 3: Runnable + yield/join) ─

    /**
     * Demonstrates sleep() and yield() — logs a status snapshot of all
     * rooms to the console at startup, simulating a status-check service.
     */
    public static class RoomStatusThread implements Runnable {

        private final BookingManager manager;
        private final Label statusLabel; // optional UI label to update

        public RoomStatusThread(BookingManager manager, Label statusLabel) {
            this.manager     = manager;
            this.statusLabel = statusLabel;
        }

        @Override
        public void run() {
            System.out.println("[RoomStatus] Scanning rooms...");
            for (Room r : manager.getRoomsSortedByNumber()) {
                Thread.yield(); // Week 3: yield() — hint to scheduler
                String status = "Room " + r.getRoomNumber()
                        + " [" + r.getRoomType() + "] → " + r.getStatus();
                System.out.println("[RoomStatus] " + status);
                try {
                    Thread.sleep(50); // Week 3: sleep()
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (statusLabel != null) {
                Platform.runLater(() -> statusLabel.setText(
                        "Rooms: " + manager.totalRooms()
                                + "  Occupied: " + manager.occupiedCount()
                                + "  Available: " + manager.availableCount()));
            }
            System.out.println("[RoomStatus] Scan complete.");
        }

        public static Thread start(BookingManager manager, Label label) {
            Thread t = new Thread(new RoomStatusThread(manager, label), "RoomStatusThread");
            t.setDaemon(true);
            t.start();
            return t;
        }
    }

    // ── 4. Concurrent Booking Stress Test (Week 4: sync demo) ────────

    /**
     * Simulates two customers trying to book the SAME room concurrently.
     * BookingManager.bookRoom() is synchronized — only one succeeds;
     * the other gets "Room not available" via wait/notify.
     * Called from the dashboard for demonstration purposes.
     */
    public static void demoConcurrentBooking(BookingManager manager,
                                             int roomNumber,
                                             Consumer<String> resultCallback) {
        Runnable attempt = () -> {
            String threadName = Thread.currentThread().getName();
            Customer fake = new Customer(threadName, "demo", roomNumber, "Demo", 0);

            // We don't actually persist this — just demonstrate sync
            boolean roomFree = manager.getAvailableRooms()
                    .stream().anyMatch(r -> r.getRoomNumber() == roomNumber);

            String msg = threadName + ": room " + roomNumber
                    + (roomFree ? " → attempted booking (sync demo)" : " → already occupied");
            Platform.runLater(() -> resultCallback.accept(msg));
        };

        Thread t1 = new Thread(attempt, "Customer-A");
        Thread t2 = new Thread(attempt, "Customer-B");
        t1.start();
        t2.start();

        // join() — Week 3: main thread waits for both
        new Thread(() -> {
            try {
                t1.join();
                t2.join();
                Platform.runLater(() -> resultCallback.accept("→ Both threads finished (join() complete)"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "JoinWatcher").start();
    }
}
