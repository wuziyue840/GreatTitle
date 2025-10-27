package xyz.tolite.greatTitle.model;

import java.util.ArrayList;
import java.util.List;

public class Title {

    private final String id;
    private final String displayName;
    private final String content;
    private final TitleType type;
    private final double price;
    private final int points;
    private final int coins;
    private final String permission;
    private final String requiredItem;
    private final boolean rgb;
    private final int duration;
    private final List<Buff> buffs;
    private final List<String> particleEffects;
    private final int switchCooldown;
    private final List<CurrencyCost> currencyCosts;
    private final List<String> description;
    private boolean showInShop;
    private long shopExpireTime;
    private final String customEconomy;
    private final double customEconomyCost;
    private final boolean isCustom;

    // 完整的构造方法
    public Title(String id, String displayName, String content, TitleType type,
                 double price, int points, int coins, String permission,
                 String requiredItem, boolean rgb, int duration,
                 List<Buff> buffs, List<String> particleEffects, int switchCooldown,
                 List<CurrencyCost> currencyCosts, List<String> description,
                 boolean showInShop, long shopExpireTime,
                 String customEconomy, double customEconomyCost, boolean isCustom) {
        this.id = id;
        this.displayName = displayName;
        this.content = content;
        this.type = type;
        this.price = price;
        this.points = points;
        this.coins = coins;
        this.permission = permission;
        this.requiredItem = requiredItem;
        this.rgb = rgb;
        this.duration = duration;
        this.buffs = buffs != null ? buffs : new ArrayList<>();
        this.particleEffects = particleEffects != null ? particleEffects : new ArrayList<>();
        this.switchCooldown = switchCooldown;
        this.currencyCosts = currencyCosts != null ? currencyCosts : new ArrayList<>();
        this.description = description != null ? description : new ArrayList<>();
        this.showInShop = showInShop;
        this.shopExpireTime = shopExpireTime;
        this.customEconomy = customEconomy;
        this.customEconomyCost = customEconomyCost;
        this.isCustom = isCustom;
    }

    // 为了向后兼容，更新简化版的构造方法
    public Title(String id, String displayName, String content, TitleType type,
                 double price, int points, int coins, String permission,
                 String requiredItem, boolean rgb, int duration) {
        this(id, displayName, content, type, price, points, coins, permission,
                requiredItem, rgb, duration, new ArrayList<>(), new ArrayList<>(),
                0, new ArrayList<>(), new ArrayList<>(), true, 0,
                null, 0.0, false); // 默认值：非自定义称号
    }

    // 另一个简化版构造方法，包含自定义经济参数
    public Title(String id, String displayName, String content, TitleType type,
                 double price, int points, int coins, String permission,
                 String requiredItem, boolean rgb, int duration,
                 String customEconomy, double customEconomyCost, boolean isCustom) {
        this(id, displayName, content, type, price, points, coins, permission,
                requiredItem, rgb, duration, new ArrayList<>(), new ArrayList<>(),
                0, new ArrayList<>(), new ArrayList<>(), true, 0,
                customEconomy, customEconomyCost, isCustom);
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getContent() { return content; }
    public TitleType getType() { return type; }
    public double getPrice() { return price; }
    public int getPoints() { return points; }
    public int getCoins() { return coins; }
    public String getPermission() { return permission; }
    public String getRequiredItem() { return requiredItem; }
    public boolean isRgb() { return rgb; }
    public int getDuration() { return duration; }
    public List<Buff> getBuffs() { return buffs; }
    public List<String> getParticleEffects() { return particleEffects; }
    public int getSwitchCooldown() { return switchCooldown; }
    public List<CurrencyCost> getCurrencyCosts() { return currencyCosts; }
    public List<String> getDescription() { return description; }
    public boolean isShowInShop() { return showInShop; }
    public long getShopExpireTime() { return shopExpireTime; }
    public String getCustomEconomy() { return customEconomy; }
    public double getCustomEconomyCost() { return customEconomyCost; }
    public boolean isCustom() { return isCustom; }

    // 新增：获取Buff配置（字符串形式）
    public String getBuff() {
        if (buffs == null || buffs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Buff buff : buffs) {
            sb.append(buff.getEffectType()).append(":").append(buff.getAmplifier()).append(";");
        }
        return sb.toString();
    }

    // 新增：获取粒子配置（字符串形式）
    public String getParticle() {
        if (particleEffects == null || particleEffects.isEmpty()) {
            return "";
        }
        return String.join(";", particleEffects);
    }

    // 新增：检查是否有Buff配置
    public boolean hasBuff() {
        return buffs != null && !buffs.isEmpty();
    }

    // 新增：检查是否有粒子配置
    public boolean hasParticle() {
        return particleEffects != null && !particleEffects.isEmpty();
    }

    // 判断是否在商城中显示
    public boolean isAvailableInShop() {
        if (!showInShop) return false;
        if (shopExpireTime == 0) return true;
        return System.currentTimeMillis() < shopExpireTime;
    }

    public String getShopExpireDisplay() {
        if (shopExpireTime == 0) return "永久";
        long remaining = shopExpireTime - System.currentTimeMillis();
        if (remaining <= 0) return "已下架";

        long days = remaining / (1000 * 60 * 60 * 24);
        long hours = (remaining % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);

        if (days > 0) {
            return days + "天" + hours + "小时后下架";
        } else {
            return hours + "小时后下架";
        }
    }

    // 检查是否使用自定义经济系统
    public boolean usesCustomEconomy() {
        return isCustom && customEconomy != null && !customEconomy.trim().isEmpty() && customEconomyCost > 0;
    }

    // 获取总成本显示
    public String getCostDisplay() {
        if (usesCustomEconomy()) {
            return customEconomyCost + " " + customEconomy;
        } else {
            // 返回原有的经济系统显示
            if (price > 0) return price + " 货币";
            if (points > 0) return points + " 点数";
            if (coins > 0) return coins + " 金币";
            return "免费";
        }
    }

    // 其他方法保持不变...
    public boolean isPermanent() {
        return duration == 0;
    }

    public String getDurationDisplay() {
        if (duration == 0) {
            return "永久";
        } else if (duration < 30) {
            return duration + "天";
        } else if (duration < 365) {
            return (duration / 30) + "个月";
        } else {
            return (duration / 365) + "年";
        }
    }

    public boolean hasBuffs() {
        return !buffs.isEmpty();
    }

    public boolean hasParticleEffects() {
        return !particleEffects.isEmpty();
    }

    public void setShowInShop(boolean showInShop) {
        this.showInShop = showInShop;
    }

    public void setShopExpireTime(long shopExpireTime) {
        this.shopExpireTime = shopExpireTime;
    }

    public String getFormattedContent() {
        if (content == null) return "";
        String formatted = content;

        // 转换所有颜色代码格式
        formatted = formatted.replace("&0", "§0")
                .replace("&1", "§1")
                .replace("&2", "§2")
                .replace("&3", "§3")
                .replace("&4", "§4")
                .replace("&5", "§5")
                .replace("&6", "§6")
                .replace("&7", "§7")
                .replace("&8", "§8")
                .replace("&9", "§9")
                .replace("&a", "§a")
                .replace("&b", "§b")
                .replace("&c", "§c")
                .replace("&d", "§d")
                .replace("&e", "§e")
                .replace("&f", "§f")
                .replace("&k", "§k")
                .replace("&l", "§l")
                .replace("&m", "§m")
                .replace("&n", "§n")
                .replace("&o", "§o")
                .replace("&r", "§r");

        // 移除可能存在的 <> 符号
        formatted = formatted.replace("<", "").replace(">", "");

        return formatted;
    }
}