package com.pcanalyzer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Summarizes whether parts work together and lists any issues found
public class CompatibilityResult {

    // Overall status and list of specific warnings or incompatibilities
    private final CompatibilityStatus status;
    private final List<CompatibilityIssue> issues;

    // Build the result object with a status and list of detected issues
    public CompatibilityResult(CompatibilityStatus status, List<CompatibilityIssue> issues) {
        this.status = status;
        this.issues = issues != null ? Collections.unmodifiableList(new ArrayList<>(issues)) : Collections.emptyList();
    }

    // Quick helper for when everything is 100% compatible
    public static CompatibilityResult compatible() {
        return new CompatibilityResult(CompatibilityStatus.COMPATIBLE, Collections.emptyList());
    }

    // Getters for status and issue list
    public CompatibilityStatus getStatus() {
        return status;
    }

    public List<CompatibilityIssue> getIssues() {
        return issues;
    }

    // Quick helper checks to see if the build passed, has warnings, or failed
    public boolean isCompatible() {
        return status == CompatibilityStatus.COMPATIBLE;
    }

    public boolean hasWarnings() {
        return status == CompatibilityStatus.WARNING;
    }

    public boolean isIncompatible() {
        return status == CompatibilityStatus.INCOMPATIBLE;
    }

    // Convert the entire result and its issues into a readable multi-line message
    @Override
    public String toString() {
        if (issues.isEmpty()) {
            return status.getLabel() + ": " + status.getDescription();
        }
        StringBuilder sb = new StringBuilder(status.getLabel()).append(":\n");
        for (CompatibilityIssue issue : issues) {
            sb.append(" - ").append(issue.getMessage()).append("\n");
        }
        return sb.toString().trim();
    }
}
