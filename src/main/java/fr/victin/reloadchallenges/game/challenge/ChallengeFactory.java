package fr.victin.reloadchallenges.game.challenge;

import fr.victin.reloadchallenges.ReloadChallengesPlugin;
import fr.victin.reloadchallenges.game.ChallengeMode;
import fr.victin.reloadchallenges.game.ChallengeType;

public final class ChallengeFactory {
    private ChallengeFactory() {
    }

    public static ChallengeGame create(ReloadChallengesPlugin plugin, ChallengeType type, ChallengeMode mode) {
        return switch (type) {
            case FIND -> new FindChallenge(plugin, mode);
            case WHERE_BLOCK -> new WhereBlockChallenge(plugin, mode);
            case CRAFT -> new CraftChallenge(plugin, mode);
            case SPEEDRUN -> new SpeedrunChallenge(plugin, mode);
            case WHERE_BIOME -> new BiomeChallenge(plugin, mode);
            case MOB_HUNT -> new MobHuntChallenge(plugin, mode);
            case BINGO -> new BingoChallenge(plugin, mode);
            case RANDOM -> throw new IllegalArgumentException("RANDOM must be resolved before creating a challenge");
        };
    }
}
