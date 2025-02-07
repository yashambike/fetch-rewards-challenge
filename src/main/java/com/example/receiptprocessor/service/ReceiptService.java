package com.example.receiptprocessor.service;

import com.example.receiptprocessor.model.Item;
import com.example.receiptprocessor.model.Receipt;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ReceiptService {

    // In-memory store mapping receipt IDs to the calculated points.
    private final Map<String, Integer> receiptPoints = new ConcurrentHashMap<>();

    /**
     * Processes the receipt by calculating its points and storing it.
     *
     * @param receipt the receipt data from the request
     * @return a generated receipt ID
     */
    public String processReceipt(Receipt receipt) {
        String id = UUID.randomUUID().toString();
        int points = calculatePoints(receipt);
        receiptPoints.put(id, points);
        return id;
    }

    /**
     * Returns the points for a given receipt id.
     *
     * @param id the receipt ID
     * @return the points or null if the id is not found
     */
    public Integer getPoints(String id) {
        return receiptPoints.get(id);
    }

    /**
     * Calculates points for the receipt according to the rules.
     */
    private int calculatePoints(Receipt receipt) {
        int points = 0;

        // 1. One point for every alphanumeric character in the retailer name.
        for (char c : receipt.getRetailer().toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                points++;
            }
        }

        // Parse total amount.
        BigDecimal total;
        try {
            total = new BigDecimal(receipt.getTotal());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid total amount.");
        }

        // 2. 50 points if the total is a round dollar amount with no cents.
        if (total.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            points += 50;
        }

        // 3. 25 points if the total is a multiple of 0.25.
        if (total.remainder(new BigDecimal("0.25")).compareTo(BigDecimal.ZERO) == 0) {
            points += 25;
        }

        // 4. 5 points for every two items on the receipt.
        int pairs = receipt.getItems().size() / 2;
        points += pairs * 5;

        // 5. For each item, if the trimmed length of the short description is a multiple of 3,
        //    multiply the price by 0.2 and round up to the nearest integer.
        for (Item item : receipt.getItems()) {
            String description = item.getShortDescription().trim();
            if (description.length() % 3 == 0) {
                BigDecimal price = new BigDecimal(item.getPrice());
                BigDecimal bonus = price.multiply(new BigDecimal("0.2"));
                bonus = bonus.setScale(0, RoundingMode.CEILING);
                points += bonus.intValue();
            }
        }

        // 6. Extra rule (applied because this solution is generated using an LLM):
        //    Add 5 points if the total is greater than 10.00.
        if (total.compareTo(new BigDecimal("10.00")) > 0) {
            points += 5;
        }

        // 7. 6 points if the day in the purchase date is odd.
        LocalDate purchaseDate = LocalDate.parse(receipt.getPurchaseDate());
        if (purchaseDate.getDayOfMonth() % 2 == 1) {
            points += 6;
        }

        // 8. 10 points if the time of purchase is after 2:00pm and before 4:00pm.
        LocalTime purchaseTime = LocalTime.parse(receipt.getPurchaseTime());
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(16, 0);
        if (purchaseTime.isAfter(start) && purchaseTime.isBefore(end)) {
            points += 10;
        }

        return points;
    }
}
