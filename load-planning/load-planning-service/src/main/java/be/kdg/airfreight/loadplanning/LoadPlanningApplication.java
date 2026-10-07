package be.kdg.airfreight.loadplanning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoadPlanningApplication {
    static void main(String[] args) {
        SpringApplication.run(LoadPlanningApplication.class, args);
    }
}
