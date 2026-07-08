package fr.victin.reloadchallenges.game.challenge;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public final class SpeedrunChallenge extends ChallengeGame {
    public SpeedrunChallenge(ReloadChallengesPlugin plugin, ChallengeMode mode) {
        super(plugin, ChallengeType.SPEEDRUN, mode);
    }

    @Override
    public void selectObjective() {
    }

    @Override
    public void start() {
        super.start();
        plugin.uiManager().updateObjectiveBar("<dark_purple><bold>Ender Dragon</bold></dark_purple> <white>Préparez-vous</white>", 1.0F, BossBar.Color.PURPLE);
    }

    @Override
    public void handleEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }
        double remaining = Math.max(0.0D, dragon.getHealth() - event.getFinalDamage());
        int percent = (int) Math.round(Math.max(0.0D, Math.min(1.0D, remaining / dragon.getMaxHealth())) * 100.0D);
        plugin.uiManager().updateObjectiveBar("<dark_purple><bold>Ender Dragon</bold></dark_purple> <white>" + percent + "%</white>", 1.0F, BossBar.Color.PURPLE);
    }

    @Override
    public void handleEntityDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ENDER_DRAGON) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null && !plugin.playerManager().gamePlayers().isEmpty()) {
            killer = plugin.playerManager().gamePlayers().get(0).bukkitPlayer().orElse(null);
        }
        if (killer != null) {
            complete(killer);
        }
    }

    @Override
    public Material icon() {
        return Material.DRAGON_HEAD;
    }

    @Override
    public String objectiveDisplayName() {
        return "Tuer l'Ender Dragon";
    }
}
