package com.qiren.demo1.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.qiren.demo1.util.CommonUtils;
import com.qiren.demo1.util.JsonSchemaHelper;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1")
public class JsonSchemaController {

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@PostMapping("/plan")
	public @ResponseBody ResponseEntity<String> createPlan(@RequestBody String planJson, HttpServletRequest request) {
		// JSON Validate
		JSONObject planJsonObject = null;
		try {
			planJsonObject = new JSONObject(planJson);
		} catch (Exception e) {
			e.printStackTrace();
			return CommonUtils.badRequest("Request body is not JSON!");
		}
		JsonSchemaHelper helper = new JsonSchemaHelper();
		if (!helper.validate(planJsonObject)) {
			// JSON not valid
			return CommonUtils.badRequest("Request JSON not Valid!");
		}
		// extract id from JSON
		String objectId = planJsonObject.optString("objectId");
		String objectType = planJsonObject.optString("objectType");
		Object storage = redisTemplate.opsForHash().get("demo1", objectId);
		boolean updateFlag = false;
		if (null != storage) {
			updateFlag = true;
		}
		// Storage
		redisTemplate.opsForHash().put("demo1", objectId, planJsonObject.toString());
		// Response
		JSONObject respObject = new JSONObject();
		respObject.put("objectId", objectId);
		if (updateFlag) {
			respObject.put("message", "exist and updated");
		}
		// ETag, used for if-not-match
		// simply use md5, we do not concern security here
		// change to the request will cause md5 to change
		String etag = CommonUtils.md5(planJsonObject.toString());
		return ResponseEntity.created(null).eTag(etag).body(respObject.toString());
	}

	@GetMapping(path = "/plan/{objectId}", produces = "application/json")
	public ResponseEntity<String> getPlan(@PathVariable String objectId, HttpServletRequest request) {
		Object json = redisTemplate.opsForHash().get("demo1", objectId);
		if (null == json) {
			return CommonUtils.notFound();
		}
		String ifNotMatch = request.getHeader("If-None-Match");
		// stored etag
		String etagStored = CommonUtils.md5(json.toString());
		if (null != ifNotMatch && !ifNotMatch.isBlank()) {
			// need check
			if (ifNotMatch.equals(etagStored)) {
				// match, return null
				return ResponseEntity.status(HttpStatusCode.valueOf(304)).body(null);
			}
		}
		// changed
		return ResponseEntity.ok().eTag(etagStored).body(json.toString());
	}

	@DeleteMapping("/plan/{objectId}")
	public ResponseEntity<String> deletePlan(@PathVariable String objectId) {
		Object json = redisTemplate.opsForHash().get("demo1", objectId);
		if (null == json) {
			// not found
			return CommonUtils.notFound();
		}
		// delete
		redisTemplate.opsForHash().delete("demo1", objectId);
		return ResponseEntity.noContent().build();
	}
}
