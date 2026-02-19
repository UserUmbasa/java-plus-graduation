package ru.practicum.requestservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFeignClients()
@ComponentScan("ru.practicum")
public class RequestService {

    public static void main(String[] args) {
        SpringApplication.run(RequestService.class, args);
    }

}
