package com.pcanalyzer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the overall result of a build compatibility check.
 *
 * Design Decision:
 * Instead of returning a primitive boolean (true/false), returning a structured
 * result object allows the UI and service layers to display rich, itemized explanations
 * of each conflict, warning, or pass condition.
 */
public class CompatibilityResult {

    private final CompatibilityStatus status;
    private final List<CompatibilityIssue> issues;

    public CompatibilityResult(CompatibilityStatus status, List<CompatibilityIssue> issues) {
        this.status = status;
        this.issues = issues != null ? Collections.unmodifiableList(new ArrayList<>(issues)) : Collections.emptyList();
    }

    public static CompatibilityResult compatible() {
        return new CompatibilityResult(CompatibilityStatus.COMPATIBLE, Collections.emptyList());
    }

    public CompatibilityStatus getStatus() {
        return status;
    }

    public List<CompatibilityIssue> getIssues() {
        return issues;
    }

    public boolean isCompatible() {
        return status == CompatibilityStatus.COMPATIBLE;
    }

    public boolean hasWarnings() {
        return status == CompatibilityStatus.WARNING;
    }

    public boolean isIncompatible() {
        return status == CompatibilityStatus.INCOMPATIBLE;
    }

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
