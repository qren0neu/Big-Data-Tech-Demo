package com.qiren.demo1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

import com.qiren.demo1.util.JsonSchemaHelper;

@SpringBootApplication
public class Demo1Application implements ApplicationListener<ApplicationStartedEvent> {

	public static void main(String[] args) {
		SpringApplication.run(Demo1Application.class, args);
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		JsonSchemaHelper.initialize("/static/planSchema.json");
	}
}
