package com.safariking.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SafariCritterStatsMod {
    public static final String MOD_ID = "safariking";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final SafariSessionStats STATS = new SafariSessionStats();
    public static final SafariCritterConfig CONFIG = SafariCritterConfig.load();

    private SafariCritterStatsMod() {}
}
