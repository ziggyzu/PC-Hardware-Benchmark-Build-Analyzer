package com.pcanalyzer.model;

import java.util.Objects;

// Stores a single compatibility problem or warning found in a build
public class CompatibilityIssue {

    // Details about the check that was triggered and how severe it is
    private final String ruleName;
    private final String message;
    private final CompatibilityStatus severity;

    // Create a new issue report
    public CompatibilityIssue(String ruleName, String message, CompatibilityStatus severity) {
        this.ruleName = Objects.requireNonNull(ruleName, "Rule name cannot be null");
        this.message = Objects.requireNonNull(message, "Message cannot be null");
        this.severity = Objects.requireNonNull(severity, "Severity cannot be null");
    }

    // Getters for the issue details
    public String getRuleName() {
        return ruleName;
    }

    public String getMessage() {
        return message;
    }

    public CompatibilityStatus getSeverity() {
        return severity;
    }

    // Format the issue into a readable text line
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", severity, ruleName, message);
    }
}
