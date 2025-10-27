package xyz.tolite.greatTitle;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.tolite.greatTitle.commands.TitleCommand;
import xyz.tolite.greatTitle.config.NameConfig;
import xyz.tolite.greatTitle.database.DatabaseManager;
import xyz.tolite.greatTitle.gui.TitleGUI;
import xyz.tolite.greatTitle.listener.PlayerListener;
import xyz.tolite.greatTitle.manager.*;
import xyz.tolite.greatTitle.placeholder.TitlePlaceholder;
import xyz.tolite.greatTitle.task.TitleExpireTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

public class GreatTitle extends JavaPlugin {

    private static GreatTitle instance;
    private TitleManager titleManager;
    private EconomyManager economyManager;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private NameConfig nameConfig;
    private ParticleManager particleManager;
    private BuffManager buffManager;
    private HeadDisplayManager headDisplayManager;
    private TitleGUI titleGUI;
    private TitleExpireTask expireTask;
    private MoneyConfigManager moneyConfigManager;
    private GUIConfigManager guiConfigManager;
    private ReloadManager reloadManager;
    private CustomTitleManager customTitleManager;

    @Override
    public void onEnable() {
        instance = this;

        try {
            // 1. 保存默认配置文件
            saveDefaultConfig();

            // 2. 初始化配置管理器
            this.configManager = new ConfigManager(this);
            configManager.loadConfig();

            // 3. 初始化名称配置管理器 - 关键：在构造函数中就加载
            this.nameConfig = new NameConfig(this);

            // 4. 创建并加载奖励配置文件
            createRewardsConfig();

            // 5. 初始化数据库管理器
            this.databaseManager = new DatabaseManager(this);
            if (!databaseManager.initDatabase()) {
                getLogger().severe("数据库初始化失败，插件将禁用!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // 6. 初始化其他管理器
            this.buffManager = new BuffManager(this);
            this.particleManager = new ParticleManager(this);
            this.moneyConfigManager = new MoneyConfigManager(this); // 先初始化 money
            this.guiConfigManager = new GUIConfigManager(this);
            this.titleManager = new TitleManager(this);
            this.economyManager = new EconomyManager(this);
            this.titleGUI = new TitleGUI(this);
            this.customTitleManager = new CustomTitleManager(this);
            this.reloadManager = new ReloadManager(this);

            // 7. 初始化头顶显示管理器 - 确保在名称配置之后
            this.headDisplayManager = new HeadDisplayManager(this);

            // 8. 初始化新增的管理器
            this.moneyConfigManager = new MoneyConfigManager(this);
            this.guiConfigManager = new GUIConfigManager(this);

            // 9. 加载称号数据
            titleManager.loadTitles();

            // 10. 加载奖励数据
            titleManager.loadRewards();

            // 11. 启动称号过期检查任务
            this.expireTask = new TitleExpireTask(this);
            expireTask.start();

            // 12. 注册命令
            getCommand("grt").setExecutor(new TitleCommand(this));

            // 13. 注册事件监听器
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

            // 14. 注册PlaceholderAPI
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new TitlePlaceholder(this).register();
                getLogger().info("PlaceholderAPI 扩展已注册!");
            }

            // 确保名称配置正确初始化
            try {
                this.nameConfig = new NameConfig(this);
                getLogger().info("名称配置初始化完成");
            } catch (Exception e) {
                getLogger().severe("名称配置初始化失败: " + e.getMessage());
                e.printStackTrace();
                // 创建备用的配置对象
                this.nameConfig = new NameConfig(this) {
                    // 重写方法提供默认值
                };
            }

            // 15. 为在线玩家加载数据（防止重载时数据丢失）
            Bukkit.getOnlinePlayers().forEach(player -> {
                titleManager.loadPlayerData(player);

                // 延迟更新头顶显示，确保所有数据已加载
                getServer().getScheduler().runTaskLater(this, () -> {
                    if (headDisplayManager != null) {
                        headDisplayManager.updatePlayerHeadDisplay(player);
                        headDisplayManager.updatePlayerTabDisplay(player);
                    }
                }, 20L); // 减少延迟到20ticks
            });

            getLogger().info("GreatTitle 插件已成功加载!");

        } catch (Exception e) {
            getLogger().severe("插件启动失败: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * 创建奖励配置文件
     */
    private void createRewardsConfig() {
        File rewardsFile = new File(getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            try {
                // 如果JAR中有rewards.yml，就使用它
                if (getResource("rewards.yml") != null) {
                    saveResource("rewards.yml", false);
                    getLogger().info("已从JAR中提取奖励配置文件: rewards.yml");
                } else {
                    // 否则创建一个默认的rewards.yml
                    String defaultRewards = "# 奖励配置\n" +
                            "# 每个奖励的配置格式：\n" +
                            "# 奖励ID:\n" +
                            "#   display-name: 显示名称\n" +
                            "#   description: 描述列表\n" +
                            "#   required-titles: 所需称号数量\n" +
                            "#   commands: 执行的命令列表\n" +
                            "#   permission: 需要的权限（可选）\n" +
                            "#   repeatable: 是否可重复领取（true/false）\n" +
                            "\n" +
                            "# 示例奖励1: 拥有5个称号的奖励\n" +
                            "reward_5_titles:\n" +
                            "  display-name: \"§6初级收藏家\"\n" +
                            "  description:\n" +
                            "    - \"§7恭喜你收集了5个称号!\"\n" +
                            "    - \"§e奖励内容:\"\n" +
                            "    - \"  §7- 1000游戏币\"\n" +
                            "    - \"  §7- 10点券\"\n" +
                            "  required-titles: 5\n" +
                            "  commands:\n" +
                            "    - \"eco give %player% 1000\"\n" +
                            "    - \"points give %player% 10\"\n" +
                            "  permission: \"\"\n" +
                            "  repeatable: false\n" +
                            "\n" +
                            "# 示例奖励2: 拥有10个称号的奖励\n" +
                            "reward_10_titles:\n" +
                            "  display-name: \"§e中级收藏家\"\n" +
                            "  description:\n" +
                            "    - \"§7恭喜你收集了10个称号!\"\n" +
                            "    - \"§e奖励内容:\"\n" +
                            "    - \"  §7- 5000游戏币\"\n" +
                            "    - \"  §7- 50点券\"\n" +
                            "    - \"  §7- 1个钻石\"\n" +
                            "  required-titles: 10\n" +
                            "  commands:\n" +
                            "    - \"eco give %player% 5000\"\n" +
                            "    - \"points give %player% 50\"\n" +
                            "    - \"give %player% diamond 1\"\n" +
                            "  permission: \"\"\n" +
                            "  repeatable: false\n" +
                            "\n" +
                            "# 示例奖励3: 拥有20个称号的奖励\n" +
                            "reward_20_titles:\n" +
                            "  display-name: \"§6高级收藏家\"\n" +
                            "  description:\n" +
                            "    - \"§7恭喜你收集了20个称号!\"\n" +
                            "    - \"§e奖励内容:\"\n" +
                            "    - \"  §7- 10000游戏币\"\n" +
                            "    - \"  §7- 100点券\"\n" +
                            "    - \"  §7- 5个钻石\"\n" +
                            "    - \"  §7- 1个精英宝箱\"\n" +
                            "  required-titles: 20\n" +
                            "  commands:\n" +
                            "    - \"eco give %player% 10000\"\n" +
                            "    - \"points give %player% 100\"\n" +
                            "    - \"give %player% diamond 5\"\n" +
                            "    - \"give %player% chest 1\"\n" +
                            "  permission: \"\"\n" +
                            "  repeatable: false\n" +
                            "\n" +
                            "# 示例奖励4: 拥有30个称号的奖励\n" +
                            "reward_30_titles:\n" +
                            "  display-name: \"§c称号大师\"\n" +
                            "  description:\n" +
                            "    - \"§7恭喜你收集了30个称号!\"\n" +
                            "    - \"§e奖励内容:\"\n" +
                            "    - \"  §7- 20000游戏币\"\n" +
                            "    - \"  §7- 200点券\"\n" +
                            "    - \"  §7- 10个钻石\"\n" +
                            "    - \"  §7- 3个精英宝箱\"\n" +
                            "    - \"  §7- 专属称号权限\"\n" +
                            "  required-titles: 30\n" +
                            "  commands:\n" +
                            "    - \"eco give %player% 20000\"\n" +
                            "    - \"points give %player% 200\"\n" +
                            "    - \"give %player% diamond 10\"\n" +
                            "    - \"give %player% chest 3\"\n" +
                            "    - \"lp user %player% permission set greattitle.master true\"\n" +
                            "  permission: \"\"\n" +
                            "  repeatable: false\n" +
                            "\n" +
                            "# 示例奖励5: 每周可重复领取的奖励\n" +
                            "weekly_reward:\n" +
                            "  display-name: \"§b每周称号奖励\"\n" +
                            "  description:\n" +
                            "    - \"§7每周可领取的称号奖励\"\n" +
                            "    - \"§e奖励内容:\"\n" +
                            "    - \"  §7- 500游戏币\"\n" +
                            "    - \"  §7- 5点券\"\n" +
                            "  required-titles: 1\n" +
                            "  commands:\n" +
                            "    - \"eco give %player% 500\"\n" +
                            "    - \"points give %player% 5\"\n" +
                            "  permission: \"\"\n" +
                            "  repeatable: true";

                    Files.write(rewardsFile.toPath(), defaultRewards.getBytes());
                    getLogger().info("已创建默认奖励配置文件: rewards.yml");
                }
            } catch (IOException e) {
                getLogger().warning("创建奖励配置文件失败: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        if (expireTask != null) {
            expireTask.stop();
        }

        if (headDisplayManager != null) {
            headDisplayManager.cleanup();
        }

        if (databaseManager != null) {
            databaseManager.closeConnection();
        }

        // 卸载所有玩家数据
        if (titleManager != null) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                titleManager.unloadPlayerData(player);
            });
        }

        if (configManager != null) {
            configManager.saveNamesConfig();
        }

        getLogger().info("GreatTitle 插件已卸载!");
    }

    public static GreatTitle getInstance() {
        return instance;
    }

    public TitleManager getTitleManager() {
        return titleManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public NameConfig getNameConfig() {
        return nameConfig;
    }

    public ParticleManager getParticleManager() {
        return particleManager;
    }

    public BuffManager getBuffManager() {
        return buffManager;
    }

    public HeadDisplayManager getHeadDisplayManager() {
        return headDisplayManager;
    }

    public TitleGUI getTitleGUI() {
        return titleGUI;
    }

    public TitleExpireTask getExpireTask() {
        return expireTask;
    }

    // 新增的getter方法
    public MoneyConfigManager getMoneyConfigManager() {
        return moneyConfigManager;
    }

    public GUIConfigManager getGUIConfigManager() {
        return guiConfigManager;
    }

    public CustomTitleManager getCustomTitleManager() {
        return customTitleManager;
    }

    public ReloadManager getReloadManager() {
        return reloadManager;
    }

    /**
     * 设置调试模式
     */
    public void setDebugEnabled(boolean enabled) {
        getConfig().set("debug-enabled", enabled);
        saveConfig();

        // 同步到各个管理器
        if (headDisplayManager != null) {
            headDisplayManager.setDebugEnabled(enabled);
        }

        getLogger().info("全局调试模式: " + (enabled ? "开启" : "关闭"));
    }
}