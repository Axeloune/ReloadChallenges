package fr.victin.reloadchallenges.player;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PlayerManager {
    private final ReloadChallengesPlugin plugin;
    private final Map<UUID, ReloadPlayer> players = new LinkedHashMap<>();
    private UUID hostUuid;

    public PlayerManager(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
    }

    public ReloadPlayer handleJoin(Player player) {
        ReloadPlayer reloadPlayer = players.computeIfAbsent(player.getUniqueId(), ignored -> new ReloadPlayer(player));
        reloadPlayer.refreshName(player);

        if (hostUuid == null) {
            hostUuid = player.getUniqueId();
            reloadPlayer.state(PlayerState.HOST);
            reloadPlayer.send(plugin, "<aqua>Tu es l'hôte de la partie. Utilise <white>/h config</white>.</aqua>");
        } else if (plugin.gameManager().state() == fr.victin.reloadchallenges.game.GameState.LOBBY) {
            reloadPlayer.state(PlayerState.SPECTATOR);
        }

        plugin.mapManager().teleportToLobby(player);
        plugin.uiManager().setupLobbyHotbar(player, isActualHost(player));
        plugin.uiManager().updateScoreboard(player);
        return reloadPlayer;
    }

    public void handleQuit(Player player) {
        ReloadPlayer reloadPlayer = players.get(player.getUniqueId());
        if (reloadPlayer != null) {
            reloadPlayer.state(PlayerState.SPECTATOR);
        }
        if (player.getUniqueId().equals(hostUuid)) {
            electNewHost();
        }
    }

    public boolean isHost(Player player) {
        return player.getUniqueId().equals(hostUuid) || player.hasPermission("reloadchallenges.host");
    }

    public boolean isActualHost(Player player) {
        return player.getUniqueId().equals(hostUuid);
    }

    public Optional<ReloadPlayer> host() {
        return Optional.ofNullable(hostUuid).map(players::get);
    }

    public Optional<ReloadPlayer> get(Player player) {
        return Optional.ofNullable(players.get(player.getUniqueId()));
    }

    public Collection<ReloadPlayer> all() {
        return players.values();
    }

    public List<ReloadPlayer> onlinePlayers() {
        return players.values().stream()
            .filter(player -> player.bukkitPlayer().isPresent())
            .toList();
    }

    public List<ReloadPlayer> gamePlayers() {
        return onlinePlayers().stream()
            .filter(player -> player.state() == PlayerState.IN_GAME)
            .toList();
    }

    public boolean requireHost(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (isHost(player)) {
            return true;
        }
        player.sendMessage(plugin.uiManager().prefixed("<red>Seul l'hôte peut utiliser cette commande.</red>"));
        return false;
    }

    private void electNewHost() {
        hostUuid = plugin.getServer().getOnlinePlayers().stream()
            .map(Player::getUniqueId)
            .min(Comparator.comparing(UUID::toString))
            .orElse(null);
        if (hostUuid == null) {
            return;
        }
        ReloadPlayer newHost = players.get(hostUuid);
        if (newHost != null && plugin.gameManager().state() == fr.victin.reloadchallenges.game.GameState.LOBBY) {
            newHost.state(PlayerState.HOST);
            newHost.send(plugin, "<aqua>Tu deviens le nouvel hôte.</aqua>");
            newHost.bukkitPlayer().ifPresent(player -> plugin.uiManager().setupLobbyHotbar(player, true));
        }
    }
}
