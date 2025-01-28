/*
 * This code is to be used exclusively in connection with Ping Identity Corporation software or services.
 * Ping Identity Corporation only offers such software or services to legal entities who have entered into
 * a binding license agreement with Ping Identity Corporation.
 *
 * Copyright 2024 Ping Identity Corporation. All Rights Reserved
 */

package org.forgerock.am.marketplace.googlechromedevicetrust;

import static org.forgerock.am.marketplace.googlechromedevicetrust.GoogleChromeDeviceTrust.GoogleChromeDeviceTrustOutcomeProvider.CLIENT_ERROR_OUTCOME_ID;

import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.inject.Inject;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.util.i18n.PreferredLocales;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.OutcomeProvider;

import com.google.inject.assistedinject.Assisted;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * The Google Chrome Device Trust node allows access to the Device Trust Signals available from a managed Google Chrome browser.
 */
@Node.Metadata(outcomeProvider = GoogleChromeDeviceTrust.GoogleChromeDeviceTrustOutcomeProvider.class,
        configClass = GoogleChromeDeviceTrust.Config.class,
        tags = {"marketplace", "trustnetwork"})
public class GoogleChromeDeviceTrust implements Node {

    private static final Logger logger = LoggerFactory.getLogger(GoogleChromeDeviceTrust.class);
    private static final String LOGGER_PREFIX = "[GoogleChromeDeviceTrust]" + GoogleChromeDeviceTrustPlugin.LOG_APPENDER;

    private static final String VERIFIED_ACCESS_CHALLENGE = "challenge";

    private static final String BUNDLE = GoogleChromeDeviceTrust.class.getName();

    private final Config config;
    private final GoogleChromeDeviceTrustClient client;

    /**
     * Configuration for the Google Chrome node.
     */
    public interface Config {
        /**
         * Shared state attribute containing Google Cloud API Key
         *
         * @return The Google Cloud API Key shared state attribute
         */
        @Attribute(order = 100, requiredValue = true)
        String apiKey();

        /**
         * Shared state attribute containing Google Admin Credentials Private Key
         *
         * @return The Google Admin Credentials Private Key shared state attribute
         */
        @Attribute(order = 200, requiredValue = true)
        default String privateKey() {
            return "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----";
        }

        /**
         * Shared state attribute containing the Key ID
         *
         * @return The Key ID shared state attribute
         */
        @Attribute(order = 300, requiredValue = true)
        String kid();

        /**
         * Shared state attribute containing Google Admin Credentials Client Email
         *
         * @return The Google Admin Credentials Client Email shared state attribute
         */
        @Attribute(order = 400, requiredValue = true)
        String clientEmail();

    }

    /**
     * The Google Chrome node constructor.
     *
     * @param config the node configuration.
     * @param client the {@link GoogleChromeDeviceTrustClient} instance.
     */
    @Inject
    public GoogleChromeDeviceTrust(@Assisted Config config, GoogleChromeDeviceTrustClient client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public Action process(TreeContext context) {

        // Create the flow input based on the node state
        NodeState nodeState = context.getStateFor(this);

        try {
            // Capture the challenge header
            Map<String, List<String>> parameters = context.request.parameters;

            // Check if "challengeresponse" exists and its value is "devicetrustdisabled"
            if (parameters.containsKey("challengeresponse") &&
                    parameters.get("challengeresponse") != null &&
                    !parameters.get("challengeresponse").isEmpty() &&
                    "devicetrustdisabled".equals(parameters.get("challengeresponse").get(0))
            ) {
                logger.error("The 'challengeresponse' parameter has been set to: {}", parameters.get("challengeresponse").get(0));
                return Action.goTo(GoogleChromeDeviceTrustOutcomeProvider.DISABLED_OUTCOME_ID).build();
            } else {
                // URL encode and decode the challenge header to preserve the '+' signs
                String challengeResponseEncoded = parameters.get("challengeresponse").get(0).replaceAll("\\+", "%2b");
                String challengeResponse = java.net.URLDecoder.decode(challengeResponseEncoded, StandardCharsets.UTF_8);

                // API call to retrieve device signals
                JsonValue getVerifiedAccessResponse = client.getVerifiedAccess(
                        config.apiKey(),
                        config.privateKey(),
                        config.kid(),
                        config.clientEmail(),
                        challengeResponse
                );

                // Dynamically places the return object key-value pairs into transient state
                for (String key : getVerifiedAccessResponse.keys()) {
                    JsonValue prop = getVerifiedAccessResponse.get(key);
                    nodeState.putTransient(key, prop);
                }

                return Action.goTo(GoogleChromeDeviceTrustOutcomeProvider.ENABLED_OUTCOME_ID).build();
            }

        } catch (Exception ex) {
            String stackTrace = ExceptionUtils.getStackTrace(ex);
            logger.error(LOGGER_PREFIX + "Exception occurred: ", ex);
            context.getStateFor(this).putTransient(LOGGER_PREFIX + "Exception", new Date() + ": " + ex.getMessage());
            context.getStateFor(this).putTransient(LOGGER_PREFIX + "StackTrace", new Date() + ": " + stackTrace);
            return Action.goTo(CLIENT_ERROR_OUTCOME_ID).build();
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[]{
                new InputState(VERIFIED_ACCESS_CHALLENGE, false)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[]{
                new OutputState("verifiedAccessResults")
        };
    }

    public static class GoogleChromeDeviceTrustOutcomeProvider implements OutcomeProvider {

        // If challenge response is not there, continue
        static final String ENABLED_OUTCOME_ID = "enabled";
        static final String DISABLED_OUTCOME_ID = "disabled";
        static final String CLIENT_ERROR_OUTCOME_ID = "clientError";

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue jsonValue) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, GoogleChromeDeviceTrustOutcomeProvider.class.getClassLoader());

            ArrayList<Outcome> outcomes = new ArrayList<>();

            outcomes.add(new Outcome(ENABLED_OUTCOME_ID, bundle.getString(ENABLED_OUTCOME_ID)));
            outcomes.add(new Outcome(DISABLED_OUTCOME_ID, bundle.getString(DISABLED_OUTCOME_ID)));
            outcomes.add(new Outcome(CLIENT_ERROR_OUTCOME_ID, bundle.getString(CLIENT_ERROR_OUTCOME_ID)));

            return outcomes;
        }
    }
}
