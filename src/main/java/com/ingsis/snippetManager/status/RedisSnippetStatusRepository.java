package com.ingsis.snippetManager.status;

import com.ingsis.snippetManager.redis.dto.format.FormatStatus;
import com.ingsis.snippetManager.redis.dto.lint.LintStatus;
import com.ingsis.snippetManager.redis.dto.testing.SnippetTestStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class RedisSnippetStatusRepository {

    private static final String PREFIX = "snippet:format:status:";

    private final StringRedisTemplate redis;

    public RedisSnippetStatusRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void save(UUID snippetId, FormatStatus status) {
        redis.opsForValue().set(PREFIX + snippetId, status.name());
    }
    public void save(UUID snippetId, LintStatus status) {
        redis.opsForValue().set(PREFIX + snippetId, status.name());
    }
    public void save(UUID snippetId, SnippetTestStatus status) {
        redis.opsForValue().set(PREFIX + snippetId, status.name());
    }

    public Optional<FormatStatus> find(UUID snippetId) {
        String value = redis.opsForValue().get(PREFIX + snippetId);
        return value == null ? Optional.empty() : Optional.of(FormatStatus.valueOf(value));
    }

    public Map<UUID, FormatStatus> findAll() {
        Set<String> keys = redis.keys(PREFIX + "*");
        Map<UUID, FormatStatus> result = new HashMap<>();

        for (String key : keys) {
            UUID id = UUID.fromString(key.replace(PREFIX, ""));
            result.put(id, FormatStatus.valueOf(redis.opsForValue().get(key)));
        }
        return result;
    }
}