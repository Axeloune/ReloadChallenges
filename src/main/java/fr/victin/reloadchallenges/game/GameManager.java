package fr.victin.reloadchallenges.game;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.challenge.ChallengeFactory;
import fr.victin.reloadchallenges.game.challenge.ChallengeGame;
import fr.victin.reloadchallenges.game.challenge.TargetMaterialChallenge;
import fr.victin.reloadchallenges.player.PlayerState;
import fr.victin.reloadchallenges.player.ReloadPlayer;
import fr.victin.reloadchallenges.team.ReloadTeam;
import fr.victin.reloadchallenges.util.Formatters;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class GameManager {
    private final ReloadChallengesPlugin plugin;
    private GameState state = GameState.LOBBY;
    private ChallengeType selectedType = ChallengeType.RANDOM;
    private ChallengeMode mode = ChallengeMode.FFA;
    private ChallengeGame currentGame;
    private BukkitTask timerTask;
    private long startedAtMillis;

    public GameManager(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
    }

    public GameState state() {
        return state;
    }

    public ChallengeType selectedType() {
        return selectedType;
    }

    public ChallengeMode mode() {
        return mode;
    }

    public ChallengeGame currentGame() {
        return currentGame;
    }

    public long elapsedMillis() {
        if (startedAtMillis == 0L) {
            return 0L;
        }
        return System.currentTimeMillis() - startedAtMillis;
    }

    public void selectType(ChallengeType selectedType) {
        this.selectedType = selectedType;
        plugin.uiManager().broadcast("<aqua>Mini-jeu sélectionné :</aqua> <white>" + selectedType.displayName() + "</white>");
        plugin.uiManager().updateAllScoreboards();
    }

    public void mode(ChallengeMode mode) {
        this.mode = mode;
        plugin.uiManager().broadcast("<aqua>Mode sélectionné :</aqua> <white>" + mode.displayName() + "</white>");
        plugin.uiManager().updateAllScoreboards();
    }

    public void start(CommandSender sender) {
        if (state != GameState.LOBBY) {
            sender.sendMessage(plugin.uiManager().prefixed("<red>Une partie est déjà en cours.</red>"));
            return;
        }
        List<ReloadPlayer> participants = plugin.playerManager().onlinePlayers();
        if (participants.isEmpty()) {
            sender.sendMessage(plugin.uiManager().prefixed("<red>Aucun joueur connecté.</red>"));
            return;
        }

        state = GameState.PREPARING;
        ChallengeType launchType = resolveSelectedType();
        currentGame = ChallengeFactory.create(plugin, launchType, mode);
        currentGame.selectObjective();
        startedAtMillis = 0L;
        plugin.uiManager().updateAllScoreboards();
        plugin.uiManager().broadcast("<aqua>Préparation de la prochaine manche.</aqua> <gray>Personne ne sera téléporté avant la fin de la génération.</gray>");
        plugin.mapManager().prepareWorld(launchType, world -> {
            if (state != GameState.PREPARING || currentGame == null) {
                return;
            }
            beginRunning(participants, world, launchType);
        });
    }

    public void stop(CommandSender sender) {
        if (state == GameState.LOBBY) {
            sender.sendMessage(plugin.uiManager().prefixed("<red>Aucune partie en cours.</red>"));
            return;
        }
        if (state == GameState.PREPARING) {
            cancelPreparing(sender);
            return;
        }
        finish(null, null, "Partie arrêtée par l'hôte.");
    }

    public void completeObjective(Player player) {
        if (state != GameState.RUNNING || currentGame == null) {
            return;
        }
        ReloadPlayer reloadPlayer = plugin.playerManager().get(player).orElse(null);
        if (reloadPlayer == null || reloadPlayer.state() != PlayerState.IN_GAME) {
            return;
        }
        reloadPlayer.statistics().markObjectiveCompleted();
        ReloadTeam winningTeam = mode == ChallengeMode.TEAMS ? reloadPlayer.team() : null;
        finish(player, winningTeam, "Objectif validé.");
    }

    public void shutdown() {
        cancelTimer();
        if (currentGame != null) {
            currentGame.stop();
        }
    }

    private void startTimer() {
        cancelTimer();
        int maxDuration = plugin.getConfig().getInt("game.max-duration-seconds", 3600);
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state != GameState.RUNNING || currentGame == null) {
                return;
            }
            int elapsedSeconds = (int) (elapsedMillis() / 1000L);
            currentGame.tick(elapsedSeconds);
            for (ReloadPlayer reloadPlayer : plugin.playerManager().gamePlayers()) {
                reloadPlayer.bukkitPlayer().ifPresent(currentGame::checkInventory);
            }
            String helper = currentGame instanceof TargetMaterialChallenge ? " <dark_gray>|</dark_gray> <aqua>/craft</aqua> <gray>aide</gray>" : "";
            Bukkit.getOnlinePlayers().forEach(player -> player.sendActionBar(plugin.uiManager().mm("<aqua>" + currentGame.type().objectiveLabel() + " :</aqua> <white>" + currentGame.objectiveDisplayName() + "</white> <dark_gray>|</dark_gray> <yellow>" + Formatters.duration(elapsedMillis()) + "</yellow>" + helper)));
            plugin.uiManager().updateAllScoreboards();
            if (elapsedSeconds >= maxDuration) {
                finish(null, null, "Temps écoulé.");
            }
        }, 0L, 20L);
    }

    private void beginRunning(List<ReloadPlayer> participants, org.bukkit.World world, ChallengeType launchType) {
        state = GameState.RUNNING;
        startedAtMillis = System.currentTimeMillis();
        for (ReloadPlayer reloadPlayer : participants) {
            reloadPlayer.statistics().reset();
            reloadPlayer.statistics().markGameStart();
            reloadPlayer.state(PlayerState.IN_GAME);
        }
        if (mode == ChallengeMode.TEAMS) {
            plugin.teamManager().rebuild(participants);
        } else {
            plugin.teamManager().clear(participants);
        }

        plugin.mapManager().teleportPlayersToGame(world, launchType);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getConfig().getBoolean("game.clear-inventory-on-start", true)) {
                player.getInventory().clear();
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8F, 1.2F);
            player.sendActionBar(plugin.uiManager().mm("<green>Arène prête.</green> <white>Bonne chance !</white>"));
        }

        currentGame.start();
        plugin.uiManager().broadcast("<green>La manche commence :</green> <white>" + launchType.displayName() + "</white> <gray>(" + mode.displayName() + ")</gray>");
        startTimer();
    }

    private ChallengeType resolveSelectedType() {
        if (selectedType != ChallengeType.RANDOM) {
            return selectedType;
        }
        List<ChallengeType> playable = Arrays.stream(ChallengeType.values())
            .filter(type -> type != ChallengeType.RANDOM)
            .toList();
        return playable.get(ThreadLocalRandom.current().nextInt(playable.size()));
    }

    private void cancelPreparing(CommandSender sender) {
        state = GameState.RESETTING;
        if (currentGame != null) {
            currentGame.stop();
        }
        sender.sendMessage(plugin.uiManager().prefixed("<yellow>Préparation annulée.</yellow>"));
        plugin.mapManager().resetToLobby(() -> {
            currentGame = null;
            startedAtMillis = 0L;
            state = GameState.LOBBY;
            plugin.uiManager().updateAllScoreboards();
            plugin.uiManager().broadcast("<green>Lobby prêt.</green>");
        });
    }

    private void finish(Player winner, ReloadTeam winningTeam, String reason) {
        if (state == GameState.ENDING || state == GameState.RESETTING) {
            return;
        }
        state = GameState.ENDING;
        cancelTimer();
        if (currentGame != null) {
            currentGame.stop();
        }

        plugin.playerManager().gamePlayers().forEach(player -> player.statistics().markGameEnd());
        plugin.uiManager().playVictory(winner, winningTeam);
        plugin.uiManager().broadcast("<gold>" + reason + "</gold>");
        broadcastTop();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            state = GameState.RESETTING;
            plugin.uiManager().broadcast("<aqua>Réinitialisation de la carte...</aqua>");
            plugin.mapManager().resetToLobby(() -> {
                plugin.teamManager().clear(plugin.playerManager().onlinePlayers());
                for (ReloadPlayer reloadPlayer : plugin.playerManager().onlinePlayers()) {
                    reloadPlayer.state(plugin.playerManager().host().filter(host -> host.uuid().equals(reloadPlayer.uuid())).isPresent() ? PlayerState.HOST : PlayerState.SPECTATOR);
                }
                currentGame = null;
                startedAtMillis = 0L;
                state = GameState.LOBBY;
                plugin.uiManager().shutdown();
                plugin.uiManager().updateAllScoreboards();
                plugin.uiManager().broadcast("<green>Lobby prêt.</green> <gray>L'hôte peut relancer une manche.</gray>");
            });
        }, 120L);
    }

    private void broadcastTop() {
        List<ReloadPlayer> top = plugin.playerManager().onlinePlayers().stream()
            .filter(player -> player.statistics().mainObjectiveMillis() > 0L)
            .sorted(Comparator.comparingLong(player -> player.statistics().mainObjectiveMillis()))
            .limit(3)
            .toList();
        if (top.isEmpty()) {
            plugin.uiManager().broadcast("<gray>Aucun classement final.</gray>");
            return;
        }
        plugin.uiManager().broadcast("<gold><bold>Top 3</bold></gold>");
        int rank = 1;
        for (ReloadPlayer player : top) {
            plugin.uiManager().broadcast("<yellow>#" + rank++ + "</yellow> <white>" + player.name() + "</white> <gray>-</gray> <aqua>" + Formatters.duration(player.statistics().mainObjectiveMillis()) + "</aqua>");
        }
    }

    private void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
}
