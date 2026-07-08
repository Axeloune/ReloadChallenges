package fr.victin.reloadchallenges.listener;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public final class HostToolListener implements Listener {
    private final ReloadChallengesPlugin plugin;

    public HostToolListener(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!plugin.uiManager().isHostMenuItem(item)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!plugin.playerManager().isActualHost(player)) {
            plugin.uiManager().playMenuDeny(player);
            return;
        }
        plugin.uiManager().openHostConfig(player);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.uiManager().isHostMenuItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            plugin.uiManager().playMenuDeny(event.getPlayer());
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (plugin.uiManager().isHostMenuItem(event.getMainHandItem()) || plugin.uiManager().isHostMenuItem(event.getOffHandItem())) {
            event.setCancelled(true);
            plugin.uiManager().playMenuDeny(event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack hotbarItem = event.getHotbarButton() >= 0 ? player.getInventory().getItem(event.getHotbarButton()) : null;
        if (plugin.uiManager().isHostMenuItem(event.getCurrentItem())
            || plugin.uiManager().isHostMenuItem(event.getCursor())
            || plugin.uiManager().isHostMenuItem(hotbarItem)) {
            event.setCancelled(true);
            plugin.uiManager().playMenuDeny(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (plugin.uiManager().isHostMenuItem(event.getOldCursor())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                plugin.uiManager().playMenuDeny(player);
            }
        }
    }
}
