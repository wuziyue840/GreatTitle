package xyz.tolite.greatTitle.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.manager.TitleCardManager;
import xyz.tolite.greatTitle.manager.TitleManager;
import xyz.tolite.greatTitle.model.Title;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

public class PlayerListener implements Listener {

    private final GreatTitle plugin;
    private final TitleManager titleManager;
    private final Random random;
    private TitleCardManager titleCardManager = null;

    public PlayerListener(GreatTitle plugin) {
        this.plugin = plugin;
        this.titleManager = plugin.getTitleManager();
        this.titleCardManager = titleCardManager;
        this.random = new Random();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        titleManager.loadPlayerData(player);

        // 只调度一次，20 ticks 后统一处理
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            /* 1. 称号核心显示与效果 */
            titleManager.updatePlayerDisplay(player);   // 你自己的显示入口
            String activeTitleId = titleManager.getActiveTitle(player);
            if (activeTitleId != null) {
                titleManager.applyActiveTitleEffects(player, activeTitleId);
            }

            /* 2. 头顶 / Tab 一致性刷新 */
            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
                plugin.getHeadDisplayManager().updatePlayerTabDisplay(player);
            }

            plugin.getLogger().info("玩家 " + player.getName() + " 登录，称号效果已应用");
        }, 20L);

        /* 3. 新玩家赠卡（同步执行，无需延迟） */
        if (!player.hasPlayedBefore()) {
            if (plugin.getConfigManager().isRandomCardEnabled() &&
                    plugin.getConfigManager().isRandomCardFirstJoin()) {
                giveRandomCard(player);
            }
            if (plugin.getConfigManager().isFixedCardEnabled() &&
                    plugin.getConfigManager().isFixedCardFirstJoin()) {
                giveFixedCard(player);
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 清理头顶显示
        if (plugin.getHeadDisplayManager() != null) {
            plugin.getHeadDisplayManager().onPlayerQuit(player);
        }

        // 清理自定义称号创建状态
        plugin.getCustomTitleManager().cancelCustomTitleCreation(player);

        titleManager.unloadPlayerData(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String inventoryTitle = event.getView().getTitle();

        // 立即取消所有相关界面的点击
        if (inventoryTitle.contains("称号仓库") || inventoryTitle.contains("称号商店") ||
                inventoryTitle.contains("称号显示设置") || inventoryTitle.contains("自定义称号") ||
                inventoryTitle.contains("自定义称号支付方式")) {
            event.setCancelled(true);
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String displayName = clickedItem.getItemMeta().getDisplayName();
        if (displayName == null) return;

        try {
            // 防止递归：界面切换时，只有界面完全关闭后再打开新界面
            if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null && player.getOpenInventory().getTopInventory().getType() == event.getView().getTopInventory().getType()) {
                // 界面未关闭，直接返回，防止递归
                return;
            }
            // 处理自定义称号支付方式界面 - 优先级最高
            if (inventoryTitle.contains("自定义称号支付方式")) {
                handlePaymentInterfaceClick(player, displayName, clickedItem);
                return;
            }

            // 处理自定义称号界面
            if (inventoryTitle.contains("自定义称号")) {
                handleCustomTitleInterfaceClick(player, displayName, clickedItem);
                return;
            }

            // 处理称号商店界面
            if (inventoryTitle.contains("称号商店")) {
                handleShopInterfaceClick(player, displayName, clickedItem, inventoryTitle);
                return;
            }

            // 处理称号仓库界面
            if (inventoryTitle.contains("称号仓库")) {
                handleStorageInterfaceClick(player, displayName, clickedItem, inventoryTitle);
                return;
            }

            // 处理称号显示设置界面
            if (inventoryTitle.contains("称号显示设置")) {
                handleSettingsInterfaceClick(player, displayName);
                return;
            }

            // 处理称号卡使用
            handleTitleCardUse(player, clickedItem);

        } catch (Exception e) {
            plugin.getLogger().warning("处理库存点击事件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理支付界面点击 - 修复无限递归问题
     */
    private void handlePaymentInterfaceClick(Player player, String displayName, ItemStack clickedItem) {
        // 返回按钮处理 - 使用唯一标识避免递归
        if ("§c返回".equals(displayName) || "§6返回商店".equals(displayName)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

            // 立即关闭当前界面
            player.closeInventory();

            // 使用最小延迟确保界面完全关闭
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isConversing()) {
                    // 直接打开商店界面，不进行额外检查
                    plugin.getTitleGUI().openTitleShop(player, 1);
                }
            }, 2L);
            return;
        }

        // 处理经济系统选择
        for (String economyKey : plugin.getMoneyConfigManager().getEnabledEconomySystems()) {
            try {
                String expectedName = plugin.getGUIConfigManager().getEconomyItemName(economyKey).replace("&", "§");
                if (displayName.equals(expectedName)) {
                    plugin.getTitleGUI().handleCustomTitleEconomySelection(player, economyKey);
                    return;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("处理经济系统选择时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 处理自定义称号界面点击
     */
    private void handleCustomTitleInterfaceClick(Player player, String displayName, ItemStack clickedItem) {
        // 返回按钮
        if (displayName.equals("§c返回商店") || displayName.equals("§c返回") ||
                (clickedItem.getType() == Material.ARROW && displayName.contains("返回"))) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isConversing()) {
                    plugin.getTitleGUI().openTitleShop(player, 1);
                }
            }, 2L);
            return;
        }

        // 处理自定义称号界面按钮
        if (displayName.equals("§6创建自定义称号")) {
            plugin.getTitleGUI().handleCustomTitleCreateClick(player);
            player.sendMessage("§a请在聊天栏输入你想要的自定义称号名称。");
        }
    }

    /**
     * 处理商店界面点击 - 修复版本（包含切换界面功能）
     */
    private void handleShopInterfaceClick(Player player, String displayName, ItemStack clickedItem, String inventoryTitle) {
        // 播放点击音效
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        // 处理商店特定按钮
        if (handleShopButtonClick(player, displayName)) {
            return;
        }

        // 处理分页按钮 - 修复分页逻辑
        if (displayName.contains("上一页") || displayName.contains("下一页")) {
            handleShopNavigationClick(player, inventoryTitle, displayName);
            return;
        }

        // 处理返回按钮
        if (displayName.equals("§6返回")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isConversing()) {
                    plugin.getTitleGUI().openTitleGUI(player, 1);
                }
            }, 1L);
            return;
        }

        // 处理退出按钮
        if (displayName.equals("§c退出")) {
            player.closeInventory();
            return;
        }

        // 处理自定义称号按钮
        if (displayName.equals("§6自定义称号")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isConversing()) {
                    plugin.getTitleGUI().openCustomTitleEconomySelection(player);
                }
            }, 1L);
            return;
        }

        // 处理切换界面按钮
        if (displayName.contains("前往称号仓库") || displayName.contains("显示设置")) {
            handleNavigationClick(player, inventoryTitle, displayName);
            return;
        }

        // 处理称号点击
        handleShopTitleClick(player, clickedItem, inventoryTitle);
    }

    /**
     * 处理商店导航点击 - 修复版本
     */
    private void handleShopNavigationClick(Player player, String inventoryTitle, String buttonName) {
        // 提取当前页码 - 修复解析逻辑
        int currentPage = extractPageFromTitle(inventoryTitle);

        int newPage;

        if (buttonName.contains("上一页")) {
            newPage = Math.max(1, currentPage - 1);
        } else if (buttonName.contains("下一页")) {
            newPage = currentPage + 1;
        } else {
            newPage = currentPage;
        }

        // 重新打开商店界面
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isConversing()) {
                plugin.getTitleGUI().openTitleShop(player, newPage);
            }
        }, 1L);
    }

    /**
     * 处理仓库界面点击 - 修复版本（包含切换界面功能）
     */
    private void handleStorageInterfaceClick(Player player, String displayName, ItemStack clickedItem, String inventoryTitle) {
        // 播放点击音效
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        // 处理分页按钮
        if (displayName.contains("上一页") || displayName.contains("下一页")) {
            handleStorageNavigationClick(player, inventoryTitle, displayName);
            return;
        }

        // 处理返回按钮
        if (displayName.equals("§c返回")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isConversing()) {
                    plugin.getTitleGUI().openTitleGUI(player, 1);
                }
            }, 1L);
            return;
        }

        // 处理切换界面按钮
        if (displayName.contains("前往称号商店") || displayName.contains("显示设置")) {
            handleNavigationClick(player, inventoryTitle, displayName);
            return;
        }

        // 处理称号点击
        handleStorageTitleClick(player, clickedItem, inventoryTitle);
    }

    /**
     * 处理仓库导航点击 - 修复版本
     */
    private void handleStorageNavigationClick(Player player, String inventoryTitle, String buttonName) {
        // 提取当前页码 - 修复解析逻辑
        int currentPage = extractPageFromTitle(inventoryTitle);

        int newPage;

        if (buttonName.contains("上一页")) {
            newPage = Math.max(1, currentPage - 1);
        } else if (buttonName.contains("下一页")) {
            newPage = currentPage + 1;
        } else {
            newPage = currentPage;
        }

        // 重新打开仓库界面
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isConversing()) {
                plugin.getTitleGUI().openTitleGUI(player, newPage);
            }
        }, 1L);
    }

    /**
     * 处理商店称号点击 - 购买称号
     */
    private void handleShopTitleClick(Player player, ItemStack clickedItem, String inventoryTitle) {
        String displayName = clickedItem.getItemMeta().getDisplayName();

        // 在配置中查找对应的称号
        for (Title title : plugin.getTitleManager().getAllTitles()) {
            if (title.getDisplayName().equals(displayName)) {
                // 在商店中购买称号
                if (plugin.getEconomyManager().purchaseTitle(player, title)) {
                    player.closeInventory();
                    player.sendMessage("§a成功购买称号: " + title.getDisplayName());

                    // 更新头顶显示（如果立即使用）
                    if (plugin.getHeadDisplayManager() != null) {
                        plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
                    }

                    // 重新打开商店（保持当前页面）
                    int currentPage = extractPageFromTitle(inventoryTitle);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline() && !player.isConversing()) {
                            plugin.getTitleGUI().openTitleShop(player, currentPage);
                        }
                    }, 5L);
                } else {
                    player.sendMessage("§c购买失败! 请检查你的余额或条件。");
                }
                break;
            }
        }
    }

    /**
     * 处理仓库称号点击 - 使用/取消使用称号
     */
    private void handleStorageTitleClick(Player player, ItemStack clickedItem, String inventoryTitle) {
        String displayName = clickedItem.getItemMeta().getDisplayName();

        // 在配置中查找对应的称号
        for (Title title : plugin.getTitleManager().getAllTitles()) {
            if (title.getDisplayName().equals(displayName)) {
                // 在仓库中使用/取消使用称号
                String activeTitle = plugin.getTitleManager().getActiveTitle(player);
                if (title.getId().equals(activeTitle)) {
                    // 取消使用
                    plugin.getTitleManager().setActiveTitle(player, null);
                    player.sendMessage("§a已取消使用称号: " + title.getDisplayName());
                } else {
                    // 使用称号
                    if (plugin.getTitleManager().setActiveTitle(player, title.getId())) {
                        player.sendMessage("§a已设置称号: " + title.getDisplayName());
                    } else {
                        player.sendMessage("§c设置称号失败! 请检查称号是否过期。");
                    }
                }

                // 更新头顶显示
                if (plugin.getHeadDisplayManager() != null) {
                    plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
                }

                // 重新打开仓库界面
                int currentPage = extractPageFromTitle(inventoryTitle);
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && !player.isConversing()) {
                        plugin.getTitleGUI().openTitleGUI(player, currentPage);
                    }
                }, 1L);
                break;
            }
        }
    }

    /**
     * 从标题解析页码 - 辅助方法
     */
    private int extractPageFromTitle(String inventoryTitle) {
        try {
            if (inventoryTitle.contains("第") && inventoryTitle.contains("页")) {
                String[] parts = inventoryTitle.split("第");
                if (parts.length >= 2) {
                    String pageStr = parts[1].replace("页", "").split(" ")[0].trim();
                    return Integer.parseInt(pageStr);
                }
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("解析库存标题页码失败: " + inventoryTitle);
        }
        return 1; // 默认返回第一页
    }

    /**
     * 处理设置界面点击
     */
    private void handleSettingsInterfaceClick(Player player, String displayName) {
        handleSettingsClick(player, displayName);
    }

    /**
     * 处理设置界面点击 - 修复版本
     */
    private void handleSettingsClick(Player player, String displayName) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        // 处理设置界面点击 - 修复版本
        switch (displayName) {
            case "§6头顶显示":
                boolean headDisplay = !plugin.getTitleManager().isHeadDisplayEnabled(player);
                plugin.getTitleManager().setHeadDisplay(player, headDisplay);
                if (plugin.getHeadDisplayManager() != null) {
                    plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
                }
                player.sendMessage("§a已" + (headDisplay ? "开启" : "关闭") + "头顶称号显示");
                plugin.getTitleGUI().openSettingsGUI(player);
                break;
            case "§6Tab列表显示":
                boolean tabDisplay = !plugin.getTitleManager().isTabListDisplayEnabled(player);
                plugin.getTitleManager().setTabListDisplay(player, tabDisplay);
                if (plugin.getHeadDisplayManager() != null) {
                    plugin.getHeadDisplayManager().updatePlayerTabDisplay(player);
                }
                player.sendMessage("§a已" + (tabDisplay ? "开启" : "关闭") + "Tab列表称号显示");
                plugin.getTitleGUI().openSettingsGUI(player);
                break;
            case "§6聊天显示":
                boolean chatDisplay = !plugin.getTitleManager().isChatDisplayEnabled(player);
                plugin.getTitleManager().setChatDisplay(player, chatDisplay);
                player.sendMessage("§a已" + (chatDisplay ? "开启" : "关闭") + "聊天称号显示");
                plugin.getTitleGUI().openSettingsGUI(player);
                break;
            case "§6粒子效果显示":
                boolean particleDisplay = !plugin.getTitleManager().isParticleDisplayEnabled(player);
                plugin.getTitleManager().setParticleDisplay(player, particleDisplay);
                player.sendMessage("§a已" + (particleDisplay ? "开启" : "关闭") + "称号粒子效果显示");
                plugin.getTitleGUI().openSettingsGUI(player);
                break;
            case "§c返回":
                plugin.getTitleGUI().openTitleGUI(player, 1);
                break;
            default:
                player.sendMessage("§c无效按钮！");
                break;
        }
    }

    /**
     * 处理称号卡使用
     */
    private void handleTitleCardUse(Player player, ItemStack clickedItem) {
        ItemMeta meta = clickedItem.getItemMeta();
        String itemName = meta.getDisplayName();

        // 随机称号卡
        if (itemName.equals(plugin.getConfigManager().getRandomCardName())) {
            handleRandomCardUse(player, clickedItem);
        }
        // 固定称号卡
        else if (itemName.equals(plugin.getConfigManager().getFixedCardName())) {
            handleFixedCardUse(player, clickedItem);
        }
        // 自定义称号卡
        else if (itemName.contains("称号卡")) {
            handleCustomCardUse(player, clickedItem);
        }
    }

    // 修复聊天输入处理问题
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // 首先检查玩家是否在创建自定义称号
        if (plugin.getCustomTitleManager().isCreatingCustomTitle(player)) {
            event.setCancelled(true);
            String titleName = event.getMessage().trim();

            // 只允许一次输入，输入后立即移除创建状态
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getCustomTitleManager().handleCustomTitleInput(player, titleName);
                plugin.getCustomTitleManager().cancelCustomTitleCreation(player);
            });
            return;
        }

        // 然后检查聊天显示设置
        if (plugin.getTitleManager().isChatDisplayEnabled(player) && plugin.getNameConfig().isChatDisplayEnabled()) {
            String activeTitleId = plugin.getTitleManager().getActiveTitle(player);
            if (activeTitleId != null) {
                Title title = plugin.getTitleManager().getTitle(activeTitleId);
                if (title != null) {
                    String formattedTitle = title.getFormattedContent();
                    formattedTitle = formattedTitle.replace("<", "").replace(">", "").trim();
                    String playerName = player.getName();
                    String newFormat = formattedTitle + " <" + playerName + "> %2$s";
                    event.setFormat(newFormat);
                }
            }
        }
    }

    private boolean handleShopButtonClick(Player player, String displayName) {
        switch (displayName) {
            case "§6自定义称号":
                // 直接打开支付方式选择界面，跳过中间界面
                plugin.getTitleGUI().openCustomTitleEconomySelection(player);
                return true;
            case "§c退出":
                player.closeInventory();
                return true;
            case "§6返回":
                plugin.getTitleGUI().openTitleGUI(player, 1);
                return true;
        }
        return false;
    }

    private void handleCustomTitleClick(Player player, String displayName) {
        switch (displayName) {
            case "§6创建自定义称号":
                // 处理自定义称号创建
                plugin.getTitleGUI().openCustomTitleEconomySelection(player);
                break;
            case "§c返回商店":
            case "§c返回": // 添加对返回按钮的支持
                plugin.getTitleGUI().openTitleShop(player, 1);
                break;
            default:
                // 检查是否是箭头物品的返回按钮
                if (displayName.contains("返回") || displayName.contains("Back")) {
                    plugin.getTitleGUI().openTitleShop(player, 1);
                }
                break;
        }
    }

    /**
     * 处理导航点击 - 切换界面功能
     */
    private void handleNavigationClick(Player player, String inventoryName, String buttonName) {
        // 播放点击音效
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        // 提取当前页码
        int currentPage = extractPageFromTitle(inventoryName);

        if (buttonName.contains("前往称号商店")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isConversing()) {
                    plugin.getTitleGUI().openTitleShop(player, 1);
                }
            }, 1L);
            return;
        } else if (buttonName.contains("前往称号仓库")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isConversing()) {
                    plugin.getTitleGUI().openTitleGUI(player, currentPage);
                }
            }, 1L);
            return;
        } else if (buttonName.contains("显示设置")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isConversing()) {
                    plugin.getTitleGUI().openSettingsGUI(player);
                }
            }, 1L);
            return;
        }

        // 处理分页导航
        int newPage;

        if (buttonName.contains("上一页")) {
            newPage = Math.max(1, currentPage - 1);
        } else if (buttonName.contains("下一页")) {
            newPage = currentPage + 1;
        } else {
            newPage = currentPage;
        }

        if (inventoryName.contains("仓库")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isConversing()) {
                    plugin.getTitleGUI().openTitleGUI(player, newPage);
                }
            }, 1L);
        } else if (inventoryName.contains("商店")) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isConversing()) {
                    plugin.getTitleGUI().openTitleShop(player, newPage);
                }
            }, 1L);
        }
    }

    /**
     * 处理称号点击 - 兼容旧版本
     */
    private void handleTitleClick(Player player, String inventoryName, ItemStack clickedItem) {
        String displayName = clickedItem.getItemMeta().getDisplayName().replace("§", "");

        // 在配置中查找对应的称号
        for (Title title : titleManager.getAllTitles()) {
            if (title.getDisplayName().replace("§", "").equals(displayName)) {
                if (inventoryName.contains("仓库")) {
                    // 在仓库中使用/取消使用称号
                    String activeTitle = titleManager.getActiveTitle(player);
                    if (title.getId().equals(activeTitle)) {
                        // 取消使用
                        titleManager.setActiveTitle(player, null);
                        player.sendMessage("§a已取消使用称号: " + title.getDisplayName());
                    } else {
                        // 使用称号
                        if (titleManager.setActiveTitle(player, title.getId())) {
                            player.sendMessage("§a已设置称号: " + title.getDisplayName());
                        } else {
                            player.sendMessage("§c设置称号失败! 请检查称号是否过期。");
                        }
                    }

                    // 更新头顶显示
                    if (plugin.getHeadDisplayManager() != null) {
                        plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
                    }

                    plugin.getTitleGUI().openTitleGUI(player, 1);
                } else if (inventoryName.contains("商店")) {
                    // 在商店中购买称号
                    if (plugin.getEconomyManager().purchaseTitle(player, title)) {
                        player.closeInventory();
                        player.sendMessage("§a成功购买称号: " + title.getDisplayName());

                        // 更新头顶显示（如果立即使用）
                        if (plugin.getHeadDisplayManager() != null) {
                            plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
                        }
                    } else {
                        player.sendMessage("§c购买失败! 请检查你的余额或条件。");
                    }
                }
                break;
            }
        }
    }

    private void giveRandomCard(Player player) {
        ItemStack card = createRandomCard();
        giveItemToPlayer(player, card, "随机称号卡");
    }

    private void giveFixedCard(Player player) {
        ItemStack card = createFixedCard();
        giveItemToPlayer(player, card, "固定称号卡");
    }

    private ItemStack createRandomCard() {
        String itemType = plugin.getConfigManager().getRandomCardItem();
        Material material = Material.getMaterial(itemType);
        if (material == null) {
            material = Material.PAPER;
        }

        ItemStack card = new ItemStack(material);
        ItemMeta meta = card.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().getRandomCardName());

            List<String> lore = plugin.getConfigManager().getRandomCardLore();
            if (lore.isEmpty()) {
                lore.add("§7右键点击获得随机称号");
                lore.add("§e类型: 随机");
            }
            meta.setLore(lore);

            card.setItemMeta(meta);
        }

        return card;
    }

    private ItemStack createFixedCard() {
        String itemType = plugin.getConfigManager().getFixedCardItem();
        Material material = Material.getMaterial(itemType);
        if (material == null) {
            material = Material.PAPER;
        }

        ItemStack card = new ItemStack(material);
        ItemMeta meta = card.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().getFixedCardName());

            List<String> lore = plugin.getConfigManager().getFixedCardLore();
            if (lore.isEmpty()) {
                String fixedTitle = plugin.getConfigManager().getFixedCardTitle();
                Title title = titleManager.getTitle(fixedTitle);
                String titleName = title != null ? title.getDisplayName() : fixedTitle;
                lore.add("§7右键点击获得称号: " + titleName);
                lore.add("§e类型: 固定");
            }
            meta.setLore(lore);

            card.setItemMeta(meta);
        }

        return card;
    }

    private void giveItemToPlayer(Player player, ItemStack item, String itemName) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
            player.sendMessage("§a你获得了一张" + itemName + "! 右键点击使用。");
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage("§a你获得了一张" + itemName + "! 已掉落在地面上。");
        }
    }

    /**
     * 处理随机称号卡使用
     */
    private void handleRandomCardUse(Player player, ItemStack item) {
        // 移除物品
        item.setAmount(item.getAmount() - 1);

        // 从配置的称号池中随机选择
        List<String> titlePool = plugin.getConfigManager().getRandomCardTitlePool();
        if (titlePool.isEmpty()) {
            player.sendMessage("§c随机称号池为空! 请联系管理员。");
            return;
        }

        String titleId = titlePool.get(random.nextInt(titlePool.size()));

        // 检查玩家是否已经拥有这个称号
        if (titleManager.getPlayerTitle(player, titleId) != null) {
            player.sendMessage("§c你已经拥有这个称号了! 请尝试其他称号卡。");
            return;
        }

        if (titleManager.giveTitle(player, titleId, 0)) {
            Title title = titleManager.getTitle(titleId);
            player.sendMessage("§a恭喜! 你获得了随机称号: " + title.getDisplayName());

            // 更新头顶显示
            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
            }
        } else {
            player.sendMessage("§c使用随机称号卡失败! 请联系管理员。");
        }
    }

    /**
     * 处理固定称号卡使用 - 修复重复获得称号问题
     */
    private void handleFixedCardUse(Player player, ItemStack item) {
        // 移除物品
        item.setAmount(item.getAmount() - 1);

        String titleId = plugin.getConfigManager().getFixedCardTitle();

        // 检查玩家是否已经拥有这个称号
        if (titleManager.getPlayerTitle(player, titleId) != null) {
            player.sendMessage("§c你已经拥有这个称号了! 无法重复获得。");
            return;
        }

        if (titleId != null && titleManager.giveTitle(player, titleId, 0)) {
            Title title = titleManager.getTitle(titleId);
            player.sendMessage("§a恭喜! 你获得了固定称号: " + title.getDisplayName());

            // 更新头顶显示
            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
            }
        } else {
            player.sendMessage("§c使用固定称号卡失败! 请联系管理员。");
        }
    }

    /**
     * 处理自定义称号卡使用 - 修复重复获得称号问题
     */
    private void handleCustomCardUse(Player player, ItemStack item) {
        // 从NBT读取称号ID列表
        List<String> titleIds = getTitleIdsFromNBT(item);
        if (titleIds.isEmpty()) {
            player.sendMessage("§c称号卡数据错误! 请联系管理员。");
            return;
        }

        // 移除物品
        item.setAmount(item.getAmount() - 1);

        // 给予所有称号，但跳过玩家已经拥有的称号
        int successCount = 0;
        List<String> newTitles = new ArrayList<>();

        for (String titleId : titleIds) {
            // 检查玩家是否已经拥有这个称号
            if (titleManager.getPlayerTitle(player, titleId) != null) {
                continue;
            }

            if (titleManager.giveTitle(player, titleId, 0)) {
                successCount++;
                Title title = titleManager.getTitle(titleId);
                if (title != null) {
                    newTitles.add(title.getDisplayName());
                }
            }
        }

        if (successCount > 0) {
            if (successCount == 1) {
                player.sendMessage("§a恭喜! 你获得了称号: " + newTitles.get(0));
            } else {
                player.sendMessage("§a恭喜! 你获得了 " + successCount + " 个新称号!");
                // 显示前3个获得的称号名称
                for (int i = 0; i < Math.min(newTitles.size(), 3); i++) {
                    player.sendMessage("§7- " + newTitles.get(i));
                }
                if (newTitles.size() > 3) {
                    player.sendMessage("§7... 还有 " + (newTitles.size() - 3) + " 个称号");
                }
            }

            // 更新头顶显示
            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
            }
        } else {
            player.sendMessage("§c使用称号卡失败! 你已经拥有这些称号了。");
        }
    }

    /**
     * 从NBT读取称号ID列表
     */
    private List<String> getTitleIdsFromNBT(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return new ArrayList<>();

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "custom_title_ids");

        String titleIdsString = data.get(key, PersistentDataType.STRING);
        if (titleIdsString == null) return new ArrayList<>();

        return Arrays.asList(titleIdsString.split(","));
    }
}