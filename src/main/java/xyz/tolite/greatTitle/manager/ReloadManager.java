package xyz.tolite.greatTitle.manager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.tolite.greatTitle.GreatTitle;

import java.util.logging.Level;

public class ReloadManager {

    private final GreatTitle plugin;

    public ReloadManager(GreatTitle plugin) {
        this.plugin = plugin;
    }

    /**
     * 安全重载所有配置
     */
    public void reloadAllConfigs(CommandSender sender) {
        try {
            sender.sendMessage("§6开始重载 GreatTitle 配置...");

            // 1. 重载基础配置
            reloadBaseConfig(sender);

            // 2. 重载名称配置
            reloadNameConfig(sender);

            // 3. 重载经济配置
            reloadMoneyConfig(sender);

            // 4. 重载GUI配置
            reloadGUIConfig(sender);

            // 5. 重载称号配置
            reloadTitlesConfig(sender);

            // 6. 重新加载称号数据
            reloadTitleData(sender);

            // 7. 重新应用所有效果
            reapplyAllEffects(sender);

            // 8. 重新加载玩家显示
            reloadPlayerDisplays(sender);

            sender.sendMessage("§a所有配置重载完成! 所有效果已重新应用");

        } catch (Exception e) {
            String errorMsg = "重载配置时发生严重错误: " + e.getMessage();
            sender.sendMessage("§c" + errorMsg);
            plugin.getLogger().log(Level.SEVERE, errorMsg, e);
        }
    }

    /**
     * 重载基础配置
     */
    public void reloadBaseConfig(CommandSender sender) {
        try {
            plugin.getConfigManager().reloadConfig();
            sender.sendMessage("§a✓ 基础配置重载完成");
        } catch (Exception e) {
            String errorMsg = "基础配置重载失败: " + e.getMessage();
            sender.sendMessage("§c✗ " + errorMsg);
            plugin.getLogger().warning(errorMsg);
        }
    }

    /**
     * 重载名称配置 - 修复版本
     */
    private void reloadNameConfig(CommandSender sender) {
        try {
            if (plugin.getNameConfig() != null) {
                plugin.getNameConfig().reloadConfig();
                sender.sendMessage("§a✓ 名称配置 (name.yml) 重载完成");
            } else {
                // 如果名称配置管理器为null，记录错误但不中断重载流程
                String errorMsg = "名称配置管理器未初始化";
                sender.sendMessage("§c✗ " + errorMsg);
                plugin.getLogger().warning(errorMsg);

                // 尝试重新初始化
                try {
                    plugin.getLogger().info("尝试重新初始化 NameConfig");
                    // 这里需要根据你的主类结构来重新初始化
                    // 例如: plugin.initializeNameConfig();
                } catch (Exception ex) {
                    plugin.getLogger().warning("重新初始化 NameConfig 失败: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            String errorMsg = "名称配置重载失败: " + e.getMessage();
            sender.sendMessage("§c✗ " + errorMsg);
            plugin.getLogger().warning(errorMsg);
        }
    }

    /**
     * 重载经济配置
     */
    private void reloadMoneyConfig(CommandSender sender) {
        try {
            if (plugin.getMoneyConfigManager() != null) {
                plugin.getMoneyConfigManager().reloadMoneyConfig();
                sender.sendMessage("§a✓ 经济配置重载完成");
            } else {
                sender.sendMessage("§c✗ 经济配置管理器未初始化");
            }
        } catch (Exception e) {
            String errorMsg = "经济配置重载失败: " + e.getMessage();
            sender.sendMessage("§c✗ " + errorMsg);
            plugin.getLogger().warning(errorMsg);
        }
    }

    /**
     * 重载GUI配置
     */
    private void reloadGUIConfig(CommandSender sender) {
        try {
            if (plugin.getGUIConfigManager() != null) {
                plugin.getGUIConfigManager().reloadConfig();
                sender.sendMessage("§a✓ GUI配置重载完成");
            } else {
                sender.sendMessage("§7- GUI配置管理器未初始化，跳过");
            }
        } catch (Exception e) {
            String errorMsg = "GUI配置重载失败: " + e.getMessage();
            sender.sendMessage("§c✗ " + errorMsg);
            plugin.getLogger().warning(errorMsg);
        }
    }

    /**
     * 重载称号配置
     */
    private void reloadTitlesConfig(CommandSender sender) {
        try {
            plugin.getConfigManager().safeReloadTitlesConfig();
            sender.sendMessage("§a✓ 称号配置重载完成");
        } catch (Exception e) {
            String errorMsg = "称号配置重载失败: " + e.getMessage();
            sender.sendMessage("§c✗ " + errorMsg);
            plugin.getLogger().warning(errorMsg);
        }
    }

    /**
     * 重新加载称号数据
     */
    private void reloadTitleData(CommandSender sender) {
        try {
            plugin.getTitleManager().loadTitles();
            sender.sendMessage("§a✓ 称号数据重载完成");
        } catch (Exception e) {
            String errorMsg = "称号数据重载失败: " + e.getMessage();
            sender.sendMessage("§c✗ " + errorMsg);
            plugin.getLogger().warning(errorMsg);
        }
    }

    /**
     * 重新应用所有效果
     */
    private void reapplyAllEffects(CommandSender sender) {
        try {
            // 重新应用BUFF效果
            if (plugin.getBuffManager() != null) {
                plugin.getBuffManager().reapplyAllBuffs();
            }

            // 重新应用粒子效果
            if (plugin.getParticleManager() != null) {
                plugin.getParticleManager().reapplyAllParticles();
            }

            // 重新应用称号效果
            plugin.getTitleManager().reapplyAllEffects();

            sender.sendMessage("§a✓ 所有效果重新应用完成");
        } catch (Exception e) {
            String errorMsg = "重新应用效果失败: " + e.getMessage();
            sender.sendMessage("§c✗ " + errorMsg);
            plugin.getLogger().warning(errorMsg);
        }
    }

    /**
     * 重新加载玩家显示
     */
    private void reloadPlayerDisplays(CommandSender sender) {
        try {
            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().reloadAllPlayers();
                sender.sendMessage("§a✓ 玩家显示重载完成");
            } else {
                sender.sendMessage("§7- 头顶显示管理器未初始化，跳过");
            }
        } catch (Exception e) {
            String errorMsg = "玩家显示重载失败: " + e.getMessage();
            sender.sendMessage("§c✗ " + errorMsg);
            plugin.getLogger().warning(errorMsg);
        }
    }

    /**
     * 部分重载 - 仅重载称号相关配置
     */
    public void reloadTitlesOnly(CommandSender sender) {
        try {
            sender.sendMessage("§6重载称号配置...");

            reloadTitlesConfig(sender);
            reloadTitleData(sender);
            reapplyAllEffects(sender);
            reloadPlayerDisplays(sender);

            sender.sendMessage("§a称号配置重载完成!");

        } catch (Exception e) {
            String errorMsg = "重载称号配置时发生错误: " + e.getMessage();
            sender.sendMessage("§c" + errorMsg);
            plugin.getLogger().log(Level.SEVERE, errorMsg, e);
        }
    }

    /**
     * 部分重载 - 仅重载经济相关配置
     */
    public void reloadEconomyOnly(CommandSender sender) {
        try {
            sender.sendMessage("§6重载经济配置...");

            reloadMoneyConfig(sender);
            reloadGUIConfig(sender);

            sender.sendMessage("§a经济配置重载完成!");

        } catch (Exception e) {
            String errorMsg = "重载经济配置时发生错误: " + e.getMessage();
            sender.sendMessage("§c" + errorMsg);
            plugin.getLogger().log(Level.SEVERE, errorMsg, e);
        }
    }

    /**
     * 重新加载单个玩家的数据
     */
    public void reloadPlayerData(Player player) {
        try {
            plugin.getTitleManager().loadPlayerData(player);

            // 更新显示
            plugin.getTitleManager().updatePlayerDisplay(player);
            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
            }

            player.sendMessage("§a你的称号数据已重新加载!");

        } catch (Exception e) {
            player.sendMessage("§c重新加载你的数据时出现错误!");
            plugin.getLogger().warning("重载玩家 " + player.getName() + " 数据失败: " + e.getMessage());
        }
    }
}