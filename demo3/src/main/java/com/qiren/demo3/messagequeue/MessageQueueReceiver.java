package com.qiren.demo3.messagequeue;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MessageQueueReceiver {
	
	private MessageListener listener;
	
	public void setListener(MessageListener listener) {
		this.listener = listener;
	}
    
//    @RabbitListener(queues = "myQueue")
    public void receiveMessage(String message) {
    	if (null != listener) {
    		listener.onReceiveMessage(message);
    	}
        System.out.println("RabbitMQ: received message " + message);
    }
    
    public interface MessageListener {
    	void onReceiveMessage(String message);
    }
}

