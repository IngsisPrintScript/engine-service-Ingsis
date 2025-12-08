package com.ingsis.snippetManager.redis.ResultProducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.redis.dto.format.FormatResultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class FormatResultProducer {

    private static final Logger logger = LoggerFactory.getLogger(FormatResultProducer.class);

    private final String streamKey;
    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;

    public FormatResultProducer(
            @Value("${redis.streams.formatResult}") String streamKey,
            RedisTemplate<String, String> redis,
            ObjectMapper objectMapper
    ) {
        this.streamKey = streamKey;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }
    public void emit(String jsonMessage) {
        ObjectRecord<String, String> record =
                StreamRecords.newRecord()
                        .ofObject(jsonMessage)
                        .withStreamKey(streamKey);

        redis.opsForStream().add(record);
    }
    public void publish(FormatResultEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);

            logger.info("Publishing FormatResultEvent for Snippet({})", event.snippetId());

            emit(json);

        } catch (Exception ex) {
            logger.error("Error publishing FormatResultEvent", ex);
        }
    }
}