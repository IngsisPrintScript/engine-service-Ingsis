package com.ingsis.snippetManager.status;

import com.ingsis.snippetManager.redis.dto.format.FormatStatus;
import com.ingsis.snippetManager.redis.dto.lint.LintStatus;
import com.ingsis.snippetManager.redis.dto.testing.SnippetTestStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SnippetStatusServiceImpl implements SnippetStatusService {

    private final RedisSnippetStatusRepository repo;

    public SnippetStatusServiceImpl(RedisSnippetStatusRepository repo) {
        this.repo = repo;
    }

    public void markFormatPending(UUID id) {
        repo.save(id, FormatStatus.PENDING);
    }
    public void markLintPending(UUID id) {
        repo.save(id, LintStatus.PENDING);
    }

    @Override
    public void markTestPending(UUID snippetId) {
        repo.save(snippetId, SnippetTestStatus.PENDING);
    }

    public void markFormatted(UUID id) {
        repo.save(id, FormatStatus.PASSED);
    }
    public void markLinted(UUID id) {
        repo.save(id, FormatStatus.PASSED);
    }

    @Override
    public void markTested(UUID snippetId) {
        repo.save(snippetId, SnippetTestStatus.PASSED);
    }

    public void markFormatFailed(UUID id, String reason) {
        repo.save(id, FormatStatus.FAILED);
    }
    public void markLintFailed(UUID id, String reason) {
        repo.save(id, FormatStatus.FAILED);
    }

    @Override
    public void markTestFailed(UUID snippetId, String reason) {
        repo.save(snippetId, FormatStatus.FAILED);
    }

}