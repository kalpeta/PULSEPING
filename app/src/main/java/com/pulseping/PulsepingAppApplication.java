package com.pulseping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PulsepingAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(PulsepingAppApplication.class, args);
	}

}
