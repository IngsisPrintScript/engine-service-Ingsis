package com.ingsis.snippetManager.redis.dto.format;

import java.util.UUID;

public record FormatRequestEvent(String ownerId, UUID snippetId, String language) {
}
