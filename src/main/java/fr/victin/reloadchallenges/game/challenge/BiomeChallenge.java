package fr.victin.reloadchallenges.game.challenge;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;
import fr.victin.reloadchallenges.player.ReloadPlayer;
import fr.victin.reloadchallenges.util.Formatters;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

public final class BiomeChallenge extends ChallengeGame {
    private Biome target;

    public BiomeChallenge(ReloadChallengesPlugin plugin, ChallengeMode mode) {
        super(plugin, ChallengeType.WHERE_BIOME, mode);
    }

    @Override
    public void selectObjective() {
        target = ConfiguredRandom.anyBiome();
    }

    @Override
    public void tick(int elapsedSeconds) {
        if (target == null) {
            return;
        }
        float pulse = 0.35F + (float) ((Math.sin(elapsedSeconds / 3.0D) + 1.0D) / 2.0D) * 0.55F;
        plugin.uiManager().updateObjectiveBar("<green><bold>Biome cible</bold></green> <white>" + objectiveDisplayName() + "</white>", pulse, BossBar.Color.GREEN);
        for (ReloadPlayer reloadPlayer : plugin.playerManager().gamePlayers()) {
            Player player = reloadPlayer.bukkitPlayer().orElse(null);
            if (player == null) {
                continue;
            }
            Biome current = player.getLocation().getBlock().getBiome();
            player.sendActionBar(plugin.uiManager().mm("<gray>Biome actuel:</gray> <aqua>" + Formatters.biome(current) + "</aqua>"));
            if (current == target) {
                complete(player);
                return;
            }
        }
    }

    @Override
    public Material icon() {
        return Material.FILLED_MAP;
    }

    @Override
    public String objectiveDisplayName() {
        return target == null ? "Inconnu" : Formatters.biome(target);
    }
}
