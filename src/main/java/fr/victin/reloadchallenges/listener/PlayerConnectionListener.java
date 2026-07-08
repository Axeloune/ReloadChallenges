package fr.victin.reloadchallenges.listener;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PlayerConnectionListener implements Listener {
    private final ReloadChallengesPlugin plugin;

    public PlayerConnectionListener(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.playerManager().handleJoin(event.getPlayer());
        plugin.uiManager().updateAllScoreboards();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.playerManager().handleQuit(event.getPlayer());
        plugin.uiManager().updateAllScoreboards();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.playerManager().get(event.getPlayer()).ifPresent(player -> player.statistics().addDeath());
        if (event.getPlayer().getKiller() != null) {
            plugin.playerManager().get(event.getPlayer().getKiller()).ifPresent(player -> player.statistics().addKill());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (plugin.gameManager().state() == fr.victin.reloadchallenges.game.GameState.LOBBY) {
            event.setRespawnLocation(plugin.mapManager().lobbyLocation());
        }
    }
}
