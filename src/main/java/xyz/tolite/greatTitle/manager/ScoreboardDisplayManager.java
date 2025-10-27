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
 * 计分板显示管理器 - 基于 Minecraft 原生计分板系统
 * 参考 Minecraft Wiki 的计分板原理 [citation:3][citation:7]
 */
public class ScoreboardDisplayManager {

    private final GreatTitle plugin;
    private Scoreboard mainScoreboard;
    private final Map<UUID, Team> playerTeams;
    private boolean debugEnabled;

    public ScoreboardDisplayManager(GreatTitle plugin) {
        this.plugin = plugin;
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        this.playerTeams = new HashMap<>();
        this.debugEnabled = plugin.getConfig().getBoolean("debug-enabled", false);

        debug("计分板显示管理器已初始化");
    }

    /**
     * 更新玩家所有显示（头顶、Tab列表）
     */
    public void updatePlayerDisplay(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        // 检查显示设置
        if (!plugin.getTitleManager().isHeadDisplayEnabled(player) ||
                !plugin.getNameConfig().isHeadPrefixEnabled()) {
            removePlayerTeam(player);
            resetPlayerDisplay(player);
            debug("玩家 " + player.getName() + " 的显示已禁用");
            return;
        }

        String activeTitleId = plugin.getTitleManager().getActiveTitle(player);
        Title title = activeTitleId != null ? plugin.getTitleManager().getTitle(activeTitleId) : null;

        // 获取格式化后的称号内容
        String titleContent = title != null ? title.getFormattedContent() : "";

        // 清理内容
        titleContent = cleanContent(titleContent);

        debug("更新玩家 " + player.getName() + " 的显示 - 称号: " + titleContent);

        // 更新头顶显示（使用计分板团队）
        updateHeadDisplay(player, titleContent);

        // 更新Tab列表显示
        updateTabDisplay(player, titleContent);
    }

    /**
     * 更新头顶显示 - 使用计分板团队方式
     * 基于 Minecraft 计分板原理 [citation:3]
     */
    private void updateHeadDisplay(Player player, String titleContent) {
        try {
            // 使用唯一团队名，避免冲突
            String teamName = "GT_" + player.getUniqueId().toString().substring(0, 8);

            // 移除旧的团队
            removePlayerTeam(player);

            // 创建或获取团队
            Team team = mainScoreboard.getTeam(teamName);
            if (team == null) {
                team = mainScoreboard.registerNewTeam(teamName);
                debug("为玩家 " + player.getName() + " 创建新团队: " + teamName);
            }

            // 关键设置：基于 Minecraft 计分板团队选项
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            team.setCanSeeFriendlyInvisibles(false);

            // 获取显示模式
            String displayMode = plugin.getNameConfig().getHeadDisplayMode();
            
            // 根据显示模式设置前缀或后缀
            if ("above_name".equalsIgnoreCase(displayMode)) {
                // above_name模式：称号显示在名字上方
                team.setPrefix(formatText(titleContent + "\n", 64));
                team.setSuffix("");
                debug("为玩家 " + player.getName() + " 设置above_name模式，前缀: " + titleContent);
            } else {
                // 默认inline模式：称号显示在名字前面
                team.setPrefix(formatText(titleContent, 64));
                team.setSuffix("");
                debug("为玩家 " + player.getName() + " 设置inline模式，前缀: " + titleContent);
            }

            // 添加玩家到团队
            team.addEntry(player.getName());
            playerTeams.put(player.getUniqueId(), team);

            // 确保玩家显示名称正确
            player.setDisplayName(player.getName());

        } catch (Exception e) {
            plugin.getLogger().severe("设置玩家头顶显示失败: " + e.getMessage());
            e.printStackTrace();
        }
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

        tabName = formatText(tabName, 16);

        player.setPlayerListName(tabName);
        debug("为玩家 " + player.getName() + " 设置Tab显示: " + tabName);
    }

    /**
     * 移除玩家团队
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
     * 重置玩家显示
     */
    private void resetPlayerDisplay(Player player) {
        try {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            debug("重置玩家 " + player.getName() + " 的显示");
        } catch (Exception e) {
            plugin.getLogger().warning("重置玩家显示失败: " + e.getMessage());
        }
    }

    /**
     * 格式化文本
     */
    private String formatText(String text, int maxLength) {
        if (text == null) return "";

        // 应用颜色代码
        text = text.replace('&', '§');

        // 清理内容
        text = cleanContent(text);

        // 长度限制
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength);
        }

        return text;
    }

    /**
     * 清理内容
     */
    private String cleanContent(String content) {
        if (content == null) return "";
        return content.replace("<", "").replace(">", "").trim();
    }

    /**
     * 处理玩家退出
     */
    public void onPlayerQuit(Player player) {
        removePlayerTeam(player);
        resetPlayerDisplay(player);
        debug("玩家 " + player.getName() + " 退出，清理显示设置");
    }

    /**
     * 重新加载所有玩家显示
     */
    public void reloadAllPlayers() {
        debug("开始重新加载所有玩家的显示");
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplay(player);
        }
        debug("所有玩家的显示已重新加载完成");
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        debug("开始清理所有显示资源");
        for (Player player : Bukkit.getOnlinePlayers()) {
            removePlayerTeam(player);
            resetPlayerDisplay(player);
        }
        playerTeams.clear();
        debug("所有显示资源已清理完成");
    }

    /**
     * 设置调试模式
     */
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
        debug("计分板显示管理器调试模式: " + (enabled ? "开启" : "关闭"));
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