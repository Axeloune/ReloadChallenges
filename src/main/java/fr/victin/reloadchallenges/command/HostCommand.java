package fr.victin.reloadchallenges.command;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class HostCommand implements CommandExecutor, TabCompleter {
    private final ReloadChallengesPlugin plugin;

    public HostCommand(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!plugin.playerManager().requireHost(sender)) {
            return true;
        }

        switch (sub) {
            case "config" -> {
                if (sender instanceof Player player) {
                    plugin.uiManager().openHostConfig(player);
                } else {
                    sender.sendMessage("Cette commande est réservée aux joueurs.");
                }
            }
            case "start" -> plugin.gameManager().start(sender);
            case "stop" -> plugin.gameManager().stop(sender);
            case "game" -> handleGame(sender, args);
            case "mode" -> handleMode(sender, args);
            case "reload" -> {
                if (!sender.hasPermission("reloadchallenges.reload")) {
                    sender.sendMessage(plugin.uiManager().prefixed("<red>Permission manquante.</red>"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.applyServerDefaults();
                sender.sendMessage(plugin.uiManager().prefixed("<green>Configuration rechargée.</green>"));
                plugin.uiManager().updateAllScoreboards();
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("config", "start", "stop", "game", "mode", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("game")) {
            return filter(Arrays.stream(ChallengeType.values()).map(ChallengeType::id).toList(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            return filter(List.of("ffa", "teams"), args[1]);
        }
        return List.of();
    }

    private void handleGame(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (sender instanceof Player player) {
                plugin.uiManager().openGameSelect(player);
            } else {
                sender.sendMessage(plugin.uiManager().prefixed("<red>Usage: /host game <type></red>"));
            }
            return;
        }
        ChallengeType.parse(args[1]).ifPresentOrElse(
            plugin.gameManager()::selectType,
            () -> sender.sendMessage(plugin.uiManager().prefixed("<red>Mini-jeu inconnu.</red>"))
        );
    }

    private void handleMode(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (sender instanceof Player player) {
                plugin.uiManager().openModeSelect(player);
            } else {
                sender.sendMessage(plugin.uiManager().prefixed("<red>Usage: /host mode <ffa|teams></red>"));
            }
            return;
        }
        plugin.gameManager().mode(ChallengeMode.parse(args[1]));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.uiManager().prefixed("<aqua>/h config</aqua> <gray>- ouvrir le menu hôte</gray>"));
        sender.sendMessage(plugin.uiManager().prefixed("<aqua>/h start</aqua> <gray>- lancer</gray>"));
        sender.sendMessage(plugin.uiManager().prefixed("<aqua>/h stop</aqua> <gray>- arrêter</gray>"));
        sender.sendMessage(plugin.uiManager().prefixed("<aqua>/h game</aqua> <gray>- choisir le mini-jeu</gray>"));
        sender.sendMessage(plugin.uiManager().prefixed("<aqua>/h mode</aqua> <gray>- FFA ou équipes</gray>"));
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }
}
