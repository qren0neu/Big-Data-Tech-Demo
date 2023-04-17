package com.qiren.demo3.messagequeue;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageQueueSender {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend("myQueue2", message);
        System.out.println("RabbitMQ: sent message " + message);
    }
    
    @RabbitListener(queues = "myQueue2")
    public void cacheQueue(String message) {
        rabbitTemplate.convertAndSend("myQueue", message);
    }
}

