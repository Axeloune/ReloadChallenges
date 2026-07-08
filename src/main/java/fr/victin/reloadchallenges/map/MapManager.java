package fr.victin.reloadchallenges.map;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;

public final class MapManager {
    private final ReloadChallengesPlugin plugin;
    private World currentGameWorld;
    private String currentGeneratedWorldName;
    private BukkitTask preloadTask;

    public MapManager(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
    }

    public void prepareWorld(ChallengeType challengeType, Consumer<World> whenReady) {
        String resetMode = plugin.getConfig().getString("game.reset-mode", "NEW_WORLD");
        if (!"NEW_WORLD".equalsIgnoreCase(resetMode)) {
            World lobbyWorld = lobbyWorld();
            currentGameWorld = lobbyWorld;
            currentGeneratedWorldName = null;
            preloadSpawn(lobbyWorld, challengeType, whenReady);
            return;
        }

        String prefix = plugin.getConfig().getString("game.generated-world-prefix", "reload_challenges_");
        String suffix = challengeType.id().toLowerCase(Locale.ROOT) + "_" + UUID.randomUUID().toString().substring(0, 8);
        String worldName = prefix + suffix;
        World.Environment environment = World.Environment.NORMAL;
        WorldCreator creator = new WorldCreator(worldName)
            .environment(environment)
            .type(WorldType.NORMAL)
            .generateStructures(true);

        currentGameWorld = Bukkit.createWorld(creator);
        currentGeneratedWorldName = worldName;
        preloadSpawn(currentGameWorld == null ? lobbyWorld() : currentGameWorld, challengeType, whenReady);
    }

    public void teleportPlayersToGame(World world, ChallengeType challengeType) {
        Location spawn = gameSpawnLocation(world, challengeType);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawn);
            player.setCompassTarget(spawn);
            player.setInvulnerable(false);
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(8.0F);
        }
    }

    public void resetToLobby(Runnable afterReset) {
        cancelPreload();
        for (Player player : Bukkit.getOnlinePlayers()) {
            teleportToLobby(player);
        }

        String generatedWorldName = currentGeneratedWorldName;
        currentGameWorld = null;
        currentGeneratedWorldName = null;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            unloadGeneratedWorld(generatedWorldName);
            afterReset.run();
        }, 40L);
    }

    public void teleportToLobby(Player player) {
        player.teleport(lobbyLocation());
        player.setGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(true);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(8.0F);
        plugin.uiManager().setupLobbyHotbar(player, plugin.playerManager().isActualHost(player));
    }

    public Location lobbyLocation() {
        World world = lobbyWorld();
        if (plugin.getConfig().getBoolean("lobby.spawn.enabled", false)) {
            return new Location(
                world,
                plugin.getConfig().getDouble("lobby.spawn.x"),
                plugin.getConfig().getDouble("lobby.spawn.y"),
                plugin.getConfig().getDouble("lobby.spawn.z"),
                (float) plugin.getConfig().getDouble("lobby.spawn.yaw"),
                (float) plugin.getConfig().getDouble("lobby.spawn.pitch")
            );
        }
        return world.getSpawnLocation().clone().add(0.5, 1.0, 0.5);
    }

    private World lobbyWorld() {
        String configured = plugin.getConfig().getString("lobby.world", "world");
        World world = Bukkit.getWorld(configured);
        if (world != null) {
            return world;
        }
        World first = Bukkit.getWorlds().isEmpty() ? Bukkit.createWorld(new WorldCreator(configured)) : Bukkit.getWorlds().get(0);
        if (first == null) {
            throw new IllegalStateException("No lobby world available");
        }
        return first;
    }

    private Location gameSpawnLocation(World world, ChallengeType challengeType) {
        return world.getSpawnLocation().clone().add(0.5, 1.0, 0.5);
    }

    private void preloadSpawn(World world, ChallengeType challengeType, Consumer<World> whenReady) {
        Location spawn = gameSpawnLocation(world, challengeType);
        int centerX = spawn.getBlockX() >> 4;
        int centerZ = spawn.getBlockZ() >> 4;
        int radius = Math.max(0, plugin.getConfig().getInt("game.preload-radius-chunks", 4));
        Queue<int[]> chunks = new ArrayDeque<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                chunks.add(new int[] {x, z});
            }
        }

        int total = Math.max(1, chunks.size());
        plugin.uiManager().showPreparationBar(challengeType, total);
        final int[] loaded = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int perTick = Math.max(1, plugin.getConfig().getInt("game.preload-chunks-per-tick", 3));
            for (int i = 0; i < perTick && !chunks.isEmpty(); i++) {
                int[] chunk = chunks.poll();
                world.getChunkAt(chunk[0], chunk[1]).load(true);
                loaded[0]++;
            }
            plugin.uiManager().updatePreparationBar(loaded[0], total);
            if (chunks.isEmpty()) {
                task[0].cancel();
                preloadTask = null;
                plugin.uiManager().hidePreparationBar();
                whenReady.accept(world);
            }
        }, 1L, 1L);
        preloadTask = task[0];
    }

    private void cancelPreload() {
        if (preloadTask != null) {
            preloadTask.cancel();
            preloadTask = null;
        }
        plugin.uiManager().hidePreparationBar();
    }

    private void unloadGeneratedWorld(String generatedWorldName) {
        if (generatedWorldName == null) {
            return;
        }
        World generatedWorld = Bukkit.getWorld(generatedWorldName);
        if (generatedWorld != null) {
            Bukkit.unloadWorld(generatedWorld, false);
        }
        if (!plugin.getConfig().getBoolean("game.keep-generated-worlds", false)) {
            deleteGeneratedWorld(generatedWorldName);
        }
    }

    private void deleteGeneratedWorld(String generatedWorldName) {
        String prefix = plugin.getConfig().getString("game.generated-world-prefix", "reload_challenges_");
        if (!generatedWorldName.startsWith(prefix)) {
            plugin.getLogger().warning("Refusing to delete non-generated world " + generatedWorldName);
            return;
        }
        Path worldPath = Bukkit.getWorldContainer().toPath().resolve(generatedWorldName).normalize();
        Path containerPath = Bukkit.getWorldContainer().toPath().normalize();
        if (!worldPath.startsWith(containerPath) || !Files.exists(worldPath)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (var stream = Files.walk(worldPath)) {
                stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        plugin.getLogger().warning("Cannot delete " + path + ": " + exception.getMessage());
                    }
                });
            } catch (IOException exception) {
                plugin.getLogger().warning("Cannot delete generated world " + generatedWorldName + ": " + exception.getMessage());
            }
        });
    }
}
