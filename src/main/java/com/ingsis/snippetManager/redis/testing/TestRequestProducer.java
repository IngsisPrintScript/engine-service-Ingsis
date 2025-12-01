package com.ingsis.snippetManager.redis.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.redis.dto.TestRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestRequestProducer {

    private static final Logger logger = LoggerFactory.getLogger(TestRequestProducer.class);

    private final String streamName;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public TestRequestProducer(@Value("${redis.streams.testRequest}") String streamName,
            RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.streamName = streamName;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(TestRequestEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForStream().add(ObjectRecord.create(streamName, json));
            logger.info("Published LintRequestEvent for Snippet({})", event.snippetId().toString());
        } catch (Exception ex) {
            logger.error("Error publishing LintRequestEvent: {}", ex.getMessage());
        }
    }
}
