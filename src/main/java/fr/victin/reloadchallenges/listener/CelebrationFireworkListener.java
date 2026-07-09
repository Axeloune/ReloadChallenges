package fr.victin.reloadchallenges.listener;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

public final class CelebrationFireworkListener implements Listener {
    private final NamespacedKey victoryFireworkKey;

    public CelebrationFireworkListener(ReloadChallengesPlugin plugin) {
        this.victoryFireworkKey = new NamespacedKey(plugin, "victory_firework");
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Firework firework)) {
            return;
        }
        if (firework.getPersistentDataContainer().has(victoryFireworkKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }
}
