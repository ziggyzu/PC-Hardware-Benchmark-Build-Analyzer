package com.pcanalyzer.model;

import java.util.Objects;

// Base class that all computer parts inherit from
public abstract class Component {

    // Common fields shared by every PC component
    private Integer id;
    private final ComponentType type;
    private String brand;
    private String name;
    private double price;
    private int tdpWatts;

    // Set up the basic info for any component
    protected Component(Integer id, ComponentType type, String brand, String name, double price, int tdpWatts) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "ComponentType must not be null");
        this.brand = Objects.requireNonNull(brand, "Brand must not be null");
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.price = Math.max(0.0, price);
        this.tdpWatts = Math.max(0, tdpWatts);
    }

    // Getters and setters for the basic component details
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ComponentType getType() {
        return type;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = Math.max(0.0, price);
    }

    public int getTdpWatts() {
        return tdpWatts;
    }

    public void setTdpWatts(int tdpWatts) {
        this.tdpWatts = Math.max(0, tdpWatts);
    }

    // Each specific part decides how to summarize its own specs
    public abstract String getKeySpecs();

    // Check if two components are the same item
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Component component = (Component) o;
        return Objects.equals(id, component.id) &&
               type == component.type &&
               Objects.equals(brand, component.brand) &&
               Objects.equals(name, component.name);
    }

    // Generate a hash code based on component identity
    @Override
    public int hashCode() {
        return Objects.hash(id, type, brand, name);
    }

    // Display a clean text label showing part name and price
    @Override
    public String toString() {
        return String.format("[%s] %s %s ($%.2f)", type, brand, name, price);
    }
}
