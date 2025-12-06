package com.ingsis.snippetManager.redis.dto.format;

import java.util.UUID;

public record FormatResultEvent(String userId, UUID snippetId, FormatStatus status) {
}
