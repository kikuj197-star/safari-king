package com.safariking;

import java.util.Locale;

public enum SafariZone {
    CAVERN,
    FOREST,
    HAUNTED,
    ICY,
    CENTER,
    UNKNOWN;

    static SafariZone fromBiomeId(String biomeId) {
        if (biomeId == null) return UNKNOWN;
        return switch (biomeId.toLowerCase(Locale.ROOT)) {
            case "hypixel:cavern" -> CAVERN;
            case "hypixel:forest" -> FOREST;
            case "hypixel:haunted" -> HAUNTED;
            case "hypixel:icy", "hypixel:icy_caves" -> ICY;
            default -> UNKNOWN;
        };
    }
}
