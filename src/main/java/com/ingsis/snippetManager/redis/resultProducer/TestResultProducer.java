package com.ingsis.snippetManager.redis.resultProducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.redis.dto.testing.TestResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestResultProducer {

    private static final Logger logger = LoggerFactory.getLogger(TestResultProducer.class);

    private final String streamKey;
    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;


    public TestResultProducer(@Value("${redis.streams.testResult}") String streamKey,
            @Qualifier("redisTemplate") RedisTemplate<String, String> redis, ObjectMapper objectMapper) {
        this.streamKey = streamKey;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void emit(String jsonMessage) {
        ObjectRecord<String, String> record = StreamRecords.newRecord().ofObject(jsonMessage).withStreamKey(streamKey);
        redis.opsForStream().add(record);
    }

    public void publish(TestResultEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);

            logger.info("Publishing TestResultEvent for Snippet({})", event.snippetId());

            emit(json);

        } catch (Exception ex) {
            logger.error("Error publishing TestResultEvent", ex);
        }
    }
}
