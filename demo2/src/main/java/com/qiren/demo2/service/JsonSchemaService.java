package com.qiren.demo2.service;

import java.io.IOException;
import java.io.InputStream;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;

@Service
public class JsonSchemaService {

	private static Schema jsonSchemaValidator;

	public static void initialize(String schemaPath) {
		try (InputStream inputStream = 
				JsonSchemaService.class.getResourceAsStream(schemaPath)) {
			JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
			jsonSchemaValidator = SchemaLoader.load(rawSchema);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean validate(String json) {
		try {
			JSONObject jsonObject = new JSONObject(json);
			jsonSchemaValidator.validate(jsonObject);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean validate(JSONObject json) {
		try {
			jsonSchemaValidator.validate(json);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
