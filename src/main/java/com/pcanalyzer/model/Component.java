package com.pcanalyzer.model;

import java.util.Objects;

/**
 * Abstract base class for all PC hardware components.
 *
 * Design Decision:
 * 1. Inheritance & Polymorphism: All components share common business attributes
 *    (ID, type, brand, name, price, and TDP wattage). By abstracting these into
 *    a base class, collections of components can be manipulated uniformly (e.g.
 *    in UI tables, build inventories, and price trackers).
 * 2. Pure Domain Model: Notice there are NO JavaFX dependencies (such as SimpleStringProperty).
 *    Keeping models as plain Java objects (POJOs) makes them lightweight, serializable,
 *    and easily unit-testable without initializing the JavaFX runtime.
 */
public abstract class Component {

    private Integer id;
    private final ComponentType type;
    private String brand;
    private String name;
    private double price;
    private int tdpWatts;

    /**
     * Constructs a new Component.
     *
     * @param id Database identifier (null if not yet persisted)
     * @param type The category of component
     * @param brand Manufacturer brand (e.g., AMD, Intel, NVIDIA)
     * @param name Model name (e.g., Ryzen 5 5600, RTX 4060)
     * @param price Current retail price in USD
     * @param tdpWatts Thermal Design Power in Watts
     */
    protected Component(Integer id, ComponentType type, String brand, String name, double price, int tdpWatts) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "ComponentType must not be null");
        this.brand = Objects.requireNonNull(brand, "Brand must not be null");
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.price = Math.max(0.0, price);
        this.tdpWatts = Math.max(0, tdpWatts);
    }

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

    /**
     * Polymorphic method to return a concise human-readable summary of component-specific specs.
     * Subclasses override this to supply details such as socket/cores for CPUs or VRAM for GPUs.
     *
     * @return Summary string suitable for table cells or tooltips.
     */
    public abstract String getKeySpecs();

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

    @Override
    public int hashCode() {
        return Objects.hash(id, type, brand, name);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s ($%.2f)", type, brand, name, price);
    }
}
