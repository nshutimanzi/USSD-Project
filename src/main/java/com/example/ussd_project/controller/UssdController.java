package com.example.ussd_project.controller;

import com.example.ussd_project.model.Customer;
import com.example.ussd_project.model.UssdMenu;
import com.example.ussd_project.service.CustomerService;
import com.example.ussd_project.service.TransactionService;
import com.example.ussd_project.service.UssdMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/ussd")
public class UssdController {

    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final UssdMenuService ussdMenuService;

    @Autowired
    private RedisTemplate<String, Map<String, String>> redisTemplate;

    public UssdController(CustomerService customerService, TransactionService transactionService, UssdMenuService ussdMenuService) {
        this.customerService = customerService;
        this.transactionService = transactionService;
        this.ussdMenuService = ussdMenuService;
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
        String redisKey = "session:" + phoneNumber;
        Map<String, String> sessionData = redisTemplate.opsForValue().get(redisKey);

        if (sessionData == null) {
            sessionData = new HashMap<>();
        }

        if (sessionData.containsKey("sendMoneyStage")) {
            return handleSendMoneyFlow(phoneNumber, input, sessionData);
        }

        List<UssdMenu> menuOptions = ussdMenuService.getMenuOptions("main");
        String menu = ussdMenuService.buildMenu(menuOptions);

        if (input.equals("0")){
            return menu;
        }

        UssdMenu selectedMenu = menuOptions.stream().filter(m -> m.getMenuOption().equals(input)).findFirst().orElse(null);

        if (selectedMenu != null){
            switch (selectedMenu.getAction()){
                case "sendMoney":
                    return handleSendMoney(phoneNumber);
                case "checkBalance":
                    return handleCheckBalance(phoneNumber);
                case "viewProfile":
                    return handleViewProfile(phoneNumber);
                default:
                    return "Invalid menu option.";
            }
        } else {
            return "Invalid option. Please select a valid menu option.";
        }
    }

    private String handleSendMoney(String phoneNumber) {
        String redisKey = "session:" + phoneNumber;
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("sendMoneyStage", "bankSelection");

        redisTemplate.opsForValue().set(redisKey, sessionData, 1, TimeUnit.MINUTES);

        return "Choose bank:\n1. I&M Bank\n2. Equity Bank\n3. BK";
    }

    private String handleSendMoneyFlow(String phoneNumber, String input, Map<String, String> sessionData) {
        String redisKey = "session:" + phoneNumber;
        String stage = sessionData.get("sendMoneyStage");

        switch (stage) {
            case "bankSelection":
                return handleBankSelection(phoneNumber, input, sessionData);
            case "receiverPhoneNumber":
                sessionData.put("receiverPhoneNumber", input);
                sessionData.put("sendMoneyStage", "amount");
                redisTemplate.opsForValue().set(redisKey, sessionData, 1, TimeUnit.MINUTES);
                return "Enter amount:";
            case "amount":
                sessionData.put("amount", input);
                sessionData.put("sendMoneyStage", "pin");
                redisTemplate.opsForValue().set(redisKey, sessionData, 1, TimeUnit.MINUTES);
                return "Type PIN:";
            case "pin":
                sessionData.put("pin", input);
                return completeTransaction(phoneNumber, sessionData);
            default:
                return "Invalid flow stage.";
        }
    }

    private String handleBankSelection(String phoneNumber, String input, Map<String, String> sessionData) {
        String redisKey = "session:" + phoneNumber;
        String bankName = getBankName(input);

        if (bankName == null) {
            return "Invalid option. Please select a valid bank.";
        }

        sessionData.put("bank", bankName);
        sessionData.put("sendMoneyStage", "receiverPhoneNumber");
        redisTemplate.opsForValue().set(redisKey, sessionData, 1, TimeUnit.MINUTES);
        return "Enter receiver's phone number:";
    }

    private String completeTransaction(String phoneNumber, Map<String, String> sessionData) {
        String redisKey = "session:" + phoneNumber;
        String receiverPhoneNumber = sessionData.get("receiverPhoneNumber");
        String bankName = sessionData.get("bank");
        BigDecimal amount = new BigDecimal(sessionData.get("amount"));
        int pin = Integer.parseInt(sessionData.get("pin"));

        try {
            transactionService.transferMoney(phoneNumber, receiverPhoneNumber, amount, bankName, pin);
            redisTemplate.delete(redisKey);
            return "Money sent successfully. \n\n Type 0 to go back to Main menu";
        } catch (Exception e) {
            return "Transaction failed: " + e.getMessage();
        }
    }

    private String getBankName(String option) {
        switch (option) {
            case "1":
                return "I&M Bank";
            case "2":
                return "Equity Bank";
            case "3":
                return "Bank of Kigali";
            default:
                return null;
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