package xyz.tolite.greatTitle.gui;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.manager.EconomyManager;
import xyz.tolite.greatTitle.manager.TitleCardManager;
import xyz.tolite.greatTitle.manager.TitleManager;
import xyz.tolite.greatTitle.model.*;
import com.cryptomorin.xseries.XMaterial;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TitleGUI {

    private final GreatTitle plugin;
    private final TitleManager titleManager;
    private TitleCardManager titleCardManager = null;

    public TitleGUI(GreatTitle plugin) {
        this.plugin = plugin;
        this.titleManager = plugin.getTitleManager();
        this.titleCardManager = titleCardManager;
    }

    // 修改创建自定义称号卡的方法
    private ItemStack createCustomCard(List<String> titleIds) {
        return titleCardManager.createCustomCard(titleIds);
    }

    // 材质兼容性方法
    private Material getGrayPaneMaterial() {
        try {
            return Material.valueOf("GRAY_STAINED_GLASS_PANE");
        } catch (IllegalArgumentException e) {
            return Material.valueOf("STAINED_GLASS_PANE");
        }
    }

    private ItemStack createGrayPane() {
        Material material = getGrayPaneMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }

        if (material == Material.valueOf("STAINED_GLASS_PANE")) {
            try {
                Class<?> itemMetaClass = meta.getClass();
                Method setColorMethod = itemMetaClass.getMethod("setColor", Color.class);
                setColorMethod.invoke(meta, Color.fromRGB(0x9D9D97));
            } catch (Exception ex) {
                // 忽略颜色设置错误
            }
        }

        return item;
    }

    public void openTitleGUI(Player player, int page) {
        int guiSize = plugin.getConfigManager().getGUISize();
        Inventory gui = Bukkit.createInventory(player, guiSize, "称号仓库 - 第" + page + "页");

        Map<String, PlayerTitle> playerTitles = titleManager.getPlayerTitles(player);
        List<String> titleIds = new ArrayList<>(playerTitles.keySet());
        List<Title> availableTitles = new ArrayList<>();

        // 添加灰色玻璃板边框 - 使用兼容性方法
        addGrayGlassPaneBorder(gui);

        // 获取玩家已拥有的称号
        for (String titleId : titleIds) {
            Title title = titleManager.getTitle(titleId);
            if (title != null) {
                availableTitles.add(title);
            }
        }

        // 计算分页 - 修复：正确计算可用槽位
        int itemsPerPage = 28; // 4行 * 7列 = 28个槽位
        int totalPages = (int) Math.ceil((double) availableTitles.size() / itemsPerPage);

        if (page > totalPages) {
            page = totalPages;
        }
        if (page < 1) {
            page = 1;
        }

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, availableTitles.size());

        // 添加称号物品 - 修复：正确计算位置
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Title title = availableTitles.get(i);
            ItemStack titleItem = createTitleItem(title, player);

            // 计算正确的槽位 - 从第10格开始（第二行第一格）
            int slot = calculateTitleSlot(slotIndex);
            gui.setItem(slot, titleItem);
            slotIndex++;
        }

        // 添加导航按钮
        addNavigationButtons(gui, page, totalPages, "仓库");

        player.openInventory(gui);
    }

    // 修复：正确计算称号槽位
    private int calculateTitleSlot(int index) {
        int row = index / 7; // 每行7个物品
        int col = index % 7;

        // 起始槽位是10（第二行第一格）
        // 每行有9个槽位，所以行偏移是 row * 9
        // 列偏移是 col + 1（因为第一列是边框）
        return 10 + row * 9 + col;
    }

    public void openTitleShop(Player player, int page) {
        int guiSize = plugin.getConfigManager().getGUISize();
        Inventory gui = Bukkit.createInventory(player, guiSize, "称号商店 - 第" + page + "页");

        // 添加灰色玻璃板边框 - 使用兼容性方法
        addGrayGlassPaneBorder(gui);

        List<Title> allTitles = new ArrayList<>(titleManager.getAllTitles());
        Map<String, PlayerTitle> playerTitles = titleManager.getPlayerTitles(player);

        // 过滤掉玩家已经拥有的永久称号
        allTitles.removeIf(title -> {
            PlayerTitle playerTitle = playerTitles.get(title.getId());
            return playerTitle != null && playerTitle.isPermanent();
        });

        int itemsPerPage = guiSize - 18; // 考虑到边框和按钮
        int totalPages = (int) Math.ceil((double) allTitles.size() / itemsPerPage);

        if (page > totalPages) {
            page = totalPages;
        }
        if (page < 1) {
            page = 1;
        }

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allTitles.size());

        // 添加可购买的称号物品 (从第10格开始，避开边框)
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            Title title = allTitles.get(i);
            ItemStack titleItem = createShopTitleItem(title, player);

            // 计算位置，确保在玻璃板内
            gui.setItem(slot, titleItem);
            slot++;

            // 每行7个物品，然后换行
            if ((slot - 10) % 7 == 0) {
                slot += 2; // 跳到下一行，跳过边框
            }
        }

        // 添加导航按钮
        addShopNavigationButtons(gui, page, totalPages);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String inventoryTitle = event.getView().getTitle();

        // 只处理称号商店相关的界面
        if (inventoryTitle.contains("称号商店")) {
            event.setCancelled(true); // 取消所有点击

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            String displayName = clickedItem.getItemMeta().getDisplayName();
            if (displayName == null) return;

            try {
                // 播放点击音效
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

                // 处理返回按钮
                if (displayName.equals("§c返回主菜单")) {
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getTitleGUI().openTitleGUI(player, 1);
                        }
                    }, 1L);
                    return;
                }

                // 处理上一页按钮
                if (displayName.equals("§6← 上一页")) {
                    int currentPage = extractPageFromTitle(inventoryTitle);
                    if (currentPage > 1) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                openTitleShop(player, currentPage - 1);
                            }
                        }, 1L);
                    }
                    return;
                }

                // 处理下一页按钮
                if (displayName.equals("§6下一页 →")) {
                    int currentPage = extractPageFromTitle(inventoryTitle);
                    int totalPages = getTotalShopPages();
                    if (currentPage < totalPages) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                openTitleShop(player, currentPage + 1);
                            }
                        }, 1L);
                    }
                    return;
                }

                // 处理自定义称号按钮
                if (displayName.equals("§6✎ 创建自定义称号")) {
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getTitleGUI().openCustomTitleEconomySelection(player);
                        }
                    }, 1L);
                    return;
                }

                // 处理称号点击（购买）
                handleShopTitleClick(player, clickedItem, inventoryTitle);

            } catch (Exception e) {
                plugin.getLogger().warning("处理商店点击事件时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 从库存标题中提取页码 - 关键修复方法
     */
    private int extractPageFromTitle(String inventoryTitle) {
        try {
            if (inventoryTitle.contains("第") && inventoryTitle.contains("页")) {
                // 处理格式: "称号商店 - 第1页"
                String[] parts = inventoryTitle.split("第");
                if (parts.length >= 2) {
                    String pageStr = parts[1].replace("页", "").split(" ")[0].trim();
                    return Integer.parseInt(pageStr);
                }
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("解析库存标题页码失败: " + inventoryTitle + " - " + e.getMessage());
        }
        return 1; // 默认返回第一页
    }

    /**
     * 获取商店总页数
     */
    private int getTotalShopPages() {
        try {
            List<Title> allTitles = new ArrayList<>(plugin.getTitleManager().getAllTitles());
            Map<String, PlayerTitle> playerTitles = plugin.getTitleManager().getPlayerTitles(null);

            // 过滤掉玩家已经拥有的永久称号
            allTitles.removeIf(title -> {
                PlayerTitle playerTitle = playerTitles.get(title.getId());
                return playerTitle != null && playerTitle.isPermanent();
            });

            int titlesPerPage = 21; // 3行 * 7列
            return Math.max(1, (int) Math.ceil((double) allTitles.size() / titlesPerPage));
        } catch (Exception e) {
            plugin.getLogger().warning("计算总页数时出错: " + e.getMessage());
            return 1;
        }
    }

    /**
     * 处理商店称号点击事件 - 购买称号
     */
    private void handleShopTitleClick(Player player, ItemStack clickedItem, String inventoryTitle) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String displayName = clickedItem.getItemMeta().getDisplayName();
        if (displayName == null) return;

        try {
            // 在配置中查找对应的称号
            for (Title title : plugin.getTitleManager().getAllTitles()) {
                if (title.getDisplayName().equals(displayName)) {
                    // 检查玩家是否已经拥有这个称号
                    if (plugin.getTitleManager().hasTitle(player, title.getId())) {
                        player.sendMessage("§c你已经拥有这个称号了!");
                        return;
                    }

                    // 尝试购买称号
                    if (plugin.getEconomyManager().purchaseTitle(player, title)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        player.closeInventory();
                        player.sendMessage("§a成功购买称号: " + title.getDisplayName());

                        // 更新头顶显示
                        if (plugin.getHeadDisplayManager() != null) {
                            plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
                        }

                        // 重新打开商店界面（同页面）
                        int currentPage = extractPageFromTitle(inventoryTitle);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                openTitleShop(player, currentPage);
                            }
                        }, 5L);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        player.sendMessage("§c购买失败! 请检查你的余额或条件。");
                    }
                    return;
                }
            }

            // 如果没有找到对应的称号
            player.sendMessage("§c未找到对应的称号信息!");

        } catch (Exception e) {
            plugin.getLogger().warning("处理商店称号点击时出错: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§c处理购买时出现错误，请联系管理员!");
        }
    }

    /**
     * 创建称号商店库存 - 完整修复版本
     */
    private Inventory createTitleShopInventory(Player player, int page) {
        int size = plugin.getConfigManager().getGUISize();
        Inventory inventory = Bukkit.createInventory(null, size, "称号商店 - 第" + page + "页");

        // 添加背景玻璃板 - 使用XSeries确保兼容性
        ItemStack background = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
        if (background == null) {
            background = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
        }
        ItemMeta backgroundMeta = background.getItemMeta();
        if (backgroundMeta != null) {
            backgroundMeta.setDisplayName(" ");
            background.setItemMeta(backgroundMeta);
        }

        // 填充背景（只填充空槽位）
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, background);
            }
        }

        // 添加返回按钮
        ItemStack backButton = XMaterial.ARROW.parseItem();
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c返回主菜单");
            List<String> backLore = new ArrayList<>();
            backLore.add("§7点击返回称号主界面");
            backMeta.setLore(backLore);
            backButton.setItemMeta(backMeta);
        }
        inventory.setItem(49, backButton);

        // 计算总页数
        int totalPages = getTotalShopPages();

        // 添加上一页按钮
        if (page > 1) {
            ItemStack prevButton = XMaterial.ARROW.parseItem();
            ItemMeta prevMeta = prevButton.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§6← 上一页");
                List<String> prevLore = new ArrayList<>();
                prevLore.add("§7点击查看第 " + (page - 1) + " 页");
                prevMeta.setLore(prevLore);
                prevButton.setItemMeta(prevMeta);
            }
            inventory.setItem(45, prevButton);
        }

        // 添加下一页按钮
        if (page < totalPages) {
            ItemStack nextButton = XMaterial.ARROW.parseItem();
            ItemMeta nextMeta = nextButton.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§6下一页 →");
                List<String> nextLore = new ArrayList<>();
                nextLore.add("§7点击查看第 " + (page + 1) + " 页");
                nextMeta.setLore(nextLore);
                nextButton.setItemMeta(nextMeta);
            }
            inventory.setItem(53, nextButton);
        }

        // 添加页面信息显示
        ItemStack pageInfo = XMaterial.BOOK.parseItem();
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§e页面信息");
            List<String> pageLore = new ArrayList<>();
            pageLore.add("§f当前: 第 " + page + " / " + totalPages + " 页");
            pageLore.add("§7总称号: " + getTotalShopTitles() + " 个");
            pageMeta.setLore(pageLore);
            pageInfo.setItemMeta(pageMeta);
        }
        inventory.setItem(48, pageInfo);

        // 添加自定义称号按钮
        ItemStack customTitleButton = XMaterial.NAME_TAG.parseItem();
        ItemMeta customMeta = customTitleButton.getItemMeta();
        if (customMeta != null) {
            customMeta.setDisplayName("§6✎ 创建自定义称号");
            List<String> customLore = new ArrayList<>();
            customLore.add("§7点击创建个性化称号");
            customLore.add("§7支持颜色代码和特殊格式");
            customLore.add("");
            customLore.add("§a点击开始创建");
            customMeta.setLore(customLore);
            customTitleButton.setItemMeta(customMeta);
        }
        inventory.setItem(50, customTitleButton);

        // 添加可购买的称号
        List<Title> availableTitles = plugin.getTitleManager().getTitlesForShop(page);
        int slot = 10; // 起始槽位
        int count = 0;

        for (Title title : availableTitles) {
            if (slot >= 44 || count >= 21) break; // 限制数量避免溢出

            ItemStack titleItem = createShopTitleItem(title, player);
            inventory.setItem(slot, titleItem);

            slot++;
            count++;

            // 每行7个物品，然后换行
            if ((slot - 10) % 7 == 0) {
                slot += 2; // 跳到下一行，跳过边框
            }
        }

        return inventory;
    }

    /**
     * 获取商店总称号数量
     */
    private int getTotalShopTitles() {
        try {
            List<Title> allTitles = new ArrayList<>(plugin.getTitleManager().getAllTitles());
            Map<String, PlayerTitle> playerTitles = plugin.getTitleManager().getPlayerTitles(null);

            // 过滤掉玩家已经拥有的永久称号
            allTitles.removeIf(title -> {
                PlayerTitle playerTitle = playerTitles.get(title.getId());
                return playerTitle != null && playerTitle.isPermanent();
            });

            return allTitles.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private void addGrayGlassPaneBorder(Inventory gui) {
        ItemStack grayGlass = createGrayPane(); // 使用兼容性方法创建灰色玻璃板

        int size = gui.getSize();
        // 添加顶部和底部边框
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, grayGlass); // 顶部
            gui.setItem(size - 9 + i, grayGlass); // 底部
        }

        // 添加左右边框
        for (int i = 1; i < (size / 9) - 1; i++) {
            gui.setItem(i * 9, grayGlass); // 左边
            gui.setItem(i * 9 + 8, grayGlass); // 右边
        }
    }

    private void addShopNavigationButtons(Inventory gui, int currentPage, int totalPages) {
        int guiSize = gui.getSize();

        // 自定义称号按钮 (左下角)
        ItemStack customTitle = new ItemStack(Material.ANVIL);
        ItemMeta customMeta = customTitle.getItemMeta();
        if (customMeta != null) {
            customMeta.setDisplayName("§6自定义称号");
            List<String> lore = new ArrayList<>();
            lore.add("§7点击创建自定义称号");
            lore.add("§7需要消耗特殊材料");
            customMeta.setLore(lore);
            customTitle.setItemMeta(customMeta);
        }
        gui.setItem(guiSize - 9, customTitle); // 左下角

        // 返回按钮 (退出按钮左边一格)
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§6返回");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(guiSize - 2, backButton); // 返回按钮

        // 退出按钮 (右下角)
        ItemStack exitButton = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exitButton.getItemMeta();
        if (exitMeta != null) {
            exitMeta.setDisplayName("§c退出");
            exitButton.setItemMeta(exitMeta);
        }
        gui.setItem(guiSize - 1, exitButton); // 退出按钮

        // 上一页按钮 (页面信息左边)
        if (currentPage > 1) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§6上一页");
                prevPage.setItemMeta(prevMeta);
            }
            gui.setItem(guiSize - 6, prevPage);
        }

        // 页面信息
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§e第 " + currentPage + " / " + totalPages + " 页");
            pageInfo.setItemMeta(pageMeta);
        }
        gui.setItem(guiSize - 5, pageInfo);

        // 下一页按钮 (页面信息右边)
        if (currentPage < totalPages) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§6下一页");
                nextPage.setItemMeta(nextMeta);
            }
            gui.setItem(guiSize - 4, nextPage);
        }
    }

    private ItemStack createTitleItem(Title title, Player player) {
        Material material = Material.NAME_TAG;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("§7" + title.getContent());
            lore.add("");

            // 获取玩家该称号的状态
            PlayerTitle playerTitle = titleManager.getPlayerTitle(player, title.getId());
            String activeTitle = titleManager.getActiveTitle(player);
            boolean isActive = title.getId().equals(activeTitle);

            if (playerTitle != null) {
                // 显示称号状态
                lore.add("§f状态: " + playerTitle.getStatusDisplay(isActive));

                // 显示剩余时间（如果不是永久）
                if (!playerTitle.isPermanent()) {
                    lore.add("§f剩余时间: " + playerTitle.getRemainingTimeDisplay());
                } else {
                    lore.add("§f持续时间: " + title.getDurationDisplay());
                }

                lore.add("");

                if (isActive) {
                    lore.add("§a✔ 当前使用中");
                    lore.add("§e点击取消使用此称号");
                } else if (!playerTitle.isExpired()) {
                    lore.add("§e点击使用此称号");
                } else {
                    lore.add("§c该称号已过期");
                }
            }

            lore.add("§7类型: " + getTypeDisplayName(title.getType()));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createShopTitleItem(Title title, Player player) {
        Material material = Material.PAPER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("§7" + title.getContent());
            lore.add("");
            lore.add("§7类型: " + getTypeDisplayName(title.getType()));
            lore.add("§f持续时间: " + title.getDurationDisplay());

            // 显示价格信息
            switch (title.getType()) {
                case VAULT:
                    lore.add("§6价格: " + title.getPrice() + " 金币");
                    break;
                case PLAYER_POINTS:
                    lore.add("§b价格: " + title.getPoints() + " 点券");
                    break;
                case COIN:
                    lore.add("§e价格: " + title.getCoins() + " 称号币");
                    break;
                case ITEM_STACK:
                    lore.add("§d需要物品: " + title.getRequiredItem());
                    break;
                case PERMISSION:
                    lore.add("§5需要权限: " + title.getPermission());
                    break;
            }

            lore.add("");
            if (plugin.getEconomyManager().canPurchase(player, title)) {
                lore.add("§a✔ 可以购买");
                lore.add("§e点击购买此称号");
            } else {
                lore.add("§c✘ 无法购买");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void addNavigationButtons(Inventory gui, int currentPage, int totalPages, String type) {
        int guiSize = gui.getSize();

        // 上一页按钮
        if (currentPage > 1) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§6上一页");
                prevPage.setItemMeta(prevMeta);
            }
            gui.setItem(guiSize - 9, prevPage);
        }

        // 页面信息
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§e第 " + currentPage + " / " + totalPages + " 页");
            pageInfo.setItemMeta(pageMeta);
        }
        gui.setItem(guiSize - 5, pageInfo);

        // 下一页按钮
        if (currentPage < totalPages) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§6下一页");
                nextPage.setItemMeta(nextMeta);
            }
            gui.setItem(guiSize - 1, nextPage);
        }

        // 切换商店/仓库按钮
        ItemStack switchButton;
        if ("仓库".equals(type)) {
            switchButton = new ItemStack(Material.GOLD_INGOT);
            ItemMeta switchMeta = switchButton.getItemMeta();
            if (switchMeta != null) {
                switchMeta.setDisplayName("§6前往称号商店");
                switchButton.setItemMeta(switchMeta);
            }
        } else {
            switchButton = new ItemStack(Material.CHEST);
            ItemMeta switchMeta = switchButton.getItemMeta();
            if (switchMeta != null) {
                switchMeta.setDisplayName("§6前往称号仓库");
                switchButton.setItemMeta(switchMeta);
            }
        }
        gui.setItem(guiSize - 3, switchButton);

        // 设置按钮
        ItemStack settingsButton = new ItemStack(Material.REDSTONE);
        ItemMeta settingsMeta = settingsButton.getItemMeta();
        if (settingsMeta != null) {
            settingsMeta.setDisplayName("§c显示设置");
            settingsButton.setItemMeta(settingsMeta);
        }
        gui.setItem(guiSize - 7, settingsButton);
    }

    // 新增：打开奖励GUI
    public void openRewardsGUI(Player player, int page) {
        int guiSize = plugin.getConfigManager().getGUISize();
        Inventory gui = Bukkit.createInventory(player, guiSize, "称号奖励 - 第" + page + "页");

        // 添加灰色玻璃板边框 - 使用兼容性方法
        addGrayGlassPaneBorder(gui);

        List<Reward> allRewards = new ArrayList<>(titleManager.getAllRewards());
        Map<Reward, RewardStatus> rewardStatus = titleManager.getPlayerRewardStatus(player);
        int playerTitleCount = titleManager.getPlayerTitleCount(player);

        // 计算分页
        int itemsPerPage = guiSize - 18; // 考虑到边框和按钮
        int totalPages = (int) Math.ceil((double) allRewards.size() / itemsPerPage);

        if (page > totalPages) {
            page = totalPages;
        }
        if (page < 1) {
            page = 1;
        }

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allRewards.size());

        // 添加奖励物品 (从第10格开始，避开边框)
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            Reward reward = allRewards.get(i);
            ItemStack rewardItem = createRewardItem(reward, rewardStatus.get(reward), playerTitleCount);

            // 计算位置，确保在玻璃板内
            gui.setItem(slot, rewardItem);
            slot++;

            // 每行7个物品，然后换行
            if ((slot - 10) % 7 == 0) {
                slot += 2; // 跳到下一行，跳过边框
            }
        }

        // 添加导航按钮
        addRewardsNavigationButtons(gui, page, totalPages, playerTitleCount);

        player.openInventory(gui);
    }

    // 新增：创建奖励物品
    private ItemStack createRewardItem(Reward reward, RewardStatus status, int playerTitleCount) {
        Material material = Material.CHEST;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(reward.getDisplayName());

            List<String> lore = new ArrayList<>();

            // 添加奖励描述
            lore.addAll(reward.getDescription());
            lore.add("");

            // 显示需求
            lore.add("§f需求称号数量: " + reward.getRequiredTitles() + " 个");
            lore.add("§f当前拥有: " + playerTitleCount + " 个称号");
            lore.add("");

            // 显示状态
            if (status.canClaim()) {
                if (status.isClaimed() && reward.isRepeatable()) {
                    lore.add("§a✔ 可以再次领取");
                    lore.add("§e点击领取奖励");
                } else if (!status.isClaimed()) {
                    lore.add("§a✔ 可以领取");
                    lore.add("§e点击领取奖励");
                } else {
                    lore.add("§a✔ 已领取");
                }
            } else {
                lore.add("§c✘ 无法领取");
                if (playerTitleCount < reward.getRequiredTitles()) {
                    lore.add("§7还需要 " + (reward.getRequiredTitles() - playerTitleCount) + " 个称号");
                }
                if (!reward.getPermission().isEmpty() && !Bukkit.getPlayerExact(meta.getDisplayName()).hasPermission(reward.getPermission())) {
                    lore.add("§7缺少权限: " + reward.getPermission());
                }
            }

            // 显示领取次数（如果已领取）
            if (status.isClaimed() && status.getPlayerReward() != null) {
                lore.add("");
                lore.add("§7已领取次数: " + status.getPlayerReward().getClaimCount());
                if (reward.isRepeatable()) {
                    lore.add("§7可重复领取");
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    // 新增：奖励页面导航按钮
    private void addRewardsNavigationButtons(Inventory gui, int currentPage, int totalPages, int titleCount) {
        int guiSize = gui.getSize();

        // 称号数量显示
        ItemStack titleCountItem = new ItemStack(Material.NAME_TAG);
        ItemMeta countMeta = titleCountItem.getItemMeta();
        if (countMeta != null) {
            countMeta.setDisplayName("§6称号收集");
            List<String> countLore = new ArrayList<>();
            countLore.add("§f当前拥有称号: " + titleCount + " 个");
            countLore.add("§7继续收集称号解锁更多奖励!");
            countMeta.setLore(countLore);
            titleCountItem.setItemMeta(countMeta);
        }
        gui.setItem(guiSize - 9, titleCountItem); // 左下角

        // 返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§6返回主菜单");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(guiSize - 2, backButton); // 返回按钮

        // 退出按钮
        ItemStack exitButton = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exitButton.getItemMeta();
        if (exitMeta != null) {
            exitMeta.setDisplayName("§c退出");
            exitButton.setItemMeta(exitMeta);
        }
        gui.setItem(guiSize - 1, exitButton); // 退出按钮

        // 上一页按钮
        if (currentPage > 1) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§6上一页");
                prevPage.setItemMeta(prevMeta);
            }
            gui.setItem(guiSize - 6, prevPage);
        }

        // 页面信息
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§e第 " + currentPage + " / " + totalPages + " 页");
            pageInfo.setItemMeta(pageMeta);
        }
        gui.setItem(guiSize - 5, pageInfo);

        // 下一页按钮
        if (currentPage < totalPages) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§6下一页");
                nextPage.setItemMeta(nextMeta);
            }
            gui.setItem(guiSize - 4, nextPage);
        }
    }

    /* -------- 染料兼容 -------- */
    private static Material compatDye(boolean on) {
        return on ? XMaterial.LIME_DYE.parseMaterial() : XMaterial.GRAY_DYE.parseMaterial();
    }

    public void openSettingsGUI(Player player) {
        Inventory settingsGUI = Bukkit.createInventory(null, 27, "称号显示设置");

        TitleManager tm = plugin.getTitleManager();
        boolean headDisplay   = tm.isHeadDisplayEnabled(player);
        boolean tabDisplay    = tm.isTabListDisplayEnabled(player);
        boolean chatDisplay   = tm.isChatDisplayEnabled(player);
        boolean particleDisplay = tm.isParticleDisplayEnabled(player);

        /* -------- 统一用 XSeries 材质 -------- */
        ItemStack headItem   = new ItemStack(compatDye(headDisplay));
        ItemStack tabItem    = new ItemStack(compatDye(tabDisplay));
        ItemStack chatItem   = new ItemStack(compatDye(chatDisplay));
        ItemStack particleItem = new ItemStack(particleDisplay
                ? XMaterial.FIREWORK_STAR.parseMaterial()
                : XMaterial.GUNPOWDER.parseMaterial());

        /* 头顶 */
        ItemMeta headMeta = headItem.getItemMeta();
        headMeta.setDisplayName("§6头顶显示");
        headMeta.setLore(Arrays.asList(
                "§7控制是否在头顶显示称号", "",
                headDisplay ? "§a✔ 已开启" : "§c✖ 已关闭", "", "§e点击切换"));
        headItem.setItemMeta(headMeta);

        /* Tab */
        ItemMeta tabMeta = tabItem.getItemMeta();
        tabMeta.setDisplayName("§6Tab列表显示");
        tabMeta.setLore(Arrays.asList(
                "§7控制是否在Tab列表显示称号", "",
                tabDisplay ? "§a✔ 已开启" : "§c✖ 已关闭", "", "§e点击切换"));
        tabItem.setItemMeta(tabMeta);

        /* 聊天 */
        ItemMeta chatMeta = chatItem.getItemMeta();
        chatMeta.setDisplayName("§6聊天显示");
        chatMeta.setLore(Arrays.asList(
                "§7控制是否在聊天时显示称号", "",
                chatDisplay ? "§a✔ 已开启" : "§c✖ 已关闭", "", "§e点击切换"));
        chatItem.setItemMeta(chatMeta);

        /* 粒子 */
        ItemMeta particleMeta = particleItem.getItemMeta();
        particleMeta.setDisplayName("§6粒子效果显示");
        particleMeta.setLore(Arrays.asList(
                "§7控制是否显示称号的粒子效果", "",
                particleDisplay ? "§a✔ 已开启" : "§c✖ 已关闭", "", "§e点击切换"));
        particleItem.setItemMeta(particleMeta);

        /* 返回 */
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§c返回");
        backItem.setItemMeta(backMeta);

        /* 摆放 */
        settingsGUI.setItem(10, headItem);
        settingsGUI.setItem(12, tabItem);
        settingsGUI.setItem(14, chatItem);
        settingsGUI.setItem(16, particleItem);
        settingsGUI.setItem(22, backItem);

        player.openInventory(settingsGUI);
    }

    // 添加自定义称号GUI方法
    public void openCustomTitleGUI(Player player) {
        openCustomTitleEconomySelection(player);
    }

    /**
     * 打开商店管理界面
     */
    public void openShopManagement(Player player, int page) {
        int guiSize = plugin.getConfigManager().getGUISize();
        Inventory gui = Bukkit.createInventory(player, guiSize, "称号商城管理 - 第" + page + "页");

        // 添加灰色玻璃板边框
        addGrayGlassPaneBorder(gui);

        List<Title> allTitles = new ArrayList<>(titleManager.getAllTitles());

        // 计算分页
        int itemsPerPage = 28; // 4行 * 7列
        int totalPages = (int) Math.ceil((double) allTitles.size() / itemsPerPage);

        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allTitles.size());

        // 添加称号物品
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Title title = allTitles.get(i);
            ItemStack titleItem = createShopManagementItem(title);

            int slot = calculateTitleSlot(slotIndex);
            gui.setItem(slot, titleItem);
            slotIndex++;
        }

        // 添加导航按钮
        addManagementNavigationButtons(gui, page, totalPages, "shop");

        player.openInventory(gui);
    }

    /**
     * 创建商店管理物品
     */
    private ItemStack createShopManagementItem(Title title) {
        Material material = Material.DIAMOND_CHESTPLATE; // 可以根据需要调整材质
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("§7" + title.getContent());
            lore.add("");

            // 获取拥有该称号的玩家数量
            int playerCount = titleManager.getPlayersWithTitle(title.getId()).size();
            lore.add("§f拥有玩家: §e" + playerCount + " 人");
            lore.add("§f购买数量: §e" + getPurchaseCount(title.getId()) + " 次");
            lore.add("");
            lore.add("§7类型: " + getTypeDisplayName(title.getType()));
            lore.add("§f价格: " + getPriceDisplay(title));
            lore.add("§f天数: " + title.getDurationDisplay());
            lore.add("");
            lore.add("§e点击查看详情和管理");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 打开称号详情管理界面
     */
    public void openTitleDetailManagement(Player admin, Title title, int playerPage) {
        Inventory gui = Bukkit.createInventory(admin, 54, "称号管理: " + title.getDisplayName());

        // 添加灰色玻璃板边框
        addGrayGlassPaneBorder(gui);

        // 显示称号信息
        ItemStack titleInfo = createTitleInfoItem(title);
        gui.setItem(4, titleInfo);

        // 获取拥有该称号的玩家列表
        List<Player> playersWithTitle = titleManager.getPlayersWithTitle(title.getId());

        // 计算玩家分页
        int playersPerPage = 28;
        int totalPlayerPages = (int) Math.ceil((double) playersWithTitle.size() / playersPerPage);

        if (playerPage > totalPlayerPages) playerPage = totalPlayerPages;
        if (playerPage < 1) playerPage = 1;

        int startIndex = (playerPage - 1) * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, playersWithTitle.size());

        // 添加玩家头像
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Player targetPlayer = playersWithTitle.get(i);
            ItemStack playerHead = createPlayerHeadItem(targetPlayer, title.getId());

            int slot = calculateTitleSlot(slotIndex);
            gui.setItem(slot, playerHead);
            slotIndex++;
        }

        // 添加管理按钮
        addTitleManagementButtons(gui, title, playerPage, totalPlayerPages);

        admin.openInventory(gui);
    }

    /**
     * 创建玩家头像物品
     */
    private ItemStack createPlayerHeadItem(Player player, String titleId) {
        /* ---- 1. 版本安全的 Material ---- */
        Material headMaterial;
        try {
            headMaterial = Material.valueOf("PLAYER_HEAD");   // 1.13+
        } catch (IllegalArgumentException ex) {
            headMaterial = Material.valueOf("SKULL_ITEM");    // 1.12-
        }

        ItemStack head = new ItemStack(headMaterial, 1, (short) 3); // SKULL_ITEM 需要 data=3 才是玩家头颅
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6" + player.getName());
            meta.setOwningPlayer(player);

            List<String> lore = new ArrayList<>();
            lore.add("§7拥有称号: " + titleManager.getTitle(titleId).getDisplayName());
            lore.add("");
            lore.add("§c点击删除该玩家的此称号");

            meta.setLore(lore);
            head.setItemMeta(meta);
        }

        return head;
    }

    /**
     * 创建称号信息物品
     */
    private ItemStack createTitleInfoItem(Title title) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6" + title.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("§7" + title.getContent());
            lore.add("");
            lore.add("§fID: §7" + title.getId());
            lore.add("§f类型: §7" + getTypeDisplayName(title.getType()));
            lore.add("§f价格: §7" + getPriceDisplay(title));
            lore.add("§f天数: §7" + title.getDurationDisplay());
            lore.add("§f上架状态: " + (title.isShowInShop() ? "§a是" : "§c否"));
            lore.add("");
            lore.add("§f拥有玩家: §e" + titleManager.getPlayersWithTitle(title.getId()).size() + " 人");
            lore.add("§f购买次数: §e" + getPurchaseCount(title.getId()) + " 次");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 添加称号管理按钮
     */
    private void addTitleManagementButtons(Inventory gui, Title title, int playerPage, int totalPlayerPages) {
        int guiSize = gui.getSize();

        // 删除按钮 (红石)
        ItemStack deleteButton = new ItemStack(Material.REDSTONE);
        ItemMeta deleteMeta = deleteButton.getItemMeta();
        if (deleteMeta != null) {
            deleteMeta.setDisplayName("§c删除称号");
            List<String> deleteLore = new ArrayList<>();
            deleteLore.add("§7点击永久删除此称号");
            deleteLore.add("§c警告: 此操作不可逆!");
            deleteMeta.setLore(deleteLore);
            deleteButton.setItemMeta(deleteMeta);
        }
        gui.setItem(guiSize - 9, deleteButton);

        // 切换上架状态按钮 (火把)
        ItemStack toggleButton = title.isShowInShop()
                ? XMaterial.TORCH.parseItem()
                : XMaterial.REDSTONE_TORCH.parseItem();   // 1.8-1.20 全兼容

        ItemMeta toggleMeta = toggleButton.getItemMeta();
        if (toggleMeta != null) {
            toggleMeta.setDisplayName(title.isShowInShop() ? "§a已上架" : "§c已下架");
            List<String> toggleLore = new ArrayList<>();
            toggleLore.add("§7当前状态: " + (title.isShowInShop() ? "§a在商店中显示" : "§c不在商店中显示"));
            toggleLore.add("§e点击切换上架状态");
            toggleMeta.setLore(toggleLore);
            toggleButton.setItemMeta(toggleMeta);
        }
        gui.setItem(guiSize - 8, toggleButton);

        // 更改名称按钮 (命名牌)
        ItemStack nameButton = new ItemStack(Material.NAME_TAG);
        ItemMeta nameMeta = nameButton.getItemMeta();
        if (nameMeta != null) {
            nameMeta.setDisplayName("§6更改名称");
            List<String> nameLore = new ArrayList<>();
            nameLore.add("§7当前: " + title.getDisplayName());
            nameLore.add("§e点击更改显示名称");
            nameMeta.setLore(nameLore);
            nameButton.setItemMeta(nameMeta);
        }
        gui.setItem(guiSize - 7, nameButton);

        // 更改类型按钮 (命名牌)
        ItemStack typeButton = new ItemStack(Material.NAME_TAG);
        ItemMeta typeMeta = typeButton.getItemMeta();
        if (typeMeta != null) {
            typeMeta.setDisplayName("§6更改类型");
            List<String> typeLore = new ArrayList<>();
            typeLore.add("§7当前: " + getTypeDisplayName(title.getType()));
            typeLore.add("§e点击更改获取类型");
            typeMeta.setLore(typeLore);
            typeButton.setItemMeta(typeMeta);
        }
        gui.setItem(guiSize - 6, typeButton);

        // 更改物品按钮 (命名牌)
        ItemStack itemButton = new ItemStack(Material.NAME_TAG);
        ItemMeta itemMeta = itemButton.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName("§6更改物品");
            List<String> itemLore = new ArrayList<>();
            itemLore.add("§7点击更改显示物品");
            itemMeta.setLore(itemLore);
            itemButton.setItemMeta(itemMeta);
        }
        gui.setItem(guiSize - 5, itemButton);

        // 更改价格按钮 (命名牌)
        ItemStack priceButton = new ItemStack(Material.NAME_TAG);
        ItemMeta priceMeta = priceButton.getItemMeta();
        if (priceMeta != null) {
            priceMeta.setDisplayName("§6更改价格");
            List<String> priceLore = new ArrayList<>();
            priceLore.add("§7当前: " + getPriceDisplay(title));
            priceLore.add("§e点击更改价格");
            priceMeta.setLore(priceLore);
            priceButton.setItemMeta(priceMeta);
        }
        gui.setItem(guiSize - 4, priceButton);

        // 更改天数按钮 (命名牌)
        ItemStack durationButton = new ItemStack(Material.NAME_TAG);
        ItemMeta durationMeta = durationButton.getItemMeta();
        if (durationMeta != null) {
            durationMeta.setDisplayName("§6更改天数");
            List<String> durationLore = new ArrayList<>();
            durationLore.add("§7当前: " + title.getDurationDisplay());
            durationLore.add("§e点击更改持续时间");
            durationMeta.setLore(durationLore);
            durationButton.setItemMeta(durationMeta);
        }
        gui.setItem(guiSize - 3, durationButton);

        // Buff配置按钮 (书)
        ItemStack buffButton = new ItemStack(Material.BOOK);
        ItemMeta buffMeta = buffButton.getItemMeta();
        if (buffMeta != null) {
            buffMeta.setDisplayName("§6Buff配置");
            List<String> buffLore = new ArrayList<>();
            buffLore.add("§7点击" + (title.hasBuff() ? "删除" : "添加") + "Buff配置");
            buffMeta.setLore(buffLore);
            buffButton.setItemMeta(buffMeta);
        }
        gui.setItem(guiSize - 2, buffButton);

        // 粒子配置按钮 (书)
        ItemStack particleButton = new ItemStack(Material.BOOK);
        ItemMeta particleMeta = particleButton.getItemMeta();
        if (particleMeta != null) {
            particleMeta.setDisplayName("§6粒子配置");
            List<String> particleLore = new ArrayList<>();
            particleLore.add("§7点击" + (title.hasParticle() ? "删除" : "添加") + "粒子配置");
            particleMeta.setLore(particleLore);
            particleButton.setItemMeta(particleMeta);
        }
        gui.setItem(guiSize - 1, particleButton);

        // 玩家分页按钮
        if (playerPage > 1) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§6上一页玩家");
                prevPage.setItemMeta(prevMeta);
            }
            gui.setItem(45, prevPage);
        }

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§e玩家列表 - 第" + playerPage + "/" + totalPlayerPages + "页");
            pageInfo.setItemMeta(pageMeta);
        }
        gui.setItem(49, pageInfo);

        if (playerPage < totalPlayerPages) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§6下一页玩家");
                nextPage.setItemMeta(nextMeta);
            }
            gui.setItem(53, nextPage);
        }

        // 返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c返回商店管理");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(48, backButton);
    }

    /**
     * 打开奖励管理界面
     */
    public void openRewardManagement(Player player, int page) {
        int guiSize = plugin.getConfigManager().getGUISize();
        Inventory gui = Bukkit.createInventory(player, guiSize, "奖励管理 - 第" + page + "页");

        // 添加灰色玻璃板边框
        addGrayGlassPaneBorder(gui);

        List<Reward> allRewards = new ArrayList<>(titleManager.getAllRewards());

        // 计算分页
        int itemsPerPage = 28; // 4行 * 7列
        int totalPages = (int) Math.ceil((double) allRewards.size() / itemsPerPage);

        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allRewards.size());

        // 添加奖励物品
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Reward reward = allRewards.get(i);
            ItemStack rewardItem = createRewardManagementItem(reward);

            int slot = calculateTitleSlot(slotIndex);
            gui.setItem(slot, rewardItem);
            slotIndex++;
        }

        // 添加管理导航按钮
        addRewardManagementNavigationButtons(gui, page, totalPages);

        player.openInventory(gui);
    }

    /**
     * 创建奖励管理物品
     */
    private ItemStack createRewardManagementItem(Reward reward) {
        Material material = Material.CHEST;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(reward.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.addAll(reward.getDescription());
            lore.add("");
            lore.add("§f需求称号数量: " + reward.getRequiredTitles() + " 个");
            lore.add("§f权限需求: " + (reward.getPermission().isEmpty() ? "无" : reward.getPermission()));
            lore.add("§f可重复领取: " + (reward.isRepeatable() ? "是" : "否"));
            lore.add("");
            lore.add("§e点击管理此奖励");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 添加奖励管理导航按钮
     */
    private void addRewardManagementNavigationButtons(Inventory gui, int currentPage, int totalPages) {
        int guiSize = gui.getSize();

        // 上一页按钮
        if (currentPage > 1) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§6上一页");
                prevPage.setItemMeta(prevMeta);
            }
            gui.setItem(guiSize - 9, prevPage);
        }

        // 页面信息
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§e第 " + currentPage + " / " + totalPages + " 页");
            pageInfo.setItemMeta(pageMeta);
        }
        gui.setItem(guiSize - 5, pageInfo);

        // 下一页按钮
        if (currentPage < totalPages) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§6下一页");
                nextPage.setItemMeta(nextMeta);
            }
            gui.setItem(guiSize - 1, nextPage);
        }

        // 切换到商店管理按钮
        ItemStack switchButton = new ItemStack(Material.GOLD_INGOT);
        ItemMeta switchMeta = switchButton.getItemMeta();
        if (switchMeta != null) {
            switchMeta.setDisplayName("§6切换到商店管理");
            switchButton.setItemMeta(switchMeta);
        }
        gui.setItem(guiSize - 3, switchButton);
    }

    /**
     * 打开玩家称号仓库管理
     */
    public void openPlayerTitleManagement(Player admin, Player targetPlayer, int page) {
        int guiSize = plugin.getConfigManager().getGUISize();
        Inventory gui = Bukkit.createInventory(admin, guiSize,
                "玩家称号管理 - " + targetPlayer.getName() + " - 第" + page + "页");

        // 添加灰色玻璃板边框
        addGrayGlassPaneBorder(gui);

        Map<String, PlayerTitle> playerTitles = titleManager.getPlayerTitles(targetPlayer);
        List<String> titleIds = new ArrayList<>(playerTitles.keySet());
        List<Title> availableTitles = new ArrayList<>();

        // 获取玩家已拥有的称号
        for (String titleId : titleIds) {
            Title title = titleManager.getTitle(titleId);
            if (title != null) {
                availableTitles.add(title);
            }
        }

        // 计算分页
        int itemsPerPage = 28;
        int totalPages = (int) Math.ceil((double) availableTitles.size() / itemsPerPage);

        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, availableTitles.size());

        // 添加称号物品
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Title title = availableTitles.get(i);
            ItemStack titleItem = createPlayerTitleItem(title, targetPlayer);

            int slot = calculateTitleSlot(slotIndex);
            gui.setItem(slot, titleItem);
            slotIndex++;
        }

        // 添加管理导航按钮
        addPlayerManagementNavigationButtons(gui, page, totalPages, targetPlayer);

        admin.openInventory(gui);
    }

    /**
     * 创建玩家称号物品（管理用）
     */
    private ItemStack createPlayerTitleItem(Title title, Player targetPlayer) {
        Material material = Material.NAME_TAG;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(title.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("§7" + title.getContent());
            lore.add("");

            PlayerTitle playerTitle = titleManager.getPlayerTitle(targetPlayer, title.getId());
            String activeTitle = titleManager.getActiveTitle(targetPlayer);
            boolean isActive = title.getId().equals(activeTitle);

            if (playerTitle != null) {
                lore.add("§f状态: " + playerTitle.getStatusDisplay(isActive));

                if (!playerTitle.isPermanent()) {
                    lore.add("§f剩余时间: " + playerTitle.getRemainingTimeDisplay());
                }

                lore.add("");
                lore.add("§c点击删除玩家此称号");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 添加玩家管理导航按钮
     */
    private void addPlayerManagementNavigationButtons(Inventory gui, int currentPage, int totalPages, Player targetPlayer) {
        int guiSize = gui.getSize();

        /* ---- 版本安全的 头驴 ---- */
        Material headMaterial = getPlayerHeadMaterial();

        /* 1. 玩家信息显示 */
        ItemStack playerInfo = new ItemStack(headMaterial, 1, (short) (headMaterial.getMaxDurability() == 0 ? 0 : 3));
        SkullMeta playerMeta = (SkullMeta) playerInfo.getItemMeta();
        if (playerMeta != null) {
            playerMeta.setDisplayName("§6" + targetPlayer.getName());
            playerMeta.setOwningPlayer(targetPlayer);   // ← 修正笔误

            List<String> playerLore = new ArrayList<>();
            playerLore.add("§f当前称号: " +
                    (titleManager.getActiveTitle(targetPlayer) != null ?
                            titleManager.getTitle(titleManager.getActiveTitle(targetPlayer)).getDisplayName() :
                            "无"));
            playerLore.add("§f拥有称号: " + titleManager.getPlayerTitleCount(targetPlayer) + " 个");

            playerMeta.setLore(playerLore);
            playerInfo.setItemMeta(playerMeta);
        }
        gui.setItem(guiSize - 9, playerInfo);

        // 返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c返回");
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(guiSize - 1, backButton);

        // 上一页按钮
        if (currentPage > 1) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§6上一页");
                prevPage.setItemMeta(prevMeta);
            }
            gui.setItem(guiSize - 6, prevPage);
        }

        // 页面信息
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§e第 " + currentPage + " / " + totalPages + " 页");
            pageInfo.setItemMeta(pageMeta);
        }
        gui.setItem(guiSize - 5, pageInfo);

        // 下一页按钮
        if (currentPage < totalPages) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§6下一页");
                nextPage.setItemMeta(nextMeta);
            }
            gui.setItem(guiSize - 4, nextPage);
        }
    }

    /* ------------- 工具方法 ------------- */
    private static Material getPlayerHeadMaterial() {
        try {
            // 1.13+
            return (Material) Material.class.getField("PLAYER_HEAD").get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 1.12-  用数字 ID 拿 SKULL_ITEM
            return Material.getMaterial(String.valueOf(397));   // 397 = SKULL_ITEM
        }
    }

    /**
     * 添加管理导航按钮
     */
    private void addManagementNavigationButtons(Inventory gui, int currentPage, int totalPages, String type) {
        int guiSize = gui.getSize();

        // 上一页按钮
        if (currentPage > 1) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§6上一页");
                prevPage.setItemMeta(prevMeta);
            }
            gui.setItem(guiSize - 9, prevPage);
        }

        // 页面信息
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§e第 " + currentPage + " / " + totalPages + " 页");
            pageInfo.setItemMeta(pageMeta);
        }
        gui.setItem(guiSize - 5, pageInfo);

        // 下一页按钮
        if (currentPage < totalPages) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§6下一页");
                nextPage.setItemMeta(nextMeta);
            }
            gui.setItem(guiSize - 1, nextPage);
        }

        // 切换界面按钮
        ItemStack switchButton;
        if ("shop".equals(type)) {
            switchButton = new ItemStack(Material.CHEST);
            ItemMeta switchMeta = switchButton.getItemMeta();
            if (switchMeta != null) {
                switchMeta.setDisplayName("§6切换到奖励管理");
                switchButton.setItemMeta(switchMeta);
            }
        } else {
            switchButton = new ItemStack(Material.GOLD_INGOT);
            ItemMeta switchMeta = switchButton.getItemMeta();
            if (switchMeta != null) {
                switchMeta.setDisplayName("§6切换到商店管理");
                switchButton.setItemMeta(switchMeta);
            }
        }
        gui.setItem(guiSize - 3, switchButton);
    }

    /**
     * 打开自定义称号支付选择界面 - 重构版本（参照商店界面）
     */
    public void openCustomTitleEconomySelection(Player player) {
        try {
            int guiSize = 54; // 使用标准商店大小
            Inventory gui = Bukkit.createInventory(player, guiSize, "§6自定义称号支付方式");

            // 添加灰色玻璃板边框
            addGrayGlassPaneBorder(gui);

            // 获取可用的经济系统
            List<String> enabledEconomies = plugin.getMoneyConfigManager().getEnabledEconomySystems();

            // 检查经济系统是否可用
            if (enabledEconomies.isEmpty()) {
                player.sendMessage("§c当前没有可用的支付方式，请联系管理员!");
                player.closeInventory();
                return;
            }

            plugin.getLogger().info("可用的经济系统: " + enabledEconomies);

            // 添加经济系统物品 - 参照商店的布局方式
            int slot = 10; // 从第二行第一格开始
            for (String economyKey : enabledEconomies) {
                if (economyKey == null) continue;

                // 检查经济系统是否可用
                if (!plugin.getMoneyConfigManager().isEconomyAvailable(economyKey)) {
                    plugin.getLogger().warning("经济系统不可用: " + economyKey);
                    continue;
                }

                // 创建经济系统物品
                ItemStack economyItem = createEconomyItem(economyKey, player);

                // 放置物品
                if (slot < guiSize - 9) { // 确保不超出范围
                    gui.setItem(slot, economyItem);
                    slot++;

                    // 每行7个物品，然后换行
                    if ((slot - 10) % 7 == 0) {
                        slot += 2; // 跳到下一行，跳过边框
                    }
                }
            }

            // 添加导航按钮 - 参照商店的布局
            addCustomTitleNavigationButtons(gui, player);

            player.openInventory(gui);

        } catch (Exception e) {
            plugin.getLogger().severe("打开自定义称号支付界面时出错: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§c打开支付界面时出现错误，请联系管理员!");
            player.closeInventory();
        }
    }

    /**
     * 创建经济系统物品 - 参照商店物品创建方式
     */
    private ItemStack createEconomyItem(String economyKey, Player player) {
        // 获取配置
        String materialName = plugin.getGUIConfigManager().getEconomyItemMaterial(economyKey);
        String itemDisplayName = plugin.getGUIConfigManager().getEconomyItemName(economyKey);
        List<String> lore = plugin.getGUIConfigManager().getEconomyItemLore(economyKey);

        // 默认值处理
        if (materialName == null) materialName = "PAPER";
        if (itemDisplayName == null) itemDisplayName = "§6" + economyKey;
        if (lore == null) lore = new ArrayList<>();

        Material material = Material.getMaterial(materialName);
        if (material == null) {
            material = Material.PAPER;
            plugin.getLogger().warning("材质不存在: " + materialName + "，使用默认材质");
        }

        ItemStack economyItem = new ItemStack(material);
        ItemMeta meta = economyItem.getItemMeta();

        if (meta != null) {
            // 设置显示名称
            meta.setDisplayName(itemDisplayName.replace("&", "§"));

            // 构建Lore
            List<String> finalLore = new ArrayList<>();
            double cost = plugin.getMoneyConfigManager().getEconomyCustomPrice(economyKey);
            String economyDisplayName = plugin.getMoneyConfigManager().getEconomyDisplayName(economyKey);

            // 添加配置的Lore
            for (String line : lore) {
                if (line != null) {
                    finalLore.add(line.replace("&", "§")
                            .replace("{cost}", String.valueOf(cost))
                            .replace("{economy}", economyDisplayName));
                }
            }

            // 添加状态信息 - 参照商店的格式
            finalLore.add("");
            finalLore.add("§f价格: §e" + cost + " " + economyDisplayName);
            finalLore.add("");

            // 检查玩家是否可以购买
            boolean canAfford = canPlayerAffordCustomTitle(player, economyKey);
            if (canAfford) {
                finalLore.add("§a✔ 可以购买");
                finalLore.add("§e点击选择此支付方式");
            } else {
                finalLore.add("§c✘ 余额不足");
                double balance = plugin.getEconomyManager().getBalance(player, economyKey);
                finalLore.add("§7当前余额: " + balance + " " + economyDisplayName);
            }

            meta.setLore(finalLore);
            economyItem.setItemMeta(meta);
        }

        return economyItem;
    }

    /**
     * 添加自定义称号界面的导航按钮 - 参照商店的导航按钮
     */
    private void addCustomTitleNavigationButtons(Inventory gui, Player player) {
        int guiSize = gui.getSize();

        // 返回按钮 - 左下角
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§6返回商店");
            List<String> backLore = new ArrayList<>();
            backLore.add("§7点击返回称号商店");
            backMeta.setLore(backLore);
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(guiSize - 9, backButton);

        // 信息显示 - 中间底部
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§6自定义称号信息");
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7创建属于你自己的独特称号");
            infoLore.add("§7支持颜色代码和特殊格式");
            infoLore.add("");
            infoLore.add("§f长度限制: §e" + plugin.getMoneyConfigManager().getCustomTitleMinLength() +
                    "-" + plugin.getMoneyConfigManager().getCustomTitleMaxLength() + "字符");
            infoLore.add("§f支持格式: §a&a颜色 §l&l粗体 §o&o斜体");
            infoMeta.setLore(infoLore);
            infoItem.setItemMeta(infoMeta);
        }
        gui.setItem(guiSize - 5, infoItem);

        // 退出按钮 - 右下角
        ItemStack exitButton = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exitButton.getItemMeta();
        if (exitMeta != null) {
            exitMeta.setDisplayName("§c退出");
            List<String> exitLore = new ArrayList<>();
            exitLore.add("§7点击关闭界面");
            exitMeta.setLore(exitLore);
            exitButton.setItemMeta(exitMeta);
        }
        gui.setItem(guiSize - 1, exitButton);
    }

    /**
     * 处理自定义称号支付界面的点击事件 - 参照商店的处理方式
     */
    public boolean handleCustomTitlePaymentClick(Player player, ItemStack clickedItem, String inventoryTitle) {
        if (!inventoryTitle.contains("自定义称号支付方式")) {
            return false;
        }

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return true;
        }

        String displayName = clickedItem.getItemMeta().getDisplayName();

        // 处理返回按钮
        if (displayName.equals("§6返回商店") || clickedItem.getType() == Material.ARROW) {
            playClickSound(player);
            openTitleShop(player, 1);
            return true;
        }

        // 处理退出按钮
        if (displayName.equals("§c退出") || clickedItem.getType() == Material.BARRIER) {
            playClickSound(player);
            player.closeInventory();
            return true;
        }

        // 处理信息物品（不执行任何操作）
        if (displayName.equals("§6自定义称号信息") || clickedItem.getType() == Material.BOOK) {
            playClickSound(player);
            return true;
        }

        // 处理经济系统选择
        List<String> enabledEconomies = plugin.getMoneyConfigManager().getEnabledEconomySystems();
        for (String economyKey : enabledEconomies) {
            String expectedName = plugin.getGUIConfigManager().getEconomyItemName(economyKey);
            if (expectedName != null) {
                expectedName = expectedName.replace("&", "§");
                if (displayName.equals(expectedName)) {
                    playClickSound(player);
                    handleCustomTitleEconomySelection(player, economyKey);
                    return true;
                }
            }
        }

        return true;
    }

    /**
     * 播放点击音效
     */
    private void playClickSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        } catch (Exception e) {
            // 兼容旧版本
            try {
                player.playSound(player.getLocation(), Sound.valueOf("CLICK"), 1f, 1f);
            } catch (Exception ex) {
                // 忽略音效错误
            }
        }
    }

    /**
     * 处理自定义称号创建点击
     */
    public void handleCustomTitleCreateClick(Player player) {
        // 检查是否有可用的经济系统
        List<String> enabledEconomies = plugin.getMoneyConfigManager().getEnabledEconomySystems();
        if (enabledEconomies.isEmpty()) {
            player.sendMessage("§c当前没有可用的支付方式，请联系管理员!");
            return;
        }

        // 如果只有一个经济系统，直接进入创建流程
        if (enabledEconomies.size() == 1) {
            String economyKey = enabledEconomies.get(0);
            handleCustomTitleEconomySelection(player, economyKey);
        } else {
            // 多个经济系统，打开选择界面
            openCustomTitleEconomySelection(player);
        }
    }

    /**
     * 处理自定义称号经济系统选择
     */
    public void handleCustomTitleEconomySelection(Player player, String economyKey) {
        // 关闭GUI
        player.closeInventory();

        try {
            // 检查经济系统是否可用
            if (!plugin.getMoneyConfigManager().isEconomyAvailable(economyKey)) {
                player.sendMessage("§c该支付方式当前不可用，请选择其他支付方式!");
                return;
            }

            // 检查玩家是否有足够余额
            if (!canPlayerAffordCustomTitle(player, economyKey)) {
                double cost = plugin.getMoneyConfigManager().getEconomyCustomPrice(economyKey);
                String economyDisplayName = plugin.getMoneyConfigManager().getEconomyDisplayName(economyKey);
                player.sendMessage("§c余额不足! 需要 " + cost + " " + economyDisplayName);
                return;
            }

            double cost = plugin.getMoneyConfigManager().getEconomyCustomPrice(economyKey);
            String economyDisplayName = plugin.getMoneyConfigManager().getEconomyDisplayName(economyKey);

            // 提示玩家输入称号名称
            player.sendMessage("§6=== 自定义称号创建 ===");
            player.sendMessage("§7价格: §e" + cost + " " + economyDisplayName);
            player.sendMessage("§7长度限制: §e" + plugin.getMoneyConfigManager().getCustomTitleMinLength() +
                    "-" + plugin.getMoneyConfigManager().getCustomTitleMaxLength() + "字符");
            player.sendMessage("§7支持颜色代码: §a&a绿色 §c&c红色 §e&e黄色 §6&6金色 §b&b蓝色");
            player.sendMessage("§7支持格式: §l&l粗体 §o&o斜体 §n&n下划线 §m&m删除线");
            player.sendMessage("");
            player.sendMessage("§6请输入你想要的自定义称号名称:");
            player.sendMessage("§c输入 'cancel' 取消创建");

            // 注册聊天监听
            plugin.getTitleManager().registerCustomTitleCreation(player, economyKey);

        } catch (Exception e) {
            player.sendMessage("§c创建自定义称号时出现错误，请联系管理员!");
            plugin.getLogger().severe("处理自定义称号经济选择时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查玩家是否可以支付自定义称号
     */
    private boolean canPlayerAffordCustomTitle(Player player, String economyKey) {
        try {
            if (economyKey == null) {
                plugin.getLogger().warning("经济系统键为null");
                return false;
            }

            // 确保 moneyConfigManager 不为 null
            if (plugin.getMoneyConfigManager() == null) {
                plugin.getLogger().warning("MoneyConfigManager 为 null");
                return false;
            }

            // 确保经济系统可用
            if (!plugin.getMoneyConfigManager().isEconomyAvailable(economyKey)) {
                plugin.getLogger().warning("经济系统不可用: " + economyKey);
                return false;
            }

            EconomyManager economyManager = plugin.getEconomyManager();
            if (economyManager == null) {
                plugin.getLogger().warning("EconomyManager为null");
                return false;
            }

            // 直接调用新添加的方法
            return economyManager.canPurchaseCustomTitle(player, economyKey);

        } catch (Exception e) {
            plugin.getLogger().warning("检查玩家余额时出错: " + e.getMessage());
            return false;
        }
    }

    // 辅助方法
    private String getPriceDisplay(Title title) {
        switch (title.getType()) {
            case VAULT: return title.getPrice() + " 金币";
            case PLAYER_POINTS: return title.getPoints() + " 点券";
            case COIN: return title.getCoins() + " 称号币";
            case ITEM_STACK: return "物品: " + title.getRequiredItem();
            case PERMISSION: return "权限: " + title.getPermission();
            default: return "免费";
        }
    }

    private int getPurchaseCount(String titleId) {
        // 这里需要实现获取购买次数的逻辑
        // 可以在TitleManager中添加统计功能
        return 0; // 暂时返回0
    }

    private String getTypeDisplayName(TitleType type) {
        switch (type) {
            case NOT: return "无条件";
            case VAULT: return "金币";
            case PLAYER_POINTS: return "点券";
            case COIN: return "称号币";
            case ITEM_STACK: return "物品";
            case PERMISSION: return "权限";
            case ACTIVITY: return "活动";
            default: return "未知";
        }
    }
}