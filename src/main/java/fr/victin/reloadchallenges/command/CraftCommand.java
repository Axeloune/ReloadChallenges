package fr.victin.reloadchallenges.command;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.GameState;
import fr.victin.reloadchallenges.game.challenge.ChallengeGame;
import fr.victin.reloadchallenges.game.challenge.TargetMaterialChallenge;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CraftCommand implements CommandExecutor {
    private final ReloadChallengesPlugin plugin;

    public CraftCommand(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        if (plugin.gameManager().state() != GameState.RUNNING) {
            player.sendMessage(plugin.uiManager().prefixed("<red>L'aide d'objectif est disponible uniquement pendant une partie.</red>"));
            return true;
        }

        ChallengeGame currentGame = plugin.gameManager().currentGame();
        if (!(currentGame instanceof TargetMaterialChallenge targetChallenge)) {
            player.sendMessage(plugin.uiManager().prefixed("<red>Le mini-jeu actuel n'a pas de craft ou d'aperçu d'item.</red>"));
            return true;
        }

        Material target = targetChallenge.targetMaterial();
        if (target == null || target.isAir()) {
            player.sendMessage(plugin.uiManager().prefixed("<red>L'objectif n'est pas encore disponible.</red>"));
            return true;
        }

        plugin.uiManager().openObjectiveRecipe(player, target, currentGame.type());
        return true;
    }
}
