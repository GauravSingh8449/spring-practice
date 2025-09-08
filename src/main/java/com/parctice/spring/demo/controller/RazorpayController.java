package com.parctice.spring.demo.controller;

import com.parctice.spring.demo.service.RazorpayService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payment")
public class RazorpayController {

    @Autowired
    private RazorpayService razorpayService;

    /**
     * API endpoint to create Razorpay order
     * @param amount amount in INR
     * @return JSON with order details
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestParam int amount) {
        try {
            JSONObject order = razorpayService.createOrder(amount); // JSONObject return
            if (order != null) {
                return ResponseEntity.ok(order.toMap()); // frontend me direct JSON milega
            } else {
                return ResponseEntity.status(500).body("Failed to create Razorpay order");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("An error occurred while creating order");
        }
    }

    /**
     * API endpoint to verify Razorpay payment and save transaction to DB
     * @param payload contains name, email, contact, amount, orderId, paymentId, razorpaySignature
     * @return success or failure message
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> payload) {

        System.out.println("------payload---"+payload);
        try {
            String name = payload.get("name");
            String email = payload.get("email");
            String contact = payload.get("contact");
            int amount = Integer.parseInt(payload.get("amount"));
            String orderId = payload.get("orderId");
            String paymentId = payload.get("paymentId");
            String razorpaySignature = payload.get("razorpaySignature");

            boolean isValid = razorpayService.verifyAndSavePayment(
                    name, email, contact, orderId, paymentId, razorpaySignature, amount
            );

            if (isValid) {
                return ResponseEntity.ok("Payment verified and saved successfully!");
            } else {
                return ResponseEntity.badRequest().body("Payment verification failed! Transaction saved as FAILED.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error while verifying payment");
        }
    }
}
