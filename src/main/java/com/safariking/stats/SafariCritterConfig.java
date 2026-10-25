package com.safariking.stats;

import com.safariking.SafariKingMod;

/** All settings are saved together in safari-king.json. */
public final class SafariCritterConfig {
    public static final float MIN_HUD_SCALE = 0.5F;
    public static final float MAX_HUD_SCALE = 2.5F;
    private SafariCritterConfig() {}
    public static SafariCritterConfig load() { return new SafariCritterConfig(); }
    public boolean isEnabled() { return SafariKingMod.CONFIG.enabled && SafariKingMod.CONFIG.statsEnabled; }
    public boolean respondsToPartyCommands() { return SafariKingMod.CONFIG.respondToPartyCommands; }
    public boolean showsHud() { return SafariKingMod.CONFIG.showStatsHud; }
    public int hudX() { return Math.max(0, SafariKingMod.CONFIG.hudX); }
    public int hudY() { return Math.max(0, SafariKingMod.CONFIG.hudY); }
    public float hudScale() { return clampScale(SafariKingMod.CONFIG.hudScale); }
    public void setEnabled(boolean enabled) {
        SafariKingMod.CONFIG.statsEnabled = enabled;
        SafariKingMod.CONFIG.save();
        SafariLocationTracker.onMasterEnabledChanged(isEnabled());
    }
    public void setRespondToPartyCommands(boolean enabled) {
        SafariKingMod.CONFIG.respondToPartyCommands = enabled;
        PartyMessageDispatcher.reset();
        SafariKingMod.CONFIG.save();
    }
    public void setShowHud(boolean enabled) {
        SafariKingMod.CONFIG.showStatsHud = enabled;
        SafariKingMod.CONFIG.save();
    }
    public void setHudLayout(int x, int y, float scale) {
        SafariKingMod.CONFIG.hudX = Math.max(0, x);
        SafariKingMod.CONFIG.hudY = Math.max(0, y);
        SafariKingMod.CONFIG.hudScale = clampScale(scale);
        SafariKingMod.CONFIG.save();
    }
    public static float clampScale(float scale) {
        if (!Float.isFinite(scale)) return 1.0F;
        return Math.max(MIN_HUD_SCALE, Math.min(MAX_HUD_SCALE, scale));
    }
}
