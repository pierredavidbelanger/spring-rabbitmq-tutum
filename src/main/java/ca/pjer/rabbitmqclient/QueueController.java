package ca.pjer.rabbitmqclient;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.MessageFormat;
import java.util.Date;

import static ca.pjer.rabbitmqclient.Application.*;

@RestController
public class QueueController {

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	Environment environment;

	@RequestMapping("/send")
	public void send(@RequestParam(required = false) String message) {
		if (StringUtils.isEmpty(message))
			message = MessageFormat.format("HelloWorld from {0} ({1,time})", environment.getProperty("HOSTNAME"), new Date());
		rabbitTemplate.convertAndSend(TEST_EXCHANGE, TEST_ROUTING_KEY, message);
	}

	@RequestMapping("/receive")
	public String receive() {
		String message = (String) rabbitTemplate.receiveAndConvert(TEST_QUEUE);
		return MessageFormat.format("{0}> {1}", environment.getProperty("HOSTNAME"), message);
	}
}
