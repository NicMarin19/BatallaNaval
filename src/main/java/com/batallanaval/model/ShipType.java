package com.batallanaval.model;

import com.jme3.math.ColorRGBA;

public enum ShipType {
    PORTAVIONES("Portaviones", 5, new ColorRGBA(0.25f, 0.35f, 0.45f, 1f)),
    ACORAZADO("Acorazado", 4, new ColorRGBA(0.35f, 0.40f, 0.45f, 1f)),
    CRUCERO("Crucero", 3, new ColorRGBA(0.40f, 0.45f, 0.40f, 1f)),
    SUBMARINO("Submarino", 3, new ColorRGBA(0.20f, 0.30f, 0.35f, 1f)),
    DESTRUCTOR("Destructor", 2, new ColorRGBA(0.45f, 0.35f, 0.30f, 1f));

    private final String displayName;
    private final int size;
    private final ColorRGBA themeColor;

    ShipType(String displayName, int size, ColorRGBA themeColor) {
        this.displayName = displayName;
        this.size = size;
        this.themeColor = themeColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSize() {
        return size;
    }

    public ColorRGBA getThemeColor() {
        return themeColor;
    }
}
