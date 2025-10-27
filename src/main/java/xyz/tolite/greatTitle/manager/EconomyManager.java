package xyz.tolite.greatTitle.manager;

import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.model.Title;
import xyz.tolite.greatTitle.model.TitleType;

import java.util.*;
import java.util.logging.Level;

public class EconomyManager {

    private final GreatTitle plugin;
    private final MoneyConfigManager moneyConfigManager;
    private Economy vaultEconomy;
    private PlayerPointsAPI playerPointsAPI;
    private Object playerPoints;
    private boolean vaultEnabled = false;
    private boolean playerPointsEnabled = false;

    /* 自定义经济处理器映射 */
    private final Map<String, CustomEconomyHandler> customEconomyHandlers = new HashMap<>();

    public EconomyManager(GreatTitle plugin) {
        this.plugin = plugin;
        this.moneyConfigManager = plugin.getMoneyConfigManager();
        setupEconomy();
        setupPlayerPoints();
        setupCustomEconomies();
    }

    /* ===========================================================
     *  1. 初始化各经济系统
     * =========================================================== */
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                vaultEconomy = rsp.getProvider();
                vaultEnabled = true;
                plugin.getLogger().info("Vault经济系统已连接");
                return;
            }
        }
        plugin.getLogger().warning("Vault未找到，金币经济功能将不可用");
    }

    private void setupPlayerPoints() {
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            playerPoints = PlayerPoints.getInstance();
            playerPointsAPI = ((PlayerPoints) playerPoints).getAPI();
            playerPointsEnabled = true;
            plugin.getLogger().info("PlayerPoints点券系统已连接");
        } else {
            plugin.getLogger().warning("PlayerPoints未找到，点券功能将不可用");
        }
    }

    private void setupCustomEconomies() {
        /* 预留：Tokens、Gems 等可在此注册 */
    }

    /* ===========================================================
     *  2. 注册 / 获取
     * =========================================================== */
    public void registerCustomEconomy(String economyKey, CustomEconomyHandler handler) {
        customEconomyHandlers.put(Objects.requireNonNull(economyKey), handler);
        plugin.getLogger().info("已注册自定义经济系统: " + economyKey);
    }

    /* ===========================================================
     *  3. 通用购买检查
     * =========================================================== */
    public boolean canPurchase(Player player, Title title) {
        if (player == null || title == null) return false;
        TitleType type = title.getType();
        switch (type) {
            case NOT:
                return true;
            case VAULT:
                return vaultEnabled && vaultEconomy.has(player, title.getPrice());
            case PLAYER_POINTS:
                return playerPointsEnabled && playerPointsAPI.look(player.getUniqueId()) >= title.getPoints();
            case COIN:
                return getPlayerCoins(player) >= title.getCoins();
            case ITEM_STACK:
                /* 物品检查留给你实现 */
                return true;
            case PERMISSION:
                return title.getPermission() != null && player.hasPermission(title.getPermission());
            case ACTIVITY:
                return true; // 活动逻辑
            default:
                return false;
        }
    }

    /* ===========================================================
     *  4. 通用购买
     * =========================================================== */
    public boolean purchaseTitle(Player player, Title title) {
        if (!canPurchase(player, title)) return false;

        TitleType type = title.getType();
        boolean success = false;

        switch (type) {
            case NOT:
                success = true;
                break;
            case VAULT:
                if (vaultEnabled)
                    success = vaultEconomy.withdrawPlayer(player, title.getPrice()).transactionSuccess();
                break;
            case PLAYER_POINTS:
                if (playerPointsEnabled)
                    success = playerPointsAPI.take(player.getUniqueId(), title.getPoints());
                break;
            case COIN:
                success = takePlayerCoins(player, title.getCoins());
                break;
            case ITEM_STACK:
                success = true; // 留给你实现
                break;
            case PERMISSION:
            case ACTIVITY:
                success = true;
                break;
        }

        if (success) {
            plugin.getTitleManager().giveTitle(player, title.getId(), title.getDuration());
        }
        return success;
    }

    /* ===========================================================
     *  5. 检查玩家是否可以购买自定义称号
     * =========================================================== */
    public boolean canPurchaseCustomTitle(Player player, String economyKey) {
        if (player == null || economyKey == null) {
            return false;
        }

        try {
            double cost = moneyConfigManager.getEconomyCustomPrice(economyKey);
            if (cost < 0) {
                plugin.getLogger().warning("经济系统 " + economyKey + " 的自定义称号价格未配置或为负数");
                return false;
            }

            switch (economyKey.toLowerCase()) {
                case "vault":
                    if (!vaultEnabled) {
                        plugin.getLogger().warning("Vault经济系统未启用");
                        return false;
                    }
                    return vaultEconomy.has(player, cost);

                case "playerpoints":
                    if (!playerPointsEnabled) {
                        plugin.getLogger().warning("PlayerPoints系统未启用");
                        return false;
                    }
                    return playerPointsAPI.look(player.getUniqueId()) >= (int) cost;

                case "titlecoins":
                    return getPlayerCoins(player) >= (int) cost;

                default:
                    CustomEconomyHandler handler = customEconomyHandlers.get(economyKey);
                    if (handler != null) {
                        return handler.hasEnough(player, cost);
                    } else {
                        plugin.getLogger().warning("未知的经济系统: " + economyKey);
                        return false;
                    }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "检查自定义称号购买资格时出错: " + e.getMessage(), e);
            return false;
        }
    }

    /* ===========================================================
     *  6. 自定义称号购买（修复版本）
     * =========================================================== */
    public boolean purchaseCustomTitle(Player player, String economyKey, String titleName) {
        if (player == null || economyKey == null || titleName == null || titleName.trim().isEmpty()) {
            return false;
        }

        // 验证称号名称
        if (!moneyConfigManager.validateCustomTitleName(titleName)) {
            player.sendMessage("§c称号名称不符合要求!");
            player.sendMessage("§7长度: " + moneyConfigManager.getCustomTitleMinLength() + "-" +
                    moneyConfigManager.getCustomTitleMaxLength() + "字符");
            player.sendMessage("§7不能包含禁止词语");
            return false;
        }

        // 检查余额
        if (!canPurchaseCustomTitle(player, economyKey)) {
            player.sendMessage("§c无法使用此支付方式，请检查余额或权限!");
            return false;
        }

        double cost = moneyConfigManager.getEconomyCustomPrice(economyKey);
        boolean success = false;

        switch (economyKey.toLowerCase()) {
            case "vault":
                if (vaultEnabled) success = vaultEconomy.withdrawPlayer(player, cost).transactionSuccess();
                break;
            case "playerpoints":
                if (playerPointsEnabled) success = playerPointsAPI.take(player.getUniqueId(), (int) cost);
                break;
            case "titlecoins":
                success = takePlayerCoins(player, (int) cost);
                break;
            default:
                CustomEconomyHandler h = customEconomyHandlers.get(economyKey);
                if (h != null) success = h.withdraw(player, cost);
                break;
        }

        if (success) {
            createCustomTitle(player, titleName);
            String economyDisplayName = moneyConfigManager.getEconomyDisplayName(economyKey);
            player.sendMessage("§a成功扣除 " + cost + " " + economyDisplayName);
            player.sendMessage("§a成功创建自定义称号: " + titleName.replace("&", "§"));
            player.sendMessage("§7你可以在称号仓库中找到并使用它!");
        } else {
            player.sendMessage("§c购买失败，请检查余额或联系管理员!");
        }
        return success;
    }

    /**
     * 重载方法：不带 titleName 参数的版本（向后兼容）
     */
    public boolean purchaseCustomTitle(Player player, String economyKey) {
        // 这个方法不应该被直接调用，应该使用带 titleName 的版本
        plugin.getLogger().warning("错误调用：purchaseCustomTitle 需要 titleName 参数");
        return false;
    }

    /* ===========================================================
     *  7. 称号币（数据库版）
     * =========================================================== */
    public int getPlayerCoins(Player player) {
        return plugin.getDatabaseManager().getPlayerCoins(player.getUniqueId());
    }

    public boolean givePlayerCoins(Player player, int amount) {
        if (amount <= 0) return false;
        return plugin.getDatabaseManager().setPlayerCoins(player.getUniqueId(), getPlayerCoins(player) + amount);
    }

    public boolean takePlayerCoins(Player player, int amount) {
        if (amount <= 0) return false;
        int current = getPlayerCoins(player);
        if (current < amount) return false;
        return plugin.getDatabaseManager().setPlayerCoins(player.getUniqueId(), current - amount);
    }

    public boolean setPlayerCoins(Player player, int amount) {
        if (amount < 0) return false;
        return plugin.getDatabaseManager().setPlayerCoins(player.getUniqueId(), amount);
    }

    public boolean hasEnoughCoins(Player player, int amount) {
        return getPlayerCoins(player) >= amount;
    }

    /* ===========================================================
     *  8. 余额查询（兼容第二份代码）
     * =========================================================== */
    public double getBalance(Player player, String economyKey) {
        if (economyKey == null) return 0;
        switch (economyKey.toLowerCase()) {
            case "vault":
                return vaultEnabled ? vaultEconomy.getBalance(player) : 0;
            case "playerpoints":
                return playerPointsEnabled ? playerPointsAPI.look(player.getUniqueId()) : 0;
            case "titlecoins":
                return getPlayerCoins(player);
            default:
                CustomEconomyHandler h = customEconomyHandlers.get(economyKey);
                return h == null ? 0 : h.getBalance(player);
        }
    }

    /* ===========================================================
     *  9. 创建自定义称号（修复版本）
     * =========================================================== */
    private void createCustomTitle(Player player, String titleName) {
        String titleId = "custom_" + player.getUniqueId().toString().substring(0, 8) + "_" + System.currentTimeMillis();

        FileConfiguration titlesCfg = plugin.getConfigManager().getTitlesConfig();

        // 修复：使用正确的配置路径（直接使用 titleId 作为根路径）
        titlesCfg.set(titleId + ".display-name", titleName.replace("&", "§"));
        titlesCfg.set(titleId + ".content", titleName.replace("&", "§"));
        titlesCfg.set(titleId + ".type", "NOT");
        titlesCfg.set(titleId + ".duration", 0);
        titlesCfg.set(titleId + ".show-in-shop", false);
        titlesCfg.set(titleId + ".custom", true);
        titlesCfg.set(titleId + ".owner", player.getUniqueId().toString());

        plugin.getConfigManager().saveTitlesConfig();
        plugin.getTitleManager().loadTitles();
        plugin.getTitleManager().giveTitle(player, titleId, 0);
    }

    /* ===========================================================
     *  10. Getter
     * =========================================================== */
    public boolean isVaultEnabled()        { return vaultEnabled; }
    public boolean isPlayerPointsEnabled() { return playerPointsEnabled; }

    /* ===========================================================
     *  11. 自定义经济处理器接口
     * =========================================================== */
    public interface CustomEconomyHandler {
        boolean hasEnough(Player player, double amount);
        boolean withdraw(Player player, double amount);
        double  getBalance(Player player);
    }

    /**
     * 检查玩家是否可以支付自定义称号（供 GUI 调用）
     */
    public boolean canPlayerAffordCustomTitle(Player player, String economyKey) {
        try {
            return canPurchaseCustomTitle(player, economyKey);
        } catch (Exception e) {
            plugin.getLogger().warning("检查玩家余额时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查玩家余额 - 修复版本
     */
    public boolean checkPlayerBalance(Player player, String economyKey, double amount) {
        try {
            if (economyKey == null || economyKey.isEmpty()) {
                plugin.getLogger().warning("经济系统键为空，无法检查玩家余额");
                return false;
            }

            switch (economyKey.toUpperCase()) {
                case "VAULT":
                    if (vaultEnabled && vaultEconomy != null) {
                        return vaultEconomy.has(player, amount);
                    }
                    break;

                case "PLAYER_POINTS":
                    if (playerPointsEnabled && playerPointsAPI != null) {
                        int points = playerPointsAPI.look(player.getUniqueId());
                        return points >= (int) amount;
                    }
                    break;

                case "TITLECOINS":
                    // 处理自定义 Coin 系统
                    return checkCoinBalance(player, amount);

                default:
                    plugin.getLogger().warning("未知的经济系统: " + economyKey);
                    break;
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "检查玩家余额时出错: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查Coin余额
     */
    private boolean checkCoinBalance(Player player, double amount) {
        try {
            // 从数据库获取玩家Coin余额
            int coins = plugin.getDatabaseManager().getPlayerCoins(player.getUniqueId());
            return coins >= (int) amount;
        } catch (Exception e) {
            plugin.getLogger().warning("检查Coin余额时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从Coin系统扣款
     */
    private boolean withdrawCoin(Player player, double amount) {
        try {
            UUID uuid = player.getUniqueId();
            int currentCoins = plugin.getDatabaseManager().getPlayerCoins(uuid);
            int newCoins = currentCoins - (int) amount;

            if (newCoins < 0) {
                return false;
            }

            return plugin.getDatabaseManager().setPlayerCoins(uuid, newCoins);
        } catch (Exception e) {
            plugin.getLogger().warning("从Coin系统扣款时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取Coin余额
     */
    private double getCoinBalance(Player player) {
        try {
            return plugin.getDatabaseManager().getPlayerCoins(player.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().warning("获取Coin余额时出错: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 获取所有可用的经济系统
     */
    public List<String> getAvailableEconomies() {
        List<String> economies = new ArrayList<>();
        if (vaultEnabled) economies.add("vault");
        if (playerPointsEnabled) economies.add("playerpoints");
        economies.add("titlecoins"); // 称号币总是可用的
        economies.addAll(customEconomyHandlers.keySet());
        return economies;
    }

    /**
     * 检查经济系统是否可用
     */
    public boolean isEconomyAvailable(String economyKey) {
        if (economyKey == null) return false;

        switch (economyKey.toLowerCase()) {
            case "vault":
                return vaultEnabled;
            case "playerpoints":
                return playerPointsEnabled;
            case "titlecoins":
                return true; // 称号币总是可用
            default:
                return customEconomyHandlers.containsKey(economyKey);
        }
    }
}