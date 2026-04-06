package com.hms;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LogEntry {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final StringProperty timestamp = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty guestName = new SimpleStringProperty();
    private final IntegerProperty roomNumber = new SimpleIntegerProperty();
    private final StringProperty details = new SimpleStringProperty();

    public LogEntry(String type, String guestName, int roomNumber, String details) {
        this.timestamp.set(LocalDateTime.now().format(FMT));
        this.type.set(type);
        this.guestName.set(guestName);
        this.roomNumber.set(roomNumber);
        this.details.set(details);
    }

    public String getTimestamp() { return timestamp.get(); }
    public StringProperty timestampProperty() { return timestamp; }

    public String getType() { return type.get(); }
    public StringProperty typeProperty() { return type; }

    public String getGuestName() { return guestName.get(); }
    public StringProperty guestNameProperty() { return guestName; }

    public int getRoomNumber() { return roomNumber.get(); }
    public IntegerProperty roomNumberProperty() { return roomNumber; }

    public String getDetails() { return details.get(); }
    public StringProperty detailsProperty() { return details; }
}
