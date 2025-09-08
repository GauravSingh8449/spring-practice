package com.parctice.spring.demo.service;

import com.parctice.spring.demo.model.PaymentTransaction;
import com.parctice.spring.demo.repository.PaymentTransactionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.razorpay.RazorpayException;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    private RazorpayClient razorpayClient;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Value("${payment.gateway.secretKey}")
    private String secretKey;

    @Value("${payment.gateway.publishableKey}")
    private String publishableKey;

    /**
     * RazorpayClient initialize karo jab keys inject ho jayein
     */
    @PostConstruct
    public void init() {
        try {
            razorpayClient = new RazorpayClient(secretKey, publishableKey);
            System.out.println("RazorpayClient initialized successfully!");
        } catch (RazorpayException e) {
            System.err.println("Error initializing RazorpayClient");
            e.printStackTrace();
        }
    }

    /**
     * Razorpay order create karta hai aur JSON return karta hai
     */
    public JSONObject createOrder(int amount) {
        JSONObject options = new JSONObject();
        options.put("amount", amount * 100); // INR to paise
        options.put("currency", "INR");
        options.put("receipt", "order_rcptid_" + System.currentTimeMillis());
        options.put("payment_capture", 1); // Auto capture

        try {
            Order order = razorpayClient.orders.create(options);
            System.out.println("Order created: " + order);
            return new JSONObject(order.toString());
        } catch (RazorpayException e) {
            System.err.println("Exception while creating order");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Payment verify + DB save
     */
    public boolean verifyAndSavePayment(String name, String email, String contact,
                                        String orderId, String paymentId,
                                        String razorpaySignature, int amount) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", razorpaySignature);
            attributes.put("name", name);
            attributes.put("email", email);
            attributes.put("contact", contact);
            attributes.put("amount", amount);

            // Signature verify
            boolean isValid = Utils.verifyPaymentSignature(attributes, publishableKey);

            // Transaction DB me save
            PaymentTransaction txn = new PaymentTransaction();
            txn.setName(name);
            txn.setEmail(email);
            txn.setContact(contact);
            txn.setAmount(amount);
            txn.setOrderId(orderId);
            txn.setPaymentId(paymentId);
            txn.setStatus(isValid ? "SUCCESS" : "FAILED");

            transactionRepository.save(txn);
            System.out.println("Transaction saved: " + txn.getId());

            return isValid;
        } catch (RazorpayException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get publishable key for frontend
     */
    public String getPublishableKey() {
        return publishableKey;
    }
}
