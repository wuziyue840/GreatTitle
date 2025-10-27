package xyz.tolite.greatTitle.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.model.Title;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 基于 UserPrefix 原理的计分板显示系统
 * 核心：权限驱动 + 计分板前缀 + 实时监听
 */
public class ScoreboardManager {

    private final GreatTitle plugin;
    private Scoreboard mainScoreboard;
    private final Map<UUID, Team> playerTeams;
    private boolean debugEnabled;

    public ScoreboardManager(GreatTitle plugin) {
        this.plugin = plugin;
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        this.playerTeams = new HashMap<>();
        this.debugEnabled = plugin.getConfig().getBoolean("debug-enabled", false);

        debug("计分板管理器已初始化");
    }

    /**
     * 更新玩家所有显示（头顶、Tab列表、聊天）
     */
    public void updatePlayerDisplay(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        String activeTitleId = plugin.getTitleManager().getActiveTitle(player);
        Title title = activeTitleId != null ? plugin.getTitleManager().getTitle(activeTitleId) : null;

        // 获取格式化后的称号内容
        String titleContent = title != null ? title.getFormattedContent() : "";

        // 清理内容
        titleContent = cleanTitleContent(titleContent);

        debug("更新玩家 " + player.getName() + " 的显示 - 称号: " + titleContent);

        // 更新头顶显示
        updateHeadDisplay(player, titleContent);

        // 更新Tab列表显示
        updateTabDisplay(player, titleContent);

        // 更新聊天显示（通过事件监听器处理）
    }

    /**
     * UserPrefix 核心方法：更新头顶显示（计分板前缀方式）
     */
    private void updateHeadDisplay(Player player, String titleContent) {
        // 检查是否启用头顶显示
        if (!plugin.getTitleManager().isHeadDisplayEnabled(player) ||
                !plugin.getNameConfig().isHeadPrefixEnabled()) {
            removePlayerTeam(player);
            debug("玩家 " + player.getName() + " 的头顶显示已禁用");
            return;
        }

        String displayMode = plugin.getNameConfig().getHeadDisplayMode();
        if ("above_name".equals(displayMode)) {
            // 使用计分板团队前缀
            updateTeamPrefix(player, titleContent);
        } else {
            // 内联方式：直接修改显示名称
            updateInlineDisplay(player, titleContent);
        }
    }

    /**
     * UserPrefix 核心：使用计分板团队前缀（最可靠的方式）
     */
    private void updateTeamPrefix(Player player, String titleContent) {
        try {
            // 使用唯一团队名，避免与其他插件冲突
            String teamName = "GT_" + player.getUniqueId().toString().substring(0, 8);

            // 移除旧的团队
            removePlayerTeam(player);

            // 创建或获取团队
            Team team = mainScoreboard.getTeam(teamName);
            if (team == null) {
                team = mainScoreboard.registerNewTeam(teamName);
                debug("为玩家 " + player.getName() + " 创建新团队: " + teamName);
            }

            // UserPrefix 关键设置：确保可见性和避免冲突
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            team.setCanSeeFriendlyInvisibles(false);

            // 设置前缀（UserPrefix 核心）
            String prefix = formatDisplayText(titleContent, 64);
            team.setPrefix(prefix);

            // 添加玩家到团队（UserPrefix 实时更新关键）
            team.addEntry(player.getName());
            playerTeams.put(player.getUniqueId(), team);

            debug("为玩家 " + player.getName() + " 设置团队前缀: " + prefix);

        } catch (Exception e) {
            plugin.getLogger().severe("设置玩家团队前缀失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 内联显示方式
     */
    private void updateInlineDisplay(Player player, String titleContent) {
        removePlayerTeam(player);

        String format = plugin.getNameConfig().getHeadPrefixFormat();
        String displayName = format.replace("%greattitle_active%", titleContent)
                .replace("%player_name%", player.getName());

        displayName = formatDisplayText(displayName, 32);

        player.setDisplayName(displayName);
        debug("为玩家 " + player.getName() + " 设置内联显示: " + displayName);
    }

    /**
     * 更新Tab列表显示
     */
    private void updateTabDisplay(Player player, String titleContent) {
        if (!plugin.getTitleManager().isTabListDisplayEnabled(player) ||
                !plugin.getNameConfig().isTabListDisplayEnabled()) {
            player.setPlayerListName(player.getName());
            return;
        }

        String format = plugin.getNameConfig().getTabListDisplayFormat();
        String tabName = format.replace("%greattitle_active%", titleContent)
                .replace("%player_name%", player.getName());

        tabName = formatDisplayText(tabName, 16);

        player.setPlayerListName(tabName);
        debug("为玩家 " + player.getName() + " 设置Tab显示: " + tabName);
    }

    /**
     * 移除玩家团队（UserPrefix 清理机制）
     */
    private void removePlayerTeam(Player player) {
        try {
            Team team = playerTeams.get(player.getUniqueId());
            if (team != null) {
                team.removeEntry(player.getName());
                if (team.getEntries().isEmpty()) {
                    team.unregister();
                }
                playerTeams.remove(player.getUniqueId());
                debug("移除玩家 " + player.getName() + " 的团队");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("移除玩家团队失败: " + e.getMessage());
        }
    }

    /**
     * 格式化显示文本（UserPrefix 兼容性处理）
     */
    private String formatDisplayText(String text, int maxLength) {
        if (text == null) return "";

        // 应用颜色代码
        text = text.replace('&', '§');

        // 清理特殊字符
        text = cleanTitleContent(text);

        // 长度限制
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength);
        }

        return text;
    }

    /**
     * 清理称号内容
     */
    private String cleanTitleContent(String content) {
        if (content == null) return "";
        return content.replace("<", "").replace(">", "").trim();
    }

    /**
     * 实时监听：处理玩家退出
     */
    public void onPlayerQuit(Player player) {
        removePlayerTeam(player);
        debug("玩家 " + player.getName() + " 退出，清理显示设置");
    }

    /**
     * 重新加载所有玩家显示（UserPrefix 热重载）
     */
    public void reloadAllPlayers() {
        debug("开始重新加载所有玩家的显示");
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplay(player);
        }
        debug("所有玩家的显示已重新加载完成");
    }

    /**
     * 设置调试模式
     */
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
        debug("计分板管理器调试模式: " + (enabled ? "开启" : "关闭"));
    }

    /**
     * 调试输出
     */
    private void debug(String message) {
        if (debugEnabled) {
            plugin.getLogger().info("[计分板调试] " + message);
        }
    }
}