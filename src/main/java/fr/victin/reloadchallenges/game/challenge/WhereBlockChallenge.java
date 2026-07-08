package fr.victin.reloadchallenges.game.challenge;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;
import fr.victin.reloadchallenges.util.Formatters;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class WhereBlockChallenge extends ChallengeGame {
    private Material target;

    public WhereBlockChallenge(ReloadChallengesPlugin plugin, ChallengeMode mode) {
        super(plugin, ChallengeType.WHERE_BLOCK, mode);
    }

    @Override
    public void selectObjective() {
        target = ConfiguredRandom.anyWorldBlock();
    }

    @Override
    public void handleInteract(PlayerInteractEvent event) {
        if (target == null || event.getClickedBlock() == null) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock().getType() == target) {
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.6F);
            complete(event.getPlayer());
        } else {
            plugin.playerManager().get(event.getPlayer()).ifPresent(player -> player.statistics().addFailedAttempt());
        }
    }

    @Override
    public Material icon() {
        return target == null ? Material.GRASS_BLOCK : target;
    }

    @Override
    public String objectiveDisplayName() {
        return target == null ? "Inconnu" : Formatters.material(target);
    }
}
