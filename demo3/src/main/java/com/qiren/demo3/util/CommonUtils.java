package com.qiren.demo3.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

public class CommonUtils {

	public static ResponseEntity<String> badRequest(String body) {
		JSONObject obj = new JSONObject();
		obj.put("message", body);
		return ResponseEntity
				.badRequest()
				.body(obj.toString());
	}
	
	public static ResponseEntity<String> badRequest(String body, String etag) {
		JSONObject obj = new JSONObject();
		obj.put("message", body);
		return ResponseEntity
				.badRequest()
				.eTag(etag)
				.body(obj.toString());
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
