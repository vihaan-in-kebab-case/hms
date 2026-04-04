package com.hms;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public class DataPersistence {

    private static final String DATA_DIR = System.getProperty("user.home")
            + File.separator + "HMS_Data";

    private static final String ROOMS_FILE    = DATA_DIR + File.separator + "rooms.dat";
    private static final String BOOKINGS_FILE = DATA_DIR + File.separator + "bookings.dat";
    private static final String LOG_FILE      = DATA_DIR + File.separator + "activity.log";

    static class RoomData implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        int roomNumber; String roomType; double price; boolean available;
        RoomData(Room r) {
            roomNumber = r.getRoomNumber(); roomType = r.getRoomType();
            price = r.getPrice();          available = r.isAvailable();
        }
    }

    static class BookingData implements Serializable {
        @Serial private static final long serialVersionUID = 2L;
        String name, contact, roomType, checkInTime;
        int roomNumber; double pricePerNight;
        BookingData(Customer c) {
            name = c.getName(); contact = c.getContact();
            roomType = c.getRoomType(); roomNumber = c.getRoomNumber();
            pricePerNight = c.getPricePerNight(); checkInTime = c.getCheckInTime();
        }
    }

    public static void saveRooms(ArrayList<Room> rooms) {
        ensureDir();
        ArrayList<RoomData> data = new ArrayList<>();
        for (Room r : rooms) data.add(new RoomData(r));
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(ROOMS_FILE))) {
            oos.writeObject(data);
        } catch (IOException e) {
            System.err.println("Save rooms error: " + e.getMessage());
        }
    }

    public static void saveBookings(ArrayList<Customer> customers) {
        ensureDir();
        ArrayList<BookingData> data = new ArrayList<>();
        for (Customer c : customers) data.add(new BookingData(c));
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(BOOKINGS_FILE))) {
            oos.writeObject(data);
        } catch (IOException e) {
            System.err.println("Save bookings error: " + e.getMessage());
        }
    }

    public static void appendLog(LogEntry entry) {
        ensureDir();
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write(entry.getTimestamp() + " | "
                    + entry.getType() + " | "
                    + entry.getGuestName() + " | "
                    + "Room " + entry.getRoomNumber() + " | "
                    + entry.getDetails() + "\n");
        } catch (IOException e) {
            System.err.println("Log write error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Room> loadRooms() {
        ArrayList<Room> rooms = new ArrayList<>();
        File f = new File(ROOMS_FILE);
        if (!f.exists()) return rooms;
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(ROOMS_FILE))) {
            ArrayList<RoomData> data = (ArrayList<RoomData>) ois.readObject();
            for (RoomData rd : data) {
                Room r = new Room(rd.roomNumber, rd.roomType, rd.price);
                r.setAvailable(rd.available);
                rooms.add(r);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Load rooms error: " + e.getMessage());
        }
        return rooms;
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Customer> loadBookings() {
        ArrayList<Customer> customers = new ArrayList<>();
        File f = new File(BOOKINGS_FILE);
        if (!f.exists()) return customers;
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(BOOKINGS_FILE))) {
            ArrayList<BookingData> data = (ArrayList<BookingData>) ois.readObject();
            for (BookingData bd : data) {
                customers.add(new Customer(
                        bd.name, bd.contact, bd.roomNumber, bd.roomType, bd.pricePerNight));
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Load bookings error: " + e.getMessage());
        }
        return customers;
    }

    public static String readLogFile() {
        File f = new File(LOG_FILE);
        if (!f.exists()) return "(No log file yet)";
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(LOG_FILE)) {
            int ch;
            while ((ch = fr.read()) != -1) sb.append((char) ch);
        } catch (IOException e) {
            return "Error reading log: " + e.getMessage();
        }
        return sb.toString();
    }

    public static boolean exportLogCopy(String destinationPath) {
        File src = new File(LOG_FILE);
        if (!src.exists()) return false;
        try (FileInputStream  fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(destinationPath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Export error: " + e.getMessage());
            return false;
        }
    }
    public static String getDataDir() { return DATA_DIR; }
    private static void ensureDir() { new File(DATA_DIR).mkdirs(); }
}
