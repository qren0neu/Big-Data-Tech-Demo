package com.qiren.demo3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

import com.qiren.demo3.service.JsonSchemaService;

@SpringBootApplication
public class Demo3Application implements ApplicationListener<ApplicationStartedEvent>{

	public static void main(String[] args) {
		SpringApplication.run(Demo3Application.class, args);
	}


	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		JsonSchemaService.initialize("/static/planSchema.json");
	}
}
