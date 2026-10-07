package org.example.ingresso.ingresso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IngressoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngressoApiApplication.class, args);
    }

}
