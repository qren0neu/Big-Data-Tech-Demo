package com.qiren.demo3.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.client.RestTemplate;

import com.qiren.demo3.messagequeue.MessageQueueReceiver;
import com.qiren.demo3.messagequeue.MessageQueueSender;
import com.qiren.demo3.util.CommonUtils;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class PlanService {

	public static final String ES_URL = "http://localhost:9200";
	public static final String INDEX = "/plan_index_test";
	public static final String DOC = "/_doc";

	public static final String FULL_URL = ES_URL + INDEX + DOC;
	public static final String SUFFIX = "refresh";

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private MessageQueueSender sender;
	@Autowired
	private MessageQueueReceiver receiver;
	@Autowired
	private JsonSchemaService helper;
	@Autowired
	private RestTemplate restTemplate;

	public ResponseEntity<String> createPlan(String token, String planJson, HttpServletRequest request) {
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
		Object storage = redisTemplate.opsForHash().get("demo3", objectId);
		if (null != storage) {
			// now we have put and patch method, so if created, we will not create it again
			return CommonUtils.badRequest("objectId: " + objectId + " already exists!");
		}
		// Storage
		redisTemplate.opsForHash().put("demo3", objectId, planJsonObject.toString());
		// Enqueue for indexing
		objectEnqueue(planJsonObject);
		// Response
		JSONObject respObject = new JSONObject();
		respObject.put("objectId", objectId);
		// ETag, used for if-not-match
		// simply use md5, we do not concern security here
		// change to the request will cause md5 to change
		String etag = CommonUtils.md5(planJsonObject.toString());
		return ResponseEntity.created(null).eTag(etag).body(respObject.toString());
	}

	public ResponseEntity<String> getPlan(String token, String ifNotMatch, String objectId,
			HttpServletRequest request) {
		Object json = redisTemplate.opsForHash().get("demo3", objectId);
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

	public ResponseEntity<String> deletePlan(String token, String objectId) {
		Object json = redisTemplate.opsForHash().get("demo3", objectId);
		if (null == json) {
			// not found
			return CommonUtils.notFound();
		}
		// delete
		redisTemplate.opsForHash().delete("demo3", objectId);
		// delete in es
		deleteData(new JSONObject(json.toString()), "");
		return ResponseEntity.noContent().build();
	}

	public ResponseEntity<String> patchPlan(String token, String ifMatch, String objectId, String planJson) {
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
		Object json = redisTemplate.opsForHash().get("demo3", objectId);
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
		// Enqueue for indexing
		objectEnqueue(planJsonObject);
		// store the stored plan again
		jsonStored.put("linkedPlanServices", storedPlans);
		String jsonString = jsonStored.toString();
		redisTemplate.opsForHash().put("demo3", objectId, jsonString);
		String newTag = CommonUtils.md5(jsonString);
		// build response
		JSONObject respObject = new JSONObject();
		respObject.put("message", "update plan success!");

		return ResponseEntity.ok().eTag(newTag).body(respObject.toString());
	}

	@PatchMapping(path = "/v2/plan/{objectId}", produces = "application/json")
	public ResponseEntity<String> patchPlan2(String token, String ifMatch, String objectId, String planJson) {
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
		Object json = redisTemplate.opsForHash().get("demo3", objectId);
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

		// Enqueue for indexing
		objectEnqueue(planJsonObject);
		// store
		String jsonString = jsonStored.toString();
		redisTemplate.opsForHash().put("demo3", objectId, jsonString);
		String newTag = CommonUtils.md5(jsonString);
		// build response
		JSONObject respObject = new JSONObject();
		respObject.put("message", "update plan success!");

		return ResponseEntity.ok().eTag(newTag).body(respObject.toString());
	}

	public ResponseEntity<String> updatePlan(String token, String ifMatch, String objectId, String planJson) {
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
		Object json = redisTemplate.opsForHash().get("demo3", objectId);
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
		redisTemplate.opsForHash().put("demo3", objectId, jsonString);
		
		// es delete and es re create
		deleteData(new JSONObject(json.toString()), null);
		objectEnqueue(planJsonObject);
		
		String newTag = CommonUtils.md5(jsonString);
		// build response
		JSONObject respObject = new JSONObject();
		respObject.put("message", "update plan success!");

		return ResponseEntity.ok().eTag(newTag).body(respObject.toString());
	}
	
	private void objectEnqueue(JSONObject planJsonObject) {
		extractObjects(planJsonObject, "");
	}

    @RabbitListener(queues = "myQueue")
	private void objectDequeue(String message) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> requestEntity;
		ResponseEntity<String> responseEntity;
		String url;
    	JSONObject json = new JSONObject(message);
		if (!json.getString("objectType").equals("plan")) {
			url = FULL_URL + "/" + json.getString("objectId") + "?routing="
					+ json.getJSONObject("relation").getString("parent") + "&refresh";
		} else {
            url = FULL_URL + "/" + json.getString("objectId") + "?routing="
                    + json.getString("objectId") + "&refresh";
		}
		requestEntity = new HttpEntity<String>(json.toString(), headers);
		responseEntity = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
		System.out.println(responseEntity);
	}

	private void extractObjects(JSONObject obj, String parent) {
		Stack<JSONObject> stack = new Stack<>();
		extractObjects(obj, "", parent, stack);

		while (!stack.isEmpty()) {
			JSONObject json = stack.pop();
			sender.sendMessage(json.toString());
		}
	}

	private void extractObjects(JSONObject obj, String name, String parent, Stack<JSONObject> stack) {
		JSONObject result = new JSONObject();
		String objectId = obj.optString("objectId");
		String objectType = obj.optString("objectType");
		for (String key : obj.keySet()) {
			Object value = obj.get(key);

			if (value instanceof JSONObject) {
				JSONObject nestedObj = (JSONObject) value;
				extractObjects(nestedObj, key, objectId, stack);

			} else if (value instanceof JSONArray) {
				JSONArray nestedArr = (JSONArray) value;
				for (int i = 0; i < nestedArr.length(); i++) {
					if (nestedArr.get(i) instanceof JSONObject) {
						JSONObject nestedObj = (JSONObject) nestedArr.get(i);
						extractObjects(nestedObj, key, objectId, stack);
					}
				}
			} else {
				result.put(key, value);
			}
		}
		if (null != parent && !parent.isBlank()) {
			JSONObject relation = new JSONObject();
			relation.put("parent", parent);
			relation.put("name", name);
			result.put("relation", relation);
		} else {
			result.put("relation", objectType);
		}
		stack.push(result);
	}

	private void deleteData(JSONObject obj, String parent) {
		// extract all the ids from redis stored data
		// and delete one by one in es
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity;
        ResponseEntity<String> responseEntity;
        String url;

        Stack<JSONObject> stack = new Stack<>();
        extractObjects(obj, "", parent, stack);

        while (!stack.isEmpty()) {
            JSONObject json = stack.pop();
            if (!json.getString("objectType").equals("plan")) {
                url = FULL_URL + "/" + json.getString("objectId") + "?routing="
                        + json.getJSONObject("relation").getString("parent") + "&refresh";
            } else {
                url = FULL_URL + "/" + json.getString("objectId") + "?routing="
                        + json.getString("objectId") + "&refresh";
            }
            requestEntity = new HttpEntity<String>(null, headers);
            try {
                responseEntity = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
                System.out.println(responseEntity);
            } catch (Exception e) {
				// TODO: handle exception
            	e.printStackTrace();
			}
        }
    }
}
