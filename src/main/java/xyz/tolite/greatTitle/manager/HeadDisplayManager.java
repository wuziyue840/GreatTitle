package xyz.tolite.greatTitle.manager;

import org.bukkit.entity.Player;
import xyz.tolite.greatTitle.GreatTitle;

/**
 * 头顶显示管理器 - 适配器层
 * 使用新的计分板显示管理器
 */
public class HeadDisplayManager {

    private final GreatTitle plugin;
    private final ScoreboardDisplayManager displayManager;

    public HeadDisplayManager(GreatTitle plugin) {
        this.plugin = plugin;
        this.displayManager = new ScoreboardDisplayManager(plugin);
    }

    /**
     * 更新玩家头顶显示
     */
    public void updatePlayerHeadDisplay(Player player) {
        displayManager.updatePlayerDisplay(player);
    }

    /**
     * 更新玩家Tab显示
     */
    public void updatePlayerTabDisplay(Player player) {
        displayManager.updatePlayerDisplay(player); // 统一处理
    }

    /**
     * 处理玩家退出
     */
    public void onPlayerQuit(Player player) {
        displayManager.onPlayerQuit(player);
    }

    /**
     * 重新加载所有玩家显示
     */
    public void reloadAllPlayers() {
        displayManager.reloadAllPlayers();
    }

    /**
     * 设置调试模式
     */
    public void setDebugEnabled(boolean enabled) {
        displayManager.setDebugEnabled(enabled);
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        displayManager.cleanup();
    }
}