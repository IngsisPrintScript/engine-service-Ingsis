package com.ingsis.snippetManager.redis.testing.dto;

import java.util.UUID;

public record TestRequestEvent(UUID snippetId, String language) {
}
