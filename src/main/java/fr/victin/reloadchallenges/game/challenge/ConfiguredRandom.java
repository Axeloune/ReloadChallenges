package fr.victin.reloadchallenges.game.challenge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.CraftingRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public final class ConfiguredRandom {
    private ConfiguredRandom() {
    }

    public static Material anySurvivalItem() {
        return materialPool(material -> material.isItem() && !material.isAir() && !isTechnicalMaterial(material), List.of(
            Material.OAK_LOG,
            Material.COBBLESTONE,
            Material.IRON_INGOT,
            Material.BREAD
        ));
    }

    public static Material anyWorldBlock() {
        return materialPool(material -> material.isBlock() && !material.isAir() && !isTechnicalMaterial(material), List.of(
            Material.STONE,
            Material.DIRT,
            Material.OAK_LOG,
            Material.SAND
        ));
    }

    public static Material anyCraftResult() {
        Set<Material> results = new LinkedHashSet<>();
        Iterator<Recipe> recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (!(recipe instanceof CraftingRecipe || recipe instanceof CookingRecipe<?>)) {
                continue;
            }
            ItemStack result = recipe.getResult();
            Material material = result.getType();
            if (material.isItem() && !material.isAir() && !isTechnicalMaterial(material) && !isNaturalConversionMaterial(material)) {
                results.add(material);
            }
        }
        List<Material> materials = new ArrayList<>(results);
        if (materials.isEmpty()) {
            materials.addAll(List.of(Material.CRAFTING_TABLE, Material.FURNACE, Material.CHEST, Material.STICK, Material.TORCH));
        }
        return random(materials);
    }

    public static Biome anyBiome() {
        List<Biome> biomes = Arrays.stream(Biome.values())
            .filter(biome -> !"CUSTOM".equals(biome.name()))
            .toList();
        if (biomes.isEmpty()) {
            biomes = List.of(Biome.PLAINS, Biome.FOREST, Biome.DESERT, Biome.TAIGA, Biome.SAVANNA);
        }
        return random(biomes);
    }

    public static EntityType anyMobTarget() {
        List<EntityType> entityTypes = Arrays.stream(EntityType.values())
            .filter(entityType -> entityType.isAlive() && entityType.isSpawnable())
            .filter(entityType -> entityType != EntityType.PLAYER)
            .filter(entityType -> entityType != EntityType.ENDER_DRAGON)
            .toList();
        if (entityTypes.isEmpty()) {
            entityTypes = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER);
        }
        return random(entityTypes);
    }

    private static Material materialPool(Predicate<Material> filter, List<Material> fallback) {
        List<Material> materials = Arrays.stream(Material.values())
            .filter(filter)
            .toList();
        if (materials.isEmpty()) {
            materials = fallback;
        }
        return random(materials);
    }

    private static boolean isTechnicalMaterial(Material material) {
        String name = material.name();
        return name.startsWith("LEGACY_")
            || name.endsWith("_SPAWN_EGG")
            || name.contains("COMMAND_BLOCK")
            || name.contains("STRUCTURE_BLOCK")
            || name.contains("STRUCTURE_VOID")
            || name.contains("JIGSAW")
            || name.equals("DEBUG_STICK")
            || name.equals("BARRIER")
            || name.equals("LIGHT")
            || name.equals("KNOWLEDGE_BOOK")
            || name.equals("BEDROCK")
            || name.equals("TRIAL_SPAWNER")
            || name.equals("SPAWNER")
            || name.equals("VAULT");
    }

    private static boolean isNaturalConversionMaterial(Material material) {
        String name = material.name();
        return name.equals("GRANITE")
            || name.equals("DIORITE")
            || name.equals("ANDESITE")
            || name.equals("TUFF")
            || name.equals("CALCITE")
            || name.endsWith("_TERRACOTTA")
            || name.endsWith("_CONCRETE")
            || name.endsWith("_CONCRETE_POWDER");
    }

    private static <T> T random(List<T> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }
}
