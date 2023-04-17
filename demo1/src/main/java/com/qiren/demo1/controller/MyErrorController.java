package com.qiren.demo1.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.qiren.demo1.util.CommonUtils;

@Controller
@ControllerAdvice
public class MyErrorController {

	@ExceptionHandler(Exception.class)
	public @ResponseBody ResponseEntity<String> errorHandling(Exception ex) {
		return ResponseEntity.badRequest().body("Not a valid request!");
	}
}
