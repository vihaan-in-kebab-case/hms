package com.hms;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Customer {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty contact = new SimpleStringProperty();
    private final IntegerProperty roomNumber = new SimpleIntegerProperty();
    private final StringProperty roomType = new SimpleStringProperty();
    private final DoubleProperty pricePerNight = new SimpleDoubleProperty();
    private final StringProperty checkInTime = new SimpleStringProperty();
    private final LocalDateTime checkInDateTime;

    public Customer(String name, String contact, int roomNumber, String roomType, double pricePerNight) {
        this.name.set(name);
        this.contact.set(contact);
        this.roomNumber.set(roomNumber);
        this.roomType.set(roomType);
        this.pricePerNight.set(pricePerNight);
        this.checkInDateTime = LocalDateTime.now();
        this.checkInTime.set(checkInDateTime.format(FMT));
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getContact() { return contact.get(); }
    public StringProperty contactProperty() { return contact; }

    public int getRoomNumber() { return roomNumber.get(); }
    public IntegerProperty roomNumberProperty() { return roomNumber; }

    public String getRoomType() { return roomType.get(); }
    public StringProperty roomTypeProperty() { return roomType; }

    public double getPricePerNight() { return pricePerNight.get(); }
    public DoubleProperty pricePerNightProperty() { return pricePerNight; }

    public String getCheckInTime() { return checkInTime.get(); }
    public StringProperty checkInTimeProperty() { return checkInTime; }

    public LocalDateTime getCheckInDateTime() { return checkInDateTime; }
}
