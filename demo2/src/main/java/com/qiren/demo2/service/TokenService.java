package com.qiren.demo2.service;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

@Service
public class TokenService {

	private AtomicBoolean initialized = new AtomicBoolean(false);

	private JacksonFactory factory;
	private GoogleIdTokenVerifier verifier;

	public void initialize() {
		initialized.set(true);
		factory = new JacksonFactory();
		verifier = new GoogleIdTokenVerifier.Builder(new ApacheHttpTransport(), factory)
				.setAudience(Arrays.asList(""))
				.build();
	}

	public boolean authorize(String idTokenString) {
		if (!initialized.get()) {
			initialize();
		}
		try {
			// we are taking the subString because the first few characters are 'Bearer '
			GoogleIdToken googleIdToken = verifier.verify(idTokenString.substring(7, idTokenString.length()));
			return googleIdToken != null;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
