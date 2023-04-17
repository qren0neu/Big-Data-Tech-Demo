package com.qiren.demo3.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.qiren.demo3.service.JsonSchemaService;
import com.qiren.demo3.service.PlanService;
import com.qiren.demo3.service.TokenService;
import com.qiren.demo3.util.CommonUtils;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class JsonSchemaController {

	@Autowired
	private PlanService planService;
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
		return planService.createPlan(token, planJson, request);
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
		return planService.getPlan(token, ifNotMatch, objectId, request);
	}

	@DeleteMapping(path = "/v1/plan/{objectId}", produces = "application/json")
	public ResponseEntity<String> deletePlan(
			@RequestHeader(name = "Authorization", required = false) String token, 
			@PathVariable String objectId) {
		// token verify
		if (!tokenService.authorize(token)) {
			return CommonUtils.badRequest("Token expired or missing!");
		}
		return planService.deletePlan(token, objectId);
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
		return planService.patchPlan(token, ifMatch, objectId, planJson);
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
		return planService.patchPlan2(token, ifMatch, objectId, planJson);
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
		return planService.updatePlan(token, ifMatch, objectId, planJson);
	}
	
}
