package com.pcanalyzer.db;

import com.pcanalyzer.model.Component;
import com.pcanalyzer.model.ComponentType;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) interface for PC hardware components.
 *
 * Design Decision:
 * 1. Interface Segregation: Defining a clean DAO contract decouples higher layers
 *    (Services and UI Controllers) from the database implementation.
 * 2. Polymorphic Querying: The DAO handles polymorphic reconstitution so callers
 *    receive fully hydrated subclasses (Cpu, Gpu, Motherboard, Ram, Psu) seamlessly.
 */
public interface ComponentDao {

    /**
     * Retrieve all hardware components across all categories.
     *
     * @return List of all components
     */
    List<Component> findAll();

    /**
     * Retrieve all components belonging to a specific category.
     *
     * @param type Category enum (e.g. CPU, GPU)
     * @return Filtered list of components
     */
    List<Component> findByType(ComponentType type);

    /**
     * Retrieve a single component by its database ID.
     *
     * @param id Component ID
     * @return Optional containing the component if found, empty otherwise
     */
    Optional<Component> findById(int id);

    /**
     * Inserts a new component or updates an existing one (based on presence of id).
     *
     * @param component Component to persist
     * @return Persisted component with assigned database ID
     */
    Component save(Component component);

    /**
     * Deletes a component by its ID.
     * Child spec rows and price history are automatically cleaned up via CASCADE.
     *
     * @param id Database ID of the component to delete
     * @return true if a component was deleted, false otherwise
     */
    boolean deleteById(int id);
}
