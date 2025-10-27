package xyz.tolite.greatTitle.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.tolite.greatTitle.GreatTitle;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIConfigManager {

    private final GreatTitle plugin;
    private File guiFolder;
    private File customTitleFile;
    private FileConfiguration customTitleConfig;

    public GUIConfigManager(GreatTitle plugin) {
        this.plugin = plugin;
        setupGUIConfig();
    }

    private void setupGUIConfig() {
        guiFolder = new File(plugin.getDataFolder(), "gui");
        if (!guiFolder.exists()) {
            guiFolder.mkdirs();
        }

        customTitleFile = new File(guiFolder, "custom_title.yml");
        if (!customTitleFile.exists()) {
            plugin.saveResource("gui/custom_title.yml", false);
        }
        customTitleConfig = YamlConfiguration.loadConfiguration(customTitleFile);
    }

    /**
     * 获取自定义称号GUI标题
     */
    public String getCustomTitleGUITitle() {
        return customTitleConfig.getString("gui-title", "自定义称号");
    }

    /**
     * 获取GUI大小
     */
    public int getCustomTitleGUISize() {
        return customTitleConfig.getInt("gui-size", 27);
    }

    /**
     * 获取经济系统选择物品的材质
     */
    public String getEconomyItemMaterial(String economyKey) {
        return customTitleConfig.getString("economy-items." + economyKey + ".material", "PAPER");
    }

    /**
     * 获取经济系统选择物品的显示名称
     */
    public String getEconomyItemName(String economyKey) {
        return customTitleConfig.getString("economy-items." + economyKey + ".display-name", "&6" + economyKey);
    }

    /**
     * 获取经济系统选择物品的Lore
     */
    public List<String> getEconomyItemLore(String economyKey) {
        return customTitleConfig.getStringList("economy-items." + economyKey + ".lore");
    }

    /**
     * 获取经济系统选择物品的位置
     */
    public int getEconomyItemSlot(String economyKey) {
        return customTitleConfig.getInt("economy-items." + economyKey + ".slot", 10);
    }

    /**
     * 获取返回按钮的配置
     */
    public Map<String, Object> getBackButtonConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("material", customTitleConfig.getString("back-button.material", "ARROW"));
        config.put("display-name", customTitleConfig.getString("back-button.display-name", "&c返回"));
        config.put("slot", customTitleConfig.getInt("back-button.slot", 22));
        return config;
    }

    /**
     * 重载配置
     */
    public void reloadGUIConfig() {
        customTitleConfig = YamlConfiguration.loadConfiguration(customTitleFile);
    }

    /**
     * 保存配置
     */
    public void saveGUIConfig() {
        try {
            customTitleConfig.save(customTitleFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存custom_title.yml失败: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        reloadGUIConfig();
    }
}