package com.konkerlabs.platform.registry.integration.gateways;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.konkerlabs.platform.registry.integration.exceptions.IntegrationException;

public class SMSMessageGatewayTwilioImpl implements SMSMessageGateway {
    private static final Logger LOGGER = LoggerFactory.getLogger(SMSMessageGatewayTwilioImpl.class);

    private RestTemplate restTemplate;
    private String username;
    private String password;
    private URI apiUri;
    private String fromPhoneNumber;

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public URI getApiUri() {
        return apiUri;
    }

    public void setApiUri(URI apiUri) {
        this.apiUri = apiUri;
    }

    public String getFromPhoneNumber() {
        return fromPhoneNumber;
    }

    public void setFromPhoneNumber(String fromPhoneNumber) {
        this.fromPhoneNumber = fromPhoneNumber;
    }

    private void addAuthorizationHeaders(MultiValueMap<String, String> headers) {
        String encodedCredentials = Base64Utils
                .encodeToString(MessageFormat.format("{0}:{1}", username, password).getBytes());

        headers.add("Authorization", MessageFormat.format("Basic {0}", encodedCredentials));
    }

    @Override
    public void send(String text, String destinationPhoneNumber) throws IntegrationException {

        if (destinationPhoneNumber == null || destinationPhoneNumber.trim().length() == 0) {
            throw new IllegalArgumentException("Destination Phone Number must be provided");
        }

        if (text == null || text.trim().length() == 0) {
            throw new IllegalArgumentException("SMS Body must be provided");
        }

        if (username == null || username.trim().length() == 0) {
            throw new IllegalStateException("API Username must be provided");
        }

        if (password == null || password.trim().length() == 0) {
            throw new IllegalStateException("API Password must be provided");
        }

        if (fromPhoneNumber == null || fromPhoneNumber.trim().length() == 0) {
            throw new IllegalStateException("From Phone Number must be provided");
        }

        if (apiUri == null) {
            throw new IllegalStateException("API URI must be provided");
        }

        if (restTemplate == null) {
            throw new IllegalStateException("RestTemplate must be provided");
        }


        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
            form.add("To", destinationPhoneNumber);
            form.add("Body", text);
            form.add("From", fromPhoneNumber);

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
            addAuthorizationHeaders(headers);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(form,
                    headers);

            LOGGER.debug("Sending SMS Message [{}] to [{}].", text, destinationPhoneNumber);

            restTemplate.postForLocation(apiUri, entity);

        } catch (RestClientException rce) {
            throw new IntegrationException(
                    MessageFormat.format("Exception while sending {0} to {1}", text, destinationPhoneNumber), rce);
        }
    }
}