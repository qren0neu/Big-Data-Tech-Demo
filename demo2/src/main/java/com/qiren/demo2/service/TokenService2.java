package com.qiren.demo2.service;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @deprecated Not available until I get things done
 */
@Deprecated
@Service
public class TokenService2 {
	private static final String GOOGLE_CERTS_URL = "https://www.googleapis.com/oauth2/v1/certs";
	private static final String TOKEN_HEADER_ALG = "RS256";
	private static final String TOKEN_HEADER_TYPE = "JWT";

	public boolean authorize(String token) {
		try {
			String[] tokenParts = token.substring(7).split("\\.");
			if (tokenParts.length != 3) {
				return false;
			}

			// Decode the JWT claims
			String claimsJson = "";
			System.out.println(tokenParts[0]);
			claimsJson = new String(Base64.getDecoder().decode(tokenParts[0]), StandardCharsets.UTF_8);

			System.out.println(claimsJson);
			JSONObject claims = new JSONObject(claimsJson);

			// Verify the header
			if (!TOKEN_HEADER_ALG.equals(claims.getString("alg"))
					|| !TOKEN_HEADER_TYPE.equals(claims.getString("typ"))) {
				return false;
			}

			// Retrieve the Google public key used to sign the token
			String kid = claims.getString("kid");
			PublicKey publicKey = null;
			try {
				publicKey = getGooglePublicKey(kid);
			} catch (GeneralSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Verify the signature
			String signature = tokenParts[2];
			String signedData = tokenParts[0] + "." + tokenParts[1];
			return verifySignature(signature, signedData, publicKey);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	private static PublicKey getGooglePublicKey(String kid) throws GeneralSecurityException, IOException {
        URL url = new URL(GOOGLE_CERTS_URL);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to retrieve Google public keys");
        }

        JSONObject json = new JSONObject(
                new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        String publicKeyPem = json.getString(kid);
//        publicKeyPem = publicKeyPem.replaceAll(" ", "");
        System.out.println(publicKeyPem);

        byte[] publicKeyBytes = Base64.getMimeDecoder().decode(publicKeyPem);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

	private static boolean verifySignature(String signature, String signedData, PublicKey publicKey)
			throws GeneralSecurityException {
		Signature verifier = Signature.getInstance("SHA256withRSA");
		verifier.initVerify(publicKey);
		verifier.update(signedData.getBytes(StandardCharsets.UTF_8));
		return verifier.verify(Base64.getUrlDecoder().decode(signature));
	}
}
