package com.ingsis.snippetManager.redis.config;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisInitializer {

    private final StringRedisTemplate template;

    @Value("${redis.streams.lintRequest}")
    private String lintRequestStream;

    @Value("${redis.streams.lintResult}")
    private String lintResultStream;

    @Value("${redis.streams.formatRequest}")
    private String formatRequestStream;

    @Value("${redis.streams.formatResult}")
    private String formatResultStream;

    @Value("${redis.streams.testRequest}")
    private String testRequestStream;

    @Value("${redis.streams.testResult}")
    private String testResultStream;

    @Value("${redis.groups.lint}")
    private String lintGroup;

    @Value("${redis.groups.format}")
    private String formatGroup;

    @Value("${redis.groups.test}")
    private String testGroup;

    public RedisInitializer(StringRedisTemplate template) {
        this.template = template;
    }

    @PostConstruct
    public void setup() {
        createStreamAndGroup(lintRequestStream, lintGroup);
        createStreamAndGroup(lintResultStream, lintGroup);

        createStreamAndGroup(formatRequestStream, formatGroup);
        createStreamAndGroup(formatResultStream, formatGroup);

        createStreamAndGroup(testRequestStream, testGroup);
        createStreamAndGroup(testResultStream, testGroup);
    }

    private void createStreamAndGroup(String stream, String group) {
        try {
            if (!template.hasKey(stream)) {
                template.opsForStream().add(stream, Map.of("init", "1"));
            }
            template.opsForStream().createGroup(stream, group);
        } catch (Exception ignored) {
        }
    }
}
