package xyz.tolite.greatTitle.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.model.Title;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TitleCardManager {

    private static TitleCardManager instance;
    private final GreatTitle plugin;
    private final TitleManager titleManager;
    private final Random random;

    private TitleCardManager(GreatTitle plugin) {
        this.plugin = plugin;
        this.titleManager = plugin.getTitleManager();
        this.random = new Random();
    }

    public static TitleCardManager getInstance(GreatTitle plugin) {
        if (instance == null) {
            instance = new TitleCardManager(plugin);
        }
        return instance;
    }

    /**
     * 创建称号卡 - 统一方法
     */
    public ItemStack createTitleCard(List<String> titleIds, String cardType) {
        Material material = getCardMaterial(cardType);
        ItemStack card = new ItemStack(material);
        ItemMeta meta = card.getItemMeta();

        if (meta != null) {
            setCardDisplayName(meta, cardType, titleIds);
            setCardLore(meta, cardType, titleIds);
            card.setItemMeta(meta);
        }

        // 对于自定义称号卡，存储称号ID到NBT
        if ("custom".equals(cardType)) {
            card = storeTitleIdsInNBT(card, titleIds);
        }

        return card;
    }

    /**
     * 创建随机称号卡
     */
    public ItemStack createRandomCard() {
        return createTitleCard(new ArrayList<>(), "random");
    }

    /**
     * 创建固定称号卡
     */
    public ItemStack createFixedCard() {
        return createTitleCard(new ArrayList<>(), "fixed");
    }

    /**
     * 创建自定义称号卡
     */
    public ItemStack createCustomCard(List<String> titleIds) {
        return createTitleCard(titleIds, "custom");
    }

    /**
     * 获取卡片材质
     */
    private Material getCardMaterial(String cardType) {
        String itemType;
        switch (cardType) {
            case "random":
                itemType = plugin.getConfigManager().getRandomCardItem();
                break;
            case "fixed":
                itemType = plugin.getConfigManager().getFixedCardItem();
                break;
            default:
                itemType = "PAPER";
                break;
        }

        Material material = Material.getMaterial(itemType);
        return material != null ? material : Material.PAPER;
    }

    /**
     * 设置卡片显示名称
     */
    private void setCardDisplayName(ItemMeta meta, String cardType, List<String> titleIds) {
        switch (cardType) {
            case "random":
                meta.setDisplayName(plugin.getConfigManager().getRandomCardName());
                break;
            case "fixed":
                meta.setDisplayName(plugin.getConfigManager().getFixedCardName());
                break;
            default:
                if (titleIds.size() == 1) {
                    Title title = titleManager.getTitle(titleIds.get(0));
                    String titleName = title != null ? title.getDisplayName() : titleIds.get(0);
                    meta.setDisplayName("§6" + titleName + " §e称号卡");
                } else {
                    meta.setDisplayName("§6多称号卡 §e(" + titleIds.size() + "个称号)");
                }
                break;
        }
    }

    /**
     * 设置卡片Lore
     */
    private void setCardLore(ItemMeta meta, String cardType, List<String> titleIds) {
        List<String> lore = new ArrayList<>();

        switch (cardType) {
            case "random":
                lore.add("§7右键点击获得随机称号");
                addRandomCardPoolInfo(lore);
                lore.add("§e类型: 随机");
                break;
            case "fixed":
                lore.add("§7右键点击获得固定称号");
                addFixedCardInfo(lore);
                lore.add("§e类型: 固定");
                break;
            default:
                lore.add("§7右键点击获得称号");
                addCustomCardInfo(lore, titleIds);
                lore.add("§e类型: 自定义");
                break;
        }

        meta.setLore(lore);
    }

    /**
     * 添加随机卡池信息
     */
    private void addRandomCardPoolInfo(List<String> lore) {
        List<String> titlePool = plugin.getConfigManager().getRandomCardTitlePool();
        if (!titlePool.isEmpty()) {
            lore.add("§6可能获得的称号:");
            for (int i = 0; i < Math.min(titlePool.size(), 3); i++) {
                String titleId = titlePool.get(i);
                Title title = titleManager.getTitle(titleId);
                String titleName = title != null ? title.getDisplayName() : titleId;
                lore.add("§7- " + titleName);
            }
            if (titlePool.size() > 3) {
                lore.add("§7... 还有 " + (titlePool.size() - 3) + " 个可能称号");
            }
        }
    }

    /**
     * 添加固定卡信息
     */
    private void addFixedCardInfo(List<String> lore) {
        String fixedTitle = plugin.getConfigManager().getFixedCardTitle();
        Title title = titleManager.getTitle(fixedTitle);
        String titleName = title != null ? title.getDisplayName() : fixedTitle;
        lore.add("§e包含称号: " + titleName);
    }

    /**
     * 添加自定义卡信息
     */
    private void addCustomCardInfo(List<String> lore, List<String> titleIds) {
        if (titleIds.size() == 1) {
            Title title = titleManager.getTitle(titleIds.get(0));
            String titleName = title != null ? title.getDisplayName() : titleIds.get(0);
            lore.add("§e包含称号: " + titleName);
        } else {
            lore.add("§e包含 " + titleIds.size() + " 个称号:");
            for (int i = 0; i < Math.min(titleIds.size(), 5); i++) {
                Title title = titleManager.getTitle(titleIds.get(i));
                String titleName = title != null ? title.getDisplayName() : titleIds.get(i);
                lore.add("§7- " + titleName);
            }
            if (titleIds.size() > 5) {
                lore.add("§7... 还有 " + (titleIds.size() - 5) + " 个称号");
            }
        }
    }

    /**
     * 将称号ID列表存储到NBT
     */
    private ItemStack storeTitleIdsInNBT(ItemStack item, List<String> titleIds) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "custom_title_ids");

        String titleIdsString = String.join(",", titleIds);
        data.set(key, PersistentDataType.STRING, titleIdsString);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 从NBT读取称号ID列表
     */
    public List<String> getTitleIdsFromNBT(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return new ArrayList<>();

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "custom_title_ids");

        String titleIdsString = data.get(key, PersistentDataType.STRING);
        if (titleIdsString == null) return new ArrayList<>();

        return List.of(titleIdsString.split(","));
    }

    /**
     * 给予玩家物品
     */
    public void giveItemToPlayer(Player player, ItemStack item, String itemName) {
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
    public void handleRandomCardUse(Player player, ItemStack item) {
        item.setAmount(item.getAmount() - 1);

        List<String> titlePool = plugin.getConfigManager().getRandomCardTitlePool();
        if (titlePool.isEmpty()) {
            player.sendMessage("§c随机称号池为空! 请联系管理员。");
            return;
        }

        String titleId = titlePool.get(random.nextInt(titlePool.size()));

        if (titleManager.getPlayerTitle(player, titleId) != null) {
            player.sendMessage("§c你已经拥有这个称号了! 请尝试其他称号卡。");
            return;
        }

        if (titleManager.giveTitle(player, titleId, 0)) {
            Title title = titleManager.getTitle(titleId);
            player.sendMessage("§a恭喜! 你获得了随机称号: " + title.getDisplayName());

            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
            }
        } else {
            player.sendMessage("§c使用随机称号卡失败! 请联系管理员。");
        }
    }

    /**
     * 处理固定称号卡使用
     */
    public void handleFixedCardUse(Player player, ItemStack item) {
        item.setAmount(item.getAmount() - 1);

        String titleId = plugin.getConfigManager().getFixedCardTitle();

        if (titleManager.getPlayerTitle(player, titleId) != null) {
            player.sendMessage("§c你已经拥有这个称号了! 无法重复获得。");
            return;
        }

        if (titleId != null && titleManager.giveTitle(player, titleId, 0)) {
            Title title = titleManager.getTitle(titleId);
            player.sendMessage("§a恭喜! 你获得了固定称号: " + title.getDisplayName());

            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
            }
        } else {
            player.sendMessage("§c使用固定称号卡失败! 请联系管理员。");
        }
    }

    /**
     * 处理自定义称号卡使用
     */
    public void handleCustomCardUse(Player player, ItemStack item) {
        List<String> titleIds = getTitleIdsFromNBT(item);
        if (titleIds.isEmpty()) {
            player.sendMessage("§c称号卡数据错误! 请联系管理员。");
            return;
        }

        item.setAmount(item.getAmount() - 1);

        int successCount = 0;
        List<String> newTitles = new ArrayList<>();

        for (String titleId : titleIds) {
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
                for (int i = 0; i < Math.min(newTitles.size(), 3); i++) {
                    player.sendMessage("§7- " + newTitles.get(i));
                }
                if (newTitles.size() > 3) {
                    player.sendMessage("§7... 还有 " + (newTitles.size() - 3) + " 个称号");
                }
            }

            if (plugin.getHeadDisplayManager() != null) {
                plugin.getHeadDisplayManager().updatePlayerHeadDisplay(player);
            }
        } else {
            player.sendMessage("§c使用称号卡失败! 你已经拥有这些称号了。");
        }
    }
}