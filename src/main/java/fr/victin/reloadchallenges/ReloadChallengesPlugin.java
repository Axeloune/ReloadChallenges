package fr.victin.reloadchallenges;

import fr.victin.reloadchallenges.command.BingoCommand;
import fr.victin.reloadchallenges.command.CraftCommand;
import fr.victin.reloadchallenges.command.HostCommand;
import fr.victin.reloadchallenges.game.GameManager;
import fr.victin.reloadchallenges.listener.CelebrationFireworkListener;
import fr.victin.reloadchallenges.listener.ChallengeListener;
import fr.victin.reloadchallenges.listener.GuiListener;
import fr.victin.reloadchallenges.listener.HostToolListener;
import fr.victin.reloadchallenges.listener.PlayerConnectionListener;
import fr.victin.reloadchallenges.map.MapManager;
import fr.victin.reloadchallenges.player.PlayerManager;
import fr.victin.reloadchallenges.team.TeamManager;
import fr.victin.reloadchallenges.ui.UIManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReloadChallengesPlugin extends JavaPlugin {
    private PlayerManager playerManager;
    private TeamManager teamManager;
    private MapManager mapManager;
    private UIManager uiManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        applyServerDefaults();

        this.playerManager = new PlayerManager(this);
        this.teamManager = new TeamManager(this);
        this.mapManager = new MapManager(this);
        this.uiManager = new UIManager(this);
        this.gameManager = new GameManager(this);

        HostCommand hostCommand = new HostCommand(this);
        PluginCommand command = getCommand("host");
        if (command != null) {
            command.setExecutor(hostCommand);
            command.setTabCompleter(hostCommand);
        }
        PluginCommand bingoCommand = getCommand("bingo");
        if (bingoCommand != null) {
            bingoCommand.setExecutor(new BingoCommand(this));
        }
        PluginCommand craftCommand = getCommand("craft");
        if (craftCommand != null) {
            craftCommand.setExecutor(new CraftCommand(this));
        }

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CelebrationFireworkListener(this), this);
        getServer().getPluginManager().registerEvents(new ChallengeListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new HostToolListener(this), this);

        getServer().getOnlinePlayers().forEach(playerManager::handleJoin);
        getLogger().info("ReloadChallenges enabled.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
        if (uiManager != null) {
            uiManager.shutdown();
        }
    }

    public PlayerManager playerManager() {
        return playerManager;
    }

    public TeamManager teamManager() {
        return teamManager;
    }

    public MapManager mapManager() {
        return mapManager;
    }

    public UIManager uiManager() {
        return uiManager;
    }

    public GameManager gameManager() {
        return gameManager;
    }

    public void applyServerDefaults() {
        boolean whitelistEnabled = getConfig().getBoolean("server.whitelist-enabled-by-default", true);
        getServer().setWhitelist(whitelistEnabled);
        getLogger().info("Whitelist " + (whitelistEnabled ? "enabled" : "disabled") + " from ReloadChallenges config.");
    }
}
