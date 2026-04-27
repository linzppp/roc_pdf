package org.roc.practice.pdf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class RocPdfApplication {

    public static void main(String[] args) {
        SpringApplication.run(RocPdfApplication.class, args);
    }
}
