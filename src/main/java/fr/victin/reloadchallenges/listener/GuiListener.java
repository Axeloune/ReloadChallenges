package fr.victin.reloadchallenges.listener;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;
import fr.victin.reloadchallenges.ui.GuiType;
import fr.victin.reloadchallenges.ui.ReloadGuiHolder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class GuiListener implements Listener {
    private static final Map<Integer, ChallengeType> GAME_SLOTS = Map.of(
        10, ChallengeType.RANDOM,
        19, ChallengeType.FIND,
        20, ChallengeType.WHERE_BLOCK,
        21, ChallengeType.CRAFT,
        22, ChallengeType.SPEEDRUN,
        23, ChallengeType.WHERE_BIOME,
        24, ChallengeType.MOB_HUNT,
        25, ChallengeType.BINGO
    );

    private final ReloadChallengesPlugin plugin;

    public GuiListener(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ReloadGuiHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (holder.type() == GuiType.ROULETTE || holder.type() == GuiType.BINGO_BOARD || holder.type() == GuiType.OBJECTIVE_RECIPE) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player) || !plugin.playerManager().isHost(player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType().name().endsWith("_STAINED_GLASS_PANE")) {
            return;
        }

        if (holder.type() == GuiType.HOST_CONFIG) {
            handleHostConfig(player, event.getRawSlot());
            return;
        }
        if (holder.type() == GuiType.GAME_SELECT) {
            ChallengeType slotType = GAME_SLOTS.get(event.getRawSlot());
            if (slotType != null) {
                plugin.uiManager().playMenuConfirm(player);
                plugin.gameManager().selectType(slotType);
                plugin.uiManager().openHostConfig(player);
                return;
            }
            if (clicked.getType() == Material.ARROW) {
                plugin.uiManager().playMenuBack(player);
                plugin.uiManager().openHostConfig(player);
                return;
            }
        }
        if (holder.type() == GuiType.MODE_SELECT) {
            if (clicked.getType() == Material.ARROW) {
                plugin.uiManager().playMenuBack(player);
                plugin.uiManager().openHostConfig(player);
                return;
            }
            if (clicked.getType() == Material.PLAYER_HEAD) {
                plugin.uiManager().playMenuConfirm(player);
                plugin.gameManager().mode(ChallengeMode.FFA);
            } else if (clicked.getType() == Material.SHIELD) {
                plugin.uiManager().playMenuConfirm(player);
                plugin.gameManager().mode(ChallengeMode.TEAMS);
            }
            plugin.uiManager().openHostConfig(player);
        }
        if (holder.type() == GuiType.TEAM_SELECT) {
            if (clicked.getType() == Material.ARROW) {
                plugin.uiManager().playMenuBack(player);
                plugin.uiManager().openHostConfig(player);
                return;
            }
            if (clicked.getType().name().endsWith("_BANNER")) {
                plugin.uiManager().playMenuConfirm(player);
                plugin.gameManager().mode(ChallengeMode.TEAMS);
                plugin.uiManager().openHostConfig(player);
            }
        }
    }

    private void handleHostConfig(Player player, int slot) {
        switch (slot) {
            case 19 -> {
                plugin.uiManager().playMenuClick(player);
                plugin.uiManager().openGameSelect(player);
            }
            case 21 -> {
                plugin.uiManager().playMenuClick(player);
                plugin.uiManager().openModeSelect(player);
            }
            case 23 -> {
                plugin.uiManager().playMenuConfirm(player);
                player.closeInventory();
                plugin.gameManager().start(player);
            }
            case 25 -> {
                plugin.uiManager().playMenuDeny(player);
                player.closeInventory();
                plugin.gameManager().stop(player);
            }
            case 31 -> {
                plugin.uiManager().playMenuClick(player);
                plugin.uiManager().openTeamSelect(player);
            }
            case 40 -> {
                plugin.uiManager().playMenuConfirm(player);
                plugin.reloadConfig();
                plugin.applyServerDefaults();
                player.sendMessage(plugin.uiManager().prefixed("<green>Configuration rechargée.</green>"));
                plugin.uiManager().openHostConfig(player);
            }
            default -> {
            }
        }
    }
}
