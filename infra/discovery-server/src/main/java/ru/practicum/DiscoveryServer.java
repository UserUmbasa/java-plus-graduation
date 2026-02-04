package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer // приложение станет реестром сервисов.
public class DiscoveryServer {

    // Когда Eureka запущена, можно посмотреть состояние сервиса обнаружения.
    // Это делается с помощью веб-консоли Eureka Dashboard. Чтобы её открыть, нужно перейти
    // по адресу http://localhost:8761/ в браузере:
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServer.class, args);
    }

}
