package com.example.ussd_project.service;

import com.example.ussd_project.model.Customer;
import com.example.ussd_project.model.Transaction;
import com.example.ussd_project.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final CustomerService customerService;
    private final TransactionRepository transactionRepository;

    public void transferMoney(String senderNumber, String receiverNumber, BigDecimal amount, String bankName, int pin) {
        Customer sender = customerService.validatePin(senderNumber, pin);
        Customer receiver = customerService.getCustomerByPhoneNumber(receiverNumber);

        if (sender.getBalance().compareTo(amount) <= 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        Transaction transaction = new Transaction();
        transaction.setSenderPhoneNumber(senderNumber);
        transaction.setReceiverPhoneNumber(receiverNumber);
        transaction.setAmount(amount);
        transaction.setBankName(bankName);
        transaction.setTransactionDate(LocalDateTime.now());

        transactionRepository.save(transaction);
    }
}
