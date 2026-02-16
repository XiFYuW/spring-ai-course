package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringAiJcStart {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(SpringAiJcStart.class);
        springApplication.run(args);
    }

}
