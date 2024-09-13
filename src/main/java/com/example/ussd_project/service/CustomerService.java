package com.example.ussd_project.service;

import com.example.ussd_project.model.Customer;
import com.example.ussd_project.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer registerCustomer(String name, String phoneNumber){
        if(!phoneNumber.startsWith("07") || phoneNumber.length() !=10){
            throw new IllegalArgumentException("Phone number must start with '07'  and have 10 digits");
        }
        if (customerRepository.findByPhoneNumber(phoneNumber).isPresent()){
            throw new IllegalArgumentException("Phone number already registered");
        }

        Customer customer = new Customer();
        customer.setName(name);
        customer.setPhoneNumber(phoneNumber);
        customer.setBalance(new BigDecimal("10000000"));
        return customerRepository.save(customer);
    }

    public Customer getCustomerByPhoneNumber(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    public BigDecimal getCustomerBalance(String phoneNumber) {
        return getCustomerByPhoneNumber(phoneNumber).getBalance();
    }

    public Customer validatePin(String phoneNumber, int enteredPin){
        Customer customer = getCustomerByPhoneNumber(phoneNumber);
        if (customer.getPin() != enteredPin) {
            throw new IllegalArgumentException("Invalid PIN");
        }
        return customer;
    }
}
