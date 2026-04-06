package com.hms;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.Label;
public class ServiceThreads {
    public static class BillGenerationThread implements Runnable {

        private final Customer customer;
        private final String outputDir;
        private final Consumer<String> onSuccess;
        private final Consumer<String> onError;

        public BillGenerationThread(Customer customer, String outputDir,
                                    Consumer<String> onSuccess, Consumer<String> onError) {
            this.customer  = customer;
            this.outputDir = outputDir;
            this.onSuccess = onSuccess;
            this.onError   = onError;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(200);
                java.io.File bill = BillGenerator.generateBill(customer, outputDir);
                Platform.runLater(() -> onSuccess.accept(bill.getAbsolutePath()));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }

        public static Thread start(Customer customer, String dir,
                                   Consumer<String> onSuccess, Consumer<String> onError) {
            Thread t = new Thread(new BillGenerationThread(customer, dir, onSuccess, onError),
                    "BillGenThread-" + customer.getRoomNumber());
            t.setDaemon(true);
            t.start();
            return t;
        }
    }

    public static class AutoSaveThread extends Thread {

        private final BookingManager manager;
        private volatile boolean running = true;
        private static final long INTERVAL_MS = 30_000;

        public AutoSaveThread(BookingManager manager) {
            super("AutoSaveThread");
            this.manager = manager;
            setDaemon(true);
        }

        @Override
        public void run() {
            System.out.println("[AutoSave] Thread started.");
            while (running) {
                try {
                    Thread.sleep(INTERVAL_MS);
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

    public static class RoomStatusThread implements Runnable {

        private final BookingManager manager;
        private final Label statusLabel;

        public RoomStatusThread(BookingManager manager, Label statusLabel) {
            this.manager     = manager;
            this.statusLabel = statusLabel;
        }

        @Override
        public void run() {
            System.out.println("[RoomStatus] Scanning rooms...");
            for (Room r : manager.getRoomsSortedByNumber()) {
                Thread.yield();
                String status = "Room " + r.getRoomNumber()
                        + " [" + r.getRoomType() + "] → " + r.getStatus();
                System.out.println("[RoomStatus] " + status);
                try {
                    Thread.sleep(50);
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

    public static void demoConcurrentBooking(BookingManager manager,
                                             int roomNumber,
                                             Consumer<String> resultCallback) {
        Runnable attempt = () -> {
            String threadName = Thread.currentThread().getName();
            Customer fake = new Customer(threadName, "demo", roomNumber, "Demo", 0);

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
