package xyz.tolite.greatTitle.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.tolite.greatTitle.GreatTitle;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class NameConfig {

    private final GreatTitle plugin;
    private FileConfiguration nameConfig;
    private File namesFile;

    public NameConfig(GreatTitle plugin) {
        this.plugin = plugin;
        // 立即初始化文件对象
        this.namesFile = new File(plugin.getDataFolder(), "name.yml");
        // 确保数据文件夹存在
        if (!plugin.getDataFolder().exists()) {
            boolean created = plugin.getDataFolder().mkdirs();
            if (created) {
                plugin.getLogger().info("创建插件数据文件夹");
            }
        }
        loadConfig();
    }

    /* -------------------------------------------------
     * 加载 / 重载 / 保存
     * ------------------------------------------------- */
    public void loadConfig() {
        try {
            // 双重保险：确保文件对象不为null
            if (namesFile == null) {
                namesFile = new File(plugin.getDataFolder(), "name.yml");
                plugin.getLogger().warning("NameConfig.loadConfig: namesFile 为 null，已重新初始化");
            }

            plugin.getLogger().info("正在加载名称配置，文件路径: " + namesFile.getAbsolutePath());
            plugin.getLogger().info("文件存在: " + namesFile.exists());

            if (!namesFile.exists()) {
                plugin.getLogger().info("名称配置文件不存在，创建默认配置");
                createDefaultConfig();
            } else {
                nameConfig = YamlConfiguration.loadConfiguration(namesFile);
                migrateOldConfig();
                plugin.getLogger().info("名称配置文件加载完成: " + namesFile.getAbsolutePath());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "加载名称配置文件时发生错误", e);
            // 创建空的配置作为后备
            nameConfig = new YamlConfiguration();
        }
    }

    public void reloadConfig() {
        try {
            // 三重保险：绝对确保 namesFile 不为 null
            if (namesFile == null) {
                namesFile = new File(plugin.getDataFolder(), "name.yml");
                plugin.getLogger().warning("NameConfig.reloadConfig: namesFile 为 null，已重新初始化");
            }

            plugin.getLogger().info("正在重载名称配置，文件路径: " + namesFile.getAbsolutePath());
            plugin.getLogger().info("文件存在: " + namesFile.exists());

            // 重新加载配置
            if (namesFile.exists()) {
                nameConfig = YamlConfiguration.loadConfiguration(namesFile);
                plugin.getLogger().info("名称配置文件重新加载完成");
            } else {
                plugin.getLogger().warning("名称配置文件不存在，创建默认配置");
                createDefaultConfig();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "重新加载名称配置文件时发生错误", e);
            // 确保即使出错也有配置对象
            nameConfig = new YamlConfiguration();
        }
    }

    public void saveConfig() {
        try {
            if (nameConfig != null && namesFile != null) {
                nameConfig.save(namesFile);
                plugin.getLogger().info("名称配置文件保存成功: " + namesFile.getAbsolutePath());
            } else {
                plugin.getLogger().warning("无法保存名称配置: nameConfig 或 namesFile 为 null");
                if (nameConfig == null) plugin.getLogger().warning("nameConfig 为 null");
                if (namesFile == null) plugin.getLogger().warning("namesFile 为 null");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "保存名称配置文件时发生错误", e);
        }
    }

    /* -------------------------------------------------
     * 默认配置创建
     * ------------------------------------------------- */
    private void createDefaultConfig() throws IOException {
        // 确保文件存在
        if (!namesFile.exists()) {
            boolean created = namesFile.createNewFile();
            if (created) {
                plugin.getLogger().info("创建名称配置文件: " + namesFile.getAbsolutePath());
            } else {
                plugin.getLogger().severe("无法创建名称配置文件: " + namesFile.getAbsolutePath());
                return;
            }
        }

        // 创建新的配置对象
        nameConfig = new YamlConfiguration();

        // 设置配置头
        nameConfig.options().header(
                "名称显示配置\n" +
                        "占位符: %greattitle_active% 玩家当前称号  %player_name% 玩家名称\n"
        );

        // 设置默认值
        nameConfig.set("tablist-display.enabled", true);
        nameConfig.set("tablist-display.format", "%greattitle_active% %player_name%");

        nameConfig.set("chat-display.enabled", true);
        nameConfig.set("chat-display.format", "%greattitle_active%");

        nameConfig.set("head-prefix.enabled", true);
        nameConfig.set("head-prefix.display-mode", "inline");
        nameConfig.set("head-prefix.format", "%greattitle_active% %player_name%");
        nameConfig.set("head-prefix.default-prefix", "§7");

        // 保存配置
        saveConfig();
        plugin.getLogger().info("创建默认名称配置文件成功");
    }

    private void migrateOldConfig() {
        if (nameConfig == null || !nameConfig.contains("display-settings")) return;

        plugin.getLogger().info("检测到旧版名称配置，正在迁移...");
        boolean needSave = false;

        // 迁移逻辑
        if (nameConfig.contains("display-settings.head-prefix.enabled")) {
            nameConfig.set("head-prefix.enabled", nameConfig.getBoolean("display-settings.head-prefix.enabled"));
            nameConfig.set("display-settings.head-prefix", null);
            needSave = true;
        }
        if (nameConfig.contains("display-settings.head-prefix.format")) {
            nameConfig.set("head-prefix.format", nameConfig.getString("display-settings.head-prefix.format"));
            nameConfig.set("display-settings.head-prefix", null);
            needSave = true;
        }

        // Tab
        if (nameConfig.contains("display-settings.tab-list.enabled")) {
            nameConfig.set("tablist-display.enabled", nameConfig.getBoolean("display-settings.tab-list.enabled"));
            nameConfig.set("display-settings.tab-list", null);
            needSave = true;
        }
        if (nameConfig.contains("display-settings.tab-list.format")) {
            nameConfig.set("tablist-display.format", nameConfig.getString("display-settings.tab-list.format"));
            nameConfig.set("display-settings.tab-list", null);
            needSave = true;
        }

        // 聊天
        if (nameConfig.contains("display-settings.chat.enabled")) {
            nameConfig.set("chat-display.enabled", nameConfig.getBoolean("display-settings.chat.enabled"));
            nameConfig.set("display-settings.chat", null);
            needSave = true;
        }
        if (nameConfig.contains("display-settings.chat.format")) {
            String old = nameConfig.getString("display-settings.chat.format", "%greattitle_active%");
            nameConfig.set("chat-display.format", old.replace(": %message%", "").trim());
            nameConfig.set("display-settings.chat", null);
            needSave = true;
        }

        nameConfig.set("display-settings", null);
        if (needSave) saveConfig();
    }

    /* -------------------------------------------------
     * 配置读取（兼容旧方法）
     * ------------------------------------------------- */
    public FileConfiguration getConfig() {
        if (nameConfig == null) {
            plugin.getLogger().warning("nameConfig 为 null，重新加载");
            reloadConfig();
        }
        return nameConfig;
    }

    /* ---------- 头顶 ---------- */
    public boolean isHeadPrefixEnabled() {
        return getConfig().getBoolean("head-prefix.enabled", true);
    }
    public String getHeadDisplayMode() {
        return getConfig().getString("head-prefix.display-mode", "inline");
    }
    public String getHeadPrefixFormat() {
        return getConfig().getString("head-prefix.format", "%greattitle_active% %player_name%");
    }
    public String getHeadPrefixDefault() {
        return getConfig().getString("head-prefix.default-prefix", "§7");
    }

    /* ---------- Tab ---------- */
    public boolean isTabListDisplayEnabled() {
        return getConfig().getBoolean("tablist-display.enabled", true);
    }
    public String getTabListDisplayFormat() {
        return getConfig().getString("tablist-display.format", "%greattitle_active% %player_name%");
    }

    /* ---------- 聊天 ---------- */
    public boolean isChatDisplayEnabled() {
        return getConfig().getBoolean("chat-display.enabled", true);
    }
    public String getChatDisplayFormat() {
        return getConfig().getString("chat-display.format", "%greattitle_active%");
    }

    /* ---------- 向后兼容旧代码 ---------- */
    public boolean isTabListEnabled()      { return isTabListDisplayEnabled(); }
    public String  getTabListFormat()      { return getTabListDisplayFormat(); }
    public boolean isChatEnabled()         { return isChatDisplayEnabled(); }
    public String  getChatFormat()         { return getChatDisplayFormat() + ": %message%"; }
    public boolean isDisplayNameEnabled()  { return isHeadPrefixEnabled(); }
    public String  getDisplayNameFormat()  { return getHeadPrefixFormat(); }
    public String  getDisplayNameDefault() { return getHeadPrefixDefault(); }
}