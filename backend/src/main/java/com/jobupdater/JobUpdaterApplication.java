package com.jobupdater;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobUpdaterApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobUpdaterApplication.class, args);
	}

}
