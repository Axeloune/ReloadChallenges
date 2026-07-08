package fr.victin.reloadchallenges.util;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.Locale;

public final class Formatters {
    private Formatters() {
    }

    public static String material(Material material) {
        return title(material.name());
    }

    public static String biome(Biome biome) {
        return title(biome.name());
    }

    public static String title(String raw) {
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    public static String duration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return "%02d:%02d".formatted(minutes, seconds);
    }
}
