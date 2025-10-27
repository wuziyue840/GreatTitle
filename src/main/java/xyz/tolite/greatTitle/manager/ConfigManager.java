package xyz.tolite.greatTitle.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.tolite.greatTitle.GreatTitle;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private final GreatTitle plugin;
    private FileConfiguration titlesConfig;
    private File titlesFile;
    private FileConfiguration rewardsConfig;
    private File rewardsFile;

    public ConfigManager(GreatTitle plugin) {
        this.plugin = plugin;
        // 确保数据文件夹存在
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        loadConfigs();
    }

    /**
     * 加载所有配置
     */
    public void loadConfigs() {
        // 加载主配置 (config.yml)
        plugin.saveDefaultConfig();

        // 加载称号配置
        loadTitlesConfig();

        // 加载奖励配置
        loadRewardsConfig();
    }

    /**
     * 重载基础配置 (config.yml)
     */
    public void reloadConfig() {
        try {
            plugin.reloadConfig();
            plugin.getLogger().info("基础配置 (config.yml) 重载完成");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "重载基础配置时发生错误", e);
        }
    }

    /**
     * 获取基础配置
     */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /**
     * 加载称号配置
     */
    public void loadTitlesConfig() {
        titlesFile = new File(plugin.getDataFolder(), "titles.yml");
        if (!titlesFile.exists()) {
            plugin.saveResource("titles.yml", false);
        }
        titlesConfig = YamlConfiguration.loadConfiguration(titlesFile);
        plugin.getLogger().info("称号配置加载完成");
    }

    /**
     * 安全重载称号配置
     */
    public void safeReloadTitlesConfig() {
        try {
            if (titlesFile == null) {
                titlesFile = new File(plugin.getDataFolder(), "titles.yml");
            }
            titlesConfig = YamlConfiguration.loadConfiguration(titlesFile);
            plugin.getLogger().info("称号配置重载完成");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "重载称号配置时发生错误", e);
            titlesConfig = new YamlConfiguration();
        }
    }

    /**
     * 加载奖励配置
     */
    public void loadRewardsConfig() {
        rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false);
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
        plugin.getLogger().info("奖励配置加载完成");
    }

    /**
     * 保存称号配置
     */
    public void saveTitlesConfig() {
        try {
            if (titlesConfig != null && titlesFile != null) {
                titlesConfig.save(titlesFile);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "保存称号配置时发生错误", e);
        }
    }

    /**
     * 保存奖励配置
     */
    public void saveRewardsConfig() {
        try {
            if (rewardsConfig != null && rewardsFile != null) {
                rewardsConfig.save(rewardsFile);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "保存奖励配置时发生错误", e);
        }
    }

    // Getters
    public FileConfiguration getTitlesConfig() {
        return titlesConfig;
    }

    public FileConfiguration getRewardsConfig() {
        return rewardsConfig;
    }

    /* ===========================================================
     *  数据库配置方法
     * =========================================================== */

    /**
     * 获取数据库类型
     * @return "sqlite" 或 "mysql"
     */
    public String getDatabaseType() {
        return getConfig().getString("database.type", "sqlite").toLowerCase();
    }

    /**
     * 获取MySQL主机地址
     * @return MySQL主机地址
     */
    public String getMySQLHost() {
        return getConfig().getString("database.mysql.host", "localhost");
    }

    /**
     * 获取MySQL端口
     * @return MySQL端口
     */
    public int getMySQLPort() {
        return getConfig().getInt("database.mysql.port", 3306);
    }

    /**
     * 获取MySQL数据库名
     * @return 数据库名
     */
    public String getMySQLDatabase() {
        return getConfig().getString("database.mysql.database", "minecraft");
    }

    /**
     * 获取MySQL用户名
     * @return 用户名
     */
    public String getMySQLUsername() {
        return getConfig().getString("database.mysql.username", "root");
    }

    /**
     * 获取MySQL密码
     * @return 密码
     */
    public String getMySQLPassword() {
        return getConfig().getString("database.mysql.password", "");
    }

    /**
     * 获取MySQL连接参数
     * @return 连接参数字符串
     */
    public String getMySQLParams() {
        return getConfig().getString("database.mysql.params", "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8");
    }

    /**
     * 获取数据库表前缀
     * @return 表前缀
     */
    public String getTablePrefix() {
        return getConfig().getString("database.table-prefix", "greattitle_");
    }

    /**
     * 检查是否启用数据库连接池
     * @return 是否启用连接池
     */
    public boolean isConnectionPoolEnabled() {
        return getConfig().getBoolean("database.connection-pool.enabled", true);
    }

    /**
     * 获取连接池最大连接数
     * @return 最大连接数
     */
    public int getConnectionPoolMaxSize() {
        return getConfig().getInt("database.connection-pool.max-size", 10);
    }

    /**
     * 获取连接池最小空闲连接数
     * @return 最小空闲连接数
     */
    public int getConnectionPoolMinIdle() {
        return getConfig().getInt("database.connection-pool.min-idle", 2);
    }

    /**
     * 获取连接超时时间（毫秒）
     * @return 连接超时时间
     */
    public long getConnectionTimeout() {
        return getConfig().getLong("database.connection-pool.connection-timeout", 30000L);
    }

    /**
     * 获取空闲连接超时时间（毫秒）
     * @return 空闲连接超时时间
     */
    public long getIdleTimeout() {
        return getConfig().getLong("database.connection-pool.idle-timeout", 600000L);
    }

    /**
     * 获取最大生命周期时间（毫秒）
     * @return 最大生命周期时间
     */
    public long getMaxLifetime() {
        return getConfig().getLong("database.connection-pool.max-lifetime", 1800000L);
    }

    /* ===========================================================
     *  其他配置方法
     * =========================================================== */

    /**
     * 获取随机称号卡物品类型
     */
    public String getRandomCardItem() {
        return getConfig().getString("random-card.item", "PAPER");
    }

    /**
     * 获取随机称号卡名称
     */
    public String getRandomCardName() {
        return getConfig().getString("random-card.name", "§6随机称号卡");
    }

    /**
     * 获取随机称号卡描述
     */
    public List<String> getRandomCardLore() {
        return getConfig().getStringList("random-card.lore");
    }

    /**
     * 获取随机称号池
     */
    public List<String> getRandomCardTitlePool() {
        return getConfig().getStringList("random-card.title-pool");
    }

    /**
     * 获取固定称号卡物品类型
     */
    public String getFixedCardItem() {
        return getConfig().getString("fixed-card.item", "PAPER");
    }

    /**
     * 获取固定称号卡名称
     */
    public String getFixedCardName() {
        return getConfig().getString("fixed-card.name", "§6固定称号卡");
    }

    /**
     * 获取固定称号卡描述
     */
    public List<String> getFixedCardLore() {
        return getConfig().getStringList("fixed-card.lore");
    }

    /**
     * 获取固定称号卡包含的称号
     */
    public String getFixedCardTitle() {
        return getConfig().getString("fixed-card.title", "newbie");
    }

    /**
     * 检查是否启用随机称号卡
     */
    public boolean isRandomCardEnabled() {
        return getConfig().getBoolean("random-card.enabled", true);
    }

    /**
     * 检查是否首次登录赠送随机称号卡
     */
    public boolean isRandomCardFirstJoin() {
        return getConfig().getBoolean("random-card.first-join", true);
    }

    /**
     * 检查是否启用固定称号卡
     */
    public boolean isFixedCardEnabled() {
        return getConfig().getBoolean("fixed-card.enabled", true);
    }

    /**
     * 检查是否首次登录赠送固定称号卡
     */
    public boolean isFixedCardFirstJoin() {
        return getConfig().getBoolean("fixed-card.first-join", true);
    }

    /**
     * 获取GUI大小
     */
    public int getGUISize() {
        return getConfig().getInt("gui.size", 54);
    }

    /**
     * 获取切换冷却时间
     */
    public int getSwitchCooldown() {
        return getConfig().getInt("switch-cooldown", 5);
    }

    /**
     * 获取随机称号池
     */
    public List<String> getRandomTitlePool() {
        return getConfig().getStringList("random-title-pool");
    }

    /**
     * 保存配置（通用方法）
     */
    public void saveConfig() {
        plugin.saveConfig();
    }

    /**
     * 保存名称配置（修复方法）
     */
    public void saveNamesConfig() {
        // 这里应该实现保存名称配置的逻辑
        // 目前暂时调用保存基础配置
        saveConfig();
        plugin.getLogger().info("名称配置已保存");
    }

    /**
     * 重载所有配置
     */
    public void reloadAllConfigs() {
        reloadConfig();
        safeReloadTitlesConfig();
        loadRewardsConfig();
        plugin.getLogger().info("所有配置重载完成");
    }

    /**
     * 检查数据库是否使用MySQL
     * @return 如果是MySQL返回true，SQLite返回false
     */
    public boolean isMySQL() {
        return "mysql".equalsIgnoreCase(getDatabaseType());
    }

    /**
     * 检查数据库是否使用SQLite
     * @return 如果是SQLite返回true，MySQL返回false
     */
    public boolean isSQLite() {
        return "sqlite".equalsIgnoreCase(getDatabaseType());
    }

    /**
     * 获取SQLite数据库文件名
     * @return SQLite数据库文件名
     */
    public String getSQLiteFileName() {
        return getConfig().getString("database.sqlite.filename", "titles.db");
    }

    /**
     * 获取完整的SQLite数据库文件路径
     * @return SQLite数据库文件完整路径
     */
    public File getSQLiteFile() {
        return new File(plugin.getDataFolder(), getSQLiteFileName());
    }

    public void loadConfig() {
        loadConfigs();
    }
}