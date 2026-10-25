package com.safariking.stats;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Per-visit party locations; every row uses the same local Safari collection. */
public final class SafariPartyProgress {
    private static final long LIVE_MILLIS = 1_500;
    private static final long RECENT_MILLIS = 30_000;
    private final Map<UUID, Member> members = new LinkedHashMap<>();
    private final Map<String, Observation> captureLocations = new HashMap<>();

    private static final class Member {
        String name;
        SafariZone zone = SafariZone.UNKNOWN;
        long observedAt = Long.MIN_VALUE;
        long positionAt = Long.MIN_VALUE;
    }

    private record Observation(SafariZone zone, long at) {}

    public synchronized void replaceMembers(Set<UUID> roster, UUID self) {
        members.keySet().removeIf(id -> id.equals(self) || !roster.contains(id));
        roster.stream().filter(id -> !id.equals(self)).sorted()
            .forEach(id -> members.computeIfAbsent(id, ignored -> new Member()));
    }

    public synchronized Set<UUID> memberIds() {
        return Set.copyOf(members.keySet());
    }

    public synchronized void identify(UUID id, String name) {
        Member member = members.get(id);
        if (member == null || name == null || !name.matches("[A-Za-z0-9_]{1,16}")) return;
        member.name = name;
        Observation capture = captureLocations.get(name.toLowerCase(Locale.ROOT));
        if (capture != null && capture.at() > member.observedAt) {
            member.zone = capture.zone();
            member.observedAt = capture.at();
        }
    }

    public synchronized void observePosition(UUID id, SafariZone zone, long now) {
        Member member = members.get(id);
        if (member == null) return;
        member.zone = zone;
        member.observedAt = now;
        member.positionAt = now;
    }

    public synchronized void observeCapture(String name, String critter, long now) {
        CritterRegion region = CritterRegion.forCritter(critter).orElse(null);
        if (region == null) return;
        SafariZone zone = switch (region) {
            case CAVERN -> SafariZone.CAVERN;
            case FOREST -> SafariZone.FOREST;
            case HAUNTED -> SafariZone.HAUNTED;
            case ICY -> SafariZone.ICY;
        };
        captureLocations.entrySet().removeIf(entry -> !recent(now, entry.getValue().at(), RECENT_MILLIS));
        if (captureLocations.size() < 100) {
            captureLocations.put(name.toLowerCase(Locale.ROOT), new Observation(zone, now));
        }
        for (Member member : members.values()) {
            if (member.name != null && member.name.equalsIgnoreCase(name)
                    && !recent(now, member.positionAt, LIVE_MILLIS)) {
                member.zone = zone;
                member.observedAt = now;
            }
        }
    }

    public synchronized List<Row> rows(SafariSessionStats stats, long now) {
        return members.entrySet().stream()
            .sorted(Comparator.comparing(entry -> displayName(entry.getKey(), entry.getValue()), String.CASE_INSENSITIVE_ORDER))
            .limit(3)
            .map(entry -> {
                Member member = entry.getValue();
                SafariZone zone = recent(now, member.observedAt, RECENT_MILLIS) ? member.zone : SafariZone.UNKNOWN;
                boolean lastSeen = zone != SafariZone.UNKNOWN
                    && (!recent(now, member.positionAt, LIVE_MILLIS) || member.positionAt != member.observedAt);
                CritterRegion region = zone.region();
                int total = region == null ? 0 : region.critters().size();
                int caught = region == null ? 0 : total - stats.missingCritters(region).size();
                return new Row(displayName(entry.getKey(), member), zone, caught, total, lastSeen);
            }).toList();
    }

    public synchronized void reset() {
        members.clear();
        captureLocations.clear();
    }

    private static String displayName(UUID id, Member member) {
        return member.name == null ? "Teammate " + id.toString().substring(0, 4) : member.name;
    }

    private static boolean recent(long now, long then, long window) {
        return then != Long.MIN_VALUE && now >= then && now - then <= window;
    }

    public record Row(String player, SafariZone zone, int caught, int total, boolean lastSeen) {
        public String text() {
            String progress = total == 0 ? "?/?" : caught + "/" + total;
            return player + ": " + zone.displayName() + (lastSeen ? "*" : "") + " " + progress;
        }
    }
}
