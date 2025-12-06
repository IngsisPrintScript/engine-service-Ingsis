package com.ingsis.snippetManager.redis.dto.testing;

import java.util.UUID;

public record TestRequestEvent(UUID snippetId, String language) {
}
