package com.ingsis.snippetManager.engine;

import com.ingsis.engine.versions.Version;
import com.ingsis.snippetManager.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.utils.result.Result;

import java.io.InputStream;

public interface EngineAdapterInterface {

    Result<String> format(InputStream src, InputStream rules, Version version);
    Result<String> analyze(InputStream src, InputStream config, Version version);
    RunSnippetResponseDTO execute(InputStream src, Version version);
}
