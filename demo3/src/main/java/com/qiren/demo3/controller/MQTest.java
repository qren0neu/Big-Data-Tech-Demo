package com.qiren.demo3.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.qiren.demo3.messagequeue.MessageQueueSender;

@Controller
public class MQTest {

	@Autowired
	private MessageQueueSender sender;
	
	@PostMapping("/mqtest/{message}")
	public String mqtest(@PathVariable String message) {
		sender.sendMessage(message);
		return "done";
	}
}
