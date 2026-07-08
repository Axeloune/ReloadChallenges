package fr.victin.reloadchallenges.player;

public final class ReloadStatistics {
    private long startedAtMillis;
    private long endedAtMillis;
    private long mainObjectiveMillis;
    private int objectivesCompleted;
    private int failedAttempts;
    private int kills;
    private int deaths;

    public void reset() {
        startedAtMillis = 0L;
        endedAtMillis = 0L;
        mainObjectiveMillis = 0L;
        objectivesCompleted = 0;
        failedAttempts = 0;
        kills = 0;
        deaths = 0;
    }

    public void markGameStart() {
        startedAtMillis = System.currentTimeMillis();
        endedAtMillis = 0L;
        mainObjectiveMillis = 0L;
    }

    public void markGameEnd() {
        if (endedAtMillis == 0L) {
            endedAtMillis = System.currentTimeMillis();
        }
    }

    public void markObjectiveCompleted() {
        objectivesCompleted++;
        if (mainObjectiveMillis == 0L && startedAtMillis > 0L) {
            mainObjectiveMillis = System.currentTimeMillis() - startedAtMillis;
        }
    }

    public void addFailedAttempt() {
        failedAttempts++;
    }

    public void addKill() {
        kills++;
    }

    public void addDeath() {
        deaths++;
    }

    public long totalPlayTimeMillis() {
        if (startedAtMillis == 0L) {
            return 0L;
        }
        long end = endedAtMillis == 0L ? System.currentTimeMillis() : endedAtMillis;
        return Math.max(0L, end - startedAtMillis);
    }

    public long mainObjectiveMillis() {
        return mainObjectiveMillis;
    }

    public int objectivesCompleted() {
        return objectivesCompleted;
    }

    public int failedAttempts() {
        return failedAttempts;
    }

    public int kills() {
        return kills;
    }

    public int deaths() {
        return deaths;
    }
}
