package com.qiren.demo2.controller;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.PatchExchange;

import com.qiren.demo2.service.JsonSchemaService;
import com.qiren.demo2.service.TokenService;
import com.qiren.demo2.service.TokenService2;
import com.qiren.demo2.util.CommonUtils;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class JsonSchemaController {

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private JsonSchemaService helper;
	@Autowired
	private TokenService tokenService;

	@PostMapping(path = "/v1/plan", produces = "application/json")
	public @ResponseBody ResponseEntity<String> createPlan(
			@RequestHeader(name = "Authorization", required = false) String token, 
			@RequestBody String planJson,
			HttpServletRequest request) {
		// token verify
		if (!tokenService.authorize(token)) {
			return CommonUtils.badRequest("Token expired or missing!");
		}
		// JSON Validate
		JSONObject planJsonObject = null;
		try {
			planJsonObject = new JSONObject(planJson);
		} catch (Exception e) {
			e.printStackTrace();
			return CommonUtils.badRequest("Request body is not JSON!");
		}
		if (!helper.validate(planJsonObject)) {
			// JSON not valid
			return CommonUtils.badRequest("Request JSON not Valid!");
		}
		// extract id from JSON
		String objectId = planJsonObject.optString("objectId");
		String objectType = planJsonObject.optString("objectType");
		Object storage = redisTemplate.opsForHash().get("demo2", objectId);
		if (null != storage) {
			// now we have put and patch method, so if created, we will not create it again
			return CommonUtils.badRequest("objectId: " + objectId + " already exists!");
		}
		// Storage
		redisTemplate.opsForHash().put("demo2", objectId, planJsonObject.toString());
		// Response
		JSONObject respObject = new JSONObject();
		respObject.put("objectId", objectId);
		// ETag, used for if-not-match
		// simply use md5, we do not concern security here
		// change to the request will cause md5 to change
		String etag = CommonUtils.md5(planJsonObject.toString());
		return ResponseEntity.created(null).eTag(etag).body(respObject.toString());
	}

	@GetMapping(path = "/v1/plan/{objectId}", produces = "application/json")
	public ResponseEntity<String> getPlan(
			@RequestHeader(name = "Authorization", required = false) String token, 
			@RequestHeader(name = "If-None-Match", required = false) String ifNotMatch,
			@PathVariable String objectId, 
			HttpServletRequest request) {
		// token verify
		if (!tokenService.authorize(token)) {
			return CommonUtils.badRequest("Token expired or missing!");
		}
		Object json = redisTemplate.opsForHash().get("demo2", objectId);
		if (null == json) {
			return CommonUtils.notFound();
		}
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

	@DeleteMapping(path = "/v1/plan/{objectId}", produces = "application/json")
	public ResponseEntity<String> deletePlan(
			@RequestHeader(name = "Authorization", required = false) String token, 
			@PathVariable String objectId) {
		// token verify
		if (!tokenService.authorize(token)) {
			return CommonUtils.badRequest("Token expired or missing!");
		}
		Object json = redisTemplate.opsForHash().get("demo2", objectId);
		if (null == json) {
			// not found
			return CommonUtils.notFound();
		}
		// delete
		redisTemplate.opsForHash().delete("demo2", objectId);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping(path = "/v1/plan/{objectId}", produces = "application/json")
	public ResponseEntity<String> patchPlan(
			@RequestHeader(name = "Authorization", required = false) String token, 
			@RequestHeader(name = "If-Match", required = false) String ifMatch,
			@PathVariable String objectId,
			@RequestBody String planJson) {
		// token verify
		if (!tokenService.authorize(token)) {
			return CommonUtils.badRequest("Token expired or missing!");
		}
		// JSON Validate
		JSONObject planJsonObject = null;
		try {
			planJsonObject = new JSONObject(planJson);
		} catch (Exception e) {
			e.printStackTrace();
			return CommonUtils.badRequest("Request body is not JSON!");
		}
		if (!helper.validate(planJsonObject)) {
			// JSON not valid
			return CommonUtils.badRequest("Request JSON not Valid!");
		}
		// check if exist
		Object json = redisTemplate.opsForHash().get("demo2", objectId);
		if (null == json) {
			// not found
			return CommonUtils.notFound();
		}
		// check if changing object Id
		String innerId = planJsonObject.getString("objectId");
		if (!innerId.equals(objectId)) {
			return CommonUtils.badRequest("Cannot change object Id!");
		}
		String etagStored = CommonUtils.md5(json.toString());
		if (null != ifMatch && !ifMatch.isBlank()) {
			// check if Match
			if (!ifMatch.equals(etagStored)) {
				// match, return null
				return CommonUtils.badRequest("Content has been modified!", etagStored);
			}
		}

		// update
		JSONObject jsonStored = new JSONObject(json.toString());
		JSONArray storedPlans = jsonStored.getJSONArray("linkedPlanServices");
		JSONArray newPlans = planJsonObject.getJSONArray("linkedPlanServices");
		List<JSONObject> addList = new ArrayList<>();
		for (int j = 0; j < newPlans.length(); j++) {
			JSONObject newPlan = newPlans.getJSONObject(j);
			String newId = newPlan.getString("objectId");
			boolean find = false;
			for (int i = 0; i < storedPlans.length(); i++) {
				JSONObject storedPlan = storedPlans.getJSONObject(i);
				String id = storedPlan.getString("objectId");
				if (id.equals(newId)) {
					// update
					storedPlans.put(i, newPlan);
					find = true;
					break;
				}
			}
			// not found, add
			if (!find) {
				addList.add(newPlan);
			}
		}
		
		for (JSONObject obj : addList) {
			storedPlans.put(obj);
		}
		// store the stored plan again
		jsonStored.put("linkedPlanServices", storedPlans);
		String jsonString = jsonStored.toString();
		redisTemplate.opsForHash().put("demo2", objectId, jsonString);
		String newTag = CommonUtils.md5(jsonString);
		// build response
		JSONObject respObject = new JSONObject();
		respObject.put("message", "update plan success!");
		
		return ResponseEntity.ok().eTag(newTag).body(respObject.toString());
	}

	@PatchMapping(path = "/v2/plan/{objectId}", produces = "application/json")
	public ResponseEntity<String> patchPlan2(
			@RequestHeader(name = "Authorization", required = false) String token, 
			@RequestHeader(name = "If-Match", required = false) String ifMatch,
			@PathVariable String objectId,
			@RequestBody String planJson) {
		// token verify
		if (!tokenService.authorize(token)) {
			return CommonUtils.badRequest("Token expired or missing!");
		}
		// JSON Validate
		JSONObject planJsonObject = null;
		try {
			planJsonObject = new JSONObject(planJson);
		} catch (Exception e) {
			e.printStackTrace();
			return CommonUtils.badRequest("Request body is not JSON!");
		}
		if (!helper.validate(planJsonObject)) {
			// JSON not valid
			return CommonUtils.badRequest("Request JSON not Valid!");
		}
		// check if exist
		Object json = redisTemplate.opsForHash().get("demo2", objectId);
		if (null == json) {
			// not found
			return CommonUtils.notFound();
		}
		// check if changing object Id
		String innerId = planJsonObject.getString("objectId");
		if (!innerId.equals(objectId)) {
			return CommonUtils.badRequest("Cannot change object Id!");
		}
		String etagStored = CommonUtils.md5(json.toString());
		if (null != ifMatch && !ifMatch.isBlank()) {
			// check if Match
			if (!ifMatch.equals(etagStored)) {
				// match, return null
				return CommonUtils.badRequest("Content has been modified!", etagStored);
			}
		}

		// update
		JSONObject jsonStored = new JSONObject(json.toString());
		for (String jsonKey : planJsonObject.keySet()) {
			Object jsonValue = planJsonObject.get(jsonKey);
			if (jsonValue instanceof JSONArray) {
				JSONArray jsonValueArray = (JSONArray) jsonValue;
				JSONArray oldJsonArray = (JSONArray) jsonStored.getJSONArray(jsonKey);
				boolean find = false;
				for (int i = 0; i < jsonValueArray.length(); i++) {
					JSONObject arrayItem = (JSONObject) jsonValueArray.get(i);
					String id = arrayItem.getString("objectId");
					for (int j = 0; j < oldJsonArray.length(); j++) {
						JSONObject oldArrayItem = (JSONObject) oldJsonArray.get(j);
						String oldId = oldArrayItem.getString("objectId");
						if (id.equals(oldId)) {
							oldJsonArray.put(j, arrayItem);
							find = true;
							break;
						}
					}
					if (!find) {
						oldJsonArray.put(arrayItem);
					}
				}
				jsonStored.put(jsonKey, oldJsonArray);
			} else {
				jsonStored.put(jsonKey, jsonValue);
			}
		}
		
		String jsonString = jsonStored.toString();
		redisTemplate.opsForHash().put("demo2", objectId, jsonString);
		String newTag = CommonUtils.md5(jsonString);
		// build response
		JSONObject respObject = new JSONObject();
		respObject.put("message", "update plan success!");
		
		return ResponseEntity.ok().eTag(newTag).body(respObject.toString());
	}
	
	@PutMapping(path = "/v1/plan/{objectId}", produces = "application/json")
	public ResponseEntity<String> updatePlan(
			@RequestHeader(name = "Authorization", required = false) String token, 
			@RequestHeader(name = "If-Match", required = false) String ifMatch,
			@PathVariable String objectId,
			@RequestBody String planJson) {
		// token verify
		if (!tokenService.authorize(token)) {
			return CommonUtils.badRequest("Token expired or missing!");
		}
		// JSON Validate
		JSONObject planJsonObject = null;
		try {
			planJsonObject = new JSONObject(planJson);
		} catch (Exception e) {
			e.printStackTrace();
			return CommonUtils.badRequest("Request body is not JSON!");
		}
		if (!helper.validate(planJsonObject)) {
			// JSON not valid
			return CommonUtils.badRequest("Request JSON not Valid!");
		}
		// check if exist
		Object json = redisTemplate.opsForHash().get("demo2", objectId);
		if (null == json) {
			// not found
			return CommonUtils.notFound();
		}
		String etagStored = CommonUtils.md5(json.toString());
		if (null != ifMatch && !ifMatch.isBlank()) {
			// check if Match
			if (!ifMatch.equals(etagStored)) {
				// match, return null
				return CommonUtils.badRequest("Content has been modified!", etagStored);
			}
		}

		// update
		String jsonString = planJsonObject.toString();
		redisTemplate.opsForHash().put("demo2", objectId, jsonString);
		String newTag = CommonUtils.md5(jsonString);
		// build response
		JSONObject respObject = new JSONObject();
		respObject.put("message", "update plan success!");
		
		return ResponseEntity.ok().eTag(newTag).body(respObject.toString());
	}
	
}
