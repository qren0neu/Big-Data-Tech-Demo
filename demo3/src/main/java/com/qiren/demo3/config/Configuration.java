package com.qiren.demo3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

import org.springframework.amqp.core.Queue;

@org.springframework.context.annotation.Configuration
public class Configuration {

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);
		return template;
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public Queue queue() {
		return new Queue("myQueue", false);
	}
	
	@Bean
	public Queue queue2() {
		return new Queue("myQueue2", false);
	}
}
