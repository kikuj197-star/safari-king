package com.safariking.stats;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SafariPartyProgressTest {
    public static void main(String[] args) {
        SafariPartyProgress party = new SafariPartyProgress();
        SafariSessionStats stats = new SafariSessionStats();
        UUID self = new UUID(0, 1);
        UUID alice = new UUID(0, 2);
        UUID bob = new UUID(0, 3);
        party.replaceMembers(Set.of(self, alice, bob), self);
        party.identify(alice, "Alice");
        party.identify(bob, "Bob");
        expect(party.rows(stats, 1000).size() == 2, "self excluded");
        expect(party.rows(stats, 1000).getFirst().text().equals("Alice: Unknown ?/?"), "unknown is not zero progress");

        party.observePosition(alice, SafariZone.HAUNTED, 1000);
        stats.record(new CaptureEvent("Areita", 2), 1000);
        expect(party.rows(stats, 1000).getFirst().text().equals("Alice: Haunted 1/10"), "shared counts");
        stats.record(new CaptureEvent("Bloodbat", 2), 1100);
        stats.record(new CaptureEvent("Bloodbat", 2), 1200);
        expect(party.rows(stats, 1200).getFirst().text().equals("Alice: Haunted 2/10"), "dynamic unique counts, no duplicate species");

        party.observePosition(alice, SafariZone.ICY, 1300);
        expect(party.rows(stats, 1300).getFirst().text().equals("Alice: Icy 0/9"), "moving selects new biome");
        party.observeCapture("ALICE", "Areita", 1400);
        expect(party.rows(stats, 1400).getFirst().zone() == SafariZone.ICY, "loaded position beats capture inference");
        party.observeCapture("Alice", "Rockmite", 4000);
        expect(party.rows(stats, 4000).getFirst().text().equals("Alice: Cavern* 0/9"), "distant capture infers recent biome");
        expect(party.rows(stats, 34001).getFirst().text().equals("Alice: Unknown ?/?"), "old location expires");

        party.observePosition(bob, SafariZone.CENTER, 40000);
        expect(party.rows(stats, 40000).get(1).text().equals("Bob: Center ?/?"), "spawn does not claim biome completion");
        party.replaceMembers(Set.of(self, bob), self);
        expect(party.rows(stats, 40000).size() == 1 && party.rows(stats, 40000).getFirst().player().equals("Bob"), "leaving member removed");
        party.reset();
        expect(party.rows(stats, 40000).isEmpty(), "new visit clears teammates");

        party.observeCapture("Alice", "Hideyho", 50000);
        party.replaceMembers(Set.of(self, alice), self);
        party.identify(alice, "Alice");
        expect(party.rows(stats, 50000).getFirst().text().equals("Alice: Haunted* 2/10"), "capture before roster survives name resolution");
        party.reset();
        party.replaceMembers(Set.of(alice), self);
        party.identify(alice, "Alice");
        expect(party.rows(stats, 50000).getFirst().zone() == SafariZone.UNKNOWN, "new visit does not reuse capture locations");
        party.replaceMembers(Set.of(self, alice, bob, new UUID(0, 4), new UUID(0, 5)), self);
        expect(party.rows(stats, 50000).size() == 3, "three teammate limit");

        expect(ChatParser.lootSharePlayer("LOOT SHARE! You received 2x Polaris Shard from CherryWoodPlank catching a Polaris!")
            .orElseThrow().equals("CherryWoodPlank"), "loot-share source");
        expect(ChatParser.lootSharePlayer("LOOT SHARE! You received 4x Hideyho Shard from [MVP+] Alice finding the Hideyho!")
            .orElseThrow().equals("Alice"), "ranked finder");
        expect(ChatParser.lootSharePlayer("LOOT SHARE! You received a Rainbow Feather and 20x Macaw Shard from Alice catching a SPARKLING Macaw!")
            .orElseThrow().equals("Alice"), "sparkling source");
        for (String message : List.of(
            "Party > Alice: LOOT SHARE! You received 2x Polaris Shard from Bob catching a Polaris!",
            "LOOT SHARE! You received 2x Polaris Shard from Alice catching a Foxtrot!",
            "CAPTURE! You caught a Bluebird and gained 2x Bluebird Shard!")) {
            expect(ChatParser.lootSharePlayer(message).isEmpty(), "reject spoofed, invalid and own captures");
        }
        System.out.println("Party HUD regression passed: shared counts, moving, expiry, roster changes and reset.");
    }

    private static void expect(boolean condition, String explanation) {
        if (!condition) throw new AssertionError(explanation);
    }
}
