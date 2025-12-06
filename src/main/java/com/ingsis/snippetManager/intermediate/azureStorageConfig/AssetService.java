package com.ingsis.snippetManager.intermediate.azureStorageConfig;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class AssetService {

    private final RestTemplate restTemplate;
    private final String bucketUrl;
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);

    @Autowired
    public AssetService(@Value("${bucket.url}") String bucketUrl) {
        this.restTemplate = new RestTemplate();
        this.bucketUrl = bucketUrl;
    }

    private String buildUrl(UUID key) {
        String container = "snippets";
        return bucketUrl + "/" + container + "/" + key.toString();
    }

    public ResponseEntity<String> getSnippet(UUID snippetId) {
        try {
            String url = buildUrl(snippetId);
            logger.info("Getting snippet from Url: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setAll(getCorrelationHeader());

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Snippet not found, status: {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error getting snippet", e);
            return ResponseEntity.badRequest().body("Error getting snippet: " + e.getMessage());
        }
    }

    public ResponseEntity<String> saveSnippet(UUID snippetId, String content) {
        try {
            String url = buildUrl(snippetId);
            logger.info("Url : {}", url);
            byte[] bodyBytes = content.getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setAll(getCorrelationHeader());
            logger.info(new String(bodyBytes, StandardCharsets.UTF_8));
            HttpEntity<byte[]> request = new HttpEntity<>(bodyBytes, headers);
            restTemplate.put(url, request);
            logger.info("Snippet saved at Url: {}", url);
            return ResponseEntity.ok("Snippet saved successfully.");
        } catch (HttpClientErrorException e) {
            logger.error("Not saved content, status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error saving snippet", e);
            return ResponseEntity.badRequest().body("Error saving snippet: " + e.getMessage());
        }
    }

    private java.util.Map<String, String> getCorrelationHeader() {
        java.util.Map<String, String> headers = new HashMap<>();
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (Objects.nonNull(correlationId)) {
            headers.put("X-Correlation-Id", correlationId);
        }
        return headers;
    }
}