/*
 * This code is to be used exclusively in connection with Ping Identity Corporation software or services.
 * Ping Identity Corporation only offers such software or services to legal entities who have entered into
 * a binding license agreement with Ping Identity Corporation.
 *
 * Copyright 2024 Ping Identity Corporation. All Rights Reserved
 */

package org.forgerock.am.marketplace.googlechromedevicetrust;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.security.SecureRandom;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Base64;
import java.util.Date;
import java.net.URI;

import java.security.PrivateKey;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;

import javax.inject.Singleton;
import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.http.header.AuthorizationHeader;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.http.header.authorization.BearerToken;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to integrate with the Google Chrome Verified Access API
 */
@Singleton
public class GoogleChromeDeviceTrustClient {

    private static final Logger logger = LoggerFactory.getLogger(GoogleChromeDeviceTrustClient.class);

    private final Handler handler;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String VA_ORIGIN = "https://verifiedaccess.googleapis.com";
    private static PrivateKey privateKey;

    /**
     * Creates a new instance that will close the underlying HTTP client upon shutdown.
     */
    @Inject
    public GoogleChromeDeviceTrustClient(@Named("CloseableHttpClientHandler") org.forgerock.http.Handler handler) {
        this.handler = handler;
    }

    /**
     * the POST {VA_ORIGIN}/v2/challenge:verify?key={apiKey}
     *
     * @param apiKey                             The Google Cloud API Key
     * @param privateKey                         The Google Admin Credentials Private Key
     * @param kid                                Used to verify the authenticity and integrity of the JWT
     * @param clientEmail                        The Google Admin Credentials Client Email
     * @param sharedStateVerifiedAccessChallenge The HTTP header that adds PingAM to the allowlist
     * @return Json containing the response from the operation
     * @throws GoogleChromeDeviceTrustException When API response != 200
     */
    public JsonValue getVerifiedAccess(
            String apiKey,
            String privateKey,
            String kid,
            String clientEmail,
            String sharedStateVerifiedAccessChallenge
    ) throws GoogleChromeDeviceTrustException {

        // Call 'buildAccessToken' to build the JWT (access token)
        String accessToken = buildAccessToken(privateKey, kid, clientEmail);

        // Create the request url
        Request request;
        URI uri = URI.create(
                VA_ORIGIN + "/v2/challenge:verify?key=" + apiKey
        );

        // Create JSON value for request the body
        JsonValue challengeResponseJson = json(object(1));
        challengeResponseJson.put("challengeResponse", sharedStateVerifiedAccessChallenge);

        // Build and send the API request
        try {
            request = new Request().setUri(uri).setMethod("POST");
            addAuthorizationHeader(request, accessToken);
            request.getHeaders().add("Content-Type", "application/json");
            request.getHeaders().add("Accept", "*/*");
            request.getHeaders().add("Accept-Encoding", "gzip, deflate, br");
            request.getHeaders().add("User-Agent", "PingAM");
            // Set the request body to the challenge response object
            request.setEntity(challengeResponseJson.toString());
            Response response = handler.handle(new RootContext(), request).getOrThrow();
            if (response.getStatus() == Status.CREATED || response.getStatus() == Status.OK) {
                return json(response.getEntity().getJson());
            } else {
                throw new GoogleChromeDeviceTrustException("Google Chrome API response with error."
                        + response.getStatus()
                        + "-" + response.getEntity().getString());
            }
        } catch (Exception e) {
            throw new GoogleChromeDeviceTrustException("Failed to process client authorization: " + e);
        }
    }

    /**
     * @param privateKeyString The Google Cloud API Key
     * @param kid              Used to verify the authenticity and integrity of the JWT
     * @param clientEmail      The Google Admin Credentials Client Email
     * @return String containing the authorization JWT
     * @throws GoogleChromeDeviceTrustException When build fails
     */
    private String buildAccessToken(
            String privateKeyString,
            String kid,
            String clientEmail
    ) throws GoogleChromeDeviceTrustException {

        // Sets the token expiration to 1 minute
        Date exp = new Date(System.currentTimeMillis() + 1000 * 60);

        // Format and generate the private key
        try {
            final byte[] privateData = Base64.getDecoder().decode(privateKeyString
                    .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", ""));

            privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateData));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error(String.valueOf(e));
        }

        // Generate unique identifier for the JWT
        String jti = generateJti();

        // Build the access token (JWT)
        try {
            JwtBuilderFactory jwtBuilderFactory = new JwtBuilderFactory();
            return jwtBuilderFactory
                    .jws(new SigningManager().newRsaSigningHandler(privateKey))
                    .headers()
                    .alg(JwsAlgorithm.RS256)
                    .kid(kid)
                    .done()
                    .claims(jwtBuilderFactory.claims()
                            .iss(clientEmail)
                            .sub(clientEmail)
                            .aud(Collections.singletonList("https://verifiedaccess.googleapis.com/"))
                            .exp(exp)
                            .jti(jti)
                            .iat(new Date(System.currentTimeMillis()))
                            .build())
                    .build();
        } catch (Exception e) {
            throw new GoogleChromeDeviceTrustException("Failed to build authorization JWT " + e);
        }
    }

    private static String generateJti() {
        return new BigInteger(160, SECURE_RANDOM).toString(Character.MAX_RADIX);
    }

    private static void addAuthorizationHeader(Request request, String accessToken) throws MalformedHeaderException {
        AuthorizationHeader header = new AuthorizationHeader();
        BearerToken bearerToken = new BearerToken(accessToken);
        header.setRawValue(BearerToken.NAME + " " + bearerToken);
        request.addHeaders(header);
    }
}
