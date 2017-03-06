package org.springframework.cloud.release;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReleaserApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReleaserApplication.class, args);
	}
}
