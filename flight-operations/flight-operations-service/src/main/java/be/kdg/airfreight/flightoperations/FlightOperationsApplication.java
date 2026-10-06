package be.kdg.airfreight.flightoperations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlightOperationsApplication {
    static void main(String[] args) {
        SpringApplication.run(FlightOperationsApplication.class, args);
    }
}
