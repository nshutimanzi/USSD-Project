package com.example.ussd_project.controller;

import com.example.ussd_project.model.Customer;
import com.example.ussd_project.service.CustomerService;
import com.example.ussd_project.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ussd")
public class UssdController {

    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final Map<String, Map<String, String>> sessionCache = new HashMap<>();

    public UssdController(CustomerService customerService, TransactionService transactionService) {
        this.customerService = customerService;
        this.transactionService = transactionService;
    }

    @PostMapping("/register")
    public ResponseEntity<Customer> register(@RequestParam String name, @RequestParam String phoneNumber){
        return ResponseEntity.ok(customerService.registerCustomer(name, phoneNumber));
    }

    @GetMapping("/profile")
    public ResponseEntity<Customer> viewProfile(@RequestParam String phoneNumber){
        return  ResponseEntity.ok(customerService.getCustomerByPhoneNumber(phoneNumber));
    }

    @PostMapping("/process")
    public String processUssdInput(@RequestParam String phoneNumber, @RequestParam String input) {
        Map<String, String> sessionData = sessionCache.getOrDefault(phoneNumber, new HashMap<>());

        if (sessionData.containsKey("sendMoneyStage")) {
            return handleSendMoneyFlow(phoneNumber, input, sessionData);
        }

        switch (input) {
            case "1":
                return handleSendMoney(phoneNumber);
            case "2":
                return handleCheckBalance(phoneNumber);
            case "3":
                return handleViewProfile(phoneNumber);
            case "0":
                return getMainMenu();
            default:
                return "Invalid option. Please select a valid menu option.";
        }
    }

    private String getMainMenu() {
        return """
               Main Menu
               ---------
               1. Send Money
               2. Display Balance
               3. Display Profile
               """;
    }

    private String handleSendMoney(String phoneNumber) {
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("sendMoneyStage", "bankSelection");
        sessionCache.put(phoneNumber, sessionData);
        return "Choose bank:\n1. I&M Bank\n2. Equity Bank\n3. BK";
    }

    private String handleSendMoneyFlow(String phoneNumber, String input, Map<String, String> sessionData) {
        String stage = sessionData.get("sendMoneyStage");

        switch (stage) {
            case "bankSelection":
                return handleBankSelection(phoneNumber, input, sessionData);

            case "receiverPhoneNumber":
                sessionData.put("receiverPhoneNumber", input);
                sessionData.put("sendMoneyStage", "amount");
                sessionCache.put(phoneNumber, sessionData);
                return "Enter amount:";

            case "amount":
                sessionData.put("amount", input);
                sessionData.put("sendMoneyStage", "pin");
                sessionCache.put(phoneNumber, sessionData);
                return "Type PIN:";

            case "pin":
                sessionData.put("pin", input);
                return completeTransaction(phoneNumber, sessionData);

            default:
                return "Invalid flow stage.";
        }
    }

    private String handleBankSelection(String phoneNumber, String input, Map<String, String> sessionData) {
        String bankName = getBankName(input);

        if (bankName == null) {
            return "Invalid option. Please select a valid bank.";
        }

        sessionData.put("bank", bankName);
        sessionData.put("sendMoneyStage", "receiverPhoneNumber");
        sessionCache.put(phoneNumber, sessionData);
        return "Enter receiver's phone number:";
    }

    private String completeTransaction(String phoneNumber, Map<String, String> sessionData) {
        String receiverPhoneNumber = sessionData.get("receiverPhoneNumber");
        String bankName = sessionData.get("bank");
        BigDecimal amount = new BigDecimal(sessionData.get("amount"));
        int pin = Integer.parseInt(sessionData.get("pin"));

        try {
            transactionService.transferMoney(phoneNumber, receiverPhoneNumber, amount, bankName, pin);
            sessionCache.remove(phoneNumber);
            return "Money sent successfully. \n\n Type 0 to go back to Main menu";
        } catch (Exception e) {
            return "Transaction failed: " + e.getMessage();
        }
    }

    private String getBankName(String option) {
        switch (option) {
            case "1": return "I&M Bank";
            case "2": return "Equity Bank";
            case "3": return "Bank of Kigali";
            default: return null;
        }
    }

    private String handleCheckBalance(String phoneNumber) {
        try {
            BigDecimal balance = customerService.getCustomerBalance(phoneNumber);
            return "Your Balance is " + balance + " RWF \n\n Type 0 to go back to Main menu";
        } catch (Exception e) {
            return "Error fetching balance: " + e.getMessage();
        }
    }

    private String handleViewProfile(String phoneNumber) {
        try {
            Customer customer = customerService.getCustomerByPhoneNumber(phoneNumber);
            return "Name: " + customer.getName() + "\nPhone number: " + customer.getPhoneNumber() + "\n\n Type 0 to go back to Main menu";
        } catch (Exception e) {
            return "Error fetching profile: " + e.getMessage();
        }
    }
}