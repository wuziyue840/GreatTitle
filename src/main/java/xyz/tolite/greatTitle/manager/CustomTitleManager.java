package xyz.tolite.greatTitle.manager;

import org.bukkit.entity.Player;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.model.TitleType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomTitleManager {

    private final GreatTitle plugin;
    private final Map<UUID, String> creatingPlayers; // 玩家UUID -> 经济系统key

    public CustomTitleManager(GreatTitle plugin) {
        this.plugin = plugin;
        this.creatingPlayers = new HashMap<>();
    }

    /**
     * 开始自定义称号创建流程
     */
    public void startCustomTitleCreation(Player player, String economyKey) {
        creatingPlayers.put(player.getUniqueId(), economyKey);

        player.closeInventory();
        player.sendMessage("§6=== 自定义称号创建 ===");

        double cost = plugin.getMoneyConfigManager().getEconomyCustomPrice(economyKey);
        String economyName = plugin.getMoneyConfigManager().getEconomyDisplayName(economyKey);
        int minLength = plugin.getMoneyConfigManager().getCustomTitleMinLength();
        int maxLength = plugin.getMoneyConfigManager().getCustomTitleMaxLength();

        player.sendMessage("§7价格: §e" + cost + " " + economyName);
        player.sendMessage("§7长度限制: §e" + minLength + "-" + maxLength + "字符");
        player.sendMessage("§7支持颜色代码: §a&a绿色 §c&c红色 §e&e黄色 §6&6金色 §b&b蓝色");
        player.sendMessage("§7支持格式: §l&l粗体 §o&o斜体 §n&n下划线 §m&m删除线");
        player.sendMessage("");
        player.sendMessage("§6请输入你想要的自定义称号名称:");
        player.sendMessage("§c输入 'cancel' 取消创建");
    }

    /**
     * 处理玩家输入的自定义称号名称
     */
    public void handleCustomTitleInput(Player player, String titleName) {
        UUID playerId = player.getUniqueId();

        if (!creatingPlayers.containsKey(playerId)) {
            player.sendMessage("§c你不在创建自定义称号的状态中!");
            return;
        }

        String economyKey = creatingPlayers.get(playerId);

        // 检查是否取消
        if (titleName.equalsIgnoreCase("cancel")) {
            creatingPlayers.remove(playerId);
            player.sendMessage("§c已取消自定义称号创建");
            return;
        }

        // 验证称号名称
        if (!validateCustomTitle(player, titleName, economyKey)) {
            return;
        }

        // 创建自定义称号
        createCustomTitle(player, titleName, economyKey);
        creatingPlayers.remove(playerId);
    }

    /**
     * 验证自定义称号
     */
    private boolean validateCustomTitle(Player player, String titleName, String economyKey) {
        MoneyConfigManager moneyConfig = plugin.getMoneyConfigManager();

        // 检查长度
        int length = titleName.replace("&", "").length();
        int minLength = moneyConfig.getCustomTitleMinLength();
        int maxLength = moneyConfig.getCustomTitleMaxLength();

        if (length < minLength || length > maxLength) {
            player.sendMessage("§c称号长度必须在 " + minLength + "-" + maxLength + " 字符之间!");
            return false;
        }

        // 检查禁止词语
        if (moneyConfig.isWordBanned(titleName)) {
            player.sendMessage("§c称号包含禁止的词语!");
            return false;
        }

        // 检查余额 - 修复：避免递归调用
        if (!plugin.getEconomyManager().canPurchaseCustomTitle(player, economyKey)) {
            double cost = moneyConfig.getEconomyCustomPrice(economyKey);
            String economyName = moneyConfig.getEconomyDisplayName(economyKey);
            player.sendMessage("§c余额不足! 需要 " + cost + " " + economyName);
            return false;
        }

        return true;
    }

    /**
     * 创建自定义称号
     *
     * @return
     */
    public boolean createCustomTitle(Player player, String titleName, String economyKey) {
        try {
            // 先扣款
            boolean paymentSuccess = plugin.getEconomyManager().purchaseCustomTitle(player, economyKey, titleName);
            if (!paymentSuccess) {
                player.sendMessage("§c支付失败! 请检查余额后重试");
                return paymentSuccess;
            }

            String titleId = "custom_" + player.getUniqueId().toString().substring(0, 8) + "_" + System.currentTimeMillis();
            String displayName = titleName.replace("&", "§");
            String content = displayName;

            // 创建称号配置
            Map<String, Object> titleConfig = new HashMap<>();
            titleConfig.put("display-name", displayName);
            titleConfig.put("content", content);
            titleConfig.put("type", TitleType.NOT.toString());
            titleConfig.put("show-in-shop", false);
            titleConfig.put("rgb", false);
            titleConfig.put("custom", true);
            titleConfig.put("owner", player.getUniqueId().toString());

            // 保存到配置文件
            plugin.getConfigManager().getTitlesConfig().set(titleId, titleConfig);
            plugin.getConfigManager().saveTitlesConfig();

            // 重新加载称号
            plugin.getTitleManager().loadTitles();

            // 给予玩家称号
            if (plugin.getTitleManager().giveTitle(player, titleId, 0)) {
                player.sendMessage("§a成功创建自定义称号: " + displayName);

                // 自动使用新创建的称号
                if (plugin.getTitleManager().setActiveTitle(player, titleId)) {
                    player.sendMessage("§a已自动使用新创建的称号");

                    // 更新显示
                    if (plugin.getHeadDisplayManager() != null) {
                        plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
                    }
                }
            } else {
                player.sendMessage("§c创建称号成功，但给予称号失败，请联系管理员!");
            }

        } catch (Exception e) {
            player.sendMessage("§c创建自定义称号时出现错误，请联系管理员!");
            plugin.getLogger().severe("创建自定义称号失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 检查玩家是否在创建自定义称号
     */
    public boolean isCreatingCustomTitle(Player player) {
        return creatingPlayers.containsKey(player.getUniqueId());
    }

    /**
     * 取消玩家的自定义称号创建
     */
    public void cancelCustomTitleCreation(Player player) {
        creatingPlayers.remove(player.getUniqueId());
    }

    /**
     * 获取所有正在创建自定义称号的玩家
     */
    public Map<UUID, String> getCreatingPlayers() {
        return new HashMap<>(creatingPlayers);
    }
}