package com.example.ussd_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class UssdProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(UssdProjectApplication.class, args);
	}

}
