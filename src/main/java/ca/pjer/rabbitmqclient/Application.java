package ca.pjer.rabbitmqclient;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	static final String TEST_EXCHANGE = "test-exchange";
	static final String TEST_QUEUE = "test-queue";
	static final String TEST_ROUTING_KEY = "test";

	@Bean
	DirectExchange testExchange() {
		return new DirectExchange(TEST_EXCHANGE);
	}

	@Bean
	Queue testQueue() {
		return new Queue(TEST_QUEUE);
	}

	@Bean
	Binding binding(Queue testQueue, DirectExchange testExchange) {
		return BindingBuilder.bind(testQueue).to(testExchange).with(TEST_ROUTING_KEY);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
