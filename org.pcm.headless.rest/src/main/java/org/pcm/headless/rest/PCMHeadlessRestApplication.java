package org.pcm.headless.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan
public class PCMHeadlessRestApplication {

	public static void main(String[] args) {
		SpringApplication.run(PCMHeadlessRestApplication.class, args);
	}

}
