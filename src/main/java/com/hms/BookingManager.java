package com.hms;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
public class BookingManager {
    private final ArrayList<Room>              rooms    = new ArrayList<>();
    private final HashMap<Integer, Customer>   bookings = new HashMap<>();
    private final ArrayList<LogEntry>          logs     = new ArrayList<>();
    public synchronized Pair<Boolean, String> bookRoom(Customer customer) {
        Room target = findRoom(customer.getRoomNumber());
        if (target == null) return new Pair<>(false, "Room not found.");
        while (!target.isAvailable()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Pair<>(false, "Booking interrupted.");
            }
        }

        target.setAvailable(false);
        bookings.put(customer.getRoomNumber(), customer);
        logs.add(new LogEntry("CHECK-IN", customer.getName(),
                customer.getRoomNumber(),
                "Type: " + customer.getRoomType()
                        + " | ₹" + customer.getPricePerNight() + "/night"));
        return new Pair<>(true, "Room " + customer.getRoomNumber() + " booked.");
    }

    public synchronized Pair<Boolean, String> releaseRoom(int roomNumber, String billInfo) {
        Room target = findRoom(roomNumber);
        if (target == null) return new Pair<>(false, "Room not found.");
        if (target.isAvailable()) return new Pair<>(false, "Room is already vacant.");

        Customer c = bookings.remove(roomNumber);
        target.setAvailable(true);

        String guestName = (c != null) ? c.getName() : "Unknown";
        logs.add(new LogEntry("CHECK-OUT", guestName, roomNumber, billInfo));

        notifyAll();
        return new Pair<>(true, "Room " + roomNumber + " released.");
    }

    public synchronized boolean addRoom(Room r) {
        for (Room existing : rooms) {
            if (existing.getRoomNumber() == r.getRoomNumber()) return false;
        }
        rooms.add(r);
        logs.add(new LogEntry("ROOM-ADDED", "—", r.getRoomNumber(),
                "Type: " + r.getRoomType() + " | ₹" + r.getPrice() + "/night"));
        return true;
    }

    public synchronized boolean removeRoom(int roomNumber) {
        Iterator<Room> it = rooms.iterator(); // Week 8: Iterator
        while (it.hasNext()) {
            Room r = it.next();
            if (r.getRoomNumber() == roomNumber) {
                if (!r.isAvailable()) return false; // occupied
                it.remove();
                logs.add(new LogEntry("ROOM-DELETED", "—", roomNumber, "Removed from inventory"));
                return true;
            }
        }
        return false;
    }

    // ── Queries ──────────────────────────────────────────────────────

    /** Week 8: Collections.sort() — sorted by room number */
    public synchronized ArrayList<Room> getRoomsSortedByNumber() {
        ArrayList<Room> copy = new ArrayList<>(rooms);
        copy.sort(Comparator.comparingInt(Room::getRoomNumber));
        return copy;
    }

    /** Week 8: Collections.sort() — sorted by price */
    public synchronized ArrayList<Room> getRoomsSortedByPrice() {
        ArrayList<Room> copy = new ArrayList<>(rooms);
        copy.sort(Comparator.comparingDouble(Room::getPrice));
        return copy;
    }

    public synchronized ArrayList<Room> getAvailableRooms() {
        ArrayList<Room> avail = new ArrayList<>();
        for (Room r : rooms) {
            if (r.isAvailable()) avail.add(r);
        }
        return avail;
    }

    public synchronized ArrayList<Customer> getAllCustomers() {
        return new ArrayList<>(bookings.values());
    }

    public synchronized ArrayList<LogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public synchronized Customer getCustomerByRoom(int roomNumber) {
        return bookings.get(roomNumber); // Week 8: HashMap.get()
    }

    private Room findRoom(int roomNumber) {
        for (Room r : rooms) {
            if (r.getRoomNumber() == roomNumber) return r;
        }
        return null;
    }

    public synchronized int totalRooms()     { return rooms.size(); }
    public synchronized long occupiedCount() { return rooms.stream().filter(r -> !r.isAvailable()).count(); }
    public synchronized long availableCount(){ return rooms.stream().filter(Room::isAvailable).count(); }

    /** Week 7: bounded generic — compute revenue using PriceCalculator */
    public synchronized double computeRevenue() {
        double total = 0;
        for (LogEntry e : logs) {
            if (!"CHECK-OUT".equals(e.getType())) continue;
            try {
                // Extract numeric value from details string (e.g. "₹54000.00 total | 3 night(s)")
                String det = e.getDetails();
                String numStr = det.replaceAll("[^0-9.]", "").split("\\.")[0]
                        + "." + det.replaceAll("[^0-9.]", "").split("\\.")[1];
                total += Double.parseDouble(numStr);
            } catch (Exception ignored) {}
        }
        return total;
    }

    /** Add a log entry externally (e.g. from persistence layer). */
    public synchronized void addLog(LogEntry entry) { logs.add(entry); }

    /** Bulk-load rooms from persistence without triggering extra logs. */
    public synchronized void loadRoom(Room r) { rooms.add(r); }

    /** Bulk-load a booking from persistence. */
    public synchronized void loadBooking(Customer c) {
        Room target = findRoom(c.getRoomNumber());
        if (target != null) target.setAvailable(false);
        bookings.put(c.getRoomNumber(), c);
    }
}
