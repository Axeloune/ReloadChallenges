package fr.victin.reloadchallenges.game;

public enum ChallengeMode {
    FFA("FFA"),
    TEAMS("Équipes");

    private final String displayName;

    ChallengeMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static ChallengeMode parse(String raw) {
        if (raw == null) {
            return FFA;
        }
        return switch (raw.toLowerCase()) {
            case "team", "teams", "equipes", "equipe", "équipes", "équipe" -> TEAMS;
            default -> FFA;
        };
    }
}
