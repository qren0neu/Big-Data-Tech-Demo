package com.qiren.demo2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import com.qiren.demo2.service.JsonSchemaService;

@SpringBootApplication
public class Demo2Application implements ApplicationListener<ApplicationStartedEvent>{

	public static void main(String[] args) {
		SpringApplication.run(Demo2Application.class, args);
	}


	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		JsonSchemaService.initialize("/static/planSchema.json");
	}
}
