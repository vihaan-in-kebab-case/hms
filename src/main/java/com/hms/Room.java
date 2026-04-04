package com.hms;
import javafx.beans.property.*;

public class Room {

    private final IntegerProperty roomNumber = new SimpleIntegerProperty();
    private final StringProperty  roomType   = new SimpleStringProperty();
    private final DoubleProperty  price      = new SimpleDoubleProperty();
    private final BooleanProperty available  = new SimpleBooleanProperty(true);
    private final StringProperty  status     = new SimpleStringProperty("Available");

    public Room(int roomNumber, RoomType type) {
        this(roomNumber, type.getDisplayName(), type.getBaseTariff());
    }

    public Room(int roomNumber, String roomType, double price) {
        this.roomNumber.set(roomNumber);
        this.roomType.set(roomType);
        this.price.set(price);
        setAvailable(true);
    }

    public int getRoomNumber()  { return roomNumber.get(); }
    public IntegerProperty roomNumberProperty() { return roomNumber; }

    public String getRoomType() { return roomType.get(); }
    public StringProperty roomTypeProperty() { return roomType; }

    public double getPrice()    { return price.get(); }
    public void setPrice(double v) { price.set(v); }
    public DoubleProperty priceProperty() { return price; }

    public boolean isAvailable() { return available.get(); }
    public void setAvailable(boolean v) {
        available.set(v);
        status.set(v ? "Available" : "Occupied");
    }
    public BooleanProperty availableProperty() { return available; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }

    public RoomType resolveType() {
        return RoomType.fromDisplayName(getRoomType());
    }

    public double calculateCost(int nights, double discountPct) {
        PriceCalculator<Double> calc = new PriceCalculator<>(getPrice(), discountPct);
        return calc.priceWithTax(nights);
    }
}
