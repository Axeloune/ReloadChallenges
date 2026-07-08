package fr.victin.reloadchallenges.game.challenge;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;
import fr.victin.reloadchallenges.util.Formatters;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public final class MobHuntChallenge extends ChallengeGame {
    private EntityType target;

    public MobHuntChallenge(ReloadChallengesPlugin plugin, ChallengeMode mode) {
        super(plugin, ChallengeType.MOB_HUNT, mode);
    }

    @Override
    public void selectObjective() {
        target = ConfiguredRandom.anyMobTarget();
    }

    @Override
    public void handleEntityDeath(EntityDeathEvent event) {
        if (target == null || event.getEntityType() != target) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        plugin.playerManager().get(killer).ifPresent(player -> player.statistics().addKill());
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.35F);
        complete(killer);
    }

    @Override
    public Material icon() {
        if (target == null) {
            return Material.CROSSBOW;
        }
        Material spawnEgg = Material.matchMaterial(target.name() + "_SPAWN_EGG");
        return spawnEgg == null ? Material.CROSSBOW : spawnEgg;
    }

    @Override
    public String objectiveDisplayName() {
        return target == null ? "Inconnu" : Formatters.title(target.name());
    }
}
