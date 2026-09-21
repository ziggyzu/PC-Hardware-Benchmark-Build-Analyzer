package com.pcanalyzer.model;

/**
 * Enumeration of supported PC hardware component categories.
 * 
 * Design Decision:
 * An enum provides type safety across the application, preventing invalid
 * category strings in the database, UI filters, and compatibility checks.
 */
public enum ComponentType {
    CPU("CPU (Processor)"),
    GPU("GPU (Graphics Card)"),
    MOTHERBOARD("Motherboard"),
    RAM("RAM (Memory)"),
    PSU("Power Supply (PSU)");

    private final String displayName;

    ComponentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Safely parse a string into a ComponentType, case-insensitively.
     *
     * @param value String representation of the type
     * @return Matching ComponentType or null if not found
     */
    public static ComponentType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (ComponentType type : values()) {
            if (type.name().equalsIgnoreCase(value) || type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
