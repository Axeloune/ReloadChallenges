package fr.victin.reloadchallenges.team;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.player.ReloadPlayer;
import org.bukkit.ChatColor;

import java.util.Collection;
import java.util.List;

public final class TeamManager {
    private final ReloadChallengesPlugin plugin;
    private final ReloadTeam redTeam;
    private final ReloadTeam blueTeam;

    public TeamManager(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
        this.redTeam = new ReloadTeam("red", plugin.getConfig().getString("teams.names.red", "Rouge"), ChatColor.RED);
        this.blueTeam = new ReloadTeam("blue", plugin.getConfig().getString("teams.names.blue", "Bleue"), ChatColor.AQUA);
    }

    public void rebuild(Collection<ReloadPlayer> players) {
        redTeam.clear();
        blueTeam.clear();
        int index = 0;
        for (ReloadPlayer reloadPlayer : players) {
            ReloadTeam team = index++ % 2 == 0 ? redTeam : blueTeam;
            team.add(reloadPlayer.uuid());
            reloadPlayer.team(team);
        }
    }

    public void clear(Collection<ReloadPlayer> players) {
        redTeam.clear();
        blueTeam.clear();
        players.forEach(player -> player.team(null));
    }

    public List<ReloadTeam> teams() {
        return List.of(redTeam, blueTeam);
    }
}
