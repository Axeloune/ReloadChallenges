package fr.victin.reloadchallenges.listener;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.GameState;
import fr.victin.reloadchallenges.game.challenge.ChallengeGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class ChallengeListener implements Listener {
    private final ReloadChallengesPlugin plugin;

    public ChallengeListener(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        currentGame().ifPresent(game -> game.handleInteract(event));
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        currentGame().ifPresent(game -> game.handleCraft(event));
    }

    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        currentGame().ifPresent(game -> game.handleFurnaceExtract(event));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        currentGame().ifPresent(game -> game.handleInventoryClick(event));
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        currentGame().ifPresent(game -> plugin.getServer().getScheduler().runTask(plugin, () -> game.checkInventory(player)));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        currentGame().ifPresent(game -> game.handleEntityDeath(event));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        currentGame().ifPresent(game -> game.handleEntityDamage(event));
    }

    private java.util.Optional<ChallengeGame> currentGame() {
        if (plugin.gameManager().state() != GameState.RUNNING || plugin.gameManager().currentGame() == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(plugin.gameManager().currentGame());
    }
}
