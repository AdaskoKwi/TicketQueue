package pl.kul.queue_service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@SpringBootApplication
public class QueueServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(QueueServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner redisTestRunner(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
		return args -> {
			String testKey = "test:key";
			String testValue = "Hello Spring from Redis!";

			reactiveRedisTemplate.opsForValue()
					.set(testKey, testValue)
					.doOnSuccess(success -> System.out.println("[REDIS SET] Key: " + testKey + " | Success: " + testValue))
					.then(reactiveRedisTemplate.opsForValue().get(testKey))
					.doOnNext(value -> System.out.println("[REDIS GET] Value: " + value))
					.subscribe();
		};
	}
}
