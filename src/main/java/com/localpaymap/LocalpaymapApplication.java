package com.localpaymap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LocalpaymapApplication {

	public static void main(String[] args) {
		SpringApplication.run(LocalpaymapApplication.class, args);
	}

}
