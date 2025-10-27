package xyz.tolite.greatTitle.model;

import java.util.List;

public class Reward {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final int requiredTitles;
    private final List<String> commands;
    private final String permission;
    private final boolean repeatable;

    public Reward(String id, String displayName, List<String> description,
                  int requiredTitles, List<String> commands, String permission,
                  boolean repeatable) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.requiredTitles = requiredTitles;
        this.commands = commands;
        this.permission = permission;
        this.repeatable = repeatable;
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getDescription() { return description; }
    public int getRequiredTitles() { return requiredTitles; }
    public List<String> getCommands() { return commands; }
    public String getPermission() { return permission; }
    public boolean isRepeatable() { return repeatable; }
}