package fr.victin.reloadchallenges.game.challenge;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;
import fr.victin.reloadchallenges.util.Formatters;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public final class CraftChallenge extends ChallengeGame implements TargetMaterialChallenge {
    private Material target;

    public CraftChallenge(ReloadChallengesPlugin plugin, ChallengeMode mode) {
        super(plugin, ChallengeType.CRAFT, mode);
    }

    @Override
    public void selectObjective() {
        target = ConfiguredRandom.anyCraftResult();
    }

    @Override
    public void handleCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || target == null) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        completeIfTarget(player, result.getType());
    }

    @Override
    public void handleFurnaceExtract(FurnaceExtractEvent event) {
        if (target == null) {
            return;
        }
        completeIfTarget(event.getPlayer(), event.getItemType());
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || target == null) {
            return;
        }
        InventoryType inventoryType = event.getView().getTopInventory().getType();
        if (inventoryType != InventoryType.FURNACE && inventoryType != InventoryType.BLAST_FURNACE && inventoryType != InventoryType.SMOKER) {
            return;
        }
        if (event.getRawSlot() != 2) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        completeIfTarget(player, clicked.getType());
    }

    private void completeIfTarget(Player player, Material material) {
        if (material == target) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.4F);
            complete(player);
        }
    }

    @Override
    public Material icon() {
        return target == null ? Material.CRAFTING_TABLE : target;
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
