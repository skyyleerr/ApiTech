package com.apitech.mk5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Mk5Application {

	public static void main(String[] args) {
		SpringApplication.run(Mk5Application.class, args);
	}

}
