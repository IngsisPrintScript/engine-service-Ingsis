package com.ingsis.snippetManager.engine.dto;

import java.util.List;
import java.util.UUID;

public record RunSnippetRequestDTO(UUID snippetId, String language, List<String> inputs, String version,String content){}
