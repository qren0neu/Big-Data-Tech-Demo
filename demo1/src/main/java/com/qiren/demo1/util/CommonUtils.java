package com.qiren.demo1.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseEntity;

public class CommonUtils {

	public static ResponseEntity<String> badRequest(String body) {
		return ResponseEntity
				.badRequest()
				.body(body);
	}
	
	public static ResponseEntity<String> notFound() {
		return ResponseEntity
				.notFound()
				.build();
	}
	
	public static String md5(String ori) {
		return DigestUtils.md5Hex(ori);
	}
}
