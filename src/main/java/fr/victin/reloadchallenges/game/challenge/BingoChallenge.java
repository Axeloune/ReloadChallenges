package fr.victin.reloadchallenges.game.challenge;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;
import fr.victin.reloadchallenges.player.ReloadPlayer;
import fr.victin.reloadchallenges.util.Formatters;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BingoChallenge extends ChallengeGame {
    private final NamespacedKey boardKey;
    private final List<BingoObjective> objectives = new ArrayList<>();
    private final Map<String, Set<Integer>> completed = new HashMap<>();

    public BingoChallenge(ReloadChallengesPlugin plugin, ChallengeMode mode) {
        super(plugin, ChallengeType.BINGO, mode);
        this.boardKey = new NamespacedKey(plugin, "bingo_board");
    }

    @Override
    public void selectObjective() {
        objectives.clear();
        Set<String> used = new HashSet<>();
        int attempts = 0;
        while (objectives.size() < 25 && attempts++ < 300) {
            BingoObjective objective = objectives.size() % 2 == 0 ? randomItemObjective() : randomMobObjective();
            if (used.add(objective.uniqueKey())) {
                objectives.add(objective);
            }
        }
        while (objectives.size() < 25) {
            objectives.add(randomItemObjective());
        }
    }

    @Override
    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            giveBoard(player);
            plugin.uiManager().openBingoBoard(player, this);
            player.showTitle(net.kyori.adventure.title.Title.title(
                plugin.uiManager().mm("<gradient:#20f2ff:#ff4fd8><bold>Bingo</bold></gradient>"),
                plugin.uiManager().mm("<white>Complète les 25 objectifs de la grille</white>"),
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(250), java.time.Duration.ofSeconds(3), java.time.Duration.ofMillis(600))
            ));
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.9F, 1.1F);
        }
    }

    @Override
    public void tick(int elapsedSeconds) {
        for (ReloadPlayer reloadPlayer : plugin.playerManager().gamePlayers()) {
            reloadPlayer.bukkitPlayer().ifPresent(player -> {
                giveBoard(player);
                checkInventory(player);
            });
        }
    }

    @Override
    public void checkInventory(Player player) {
        for (int i = 0; i < objectives.size(); i++) {
            BingoObjective objective = objectives.get(i);
            if (objective.kind() == BingoObjectiveKind.ITEM && player.getInventory().contains(objective.item())) {
                markCompleted(player, i);
            }
        }
    }

    @Override
    public void handleInteract(PlayerInteractEvent event) {
        if (isBoardItem(event.getItem())) {
            event.setCancelled(true);
            plugin.uiManager().openBingoBoard(event.getPlayer(), this);
        }
    }

    @Override
    public void handleCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            markItem(player, event.getRecipe().getResult().getType());
        }
    }

    @Override
    public void handleFurnaceExtract(FurnaceExtractEvent event) {
        markItem(event.getPlayer(), event.getItemType());
    }

    @Override
    public void handleEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        for (int i = 0; i < objectives.size(); i++) {
            BingoObjective objective = objectives.get(i);
            if (objective.kind() == BingoObjectiveKind.MOB && objective.entityType() == event.getEntityType()) {
                markCompleted(killer, i);
            }
        }
    }

    @Override
    public Material icon() {
        return Material.FILLED_MAP;
    }

    @Override
    public String objectiveDisplayName() {
        return "25 objectifs à compléter";
    }

    public List<BingoObjective> objectives() {
        return objectives;
    }

    public boolean isCompleted(Player player, int index) {
        return completed.getOrDefault(ownerKey(player), Set.of()).contains(index);
    }

    public int completedCount(Player player) {
        return completed.getOrDefault(ownerKey(player), Set.of()).size();
    }

    private BingoObjective randomItemObjective() {
        Material material = ConfiguredRandom.anySurvivalItem();
        return new BingoObjective(BingoObjectiveKind.ITEM, material, null);
    }

    private BingoObjective randomMobObjective() {
        EntityType entityType = ConfiguredRandom.anyMobTarget();
        return new BingoObjective(BingoObjectiveKind.MOB, null, entityType);
    }

    private void markItem(Player player, Material material) {
        for (int i = 0; i < objectives.size(); i++) {
            BingoObjective objective = objectives.get(i);
            if (objective.kind() == BingoObjectiveKind.ITEM && objective.item() == material) {
                markCompleted(player, i);
            }
        }
    }

    private void markCompleted(Player player, int index) {
        String ownerKey = ownerKey(player);
        Set<Integer> ownerCompleted = completed.computeIfAbsent(ownerKey, ignored -> new HashSet<>());
        if (!ownerCompleted.add(index)) {
            return;
        }

        plugin.playerManager().get(player).ifPresent(reloadPlayer -> reloadPlayer.statistics().markObjectiveCompleted());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.65F);
        player.sendMessage(plugin.uiManager().prefixed("<green>Bingo :</green> <white>" + objectives.get(index).displayName() + "</white> <gray>(" + ownerCompleted.size() + "/25)</gray>"));
        refreshOpenBoards();

        if (ownerCompleted.size() >= 25) {
            complete(player);
        }
    }

    private void refreshOpenBoards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof fr.victin.reloadchallenges.ui.ReloadGuiHolder holder
                && holder.type() == fr.victin.reloadchallenges.ui.GuiType.BINGO_BOARD) {
                plugin.uiManager().openBingoBoard(player, this);
            }
        }
    }

    private String ownerKey(Player player) {
        if (mode() == ChallengeMode.TEAMS) {
            ReloadPlayer reloadPlayer = plugin.playerManager().get(player).orElse(null);
            if (reloadPlayer != null && reloadPlayer.team() != null) {
                return "team:" + reloadPlayer.team().id();
            }
        }
        return "player:" + player.getUniqueId();
    }

    private void giveBoard(Player player) {
        if (!isBoardItem(player.getInventory().getItem(8))) {
            player.getInventory().setItem(8, boardItem());
        }
    }

    private ItemStack boardItem() {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.uiManager().mm("<gradient:#20f2ff:#ff4fd8><bold>Grille Bingo</bold></gradient>"));
            meta.lore(List.of(
                plugin.uiManager().mm("<gray>Affiche la grille 5x5.</gray>"),
                plugin.uiManager().mm("<yellow>➜ Clic droit : ouvrir</yellow>")
            ));
            meta.addItemFlags(ItemFlag.values());
            meta.getPersistentDataContainer().set(boardKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isBoardItem(ItemStack item) {
        return item != null
            && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(boardKey, PersistentDataType.BYTE);
    }

    public enum BingoObjectiveKind {
        ITEM,
        MOB
    }

    public record BingoObjective(BingoObjectiveKind kind, Material item, EntityType entityType) {
        public String displayName() {
            return kind == BingoObjectiveKind.ITEM ? Formatters.material(item) : Formatters.title(entityType.name());
        }

        public String kindLabel() {
            return kind == BingoObjectiveKind.ITEM ? "Item à récupérer" : "Mob à tuer";
        }

        public Material icon() {
            if (kind == BingoObjectiveKind.ITEM) {
                return item;
            }
            Material spawnEgg = Material.matchMaterial(entityType.name() + "_SPAWN_EGG");
            return spawnEgg == null ? Material.CROSSBOW : spawnEgg;
        }

        public String uniqueKey() {
            return kind.name() + ":" + (kind == BingoObjectiveKind.ITEM ? item.name() : entityType.name());
        }
    }
}
