package fr.victin.reloadchallenges.game.challenge;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;
import fr.victin.reloadchallenges.util.Formatters;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class FindChallenge extends ChallengeGame implements TargetMaterialChallenge {
    private Material target;

    public FindChallenge(ReloadChallengesPlugin plugin, ChallengeMode mode) {
        super(plugin, ChallengeType.FIND, mode);
    }

    @Override
    public void selectObjective() {
        target = ConfiguredRandom.anySurvivalItem();
    }

    @Override
    public void checkInventory(Player player) {
        if (target != null && player.getInventory().contains(target)) {
            complete(player);
        }
    }

    @Override
    public Material icon() {
        return target == null ? Material.COMPASS : target;
    }

    @Override
    public String objectiveDisplayName() {
        return target == null ? "Inconnu" : Formatters.material(target);
    }

    @Override
    public Material targetMaterial() {
        return target;
    }
}
