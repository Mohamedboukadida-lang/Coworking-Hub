package com.coworkinghub.coworkinghub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Authentication is JWT-based (see security package); the default in-memory user is not wanted.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CoworkinghubApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoworkinghubApplication.class, args);
	}

}
