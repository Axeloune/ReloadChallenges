package fr.victin.reloadchallenges.game;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Optional;

public enum ChallengeType {
    RANDOM("random", "Aléatoire", Material.NETHER_STAR, "Jeu surprise"),
    FIND("find", "Trouve l'objet", Material.COMPASS, "Objet à trouver"),
    WHERE_BLOCK("where_block", "Où est ce bloc ?", Material.GRASS_BLOCK, "Bloc cible"),
    CRAFT("craft", "Défi de craft", Material.CRAFTING_TABLE, "Craft cible"),
    SPEEDRUN("speedrun", "Speedrun Dragon", Material.DRAGON_HEAD, "Objectif"),
    WHERE_BIOME("where_biome", "Où est ce biome ?", Material.FILLED_MAP, "Biome cible"),
    MOB_HUNT("mob_hunt", "Chasse au mob", Material.CROSSBOW, "Mob cible"),
    BINGO("bingo", "Bingo", Material.FILLED_MAP, "Grille");

    private final String id;
    private final String displayName;
    private final Material icon;
    private final String objectiveLabel;

    ChallengeType(String id, String displayName, Material icon, String objectiveLabel) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.objectiveLabel = objectiveLabel;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public String objectiveLabel() {
        return objectiveLabel;
    }

    public static Optional<ChallengeType> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.toLowerCase().replace("-", "_");
        return Arrays.stream(values())
            .filter(type -> type.id.equals(normalized) || type.name().equalsIgnoreCase(normalized))
            .findFirst();
    }
}
