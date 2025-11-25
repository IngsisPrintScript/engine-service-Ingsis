package com.ingsis.snippetManager.engine;

import com.ingsis.snippetManager.engine.dto.RunSnippetRequestDTO;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.logging.Logger;

@Service
public class PrintScriptRunnerService {

    private Logger logger = Logger.getLogger(PrintScriptRunnerService.class.getName());

    public RunSnippetRequestDTO runSnippet(UUID userId, RunSnippetRequestDTO snippetDTO){
        logger.info("Running snippet for userId: {} " + userId);

    }
}
