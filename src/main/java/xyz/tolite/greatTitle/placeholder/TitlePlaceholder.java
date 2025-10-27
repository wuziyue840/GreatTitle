package xyz.tolite.greatTitle.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.manager.TitleManager;
import xyz.tolite.greatTitle.model.PlayerTitle;
import xyz.tolite.greatTitle.model.Title;

public class TitlePlaceholder extends PlaceholderExpansion {

    private final GreatTitle plugin;
    private final TitleManager titleManager;

    public TitlePlaceholder(GreatTitle plugin) {
        this.plugin = plugin;
        this.titleManager = plugin.getTitleManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "greattitle";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Tolite";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        String[] args = params.split("_");
        String mainParam = args[0].toLowerCase();

        switch (mainParam) {
            case "active":
                return handleActivePlaceholder(player, args);

            case "has":
                return handleHasPlaceholder(player, args);

            case "count":
                return handleCountPlaceholder(player, args);

            case "display":
                return handleDisplayPlaceholder(player, args);

            case "status":
                return handleStatusPlaceholder(player, args);

            default:
                return null;
        }
    }

    private String handleActivePlaceholder(Player player, String[] args) {
        String activeTitleId = titleManager.getActiveTitle(player);
        if (activeTitleId == null) return "";

        Title title = titleManager.getTitle(activeTitleId);
        if (title == null) return "";

        if (args.length == 1) {
            return title.getContent();
        }

        switch (args[1].toLowerCase()) {
            case "name":
                return title.getDisplayName();
            case "id":
                return title.getId();
            case "content":
                return title.getContent();
            case "raw": // 无颜色代码的内容
                return title.getContent().replace("§", "&");
            default:
                return title.getContent();
        }
    }

    private String handleHasPlaceholder(Player player, String[] args) {
        if (args.length < 2) return "false";

        if (args[1].equalsIgnoreCase("title")) {
            return titleManager.getActiveTitle(player) != null ? "true" : "false";
        }

        // 检查是否拥有特定称号
        String titleId = args[1];
        PlayerTitle playerTitle = titleManager.getPlayerTitle(player, titleId);

        if (playerTitle == null) return "false";

        if (args.length > 2 && args[2].equalsIgnoreCase("expired")) {
            return playerTitle.isExpired() ? "true" : "false";
        }

        return "true";
    }

    private String handleCountPlaceholder(Player player, String[] args) {
        int count = titleManager.getPlayerTitles(player).size();

        if (args.length > 1) {
            switch (args[1].toLowerCase()) {
                case "active":
                    return titleManager.getActiveTitle(player) != null ? "1" : "0";
                case "expired":
                    int expiredCount = 0;
                    for (PlayerTitle playerTitle : titleManager.getPlayerTitles(player).values()) {
                        if (playerTitle.isExpired()) {
                            expiredCount++;
                        }
                    }
                    return String.valueOf(expiredCount);
                case "permanent":
                    int permanentCount = 0;
                    for (PlayerTitle playerTitle : titleManager.getPlayerTitles(player).values()) {
                        if (playerTitle.isPermanent()) {
                            permanentCount++;
                        }
                    }
                    return String.valueOf(permanentCount);
            }
        }

        return String.valueOf(count);
    }

    private String handleDisplayPlaceholder(Player player, String[] args) {
        if (args.length < 2) return "";

        switch (args[1].toLowerCase()) {
            case "head":
                return String.valueOf(titleManager.isHeadDisplayEnabled(player));
            case "tab":
                return String.valueOf(titleManager.isTabListDisplayEnabled(player));
            case "chat":
                return String.valueOf(titleManager.isChatDisplayEnabled(player));
            default:
                return "";
        }
    }

    private String handleStatusPlaceholder(Player player, String[] args) {
        if (args.length < 2) return "";

        String titleId = args[1];
        PlayerTitle playerTitle = titleManager.getPlayerTitle(player, titleId);

        if (playerTitle == null) return "none";

        if (playerTitle.isExpired()) {
            return "expired";
        }

        String activeTitle = titleManager.getActiveTitle(player);
        if (titleId.equals(activeTitle)) {
            return "active";
        }

        return "inactive";
    }
}