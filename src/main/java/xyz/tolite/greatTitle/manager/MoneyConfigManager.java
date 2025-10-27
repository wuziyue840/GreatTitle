package xyz.tolite.greatTitle.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.tolite.greatTitle.GreatTitle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class MoneyConfigManager {

    private final GreatTitle plugin;
    private File moneyFile;
    private FileConfiguration moneyConfig;

    public MoneyConfigManager(GreatTitle plugin) {
        this.plugin = plugin;
        setupMoneyConfig();
    }

    private void setupMoneyConfig() {
        // 确保数据文件夹存在
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        moneyFile = new File(plugin.getDataFolder(), "money.yml");
        if (!moneyFile.exists()) {
            // 从JAR中复制默认配置
            try {
                InputStream inputStream = plugin.getResource("money.yml");
                if (inputStream != null) {
                    Files.copy(inputStream, moneyFile.toPath());
                    plugin.getLogger().info("创建默认 money.yml 配置文件");
                } else {
                    // 如果JAR中没有，创建空的配置文件
                    moneyFile.createNewFile();
                    createDefaultConfig();
                    plugin.getLogger().info("创建新的 money.yml 配置文件");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "创建 money.yml 失败", e);
                // 创建空的配置对象作为后备
                moneyConfig = new YamlConfiguration();
                return;
            }
        }

        // 加载配置
        reloadMoneyConfig();
    }

    /**
     * 创建默认配置
     */
    private void createDefaultConfig() {
        moneyConfig = new YamlConfiguration();

        // 自定义称号设置
        moneyConfig.set("custom-title.base-price", 1000.0);
        moneyConfig.set("custom-title.min-length", 2);
        moneyConfig.set("custom-title.max-length", 20);
        moneyConfig.set("custom-title.allowed-colors", java.util.Arrays.asList(
                "&a", "&b", "&c", "&d", "&e", "&f", "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9"
        ));
        moneyConfig.set("custom-title.banned-words", java.util.Arrays.asList(
                "admin", "op", "owner", "fuck", "shit"
        ));

        // 经济系统设置
        moneyConfig.set("economy-systems.vault.enabled", true);
        moneyConfig.set("economy-systems.vault.display-name", "金币");
        moneyConfig.set("economy-systems.vault.cost", 1000.0);
        moneyConfig.set("economy-systems.vault.custom-price", 1000.0);
        moneyConfig.set("economy-systems.vault.plugin", "Vault");
        moneyConfig.set("economy-systems.vault.permission", "");

        moneyConfig.set("economy-systems.playerpoints.enabled", true);
        moneyConfig.set("economy-systems.playerpoints.display-name", "点券");
        moneyConfig.set("economy-systems.playerpoints.cost", 500.0);
        moneyConfig.set("economy-systems.playerpoints.custom-price", 500.0);
        moneyConfig.set("economy-systems.playerpoints.plugin", "PlayerPoints");
        moneyConfig.set("economy-systems.playerpoints.permission", "");

        moneyConfig.set("economy-systems.titlecoins.enabled", true);
        moneyConfig.set("economy-systems.titlecoins.display-name", "称号币");
        moneyConfig.set("economy-systems.titlecoins.cost", 100.0);
        moneyConfig.set("economy-systems.titlecoins.custom-price", 100.0);
        moneyConfig.set("economy-systems.titlecoins.plugin", "GreatTitle");
        moneyConfig.set("economy-systems.titlecoins.permission", "");

        saveMoneyConfig();
    }

    /**
     * 获取自定义称号基础价格
     */
    public double getCustomTitleBasePrice() {
        if (moneyConfig == null) reloadMoneyConfig();
        return moneyConfig.getDouble("custom-title.base-price", 1000.0);
    }

    /**
     * 获取允许的颜色代码列表
     */
    public List<String> getAllowedColors() {
        if (moneyConfig == null) reloadMoneyConfig();
        return moneyConfig.getStringList("custom-title.allowed-colors");
    }

    /**
     * 获取自定义称号最大长度
     */
    public int getCustomTitleMaxLength() {
        if (moneyConfig == null) reloadMoneyConfig();
        return moneyConfig.getInt("custom-title.max-length", 20);
    }

    /**
     * 获取自定义称号最小长度
     */
    public int getCustomTitleMinLength() {
        if (moneyConfig == null) reloadMoneyConfig();
        return moneyConfig.getInt("custom-title.min-length", 2);
    }

    /**
     * 获取禁止的词语列表
     */
    public List<String> getBannedWords() {
        if (moneyConfig == null) reloadMoneyConfig();
        return moneyConfig.getStringList("custom-title.banned-words");
    }

    /**
     * 检查词语是否被禁止
     */
    public boolean isWordBanned(String word) {
        List<String> bannedWords = getBannedWords();
        String lowerWord = word.toLowerCase();

        for (String bannedWord : bannedWords) {
            if (lowerWord.contains(bannedWord.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证自定义称号名称
     */
    public boolean validateCustomTitleName(String titleName) {
        // 检查长度
        int length = titleName.replace("&", "").length();
        if (length < getCustomTitleMinLength() || length > getCustomTitleMaxLength()) {
            return false;
        }

        // 检查禁止词语
        if (isWordBanned(titleName)) {
            return false;
        }

        return true;
    }

    /**
     * 获取经济系统的自定义称号价格
     */
    public double getEconomyCustomPrice(String economyKey) {
        if (moneyConfig == null) reloadMoneyConfig();
        if (moneyConfig.contains("economy-systems." + economyKey + ".custom-price")) {
            return moneyConfig.getDouble("economy-systems." + economyKey + ".custom-price");
        }
        return getCustomTitleBasePrice();
    }

    /**
     * 获取所有可用的经济系统
     */
    public Map<String, Map<String, Object>> getEconomySystems() {
        if (moneyConfig == null) reloadMoneyConfig();
        Map<String, Map<String, Object>> economies = new HashMap<>();

        if (moneyConfig.contains("economy-systems")) {
            for (String key : moneyConfig.getConfigurationSection("economy-systems").getKeys(false)) {
                Map<String, Object> economyData = new HashMap<>();
                economyData.put("display-name", moneyConfig.getString("economy-systems." + key + ".display-name"));
                economyData.put("enabled", moneyConfig.getBoolean("economy-systems." + key + ".enabled", true));
                economyData.put("cost", moneyConfig.getDouble("economy-systems." + key + ".cost", 1000.0));
                economyData.put("custom-price", getEconomyCustomPrice(key));
                economyData.put("plugin", moneyConfig.getString("economy-systems." + key + ".plugin"));
                economyData.put("permission", moneyConfig.getString("economy-systems." + key + ".permission", ""));

                economies.put(key, economyData);
            }
        }

        return economies;
    }

    /**
     * 获取启用的经济系统
     */
    public List<String> getEnabledEconomySystems() {
        if (moneyConfig == null) reloadMoneyConfig();
        List<String> enabled = new ArrayList<>();
        Map<String, Map<String, Object>> economies = getEconomySystems();

        for (Map.Entry<String, Map<String, Object>> entry : economies.entrySet()) {
            if ((Boolean) entry.getValue().get("enabled")) {
                enabled.add(entry.getKey());
            }
        }

        return enabled;
    }

    /**
     * 获取经济系统的显示名称
     */
    public String getEconomyDisplayName(String economyKey) {
        if (moneyConfig == null) reloadMoneyConfig();
        return moneyConfig.getString("economy-systems." + economyKey + ".display-name", economyKey);
    }

    /**
     * 获取经济系统的价格（普通购买）
     */
    public double getEconomyCost(String economyKey) {
        if (moneyConfig == null) reloadMoneyConfig();
        return moneyConfig.getDouble("economy-systems." + economyKey + ".cost", 1000.0);
    }

    /**
     * 获取经济系统所需的插件
     */
    public String getEconomyPlugin(String economyKey) {
        if (moneyConfig == null) reloadMoneyConfig();
        return moneyConfig.getString("economy-systems." + economyKey + ".plugin");
    }

    /**
     * 获取经济系统所需的权限
     */
    public String getEconomyPermission(String economyKey) {
        if (moneyConfig == null) reloadMoneyConfig();
        return moneyConfig.getString("economy-systems." + economyKey + ".permission", "");
    }

    /**
     * 检查经济系统是否可用
     */
    public boolean isEconomyAvailable(String economyKey) {
        if (moneyConfig == null) reloadMoneyConfig();
        Map<String, Map<String, Object>> economies = getEconomySystems();
        if (!economies.containsKey(economyKey)) return false;

        Map<String, Object> economyData = economies.get(economyKey);
        if (!(Boolean) economyData.get("enabled")) return false;

        String pluginName = (String) economyData.get("plugin");
        if (pluginName != null && !pluginName.isEmpty()) {
            return plugin.getServer().getPluginManager().getPlugin(pluginName) != null;
        }

        return true;
    }

    /**
     * 添加新的经济系统
     */
    public boolean addEconomySystem(String key, String displayName, double cost, double customPrice, String pluginName, String permission) {
        try {
            moneyConfig.set("economy-systems." + key + ".display-name", displayName);
            moneyConfig.set("economy-systems." + key + ".enabled", true);
            moneyConfig.set("economy-systems." + key + ".cost", cost);
            moneyConfig.set("economy-systems." + key + ".custom-price", customPrice);
            moneyConfig.set("economy-systems." + key + ".plugin", pluginName);
            moneyConfig.set("economy-systems." + key + ".permission", permission);

            saveMoneyConfig();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("添加经济系统失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 重载配置
     */
    public void reloadMoneyConfig() {
        try {
            if (moneyFile == null) {
                moneyFile = new File(plugin.getDataFolder(), "money.yml");
            }
            moneyConfig = YamlConfiguration.loadConfiguration(moneyFile);
            plugin.getLogger().info("money.yml 重载完成");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "重载 money.yml 失败", e);
            moneyConfig = new YamlConfiguration(); // 创建空的配置作为后备
        }
    }

    /**
     * 保存配置
     */
    public void saveMoneyConfig() {
        try {
            if (moneyConfig != null && moneyFile != null) {
                moneyConfig.save(moneyFile);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("保存money.yml失败: " + e.getMessage());
        }
    }
}