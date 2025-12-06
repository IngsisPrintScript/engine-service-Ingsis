package com.ingsis.snippetManager.engine.supportedRules;

import org.springframework.web.bind.annotation.RequestAttribute;

public record FormatterSupportedRules(@RequestAttribute(required = false) boolean hasPostAscriptionSpace,
        @RequestAttribute(required = false) boolean hasPreAscriptionSpace,
        @RequestAttribute(required = false) boolean isAssignationSpaced,
        @RequestAttribute(required = false) int printlnSeparationLines) {
}
