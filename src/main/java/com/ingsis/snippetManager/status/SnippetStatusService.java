package com.ingsis.snippetManager.status;

import java.util.UUID;

public interface SnippetStatusService {
    void markFormatPending(UUID snippetId);
    void markLintPending(UUID snippetId);
    void markTestPending(UUID snippetId);
    void markFormatted(UUID snippetId);
    void markLinted(UUID snippetId);
    void markTested(UUID snippetId);
    void markFormatFailed(UUID snippetId, String reason);
    void markLintFailed(UUID snippetId, String reason);
    void markTestFailed(UUID snippetId, String reason);
}
