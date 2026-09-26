package com.pcanalyzer.model;

import java.util.Objects;

// Power supply (PSU) details like maximum wattage and efficiency badge
public class Psu extends Component {

    // Total power output in watts and efficiency rating (like 80 Plus Gold)
    private int wattage;
    private String efficiencyRating;

    // Create a new power supply entry
    public Psu(Integer id, String brand, String name, double price, int tdpWatts,
               int wattage, String efficiencyRating) {
        super(id, ComponentType.PSU, brand, name, price, tdpWatts);
        this.wattage = Math.max(100, wattage);
        this.efficiencyRating = Objects.requireNonNullElse(efficiencyRating, "Standard");
    }

    // Getters and setters for power supply specs
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

    // Show the wattage and efficiency certification
    @Override
    public String getKeySpecs() {
        return String.format("%dW (%s)", wattage, efficiencyRating);
    }
}
