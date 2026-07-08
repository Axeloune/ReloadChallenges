package fr.victin.reloadchallenges.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ReloadGuiHolder implements InventoryHolder {
    private final GuiType type;

    public ReloadGuiHolder(GuiType type) {
        this.type = type;
    }

    public GuiType type() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
