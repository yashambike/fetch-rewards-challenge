package com.example.receiptprocessor.controller;

import com.example.receiptprocessor.model.PointsResponse;
import com.example.receiptprocessor.model.ProcessReceiptResponse;
import com.example.receiptprocessor.model.Receipt;
import com.example.receiptprocessor.service.ReceiptService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    @Autowired
    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    /**
     * Endpoint: POST /receipts/process
     * Accepts a receipt JSON, processes it and returns a generated id.
     */
    @PostMapping("/process")
    public ResponseEntity<ProcessReceiptResponse> processReceipt(@Valid @RequestBody Receipt receipt) {
        String id = receiptService.processReceipt(receipt);
        return ResponseEntity.ok(new ProcessReceiptResponse(id));
    }

    /**
     * Endpoint: GET /receipts/{id}/points
     * Returns the points awarded for the receipt.
     */
    @GetMapping("/{id}/points")
    public ResponseEntity<PointsResponse> getPoints(@PathVariable String id) {
        Integer points = receiptService.getPoints(id);
        if (points == null) {
            // Return a 404 if the receipt id is not found.
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(new PointsResponse(points));
    }
}
