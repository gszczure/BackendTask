package org.codibly.model;

public enum EnergySource {
    BIOMASS("biomass"),
    NUCLEAR("nuclear"),
    HYDRO("hydro"),
    WIND("wind"),
    SOLAR("solar");

    private final String fuelName;

    EnergySource(String fuelName) {
        this.fuelName = fuelName;
    }

    public String getFuelName() {
        return fuelName;
    }
}
