package xyz.tolite.greatTitle.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.database.DatabaseManager;
import xyz.tolite.greatTitle.model.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TitleManager {

    private final GreatTitle plugin;
    private final DatabaseManager databaseManager;
    private final BuffManager buffManager;
    private final ParticleManager particleManager;
    private final Map<String, Title> titles;
    private final Map<UUID, Map<String, PlayerTitle>> playerTitles;
    private final Map<UUID, String> activeTitles;
    private final Map<UUID, Boolean> headDisplaySettings;
    private final Map<UUID, Boolean> tabListDisplaySettings;
    private final Map<UUID, Boolean> chatDisplaySettings;
    private final Map<UUID, Long> switchCooldowns;
    private final Map<String, Reward> rewards;
    private final Map<UUID, Map<String, PlayerReward>> playerRewards;
    // 添加粒子显示设置映射
    private final Map<UUID, Boolean> particleDisplaySettings = new ConcurrentHashMap<>();

    // 日期格式解析器
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");

    public TitleManager(GreatTitle plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.buffManager = plugin.getBuffManager();
        this.particleManager = plugin.getParticleManager();
        this.titles = new ConcurrentHashMap<>();
        this.playerTitles = new ConcurrentHashMap<>();
        this.activeTitles = new ConcurrentHashMap<>();
        this.headDisplaySettings = new ConcurrentHashMap<>();
        this.tabListDisplaySettings = new ConcurrentHashMap<>();
        this.chatDisplaySettings = new ConcurrentHashMap<>();
        this.switchCooldowns = new ConcurrentHashMap<>();
        this.rewards = new ConcurrentHashMap<>();
        this.playerRewards = new ConcurrentHashMap<>();

        // 设置时区
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public void loadTitles() {
        titles.clear();
        FileConfiguration titlesConfig = plugin.getConfigManager().getTitlesConfig();

        if (titlesConfig.getKeys(false).isEmpty()) {
            createDefaultTitles();
            return;
        }

        for (String key : titlesConfig.getKeys(false)) {
            if (titlesConfig.isConfigurationSection(key)) {
                try {
                    Title title = parseTitleFromConfig(key, titlesConfig);
                    if (title != null) {
                        titles.put(title.getId(), title);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("加载称号 " + key + " 时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        plugin.getLogger().info("已加载 " + titles.size() + " 个称号");
    }

    private Title parseTitleFromConfig(String titleId, FileConfiguration config) {
        String displayName = config.getString(titleId + ".display-name", "未命名称号");
        String content = config.getString(titleId + ".content", "[称号]");
        TitleType type = TitleType.valueOf(config.getString(titleId + ".type", "NOT").toUpperCase());
        double price = config.getDouble(titleId + ".price", 0);
        int points = config.getInt(titleId + ".points", 0);
        int coins = config.getInt(titleId + ".coins", 0);
        String permission = config.getString(titleId + ".permission", "");
        String requiredItem = config.getString(titleId + ".required-item", "");
        boolean rgb = config.getBoolean(titleId + ".rgb", false);
        int duration = config.getInt(titleId + ".duration", 0);
        int switchCooldown = config.getInt(titleId + ".switch-cooldown", 0);

        // 解析介绍
        List<String> description = config.getStringList(titleId + ".description");

        // 解析商城显示设置
        boolean showInShop = config.getBoolean(titleId + ".show-in-shop", true);
        long shopExpireTime = 0;

        // 解析下架时间
        String shopExpireTimeStr = config.getString(titleId + ".shop-expire-time", "");
        if (!shopExpireTimeStr.isEmpty()) {
            try {
                Date date = dateFormat.parse(shopExpireTimeStr);
                shopExpireTime = date.getTime();
            } catch (ParseException e) {
                plugin.getLogger().warning("称号 " + titleId + " 的下架时间格式错误: " + shopExpireTimeStr);
                plugin.getLogger().warning("正确格式: yyyy-MM-dd-HH-mm (例如: 2024-12-31-23-59)");
            }
        }

        // 解析自定义经济系统字段
        String customEconomy = config.getString(titleId + ".custom-economy", "");
        double customEconomyCost = config.getDouble(titleId + ".custom-economy-cost", 0.0);
        boolean isCustom = config.getBoolean(titleId + ".is-custom", false);

        // 解析BUFFS
        List<Buff> buffs = new ArrayList<>();
        if (config.isConfigurationSection(titleId + ".buffs")) {
            for (String buffKey : config.getConfigurationSection(titleId + ".buffs").getKeys(false)) {
                String effectType = config.getString(titleId + ".buffs." + buffKey + ".type");
                int amplifier = config.getInt(titleId + ".buffs." + buffKey + ".amplifier", 0);
                int buffDuration = config.getInt(titleId + ".buffs." + buffKey + ".duration", 0);
                boolean ambient = config.getBoolean(titleId + ".buffs." + buffKey + ".ambient", true);
                boolean showParticles = config.getBoolean(titleId + ".buffs." + buffKey + ".show-particles", true);
                boolean showIcon = config.getBoolean(titleId + ".buffs." + buffKey + ".show-icon", true);

                if (effectType != null) {
                    buffs.add(new Buff(effectType, amplifier, buffDuration, ambient, showParticles, showIcon));
                }
            }
        }

        // 解析粒子效果
        List<String> particleEffects = config.getStringList(titleId + ".particle-effects");

        // 解析多种货币成本
        List<CurrencyCost> currencyCosts = new ArrayList<>();
        if (config.isConfigurationSection(titleId + ".currencies")) {
            for (String currencyKey : config.getConfigurationSection(titleId + ".currencies").getKeys(false)) {
                double amount = config.getDouble(titleId + ".currencies." + currencyKey, 0);
                if (amount > 0) {
                    currencyCosts.add(new CurrencyCost(currencyKey, amount));
                }
            }
        }

        // 使用完整的构造函数，包含自定义经济系统字段
        return new Title(titleId, displayName, content, type, price, points, coins,
                permission, requiredItem, rgb, duration, buffs, particleEffects,
                switchCooldown, currencyCosts, description, showInShop, shopExpireTime,
                customEconomy, customEconomyCost, isCustom);
    }

    private void createDefaultTitles() {
        plugin.getLogger().info("创建默认称号...");

        // 创建一些默认称号 - 使用新的构造函数
        List<Title> defaultTitles = new ArrayList<>();

        // 新手称号 - 无条件
        List<String> newbieDesc = Arrays.asList(
                "§7欢迎来到服务器的初始称号",
                "§7拥有这个称号代表你刚刚开始冒险",
                "",
                "§e效果:",
                "  §7- 无特殊效果"
        );

        defaultTitles.add(new Title(
                "newbie",
                "§a新手",
                "§a[新手] ",
                TitleType.NOT,
                0, 0, 0,
                "", "",
                false, 0,
                new ArrayList<>(),
                new ArrayList<>(),
                0,
                new ArrayList<>(),
                newbieDesc,
                true,
                0,
                "", 0.0, false  // 自定义经济系统字段的默认值
        ));

        // VIP称号 - 金币购买
        List<Buff> vipBuffs = Arrays.asList(
                new Buff("SPEED", 1, 0, true, true, true),
                new Buff("JUMP", 1, 0, true, true, true)
        );
        List<String> vipParticles = Arrays.asList("flame", "heart");
        List<CurrencyCost> vipCurrencies = Arrays.asList(
                new CurrencyCost("VAULT", 1000.0),
                new CurrencyCost("PLAYER_POINTS", 500)
        );
        List<String> vipDesc = Arrays.asList(
                "§6尊贵的VIP会员称号",
                "§7展示你在服务器中的特殊身份",
                "",
                "§e效果:",
                "  §7- §b速度 I (永久)",
                "  §7- §a跳跃提升 I (永久)",
                "  §7- §d火焰粒子效果",
                "  §7- §c爱心粒子效果"
        );

        // 设置VIP下架时间为2024-12-31-23-59
        long vipExpireTime;
        try {
            Date date = dateFormat.parse("2024-12-31-23-59");
            vipExpireTime = date.getTime();
        } catch (ParseException e) {
            vipExpireTime = 0;
        }

        defaultTitles.add(new Title(
                "vip",
                "§6VIP",
                "§6[VIP] ",
                TitleType.VAULT,
                1000, 0, 0,
                "", "",
                true, 30,
                vipBuffs,
                vipParticles,
                10,
                vipCurrencies,
                vipDesc,
                true,
                vipExpireTime,
                "", 0.0, false  // 自定义经济系统字段的默认值
        ));

        // 土豪称号 - 点券购买
        List<Buff> richBuffs = Arrays.asList(
                new Buff("INCREASE_DAMAGE", 1, 0, true, true, true)
        );
        List<String> richParticles = Arrays.asList("crit", "magic_crit");
        List<CurrencyCost> richCurrencies = Arrays.asList(
                new CurrencyCost("PLAYER_POINTS", 800),
                new CurrencyCost("COIN", 50)
        );
        List<String> richDesc = Arrays.asList(
                "§e彰显财富的豪华称号",
                "§7拥有这个称号证明你是真正的土豪",
                "",
                "§e效果:",
                "  §7- §c力量 I (永久)",
                "  §7- §6暴击粒子效果",
                "  §7- §b魔法暴击粒子效果"
        );

        // 设置土豪下架时间为2024-12-01-18-00
        long richExpireTime;
        try {
            Date date = dateFormat.parse("2024-12-01-18-00");
            richExpireTime = date.getTime();
        } catch (ParseException e) {
            richExpireTime = 0;
        }

        defaultTitles.add(new Title(
                "rich",
                "§e土豪",
                "§e[土豪] ",
                TitleType.PLAYER_POINTS,
                0, 500, 0,
                "", "",
                true, 7,
                richBuffs,
                richParticles,
                5,
                richCurrencies,
                richDesc,
                true,
                richExpireTime,
                "", 0.0, false  // 自定义经济系统字段的默认值
        ));

        // 管理员称号 - 权限获取
        List<Buff> adminBuffs = Arrays.asList(
                new Buff("SPEED", 2, 0, true, true, true),
                new Buff("JUMP", 2, 0, true, true, true),
                new Buff("INCREASE_DAMAGE", 2, 0, true, true, true)
        );
        List<String> adminParticles = Arrays.asList("enchant", "portal", "dragon_breath");
        List<String> adminDesc = Arrays.asList(
                "§4服务器管理员的专属称号",
                "§7拥有最高权限的管理者身份象征",
                "",
                "§e效果:",
                "  §7- §b速度 II (永久)",
                "  §7- §a跳跃提升 II (永久)",
                "  §7- §c力量 II (永久)",
                "  §7- §5附魔台粒子效果",
                "  §7- §d末地门粒子效果",
                "  §7- §6龙息粒子效果"
        );

        defaultTitles.add(new Title(
                "admin",
                "§4管理员",
                "§4[管理员] ",
                TitleType.PERMISSION,
                0, 0, 0,
                "greattitle.admin", "",
                true, 0,
                adminBuffs,
                adminParticles,
                0,
                new ArrayList<>(),
                adminDesc,
                false,
                0,
                "", 0.0, false  // 自定义经济系统字段的默认值
        ));

        for (Title title : defaultTitles) {
            titles.put(title.getId(), title);
            saveTitleToConfig(title);
        }

        plugin.getLogger().info("已创建 " + defaultTitles.size() + " 个默认称号");
    }

    public void loadPlayerData(Player player) {
        if (databaseManager == null) {
            plugin.getLogger().warning("数据库管理器为空，无法加载玩家数据: " + player.getName());
            return;
        }

        UUID uuid = player.getUniqueId();
        try {
            Map<String, PlayerTitle> playerTitleMap = databaseManager.getPlayerTitles(uuid);
            playerTitles.put(uuid, playerTitleMap);

            String activeTitle = databaseManager.getActiveTitle(uuid);
            if (activeTitle != null) {
                activeTitles.put(uuid, activeTitle);
            }

            boolean headDisplay = databaseManager.getHeadDisplaySetting(uuid);
            headDisplaySettings.put(uuid, headDisplay);

            boolean tabListDisplay = databaseManager.getTabListDisplaySetting(uuid);
            tabListDisplaySettings.put(uuid, tabListDisplay);

            boolean chatDisplay = databaseManager.getChatDisplaySetting(uuid);
            chatDisplaySettings.put(uuid, chatDisplay);

            updatePlayerDisplay(player);

            // 给新玩家一些默认称号
            if (playerTitleMap.isEmpty()) {
                giveDefaultTitles(player);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("加载玩家数据失败: " + player.getName() + " - " + e.getMessage());
        }

        // 加载粒子显示设置
        boolean particleDisplay = databaseManager.getParticleDisplaySetting(uuid);
        particleDisplaySettings.put(uuid, particleDisplay);
    }

    // 添加粒子显示设置方法
    public boolean isParticleDisplayEnabled(Player player) {
        return particleDisplaySettings.getOrDefault(player.getUniqueId(), true);
    }

    public void setParticleDisplay(Player player, boolean display) {
        UUID uuid = player.getUniqueId();
        particleDisplaySettings.put(uuid, display);
        if (databaseManager != null) {
            databaseManager.setParticleDisplaySetting(uuid, display);
        }

        // 如果关闭粒子显示，立即移除粒子效果
        if (!display) {
            String activeTitleId = getActiveTitle(player);
            if (activeTitleId != null) {
                Title title = getTitle(activeTitleId);
                if (title != null && title.hasParticleEffects()) {
                    plugin.getParticleManager().removeParticles(player, title.getParticleEffects());
                }
            }
        } else {
            // 如果开启粒子显示，重新应用粒子效果
            String activeTitleId = getActiveTitle(player);
            if (activeTitleId != null) {
                applyActiveTitleEffects(player, activeTitleId);
            }
        }
    }

    public void loadPlayerRewards(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, PlayerReward> rewards = databaseManager.getPlayerRewards(uuid);
        playerRewards.put(uuid, rewards);
    }

    public void unloadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        playerTitles.remove(uuid);
        activeTitles.remove(uuid);
        headDisplaySettings.remove(uuid);
        tabListDisplaySettings.remove(uuid);
        chatDisplaySettings.remove(uuid);
        particleDisplaySettings.remove(uuid);

        // 清理自定义称号创建状态
        plugin.getCustomTitleManager().cancelCustomTitleCreation(player);
    }

    public void unloadPlayerRewards(Player player) {
        playerRewards.remove(player.getUniqueId());
    }

    private void giveDefaultTitles(Player player) {
        // 给新玩家一些默认的无条件称号
        for (Title title : titles.values()) {
            if (title.getType() == TitleType.NOT && title.getDuration() == 0) {
                giveTitle(player, title.getId(), 0);
            }
        }
    }

    public boolean giveTitle(Player player, String titleId, int customDuration) {
        if (databaseManager == null) {
            plugin.getLogger().warning("数据库管理器为空，无法给予称号");
            return false;
        }

        UUID uuid = player.getUniqueId();
        Title title = titles.get(titleId);
        if (title == null) {
            plugin.getLogger().warning("尝试给予不存在的称号: " + titleId);
            return false;
        }

        // 计算实际持续时间
        int duration = customDuration > 0 ? customDuration : title.getDuration();
        long obtainTime = System.currentTimeMillis();
        long expireTime = duration > 0 ? obtainTime + (duration * 24L * 60 * 60 * 1000) : 0;

        Map<String, PlayerTitle> playerTitleMap = playerTitles.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        // 如果玩家已经拥有该称号，更新过期时间
        if (playerTitleMap.containsKey(titleId)) {
            PlayerTitle existing = playerTitleMap.get(titleId);
            if (duration > 0) {
                long newExpireTime = Math.max(existing.getExpireTime(), expireTime);
                existing.setExpireTime(newExpireTime);
            }
            databaseManager.updatePlayerTitle(uuid, titleId, obtainTime, expireTime);
        } else {
            // 新增称号
            PlayerTitle playerTitle = new PlayerTitle(titleId, obtainTime, expireTime);
            playerTitleMap.put(titleId, playerTitle);
            databaseManager.addPlayerTitle(uuid, titleId, obtainTime, expireTime);
            player.sendMessage("§a你获得了新称号: " + title.getDisplayName());
        }

        return true;
    }

    public boolean setActiveTitle(Player player, String titleId) {
        if (databaseManager == null) {
            plugin.getLogger().warning("数据库管理器为空，无法设置活跃称号");
            return false;
        }

        // 检查冷却时间
        if (isInCooldown(player)) {
            long remaining = getCooldownRemaining(player);
            player.sendMessage("§c切换称号冷却中，请等待 " + (remaining / 1000) + " 秒");
            return false;
        }

        UUID uuid = player.getUniqueId();
        if (titleId == null) {
            // 移除当前称号
            removeActiveTitleEffects(player);
            activeTitles.remove(uuid);
            databaseManager.setActiveTitle(uuid, null);
            updatePlayerDisplay(player);
            return true;
        }

        Map<String, PlayerTitle> playerTitleMap = playerTitles.get(uuid);
        if (playerTitleMap != null && playerTitleMap.containsKey(titleId)) {
            // 检查称号是否过期
            PlayerTitle playerTitle = playerTitleMap.get(titleId);
            if (playerTitle.isExpired()) {
                removeExpiredTitle(player, titleId);
                player.sendMessage("§c该称号已过期!");
                return false;
            }

            // 移除旧称号效果
            removeActiveTitleEffects(player);

            // 设置新称号
            activeTitles.put(uuid, titleId);
            databaseManager.setActiveTitle(uuid, titleId);

            // 应用新称号效果
            applyActiveTitleEffects(player, titleId);

            // 设置冷却时间
            setCooldown(player, titleId);

            updatePlayerDisplay(player);
            return true;
        }

        // 设置新称号后，强制更新所有显示
        if (plugin.getHeadDisplayManager() != null) {
            plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
        }

        // 更新聊天显示设置
        updatePlayerDisplay(player);
        return false;
    }

    /**
     * 获取拥有指定称号的所有玩家
     */
    public List<Player> getPlayersWithTitle(String titleId) {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasTitle(player, titleId)) {
                players.add(player);
            }
        }
        return players;
    }

    /**
     * 删除玩家的称号
     */
    public boolean removePlayerTitle(Player player, String titleId) {
        UUID playerUUID = player.getUniqueId();
        if (playerTitles.containsKey(playerUUID) && playerTitles.get(playerUUID).containsKey(titleId)) {
            playerTitles.get(playerUUID).remove(titleId);
            databaseManager.removePlayerTitle(playerUUID, titleId);

            // 如果删除的是当前使用的称号，清空活跃称号
            if (titleId.equals(getActiveTitle(player))) {
                setActiveTitle(player, null);
            }

            return true;
        }
        return false;
    }

    /**
     * 检查称号是否有Buff配置
     */
    public boolean hasBuff(String titleId) {
        Title title = getTitle(titleId);
        return title != null && title.getBuff() != null && !title.getBuff().isEmpty();
    }

    /**
     * 检查称号是否有粒子配置
     */
    public boolean hasParticle(String titleId) {
        Title title = getTitle(titleId);
        return title != null && title.getParticle() != null && !title.getParticle().isEmpty();
    }

    /**
     * 应用活跃称号的效果
     */
    public void applyActiveTitleEffects(Player player, String titleId) {
        Title title = titles.get(titleId);
        if (title == null) return;

        plugin.getLogger().info("为玩家 " + player.getName() + " 应用称号效果: " + title.getDisplayName());

        // 应用BUFFS
        if (title.hasBuffs()) {
            plugin.getBuffManager().applyBuffs(player, title.getBuffs());
            plugin.getLogger().info("应用 " + title.getBuffs().size() + " 个BUFF");
        }

        // 应用粒子效果
        if (title.hasParticleEffects()) {
            plugin.getParticleManager().applyParticles(player, title.getParticleEffects());
            plugin.getLogger().info("应用 " + title.getParticleEffects().size() + " 个粒子效果");
        }
    }

    /**
     * 移除活跃称号的效果
     */
    private void removeActiveTitleEffects(Player player) {
        String activeTitleId = activeTitles.get(player.getUniqueId());
        if (activeTitleId != null) {
            Title oldTitle = titles.get(activeTitleId);
            if (oldTitle != null) {
                plugin.getLogger().info("移除玩家 " + player.getName() + " 的称号效果: " + oldTitle.getDisplayName());

                // 移除BUFFS
                if (oldTitle.hasBuffs()) {
                    plugin.getBuffManager().removeBuffs(player, oldTitle.getBuffs());
                }

                // 移除粒子效果
                if (oldTitle.hasParticleEffects()) {
                    plugin.getParticleManager().removeParticles(player, oldTitle.getParticleEffects());
                }
            }
        }
    }

    /**
     * 重新为所有在线玩家应用效果（重载后）
     */
    public void reapplyAllEffects() {
        plugin.getLogger().info("重新为所有在线玩家应用称号效果");

        for (Player player : Bukkit.getOnlinePlayers()) {
            String activeTitleId = getActiveTitle(player);
            if (activeTitleId != null) {
                applyActiveTitleEffects(player, activeTitleId);
            }
        }
    }

    /**
     * 检查玩家是否在创建自定义称号
     */
    public boolean isCreatingCustomTitle(Player player) {
        return plugin.getCustomTitleManager().isCreatingCustomTitle(player);
    }

    /**
     * 处理自定义称号名称输入
     */
    public void handleCustomTitleNameInput(Player player, String titleName) {
        plugin.getCustomTitleManager().handleCustomTitleInput(player, titleName);
    }

    /**
     * 开始自定义称号创建
     */
    public void startCustomTitleCreation(Player player) {
        // 这个方法现在应该重定向到支付选择界面
        plugin.getTitleGUI().openCustomTitleEconomySelection(player);
    }

    private boolean isInCooldown(Player player) {
        Long lastSwitch = switchCooldowns.get(player.getUniqueId());
        if (lastSwitch == null) return false;

        String activeTitleId = activeTitles.get(player.getUniqueId());
        if (activeTitleId == null) return false;

        Title title = titles.get(activeTitleId);
        if (title == null) return false;

        int cooldown = title.getSwitchCooldown();
        if (cooldown == 0) {
            cooldown = plugin.getConfigManager().getSwitchCooldown();
        }

        return System.currentTimeMillis() < lastSwitch + (cooldown * 1000L);
    }

    private long getCooldownRemaining(Player player) {
        Long lastSwitch = switchCooldowns.get(player.getUniqueId());
        if (lastSwitch == null) return 0;

        String activeTitleId = activeTitles.get(player.getUniqueId());
        if (activeTitleId == null) return 0;

        Title title = titles.get(activeTitleId);
        if (title == null) return 0;

        int cooldown = title.getSwitchCooldown();
        if (cooldown == 0) {
            cooldown = plugin.getConfigManager().getSwitchCooldown();
        }

        long remaining = (lastSwitch + (cooldown * 1000L)) - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    private void setCooldown(Player player, String titleId) {
        Title title = titles.get(titleId);
        if (title != null) {
            int cooldown = title.getSwitchCooldown();
            if (cooldown == 0) {
                cooldown = plugin.getConfigManager().getSwitchCooldown();
            }
            if (cooldown > 0) {
                switchCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    public void setHeadDisplay(Player player, boolean display) {
        if (databaseManager == null) {
            plugin.getLogger().warning("数据库管理器为空，无法设置头顶显示");
            return;
        }

        UUID uuid = player.getUniqueId();
        headDisplaySettings.put(uuid, display);
        databaseManager.setHeadDisplaySetting(uuid, display);
        updatePlayerDisplay(player);
    }

    public void setTabListDisplay(Player player, boolean display) {
        if (databaseManager == null) {
            plugin.getLogger().warning("数据库管理器为空，无法设置Tab列表显示");
            return;
        }

        UUID uuid = player.getUniqueId();
        tabListDisplaySettings.put(uuid, display);
        databaseManager.setTabListDisplaySetting(uuid, display);
        updatePlayerDisplay(player);
    }

    public void setChatDisplay(Player player, boolean display) {
        if (databaseManager == null) {
            plugin.getLogger().warning("数据库管理器为空，无法设置聊天显示");
            return;
        }

        UUID uuid = player.getUniqueId();
        chatDisplaySettings.put(uuid, display);
        databaseManager.setChatDisplaySetting(uuid, display);
        updatePlayerDisplay(player);
    }

    public void updatePlayerDisplay(Player player) {
        // 优先使用 HeadDisplayManager 来管理显示
        if (plugin.getHeadDisplayManager() != null) {
            plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
            plugin.getHeadDisplayManager().updatePlayerTabDisplay(player);
        } else {
            // 备用方案：直接设置显示名称 (确保逻辑正确)
            plugin.getLogger().warning("HeadDisplayManager 为空，使用备用显示方案");

            String activeTitleId = activeTitles.get(player.getUniqueId());
            Title title = null;
            if (activeTitleId != null) {
                title = titles.get(activeTitleId);
            }

            String prefixContent = "";
            if (title != null) {
                prefixContent = title.getContent();
                // 清理称号内容
                prefixContent = prefixContent.replace("<", "").replace(">", "").trim();
            } else {
                prefixContent = plugin.getNameConfig().getHeadPrefixDefault();
            }

            // 头顶显示
            if (plugin.getNameConfig().isHeadPrefixEnabled() &&
                    isHeadDisplayEnabled(player)) {

                String format = plugin.getNameConfig().getHeadPrefixFormat()
                        .replace("%greattitle_active%", prefixContent)
                        .replace("%player_name%", player.getName());

                // 清理格式
                format = format.replace("<", "").replace(">", "").trim();

                player.setDisplayName(format);
                plugin.getLogger().info("DEBUG: 通过TitleManager设置头顶显示: " + format);
            } else {
                player.setDisplayName(player.getName());
            }

            // Tab列表显示
            if (plugin.getNameConfig().isTabListEnabled() &&
                    isTabListDisplayEnabled(player)) {

                String format = plugin.getNameConfig().getTabListDisplayFormat()
                        .replace("%greattitle_active%", prefixContent)
                        .replace("%player_name%", player.getName());

                format = format.replace("<", "").replace(">", "").trim();
                player.setPlayerListName(format);
            } else {
                player.setPlayerListName(player.getName());
            }
        }
    }

    public void removeExpiredTitle(Player player, String titleId) {
        UUID uuid = player.getUniqueId();
        Map<String, PlayerTitle> playerTitleMap = playerTitles.get(uuid);
        if (playerTitleMap != null) {
            playerTitleMap.remove(titleId);
            databaseManager.removePlayerTitle(uuid, titleId);

            // 如果当前使用的是过期称号，取消使用
            if (titleId.equals(activeTitles.get(uuid))) {
                setActiveTitle(player, null);
                player.sendMessage("§c你使用的称号已过期，已自动取消使用!");
            }
        }
    }

    public void checkAndRemoveExpiredTitles(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, PlayerTitle> playerTitleMap = playerTitles.get(uuid);
        if (playerTitleMap != null) {
            List<String> expiredTitles = new ArrayList<>();
            for (Map.Entry<String, PlayerTitle> entry : playerTitleMap.entrySet()) {
                if (entry.getValue().isExpired()) {
                    expiredTitles.add(entry.getKey());
                }
            }

            for (String titleId : expiredTitles) {
                removeExpiredTitle(player, titleId);
            }

            if (!expiredTitles.isEmpty()) {
                player.sendMessage("§c你有 " + expiredTitles.size() + " 个称号已过期!");
            }
        }
    }

    public void checkAndRemoveExpiredTitlesForAll() {
        for (UUID uuid : playerTitles.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                checkAndRemoveExpiredTitles(player);
            }
        }
    }

    // 删除称号方法
    public boolean deleteTitle(String titleId) {
        Title title = titles.remove(titleId);
        if (title != null) {
            // 从配置文件中删除
            FileConfiguration titlesConfig = plugin.getConfigManager().getTitlesConfig();
            titlesConfig.set(titleId, null);
            plugin.getConfigManager().saveTitlesConfig();

            // 从所有玩家的活跃称号中移除
            for (Map.Entry<UUID, String> entry : activeTitles.entrySet()) {
                if (titleId.equals(entry.getValue())) {
                    UUID playerUUID = entry.getKey();
                    activeTitles.remove(playerUUID);
                    databaseManager.setActiveTitle(playerUUID, null);

                    // 通知在线玩家
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null && player.isOnline()) {
                        player.sendMessage("§c你使用的称号已被删除，已自动取消使用!");
                    }
                }
            }

            plugin.getLogger().info("已删除称号: " + titleId);
            return true;
        }
        return false;
    }

    // 加载奖励配置
    public void loadRewards() {
        rewards.clear();
        FileConfiguration rewardsConfig = plugin.getConfigManager().getRewardsConfig();

        for (String key : rewardsConfig.getKeys(false)) {
            if (rewardsConfig.isConfigurationSection(key)) {
                try {
                    String id = key;
                    String displayName = rewardsConfig.getString(key + ".display-name", "未命名奖励");
                    List<String> description = rewardsConfig.getStringList(key + ".description");
                    int requiredTitles = rewardsConfig.getInt(key + ".required-titles", 1);
                    List<String> commands = rewardsConfig.getStringList(key + ".commands");
                    String permission = rewardsConfig.getString(key + ".permission", "");
                    boolean repeatable = rewardsConfig.getBoolean(key + ".repeatable", false);

                    Reward reward = new Reward(id, displayName, description, requiredTitles, commands, permission, repeatable);
                    rewards.put(id, reward);
                } catch (Exception e) {
                    plugin.getLogger().warning("加载奖励 " + key + " 时出错: " + e.getMessage());
                }
            }
        }

        plugin.getLogger().info("已加载 " + rewards.size() + " 个奖励");
    }

    // 领取奖励
    public boolean claimReward(Player player, String rewardId) {
        UUID uuid = player.getUniqueId();
        Reward reward = rewards.get(rewardId);
        if (reward == null) return false;

        // 检查权限
        if (!reward.getPermission().isEmpty() && !player.hasPermission(reward.getPermission())) {
            return false;
        }

        // 检查称号数量
        int titleCount = getPlayerTitles(player).size();
        if (titleCount < reward.getRequiredTitles()) {
            return false;
        }

        // 检查是否可重复领取
        Map<String, PlayerReward> playerRewardMap = playerRewards.get(uuid);
        PlayerReward playerReward = playerRewardMap != null ? playerRewardMap.get(rewardId) : null;

        if (playerReward != null && !reward.isRepeatable()) {
            return false; // 已领取且不可重复
        }

        // 执行奖励命令
        for (String command : reward.getCommands()) {
            String formattedCommand = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
        }

        // 更新领取记录
        if (playerReward == null) {
            playerReward = new PlayerReward(rewardId, System.currentTimeMillis(), 1);
        } else {
            playerReward = playerReward.incrementClaim();
        }

        Map<String, PlayerReward> rewardMap = playerRewards.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        rewardMap.put(rewardId, playerReward);
        databaseManager.savePlayerReward(uuid, rewardId, playerReward.getClaimTime(), playerReward.getClaimCount());

        return true;
    }

    // 获取玩家可领取的奖励状态
    public Map<Reward, xyz.tolite.greatTitle.model.RewardStatus> getPlayerRewardStatus(Player player) {
        Map<Reward, xyz.tolite.greatTitle.model.RewardStatus> statusMap = new HashMap<>();
        int titleCount = getPlayerTitles(player).size();
        Map<String, PlayerReward> playerRewardMap = playerRewards.get(player.getUniqueId());

        for (Reward reward : rewards.values()) {
            boolean hasPermission = reward.getPermission().isEmpty() || player.hasPermission(reward.getPermission());
            boolean hasEnoughTitles = titleCount >= reward.getRequiredTitles();
            boolean canClaim = hasPermission && hasEnoughTitles;

            PlayerReward playerReward = playerRewardMap != null ? playerRewardMap.get(reward.getId()) : null;
            boolean claimed = playerReward != null;
            boolean canClaimAgain = claimed && reward.isRepeatable();

            statusMap.put(reward, new xyz.tolite.greatTitle.model.RewardStatus(canClaim, claimed, canClaimAgain, playerReward));
        }

        return statusMap;
    }

    // 获取随机称号
    public String getRandomTitle() {
        List<String> titlePool = plugin.getConfigManager().getRandomTitlePool();

        // 如果配置中指定了随机称号池，则从池中随机选择
        if (!titlePool.isEmpty()) {
            Random random = new Random();
            return titlePool.get(random.nextInt(titlePool.size()));
        }

        // 否则从所有无条件称号中随机选择
        List<String> availableTitles = new ArrayList<>();
        for (Title title : titles.values()) {
            if (title.getType() == TitleType.NOT) {
                availableTitles.add(title.getId());
            }
        }

        if (!availableTitles.isEmpty()) {
            Random random = new Random();
            return availableTitles.get(random.nextInt(availableTitles.size()));
        }

        return null; // 没有可用的随机称号
    }

    // Getters
    public String getActiveTitle(Player player) {
        return activeTitles.get(player.getUniqueId());
    }

    public Title getTitle(String titleId) {
        return titles.get(titleId);
    }

    public Map<String, PlayerTitle> getPlayerTitles(Player player) {
        return playerTitles.getOrDefault(player.getUniqueId(), new ConcurrentHashMap<>());
    }

    public Collection<Title> getAllTitles() {
        return titles.values();
    }

    public boolean hasTitle(Player player, String titleId) {
        Map<String, PlayerTitle> playerTitleMap = playerTitles.get(player.getUniqueId());
        return playerTitleMap != null && playerTitleMap.containsKey(titleId);
    }

    public boolean isHeadDisplayEnabled(Player player) {
        return headDisplaySettings.getOrDefault(player.getUniqueId(), true);
    }

    public boolean isTabListDisplayEnabled(Player player) {
        return tabListDisplaySettings.getOrDefault(player.getUniqueId(), true);
    }

    public boolean isChatDisplayEnabled(Player player) {
        return chatDisplaySettings.getOrDefault(player.getUniqueId(), true);
    }

    public PlayerTitle getPlayerTitle(Player player, String titleId) {
        Map<String, PlayerTitle> playerTitleMap = playerTitles.get(player.getUniqueId());
        return playerTitleMap != null ? playerTitleMap.get(titleId) : null;
    }

    public Collection<Reward> getAllRewards() {
        return rewards.values();
    }

    public PlayerReward getPlayerReward(Player player, String rewardId) {
        Map<String, PlayerReward> playerRewardMap = playerRewards.get(player.getUniqueId());
        return playerRewardMap != null ? playerRewardMap.get(rewardId) : null;
    }

    public int getPlayerTitleCount(Player player) {
        return getPlayerTitles(player).size();
    }

    public void createTitle(Title title) {
        titles.put(title.getId(), title);
        saveTitleToConfig(title);
    }

    private void saveTitleToConfig(Title title) {
        FileConfiguration titlesConfig = plugin.getConfigManager().getTitlesConfig();
        String path = title.getId();

        titlesConfig.set(path + ".display-name", title.getDisplayName());
        titlesConfig.set(path + ".content", title.getContent());
        titlesConfig.set(path + ".type", title.getType().name());
        titlesConfig.set(path + ".price", title.getPrice());
        titlesConfig.set(path + ".points", title.getPoints());
        titlesConfig.set(path + ".coins", title.getCoins());
        titlesConfig.set(path + ".permission", title.getPermission());
        titlesConfig.set(path + ".required-item", title.getRequiredItem());
        titlesConfig.set(path + ".rgb", title.isRgb());
        titlesConfig.set(path + ".duration", title.getDuration());
        titlesConfig.set(path + ".switch-cooldown", title.getSwitchCooldown());
        titlesConfig.set(path + ".description", title.getDescription());
        titlesConfig.set(path + ".show-in-shop", title.isShowInShop());

        // 保存自定义经济系统字段
        titlesConfig.set(path + ".custom-economy", title.getCustomEconomy());
        titlesConfig.set(path + ".custom-economy-cost", title.getCustomEconomyCost());
        titlesConfig.set(path + ".is-custom", title.isCustom());

        // 保存下架时间
        if (title.getShopExpireTime() > 0) {
            String expireTimeStr = dateFormat.format(new Date(title.getShopExpireTime()));
            titlesConfig.set(path + ".shop-expire-time", expireTimeStr);
        } else {
            titlesConfig.set(path + ".shop-expire-time", "");
        }

        // 保存BUFFS
        if (!title.getBuffs().isEmpty()) {
            for (int i = 0; i < title.getBuffs().size(); i++) {
                Buff buff = title.getBuffs().get(i);
                String buffPath = path + ".buffs.buff" + i;
                titlesConfig.set(buffPath + ".type", buff.getEffectType());
                titlesConfig.set(buffPath + ".amplifier", buff.getAmplifier());
                titlesConfig.set(buffPath + ".duration", buff.getDuration());
                titlesConfig.set(buffPath + ".ambient", buff.isAmbient());
                titlesConfig.set(buffPath + ".show-particles", buff.showParticles());
                titlesConfig.set(buffPath + ".show-icon", buff.showIcon());
            }
        } else {
            titlesConfig.set(path + ".buffs", null);
        }

        // 保存粒子效果
        titlesConfig.set(path + ".particle-effects", title.getParticleEffects());

        // 保存货币成本
        if (!title.getCurrencyCosts().isEmpty()) {
            for (CurrencyCost cost : title.getCurrencyCosts()) {
                titlesConfig.set(path + ".currencies." + cost.getCurrencyType(), cost.getAmount());
            }
        } else {
            titlesConfig.set(path + ".currencies", null);
        }

        plugin.getConfigManager().saveTitlesConfig();
    }

    public void registerCustomTitleCreation(Player player, String economyKey) {
        plugin.getTitleManager().registerCustomTitleCreation(player, economyKey);
    }

    /**
     * 获取指定页面的商店称号列表
     */
    public List<Title> getTitlesForShop(int page) {
        try {
            List<Title> allTitles = new ArrayList<>(getAllTitles());
            Map<String, PlayerTitle> playerTitles = getPlayerTitles(null);

            // 过滤掉玩家已经拥有的永久称号
            allTitles.removeIf(title -> {
                PlayerTitle playerTitle = playerTitles.get(title.getId());
                return playerTitle != null && playerTitle.isPermanent();
            });

            // 计算分页
            int itemsPerPage = 21; // 3行 * 7列
            int totalPages = Math.max(1, (int) Math.ceil((double) allTitles.size() / itemsPerPage));

            if (page > totalPages) page = totalPages;
            if (page < 1) page = 1;

            int startIndex = (page - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, allTitles.size());

            return allTitles.subList(startIndex, endIndex);
        } catch (Exception e) {
            plugin.getLogger().warning("获取商店称号列表时出错: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}