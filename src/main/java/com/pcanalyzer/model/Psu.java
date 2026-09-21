package com.pcanalyzer.model;

import java.util.Objects;

/**
 * Represents a Power Supply Unit (PSU).
 *
 * Design Decision:
 * Holds the rated maximum wattage and efficiency rating.
 * The compatibility engine uses wattage to check against the combined
 * CPU TDP + GPU Board Power + system overhead.
 */
public class Psu extends Component {

    private int wattage;
    private String efficiencyRating;

    public Psu(Integer id, String brand, String name, double price, int tdpWatts,
               int wattage, String efficiencyRating) {
        super(id, ComponentType.PSU, brand, name, price, tdpWatts);
        this.wattage = Math.max(100, wattage);
        this.efficiencyRating = Objects.requireNonNullElse(efficiencyRating, "Standard");
    }

    public int getWattage() {
        return wattage;
    }

    public void setWattage(int wattage) {
        this.wattage = Math.max(100, wattage);
    }

    public String getEfficiencyRating() {
        return efficiencyRating;
    }

    public void setEfficiencyRating(String efficiencyRating) {
        this.efficiencyRating = Objects.requireNonNullElse(efficiencyRating, "Standard");
    }

    @Override
    public String getKeySpecs() {
        return String.format("%dW (%s)", wattage, efficiencyRating);
    }
}
