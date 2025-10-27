package xyz.tolite.greatTitle.task;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.manager.TitleManager;

public class TitleExpireTask {

    private final GreatTitle plugin;
    private final TitleManager titleManager;
    private BukkitRunnable task;
    private final long CHECK_INTERVAL = 20L * 60 * 5; // 5分钟检查一次

    public TitleExpireTask(GreatTitle plugin) {
        this.plugin = plugin;
        this.titleManager = plugin.getTitleManager();
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                // 检查所有在线玩家的过期称号
                for (Player player : Bukkit.getOnlinePlayers()) {
                    titleManager.checkAndRemoveExpiredTitles(player);
                }

                plugin.getLogger().fine("称号过期检查完成");
            }
        };

        task.runTaskTimer(plugin, CHECK_INTERVAL, CHECK_INTERVAL);
        plugin.getLogger().info("称号过期检查任务已启动");
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            plugin.getLogger().info("称号过期检查任务已停止");
        }
    }
}