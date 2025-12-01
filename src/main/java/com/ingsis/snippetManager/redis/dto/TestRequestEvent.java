package com.ingsis.snippetManager.redis.dto;

import java.util.UUID;

public record TestRequestEvent(UUID snippetId, String language) {
}
