package fr.victin.reloadchallenges.team;

import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class ReloadTeam {
    private final String id;
    private final String displayName;
    private final ChatColor color;
    private final Set<UUID> members = new LinkedHashSet<>();

    public ReloadTeam(String id, String displayName, ChatColor color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public ChatColor color() {
        return color;
    }

    public Set<UUID> members() {
        return Collections.unmodifiableSet(members);
    }

    public void add(UUID uuid) {
        members.add(uuid);
    }

    public void clear() {
        members.clear();
    }
}
