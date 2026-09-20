package pl.kul.fraud_insight_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FraudInsightServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FraudInsightServiceApplication.class, args);
	}

}
