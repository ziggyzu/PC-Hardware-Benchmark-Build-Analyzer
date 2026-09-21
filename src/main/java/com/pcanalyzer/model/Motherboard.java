package com.pcanalyzer.model;

import java.util.Objects;

/**
 * Represents a Motherboard (Mainboard).
 *
 * Design Decision:
 * Holds the CPU socket standard and supported RAM generation (DDR4 / DDR5)
 * which are the two critical compatibility criteria for build verification.
 */
public class Motherboard extends Component {

    private String socket;
    private String ramType;

    public Motherboard(Integer id, String brand, String name, double price, int tdpWatts,
                       String socket, String ramType) {
        super(id, ComponentType.MOTHERBOARD, brand, name, price, tdpWatts);
        this.socket = Objects.requireNonNull(socket, "Socket must not be null");
        this.ramType = Objects.requireNonNull(ramType, "RAM type must not be null");
    }

    public String getSocket() {
        return socket;
    }

    public void setSocket(String socket) {
        this.socket = socket;
    }

    public String getRamType() {
        return ramType;
    }

    public void setRamType(String ramType) {
        this.ramType = ramType;
    }

    @Override
    public String getKeySpecs() {
        return String.format("Socket: %s | RAM: %s", socket, ramType);
    }
}
