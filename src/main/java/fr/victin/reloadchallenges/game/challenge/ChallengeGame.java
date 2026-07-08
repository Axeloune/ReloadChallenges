package fr.victin.reloadchallenges.game.challenge;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public abstract class ChallengeGame {
    protected final ReloadChallengesPlugin plugin;
    private final ChallengeType type;
    private final ChallengeMode mode;

    protected ChallengeGame(ReloadChallengesPlugin plugin, ChallengeType type, ChallengeMode mode) {
        this.plugin = plugin;
        this.type = type;
        this.mode = mode;
    }

    public final ChallengeType type() {
        return type;
    }

    public final ChallengeMode mode() {
        return mode;
    }

    public abstract void selectObjective();

    public void start() {
        plugin.uiManager().showObjectiveBar(this);
        plugin.uiManager().playStartAnimation(this);
    }

    public void stop() {
    }

    public void tick(int elapsedSeconds) {
    }

    public void checkInventory(Player player) {
    }

    public void handleInteract(PlayerInteractEvent event) {
    }

    public void handleCraft(CraftItemEvent event) {
    }

    public void handleFurnaceExtract(FurnaceExtractEvent event) {
    }

    public void handleInventoryClick(InventoryClickEvent event) {
    }

    public void handleEntityDeath(EntityDeathEvent event) {
    }

    public void handleEntityDamage(EntityDamageEvent event) {
    }

    public abstract Material icon();

    public abstract String objectiveDisplayName();

    protected void complete(Player player) {
        plugin.gameManager().completeObjective(player);
    }
}
