package com.hms;
import java.util.ArrayList;
import java.util.List;
public class Validator {
    public static List<String> validateNewRoom(String roomNumText, RoomType type) {
        List<String> errors = new ArrayList<>();

        if (roomNumText == null || roomNumText.isBlank())
            errors.add("Room number is required.");
        else {
            int num;
            try { num = Integer.parseInt(roomNumText.trim()); }
            catch (NumberFormatException e) {
                errors.add("Room number must be a whole number (e.g. 101).");
                return errors;
            }
            if (num <= 0)
                errors.add("Room number must be a positive integer.");
            if (num > 9999)
                errors.add("Room number cannot exceed 9999.");
        }

        if (type == null)
            errors.add("Please select a room type.");

        return errors;
    }

    public static List<String> validateBooking(String name, String contact,
                                               Integer roomNo, int totalRooms) {
        List<String> errors = new ArrayList<>();

        if (name == null || name.isBlank())
            errors.add("Guest name is required.");
        else if (name.trim().length() < 2)
            errors.add("Guest name must be at least 2 characters.");
        else if (name.trim().length() > 80)
            errors.add("Guest name is too long (max 80 characters).");

        if (contact == null || contact.isBlank())
            errors.add("Contact (phone or email) is required.");
        else if (!contact.trim().matches(".*[0-9@].*"))
            errors.add("Contact must contain digits (phone) or '@' (email).");

        if (roomNo == null)
            errors.add("Please select a room from the list.");

        if (totalRooms == 0)
            errors.add("No rooms have been added to the system yet.");

        return errors;
    }

    public static List<String> validateCheckout(String enteredName, String enteredContact,
                                                Customer selected) {
        List<String> errors = new ArrayList<>();

        if (selected == null) {
            errors.add("Select a booking row from the table to checkout.");
            return errors;
        }

        if (enteredName == null || enteredName.isBlank()) {
            errors.add("Enter the guest name to confirm checkout.");
        } else if (!enteredName.trim().equalsIgnoreCase(selected.getName().trim())) {
            errors.add("Guest name does not match the selected booking.\n" + "Expected: \"" + selected.getName() + "\"");
        }

        if (enteredContact == null || enteredContact.isBlank()) {
            errors.add("Enter the contact to confirm checkout.");
        } else if (!enteredContact.trim().equals(selected.getContact().trim())) {
            errors.add("Contact does not match the selected booking.");
        }

        return errors;
    }

    public static List<String> validateDiscount(String discountText) {
        List<String> errors = new ArrayList<>();
        if (discountText == null || discountText.isBlank()) return errors;

        double d;
        try { d = Double.parseDouble(discountText.trim()); }
        catch (NumberFormatException e) {
            errors.add("Discount must be a number between 0 and 100.");
            return errors;
        }
        if (d < 0)   errors.add("Discount cannot be negative.");
        if (d > 100) errors.add("Discount cannot exceed 100%.");
        return errors;
    }

    public static String format(List<String> errors) {
        StringBuilder sb = new StringBuilder();
        for (String e : errors) sb.append("• ").append(e).append("\n");
        return sb.toString().stripTrailing();
    }
}
