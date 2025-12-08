package com.ingsis.snippetManager.engine.dto.response;

import com.ingsis.snippetManager.redis.dto.testing.SnippetTestStatus;

public record ValidationResult(String message, boolean valid){}
