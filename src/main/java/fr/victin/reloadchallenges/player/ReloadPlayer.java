package fr.victin.reloadchallenges.player;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.team.ReloadTeam;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public final class ReloadPlayer {
    private final UUID uuid;
    private String name;
    private PlayerState state;
    private ReloadTeam team;
    private final ReloadStatistics statistics;

    public ReloadPlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.state = PlayerState.SPECTATOR;
        this.statistics = new ReloadStatistics();
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void refreshName(Player player) {
        this.name = player.getName();
    }

    public PlayerState state() {
        return state;
    }

    public void state(PlayerState state) {
        this.state = state;
    }

    public ReloadTeam team() {
        return team;
    }

    public void team(ReloadTeam team) {
        this.team = team;
    }

    public ReloadStatistics statistics() {
        return statistics;
    }

    public Optional<Player> bukkitPlayer() {
        return Optional.ofNullable(Bukkit.getPlayer(uuid));
    }

    public void send(ReloadChallengesPlugin plugin, String miniMessage) {
        bukkitPlayer().ifPresent(player -> player.sendMessage(plugin.uiManager().prefixed(miniMessage)));
    }

    public void play(Sound sound, float volume, float pitch) {
        bukkitPlayer().ifPresent(player -> player.playSound(player.getLocation(), sound, volume, pitch));
    }
}
