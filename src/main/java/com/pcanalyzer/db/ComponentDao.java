package com.pcanalyzer.db;

import com.pcanalyzer.model.Component;
import com.pcanalyzer.model.ComponentType;

import java.util.List;
import java.util.Optional;

// Rules for saving, reading, and deleting hardware parts from the database
public interface ComponentDao {

    // Get every part stored in the catalog
    List<Component> findAll();

    // Get parts belonging to a single category like CPU or GPU
    List<Component> findByType(ComponentType type);

    // Look up one specific part by its database ID number
    Optional<Component> findById(int id);

    // Save a new part or update an existing one
    Component save(Component component);

    // Delete a part from the database using its ID
    boolean deleteById(int id);
}
