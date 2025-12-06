package com.ingsis.snippetManager.redis.dto.lint;

import java.util.UUID;

public record LintResultEvent(String userId, UUID snippetId, LintStatus status) {
}
