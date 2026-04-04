package com.hms;
public class PriceCalculator<T extends Number> {

    private T basePrice;
    private T discount;

    public PriceCalculator(T basePrice, T discount) {
        this.basePrice = basePrice;
        this.discount  = discount;
    }

    public double totalPrice(int nights) {
        return basePrice.doubleValue() * nights;
    }

    public double discountedPrice(int nights) {
        double total      = totalPrice(nights);
        double discountAmt = total * (discount.doubleValue() / 100.0);
        return total - discountAmt;
    }

    public double priceWithTax(int nights) {
        return discountedPrice(nights) * 1.12;
    }

    public static <T extends Number> double applyGST(T amount) {
        return amount.doubleValue() * 1.12;
    }

    public static <T> void display(T value) {
        System.out.println("Value: " + value);
    }

    public static <T> void printArray(T[] array) {
        for (T element : array) System.out.print(element + "  ");
        System.out.println();
    }
}
