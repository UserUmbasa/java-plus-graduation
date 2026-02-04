package ru.practicum;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;



@SpringBootApplication
//@EnableFeignClients(clients = {StatsApi.class}) пока оставлю
@EnableFeignClients()
public class MainService {

    public static void main(String[] args) {
        SpringApplication.run(MainService.class, args);
    }
}