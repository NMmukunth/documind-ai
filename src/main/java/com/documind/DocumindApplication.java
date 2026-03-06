package com.documind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocumindApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumindApplication.class, args);
        System.out.println("==============================================");
        System.out.println("  DocuMind AI is running!");
        System.out.println("  Open: http://localhost:8080/login.html");
        System.out.println("==============================================");
    }
}
