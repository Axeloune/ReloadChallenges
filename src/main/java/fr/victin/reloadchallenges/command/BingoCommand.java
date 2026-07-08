package fr.victin.reloadchallenges.command;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.GameState;
import fr.victin.reloadchallenges.game.challenge.BingoChallenge;
import fr.victin.reloadchallenges.game.challenge.ChallengeGame;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BingoCommand implements CommandExecutor {
    private final ReloadChallengesPlugin plugin;

    public BingoCommand(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        if (plugin.gameManager().state() != GameState.RUNNING) {
            player.sendMessage(plugin.uiManager().prefixed("<red>La grille Bingo n'est disponible que pendant une partie.</red>"));
            return true;
        }

        ChallengeGame currentGame = plugin.gameManager().currentGame();
        if (!(currentGame instanceof BingoChallenge bingoChallenge)) {
            player.sendMessage(plugin.uiManager().prefixed("<red>La partie en cours n'est pas un Bingo.</red>"));
            return true;
        }

        plugin.uiManager().openBingoBoard(player, bingoChallenge);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8F, 1.25F);
        return true;
    }
}
