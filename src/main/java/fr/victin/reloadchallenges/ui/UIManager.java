package fr.victin.reloadchallenges.ui;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;
import fr.victin.reloadchallenges.game.GameState;
import fr.victin.reloadchallenges.game.challenge.BingoChallenge;
import fr.victin.reloadchallenges.game.challenge.ChallengeGame;
import fr.victin.reloadchallenges.player.PlayerState;
import fr.victin.reloadchallenges.player.ReloadPlayer;
import fr.victin.reloadchallenges.team.ReloadTeam;
import fr.victin.reloadchallenges.util.Formatters;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class UIManager {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter SCOREBOARD_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReloadChallengesPlugin plugin;
    private final NamespacedKey hostToolKey;
    private BossBar objectiveBar;
    private BossBar endingBar;
    private BossBar preparationBar;
    private final Map<UUID, BossBar> bingoProgressBars = new HashMap<>();
    private BukkitTask rouletteTask;
    private final List<ItemDisplay> objectiveDisplays = new ArrayList<>();

    public UIManager(ReloadChallengesPlugin plugin) {
        this.plugin = plugin;
        this.hostToolKey = new NamespacedKey(plugin, "host_menu_tool");
    }

    public Component mm(String input) {
        return MINI.deserialize(input);
    }

    public Component prefixed(String input) {
        return mm(plugin.getConfig().getString("prefix", "") + input);
    }

    public void broadcast(String miniMessage) {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(prefixed(miniMessage)));
    }

    public void openHostConfig(Player player) {
        playMenuOpen(player);
        Inventory inventory = Bukkit.createInventory(new ReloadGuiHolder(GuiType.HOST_CONFIG), 45, mm("<gradient:#20f2ff:#ff4fd8><bold>Reload Challenges</bold></gradient>"));
        ChallengeType selected = plugin.gameManager().selectedType();
        ChallengeMode mode = plugin.gameManager().mode();

        inventory.setItem(4, item(Material.NETHER_STAR, "<gradient:#20f2ff:#ff4fd8><bold>Panneau de l'hôte</bold></gradient>", List.of(
            "<gray>Statut :</gray> <white>" + stateLabel(plugin.gameManager().state()) + "</white>",
            "<gray>Joueurs :</gray> <aqua>" + Bukkit.getOnlinePlayers().size() + "</aqua>",
            "<dark_gray>" + currentDate() + "</dark_gray>"
        )));
        inventory.setItem(19, item(selected.icon(), "<aqua><bold>Mini-jeu</bold></aqua>", List.of(
            "<gray>Actuel :</gray> <white>" + selected.displayName() + "</white>",
            selected == ChallengeType.RANDOM ? "<dark_gray>Un challenge sera tiré au lancement.</dark_gray>" : "<dark_gray>Ce challenge sera joué à la prochaine manche.</dark_gray>",
            "",
            "<yellow>➜ Clic gauche : ouvrir la sélection</yellow>"
        )));
        inventory.setItem(21, item(mode == ChallengeMode.FFA ? Material.PLAYER_HEAD : Material.SHIELD, "<yellow><bold>Mode</bold></yellow>", List.of(
            "<gray>Actuel :</gray> <white>" + mode.displayName() + "</white>",
            mode == ChallengeMode.FFA ? "<dark_gray>Chaque joueur joue pour lui.</dark_gray>" : "<dark_gray>Les joueurs sont répartis en équipes.</dark_gray>",
            "",
            "<yellow>➜ Clic gauche : changer le format</yellow>"
        )));
        inventory.setItem(23, item(Material.LIME_DYE, "<green><bold>Lancer la manche</bold></green>", List.of(
            "<gray>Crée et précharge le prochain monde.</gray>",
            "<gray>La téléportation arrive uniquement quand le spawn est prêt.</gray>",
            "",
            "<green>➜ Clic gauche : démarrer</green>"
        )));
        inventory.setItem(25, item(Material.RED_DYE, "<red><bold>Arrêter</bold></red>", List.of(
            "<gray>Annule la préparation ou termine la manche en cours.</gray>",
            "",
            "<red>➜ Clic gauche : arrêter</red>"
        )));
        inventory.setItem(31, item(Material.WHITE_BANNER, "<light_purple><bold>Équipes</bold></light_purple>", List.of(
            "<gray>Affiche la répartition actuelle.</gray>",
            "<gray>Permet aussi de passer en mode équipes.</gray>",
            "",
            "<light_purple>➜ Clic gauche : ouvrir les équipes</light_purple>"
        )));
        inventory.setItem(40, item(Material.COMPARATOR, "<gold><bold>Recharger</bold></gold>", List.of(
            "<gray>Recharge config.yml, la whitelist et les paramètres.</gray>",
            "",
            "<gold>➜ Clic gauche : recharger</gold>"
        )));

        decorateCorners(inventory);
        player.openInventory(inventory);
    }

    public void openGameSelect(Player player) {
        playMenuOpen(player);
        Inventory inventory = Bukkit.createInventory(new ReloadGuiHolder(GuiType.GAME_SELECT), 54, mm("<aqua><bold>Choisir un mini-jeu</bold>"));
        inventory.setItem(10, item(ChallengeType.RANDOM.icon(), "<gradient:#20f2ff:#ff4fd8><bold>" + ChallengeType.RANDOM.displayName() + "</bold></gradient>", gameLore(ChallengeType.RANDOM)));
        inventory.setItem(19, item(ChallengeType.FIND.icon(), "<aqua><bold>" + ChallengeType.FIND.displayName() + "</bold></aqua>", gameLore(ChallengeType.FIND)));
        inventory.setItem(20, item(ChallengeType.WHERE_BLOCK.icon(), "<aqua><bold>" + ChallengeType.WHERE_BLOCK.displayName() + "</bold></aqua>", gameLore(ChallengeType.WHERE_BLOCK)));
        inventory.setItem(21, item(ChallengeType.CRAFT.icon(), "<aqua><bold>" + ChallengeType.CRAFT.displayName() + "</bold></aqua>", gameLore(ChallengeType.CRAFT)));
        inventory.setItem(22, item(ChallengeType.SPEEDRUN.icon(), "<aqua><bold>" + ChallengeType.SPEEDRUN.displayName() + "</bold></aqua>", gameLore(ChallengeType.SPEEDRUN)));
        inventory.setItem(23, item(ChallengeType.WHERE_BIOME.icon(), "<aqua><bold>" + ChallengeType.WHERE_BIOME.displayName() + "</bold></aqua>", gameLore(ChallengeType.WHERE_BIOME)));
        inventory.setItem(24, item(ChallengeType.MOB_HUNT.icon(), "<aqua><bold>" + ChallengeType.MOB_HUNT.displayName() + "</bold></aqua>", gameLore(ChallengeType.MOB_HUNT)));
        inventory.setItem(25, item(ChallengeType.BINGO.icon(), "<aqua><bold>" + ChallengeType.BINGO.displayName() + "</bold></aqua>", gameLore(ChallengeType.BINGO)));
        inventory.setItem(37, item(Material.BOOK, "<yellow><bold>Mini-jeux disponibles</bold></yellow>", List.of(
            "<gray>Nombre :</gray> <white>8</white>",
            "<gray>Le mode aléatoire tire parmi tous les jeux.</gray>",
            "<gray>Bingo est une grille complète 5x5.</gray>"
        )));
        inventory.setItem(40, item(Material.ARROW, "<gray><bold>Retour</bold></gray>", List.of("<yellow>➜ Retour au panneau de l'hôte</yellow>")));
        decorateCorners(inventory);
        player.openInventory(inventory);
    }

    public void openModeSelect(Player player) {
        playMenuOpen(player);
        Inventory inventory = Bukkit.createInventory(new ReloadGuiHolder(GuiType.MODE_SELECT), 45, mm("<yellow><bold>Choisir le mode</bold>"));
        inventory.setItem(20, item(Material.PLAYER_HEAD, "<aqua><bold>FFA</bold></aqua>", List.of(
            "<gray>Tous les joueurs jouent en solo.</gray>",
            "<white>Le premier qui valide l'objectif gagne.</white>",
            "",
            "<yellow>➜ Clic gauche : choisir FFA</yellow>"
        )));
        inventory.setItem(24, item(Material.SHIELD, "<gold><bold>Équipes</bold></gold>", List.of(
            "<gray>Les joueurs sont répartis automatiquement.</gray>",
            "<white>La première équipe qui valide gagne.</white>",
            "",
            "<yellow>➜ Clic gauche : choisir équipes</yellow>"
        )));
        inventory.setItem(40, item(Material.ARROW, "<gray><bold>Retour</bold></gray>", List.of("<yellow>➜ Retour au panneau de l'hôte</yellow>")));
        decorateCorners(inventory);
        player.openInventory(inventory);
    }

    public void openTeamSelect(Player player) {
        playMenuOpen(player);
        Inventory inventory = Bukkit.createInventory(new ReloadGuiHolder(GuiType.TEAM_SELECT), 45, mm("<light_purple><bold>Équipes</bold>"));
        inventory.setItem(13, item(Material.WHITE_BANNER, "<light_purple><bold>Mode équipes</bold></light_purple>", List.of(
            "<gray>Les équipes sont équilibrées automatiquement</gray>",
            "<gray>au lancement de la manche.</gray>",
            "",
            "<yellow>➜ Clic gauche : utiliser le mode équipes</yellow>"
        )));
        inventory.setItem(20, item(Material.RED_BANNER, "<red><bold>Équipe Rouge</bold></red>", List.of(
            "<gray>Nom :</gray> <white>" + plugin.teamManager().teams().get(0).displayName() + "</white>",
            "<gray>Membres :</gray> <red>" + plugin.teamManager().teams().get(0).members().size() + "</red>",
            "",
            "<dark_gray>Répartition automatique au lancement.</dark_gray>"
        )));
        inventory.setItem(24, item(Material.BLUE_BANNER, "<aqua><bold>Équipe Bleue</bold></aqua>", List.of(
            "<gray>Nom :</gray> <white>" + plugin.teamManager().teams().get(1).displayName() + "</white>",
            "<gray>Membres :</gray> <aqua>" + plugin.teamManager().teams().get(1).members().size() + "</aqua>",
            "",
            "<dark_gray>Répartition automatique au lancement.</dark_gray>"
        )));
        inventory.setItem(40, item(Material.ARROW, "<gray><bold>Retour</bold></gray>", List.of("<yellow>➜ Retour au panneau de l'hôte</yellow>")));
        decorateCorners(inventory);
        player.openInventory(inventory);
    }

    public void openBingoBoard(Player player, BingoChallenge bingoChallenge) {
        Inventory inventory = Bukkit.createInventory(new ReloadGuiHolder(GuiType.BINGO_BOARD), 54, mm("<gradient:#20f2ff:#ff4fd8><bold>Bingo</bold></gradient> <gray>5x5</gray>"));
        int[] slots = bingoSlots();
        for (int i = 0; i < slots.length; i++) {
            inventory.setItem(slots[i], bingoObjectiveItem(player, bingoChallenge, i));
        }
        inventory.setItem(52, item(Material.BOOK, "<aqua><bold>Progression</bold></aqua>", List.of(
            "<gray>Objectifs validés :</gray> <white>" + bingoChallenge.completedCount(player) + "/25</white>",
            "<gray>Mode :</gray> <yellow>" + bingoChallenge.mode().displayName() + "</yellow>",
            "",
            "<dark_gray>Complète toute la grille pour gagner.</dark_gray>"
        )));
        decorateCorners(inventory);
        player.openInventory(inventory);
        playMenuOpen(player);
    }

    public void showObjectiveBar(ChallengeGame game) {
        hideObjectiveBar();
        if (game.type() != ChallengeType.SPEEDRUN) {
            return;
        }
        objectiveBar = BossBar.bossBar(
            mm("<gradient:#20f2ff:#ff4fd8>Objectif</gradient> <white>" + game.objectiveDisplayName() + "</white>"),
            1.0F,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
        );
        Bukkit.getOnlinePlayers().forEach(player -> player.showBossBar(objectiveBar));
    }

    public void updateObjectiveBar(String miniMessage, float progress, BossBar.Color color) {
        if (objectiveBar == null) {
            return;
        }
        objectiveBar.name(mm(miniMessage));
        objectiveBar.progress(1.0F);
        objectiveBar.color(color);
    }

    public void showPreparationBar(ChallengeType type, int totalChunks) {
        hidePreparationBar();
        preparationBar = BossBar.bossBar(
            mm("<aqua><bold>Préparation du monde</bold></aqua> <gray>0/" + totalChunks + " chunks</gray>"),
            0.0F,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(preparationBar);
            player.sendActionBar(mm("<aqua>Génération de la prochaine arène...</aqua> <gray>Vous restez au lobby.</gray>"));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.45F, 1.2F);
        }
        broadcast("<aqua>Préparation du monde pour</aqua> <white>" + type.displayName() + "</white><aqua>...</aqua>");
    }

    public void updatePreparationBar(int loadedChunks, int totalChunks) {
        if (preparationBar == null) {
            return;
        }
        float progress = Math.max(0.0F, Math.min(1.0F, loadedChunks / (float) Math.max(1, totalChunks)));
        preparationBar.progress(progress);
        preparationBar.name(mm("<aqua><bold>Préparation du monde</bold></aqua> <white>" + loadedChunks + "/" + totalChunks + "</white> <gray>chunks</gray>"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(mm("<aqua>Préchargement</aqua> <white>" + Math.round(progress * 100.0F) + "%</white> <dark_gray>|</dark_gray> <gray>Téléportation après génération.</gray>"));
        }
    }

    public void hidePreparationBar() {
        if (preparationBar == null) {
            return;
        }
        BossBar bar = preparationBar;
        Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bar));
        preparationBar = null;
    }

    public void hideObjectiveBar() {
        if (objectiveBar == null) {
            return;
        }
        BossBar bar = objectiveBar;
        Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bar));
        objectiveBar = null;
    }

    public void showBingoProgressBar(BingoChallenge bingoChallenge) {
        hideObjectiveBar();
        hideBingoProgressBars();
        updateBingoProgressBar(bingoChallenge);
    }

    public void updateBingoProgressBar(BingoChallenge bingoChallenge) {
        for (ReloadPlayer reloadPlayer : plugin.playerManager().gamePlayers()) {
            Player player = reloadPlayer.bukkitPlayer().orElse(null);
            if (player == null) {
                continue;
            }
            int completed = bingoChallenge.completedCount(player);
            float progress = Math.max(0.0F, Math.min(1.0F, completed / 25.0F));
            BossBar bar = bingoProgressBars.computeIfAbsent(player.getUniqueId(), ignored -> {
                BossBar created = BossBar.bossBar(Component.empty(), 0.0F, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
                player.showBossBar(created);
                return created;
            });
            bar.name(mm("<gradient:#20f2ff:#ff4fd8><bold>Bingo</bold></gradient> <white>" + completed + "/25</white> <gray>|</gray> <aqua>/bingo</aqua> <gray>grille</gray>"));
            bar.progress(progress);
            bar.color(completed >= 25 ? BossBar.Color.YELLOW : BossBar.Color.GREEN);
        }
    }

    public void hideBingoProgressBars() {
        if (bingoProgressBars.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, BossBar> entry : bingoProgressBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        bingoProgressBars.clear();
    }

    public void playStartAnimation(ChallengeGame game) {
        cancelRoulette();
        clearObjectiveDisplays();
        Inventory roulette = Bukkit.createInventory(new ReloadGuiHolder(GuiType.ROULETTE), 27, mm("<gold><bold>Sélection de l'objectif</bold>"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.openInventory(roulette);
            spawnObjectiveDisplay(player, game.icon());
        }

        List<Material> icons = new ArrayList<>();
        for (ChallengeType type : ChallengeType.values()) {
            icons.add(type.icon());
        }
        icons.add(game.icon());

        rouletteTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int ticks;

            @Override
            public void run() {
                ticks++;
                Material icon = icons.get(ticks % icons.size());
                roulette.setItem(13, item(icon, "<yellow><bold>Recherche...</bold></yellow>", List.of("<gray>La roulette choisit l'objectif.</gray>")));
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.55F, 0.8F + ticks * 0.04F);
                    player.spawnParticle(Particle.END_ROD, player.getLocation().add(0.0D, 1.4D, 0.0D), 2, 0.35D, 0.2D, 0.35D, 0.01D);
                }
                for (ItemDisplay display : objectiveDisplays) {
                    display.setItemStack(new ItemStack(icon));
                }
                if (ticks >= 24) {
                    cancelRoulette();
                    roulette.setItem(13, item(game.icon(), "<green><bold>" + game.objectiveDisplayName() + "</bold></green>", List.of("<gray>" + game.type().displayName() + "</gray>")));
                    for (ItemDisplay display : objectiveDisplays) {
                        display.setItemStack(new ItemStack(game.icon()));
                    }
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.closeInventory();
                        player.showTitle(Title.title(
                            mm("<gradient:#20f2ff:#ff4fd8><bold>" + game.type().displayName() + "</bold></gradient>"),
                            mm("<white>" + game.type().objectiveLabel() + " : </white><yellow>" + game.objectiveDisplayName() + "</yellow>"),
                            Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(600))
                        ));
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9F, 1.2F);
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, UIManager.this::clearObjectiveDisplays, 50L);
                }
            }
        }, 0L, 2L);
    }

    public void playVictory(Player winner, ReloadTeam winnerTeam) {
        hideObjectiveBar();
        hideBingoProgressBars();
        if (endingBar != null) {
            Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(endingBar));
        }
        String winnerName = winnerTeam != null ? winnerTeam.displayName() : winner == null ? "Personne" : winner.getName();
        endingBar = BossBar.bossBar(mm("<gold><bold>Partie terminée</bold></gold> <white>Victoire : " + winnerName + "</white>"), 1.0F, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(endingBar);
            player.showTitle(Title.title(
                mm("<gold><bold>Victoire !</bold></gold>"),
                mm("<white>" + winnerName + "</white>"),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(800))
            ));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
        }
        if (winner != null) {
            launchFireworks(winner);
        }
    }

    public void updateAllScoreboards() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            updateScoreboard(player);
            updatePlayerList(player);
        });
    }

    public void updateScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("reload", "dummy", ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Reload" + ChatColor.BLUE + " Board");
        objective.displayName(mm("<gradient:#20f2ff:#ff4fd8><bold>Reload Challenges</bold></gradient>"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.DARK_GRAY + " ");
        if (plugin.gameManager().state() == GameState.RUNNING && plugin.gameManager().currentGame() != null) {
            ChallengeGame game = plugin.gameManager().currentGame();
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Statut: " + ChatColor.GREEN + stateLabel(plugin.gameManager().state()));
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Jeu: " + ChatColor.AQUA + trimScoreboard(game.type().displayName()));
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Mode: " + ChatColor.YELLOW + plugin.gameManager().mode().displayName());
            lines.add(ChatColor.DARK_GRAY + " ");
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Objectif:");
            lines.add(ChatColor.GOLD + "  " + trimScoreboard(game.objectiveDisplayName()));
            lines.add(ChatColor.DARK_GRAY + "  ");
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Temps: " + ChatColor.GREEN + Formatters.duration(plugin.gameManager().elapsedMillis()));
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Joueurs: " + ChatColor.AQUA + plugin.playerManager().gamePlayers().size());
            if (plugin.gameManager().mode() == ChallengeMode.TEAMS) {
                for (ReloadTeam team : plugin.teamManager().teams()) {
                    lines.add(ChatColor.DARK_GRAY + "• " + team.color() + team.displayName() + ChatColor.WHITE + ": " + team.members().size());
                }
            }
        } else {
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Statut: " + ChatColor.GOLD + stateLabel(plugin.gameManager().state()));
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Hôte: " + ChatColor.RED + plugin.playerManager().host().map(ReloadPlayer::name).orElse("Aucun"));
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Jeu: " + ChatColor.AQUA + trimScoreboard(plugin.gameManager().selectedType().displayName()));
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Mode: " + ChatColor.YELLOW + plugin.gameManager().mode().displayName());
            lines.add(ChatColor.DARK_GRAY + " ");
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Joueurs: " + ChatColor.GREEN + Bukkit.getOnlinePlayers().size());
            lines.add(ChatColor.DARK_GRAY + "• " + ChatColor.WHITE + "Whitelist: " + (Bukkit.hasWhitelist() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        }
        lines.add(ChatColor.DARK_GRAY + "  ");
        lines.add(ChatColor.GOLD + currentDate());

        int score = lines.size();
        for (String line : lines) {
            objective.getScore(uniqueLine(line, score)).setScore(score--);
        }
        applyNametags(board);
        player.setScoreboard(board);
    }

    public void updatePlayerList(Player player) {
        ChallengeGame game = plugin.gameManager().currentGame();
        boolean revealObjective = plugin.gameManager().state() == GameState.RUNNING && game != null;
        String challenge = revealObjective ? game.type().displayName() : plugin.gameManager().selectedType().displayName();
        String objective = revealObjective ? game.objectiveDisplayName() : "Révélé au lancement";
        player.sendPlayerListHeaderAndFooter(
            mm("<gradient:#20f2ff:#ff4fd8><bold>Reload Challenges</bold></gradient>\n<gray>" + currentDate() + "</gray>"),
            mm("<gray>Statut:</gray> <aqua>" + stateLabel(plugin.gameManager().state()) + "</aqua> <white>|</white> <gray>Jeu:</gray> <white>" + challenge + "</white>\n<gray>Objectif:</gray> <yellow>" + objective + "</yellow>")
        );
        player.playerListName(mm(playerListName(player)));
    }

    public void resetScoreboards() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Bukkit.getOnlinePlayers().forEach(player -> player.setScoreboard(main));
    }

    public void shutdown() {
        cancelRoulette();
        hideObjectiveBar();
        hideBingoProgressBars();
        hidePreparationBar();
        if (endingBar != null) {
            BossBar bar = endingBar;
            Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bar));
            endingBar = null;
        }
        resetScoreboards();
    }

    private void launchFireworks(Player winner) {
        for (int i = 0; i < 4; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Firework firework = winner.getWorld().spawn(winner.getLocation(), Firework.class);
                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                    .withColor(Color.AQUA, Color.FUCHSIA, Color.YELLOW)
                    .withFade(Color.WHITE)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .trail(true)
                    .flicker(true)
                    .build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
            }, i * 10L);
        }
    }

    private void cancelRoulette() {
        if (rouletteTask != null) {
            rouletteTask.cancel();
            rouletteTask = null;
        }
        clearObjectiveDisplays();
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm(name));
            meta.lore(lore.stream().map(this::mm).toList());
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack item(Material material, String name, List<String> lore, boolean glint) {
        ItemStack item = item(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(glint);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack hostMenuItem() {
        ItemStack item = item(Material.COMPARATOR, "<gradient:#20f2ff:#ff4fd8><bold>Menu hôte</bold></gradient>", List.of(
            "<gray>Ouvre le panneau de configuration.</gray>",
            "<gray>Indéplaçable et réservé à l'hôte.</gray>",
            "",
            "<yellow>➜ Clic droit : ouvrir</yellow>"
        ));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(hostToolKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isHostMenuItem(ItemStack item) {
        if (item == null || item.getType() != Material.COMPARATOR || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(hostToolKey, PersistentDataType.BYTE);
    }

    public void setupLobbyHotbar(Player player, boolean host) {
        player.getInventory().clear();
        if (host) {
            player.getInventory().setItem(4, hostMenuItem());
        }
    }

    public void playMenuOpen(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.65F, 1.35F);
    }

    public void playMenuClick(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7F, 1.45F);
    }

    public void playMenuBack(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 0.8F);
    }

    public void playMenuConfirm(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F);
    }

    public void playMenuDeny(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.7F);
    }

    private void decorateCorners(Inventory inventory) {
        ItemStack blue = item(Material.CYAN_STAINED_GLASS_PANE, "<aqua>.</aqua>", List.of());
        ItemStack purple = item(Material.PURPLE_STAINED_GLASS_PANE, "<light_purple>.</light_purple>", List.of());
        int rows = inventory.getSize() / 9;
        int lastRowStart = (rows - 1) * 9;
        int[] slots = {
            0, 1, 7, 8, 9, 17,
            lastRowStart - 9, lastRowStart - 1, lastRowStart, lastRowStart + 1, lastRowStart + 7, lastRowStart + 8
        };
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] >= 0 && slots[i] < inventory.getSize() && inventory.getItem(slots[i]) == null) {
                inventory.setItem(slots[i], i % 2 == 0 ? blue : purple);
            }
        }
    }

    private void spawnObjectiveDisplay(Player player, Material initialIcon) {
        Location location = player.getLocation().clone()
            .add(player.getLocation().getDirection().normalize().multiply(2.2D))
            .add(0.0D, 1.45D, 0.0D);
        ItemDisplay display = player.getWorld().spawn(location, ItemDisplay.class);
        display.setItemStack(new ItemStack(initialIcon));
        display.setBillboard(Display.Billboard.CENTER);
        display.setGlowing(true);
        display.setPersistent(false);
        objectiveDisplays.add(display);
    }

    private void clearObjectiveDisplays() {
        for (ItemDisplay display : objectiveDisplays) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        objectiveDisplays.clear();
    }

    private String uniqueLine(String line, int index) {
        ChatColor[] colors = ChatColor.values();
        return line + colors[Math.floorMod(index, colors.length)];
    }

    private String stateLabel(GameState state) {
        return switch (state) {
            case LOBBY -> "En attente...";
            case PREPARING -> "Génération...";
            case RUNNING -> "En cours";
            case ENDING -> "Fin...";
            case RESETTING -> "Reset...";
        };
    }

    private String trimScoreboard(String value) {
        if (value.length() <= 28) {
            return value;
        }
        return value.substring(0, 27) + ".";
    }

    private String currentDate() {
        return ZonedDateTime.now(PARIS_ZONE).format(SCOREBOARD_DATE);
    }

    private List<String> gameLore(ChallengeType type) {
        return switch (type) {
            case RANDOM -> List.of(
                "<gray>Laisse le plugin choisir le mini-jeu au lancement.</gray>",
                "<white>Parfait pour enchaîner les manches sans débat.</white>",
                "",
                "<yellow>➜ Clic gauche : jouer en aléatoire</yellow>"
            );
            case FIND -> List.of(
                "<gray>Un item ou bloc est tiré au hasard.</gray>",
                "<white>Le premier joueur qui l'obtient valide l'objectif.</white>",
                "",
                "<yellow>➜ Clic gauche : sélectionner</yellow>"
            );
            case WHERE_BLOCK -> List.of(
                "<gray>Un bloc cible est révélé.</gray>",
                "<white>Il faut le trouver dans le monde et cliquer dessus.</white>",
                "",
                "<yellow>➜ Clic gauche : sélectionner</yellow>"
            );
            case CRAFT -> List.of(
                "<gray>Un craft est choisi par la roulette.</gray>",
                "<white>Le premier craft réussi remporte la manche.</white>",
                "",
                "<yellow>➜ Clic gauche : sélectionner</yellow>"
            );
            case SPEEDRUN -> List.of(
                "<gray>Départ en monde normal.</gray>",
                "<white>L'objectif est de tuer l'Ender Dragon.</white>",
                "",
                "<yellow>➜ Clic gauche : sélectionner</yellow>"
            );
            case WHERE_BIOME -> List.of(
                "<gray>Un biome cible est choisi.</gray>",
                "<white>Il faut s'y rendre le plus vite possible.</white>",
                "",
                "<yellow>➜ Clic gauche : sélectionner</yellow>"
            );
            case MOB_HUNT -> List.of(
                "<gray>Une créature est tirée au hasard.</gray>",
                "<white>Le premier joueur qui tue ce mob valide la manche.</white>",
                "",
                "<yellow>➜ Clic gauche : sélectionner</yellow>"
            );
            case BINGO -> List.of(
                "<gray>Une grille de 25 objectifs est générée.</gray>",
                "<white>Récupère les items et tue les mobs indiqués.</white>",
                "<white>La première grille complète gagne.</white>",
                "",
                "<yellow>➜ Clic gauche : sélectionner</yellow>"
            );
        };
    }

    private ItemStack bingoObjectiveItem(Player player, BingoChallenge bingoChallenge, int index) {
        BingoChallenge.BingoObjective objective = bingoChallenge.objectives().get(index);
        boolean done = bingoChallenge.isCompleted(player, index);
        String status = done ? "<green>Validé</green>" : "<yellow>À faire</yellow>";
        return item(objective.icon(), (done ? "<green><bold>" : "<yellow><bold>") + objective.displayName() + "</bold>", List.of(
            "<gray>Type :</gray> <white>" + objective.kindLabel() + "</white>",
            "<gray>Statut :</gray> " + status,
            "",
            done ? "<green>Cette case est complétée.</green>" : "<gray>Objectif requis pour finir la grille.</gray>"
        ), done);
    }

    private int[] bingoSlots() {
        return new int[] {
            10, 11, 12, 13, 14,
            19, 20, 21, 22, 23,
            28, 29, 30, 31, 32,
            37, 38, 39, 40, 41,
            46, 47, 48, 49, 50
        };
    }

    private void applyNametags(Scoreboard board) {
        Team host = board.registerNewTeam("rc_host");
        host.setPrefix(ChatColor.RED + "★ " + ChatColor.RED);
        Team inGame = board.registerNewTeam("rc_ingame");
        inGame.setPrefix(ChatColor.AQUA + "◆ " + ChatColor.AQUA);
        Team spectator = board.registerNewTeam("rc_spec");
        spectator.setPrefix(ChatColor.GRAY + "◇ " + ChatColor.GRAY);

        for (Player online : Bukkit.getOnlinePlayers()) {
            ReloadPlayer reloadPlayer = plugin.playerManager().get(online).orElse(null);
            Team target = spectator;
            if (isActualHost(online)) {
                target = host;
            } else if (reloadPlayer != null && reloadPlayer.state() == PlayerState.IN_GAME) {
                target = inGame;
            }
            target.addEntry(online.getName());
        }
    }

    private String playerListName(Player player) {
        if (isActualHost(player)) {
            return "<red>★ " + player.getName() + "</red>";
        }
        ReloadPlayer reloadPlayer = plugin.playerManager().get(player).orElse(null);
        if (reloadPlayer != null && reloadPlayer.state() == PlayerState.IN_GAME) {
            return "<aqua>◆ " + player.getName() + "</aqua>";
        }
        return "<gray>◇ " + player.getName() + "</gray>";
    }

    private boolean isActualHost(Player player) {
        return plugin.playerManager().host()
            .map(host -> host.uuid().equals(player.getUniqueId()))
            .orElse(false);
    }
}
