package xyz.tolite.greatTitle.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.manager.TitleCardManager;
import xyz.tolite.greatTitle.manager.TitleManager;
import xyz.tolite.greatTitle.model.Buff;
import xyz.tolite.greatTitle.model.Title;
import xyz.tolite.greatTitle.model.TitleType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TitleCommand implements CommandExecutor, TabCompleter {

    private final GreatTitle plugin;
    private final TitleManager titleManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
    private static boolean debugEnabled = false;
    private TitleCardManager titleCardManager = null;

    public TitleCommand(GreatTitle plugin) {
        this.plugin = plugin;
        this.titleManager = plugin.getTitleManager();
        this.titleCardManager = titleCardManager;
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c只有玩家才能使用此命令!");
                return true;
            }
            Player player = (Player) sender;
            plugin.getTitleGUI().openTitleGUI(player, 1);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "shop":
                handleShopCommand(sender, args);
                break;

            case "use":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家才能使用此命令!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /grt use <称号ID>");
                    return true;
                }
                handleUseCommand((Player) sender, args[1]);
                break;

            case "delete":
                if (sender.hasPermission("greattitle.admin")) {
                    handleDeleteCommand(sender, args);
                } else {
                    sender.sendMessage("§c你没有权限执行此命令!");
                }
                break;

            case "rewards":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家才能使用此命令!");
                    return true;
                }
                plugin.getTitleGUI().openRewardsGUI((Player) sender, 1);
                break;

            case "remove":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家才能使用此命令!");
                    return true;
                }
                handleRemoveCommand((Player) sender);
                break;

            case "display":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家才能使用此命令!");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /grt display <head|tab|chat> <on|off>");
                    return true;
                }
                handleDisplayCommand((Player) sender, args[1], args[2]);
                break;

            case "settings":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家才能使用此命令!");
                    return true;
                }
                plugin.getTitleGUI().openSettingsGUI((Player) sender);
                break;

            case "check":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家才能使用此命令!");
                    return true;
                }
                titleManager.checkAndRemoveExpiredTitles((Player) sender);
                sender.sendMessage("§a已检查过期称号!");
                break;

            // 在 TitleCommand 的 reload 部分修改
            case "reload":
                if (sender.hasPermission("greattitle.admin")) {
                    if (args.length > 1) {
                        // 部分重载
                        switch (args[1].toLowerCase()) {
                            case "titles":
                                plugin.getReloadManager().reloadTitlesOnly(sender);
                                break;
                            case "economy":
                                plugin.getReloadManager().reloadEconomyOnly(sender);
                                break;
                            case "config":
                                plugin.getReloadManager().reloadBaseConfig(sender);
                                break;
                            default:
                                sender.sendMessage("§c用法: /grt reload [titles|economy|config]");
                                break;
                        }
                    } else {
                        // 完全重载
                        plugin.getReloadManager().reloadAllConfigs(sender);
                    }
                } else {
                    sender.sendMessage("§c你没有权限执行此命令!");
                }
                break;

            case "debug":
                if (sender.hasPermission("greattitle.admin")) {
                    if (args.length < 2) {
                        boolean currentState = plugin.getConfig().getBoolean("debug-enabled", false);
                        sender.sendMessage("§6当前调试状态: " + (currentState ? "§a开启" : "§c关闭"));
                        sender.sendMessage("§e用法: /grt debug <on/off/toggle>");
                        return true;
                    }

                    String debugAction = args[1].toLowerCase();
                    boolean newState;

                    switch (debugAction) {
                        case "on":
                            newState = true;
                            break;
                        case "off":
                            newState = false;
                            break;
                        case "toggle":
                            newState = !plugin.getConfig().getBoolean("debug-enabled", false);
                            break;
                        default:
                            sender.sendMessage("§c无效的参数! 使用: /grt debug <on/off/toggle>");
                            return true;
                    }

                    // 保存到配置
                    plugin.getConfig().set("debug-enabled", newState);
                    plugin.saveConfig();

                    // 同步到所有管理器
                    if (plugin.getHeadDisplayManager() != null) {
                        plugin.getHeadDisplayManager().setDebugEnabled(newState);
                    }

                    sender.sendMessage("§a调试模式已" + (newState ? "§a开启" : "§c关闭") + "§a!");
                    plugin.getLogger().info("调试模式已" + (newState ? "开启" : "关闭"));

                } else {
                    sender.sendMessage("§c你没有权限执行此命令!");
                }
                break;

            case "effects":
                if (sender.hasPermission("greattitle.admin")) {
                    Player player = (Player) sender;

                    String activeTitleId = titleManager.getActiveTitle(player);
                    Title title = activeTitleId != null ? titleManager.getTitle(activeTitleId) : null;

                    sender.sendMessage("§6=== 效果状态 ===");
                    sender.sendMessage("§e当前称号: " + (title != null ? title.getDisplayName() : "无"));

                    if (title != null) {
                        // BUFF信息
                        if (title.hasBuffs()) {
                            sender.sendMessage("§aBUFF效果:");
                            for (Buff buff : title.getBuffs()) {
                                sender.sendMessage("§7- " + buff.getEffectType() + " " + (buff.getAmplifier() + 1));
                            }
                        } else {
                            sender.sendMessage("§c无BUFF效果");
                        }

                        // 粒子信息
                        if (title.hasParticleEffects()) {
                            sender.sendMessage("§a粒子效果:");
                            for (String particle : title.getParticleEffects()) {
                                sender.sendMessage("§7- " + particle);
                            }
                        } else {
                            sender.sendMessage("§c无粒子效果");
                        }
                    }

                    // 当前激活的效果
                    List<Buff> activeBuffs = plugin.getBuffManager().getActiveBuffs(player);
                    sender.sendMessage("§a当前激活BUFF: " + activeBuffs.size() + " 个");

                    Set<String> activeParticles = plugin.getParticleManager().getActiveParticles(player);
                    sender.sendMessage("§a当前激活粒子: " + activeParticles.size() + " 个");
                }
                break;

            case "player":
                if (sender.hasPermission("greattitle.admin")) {
                    handlePlayerCommand(sender, args);
                } else {
                    sender.sendMessage("§c你没有权限执行此命令!");
                }
                break;

            case "title":
                if (sender.hasPermission("greattitle.admin")) {
                    handleTitleCommand(sender, args);
                } else {
                    sender.sendMessage("§c你没有权限执行此命令!");
                }
                break;

            case "buff":
                if (sender.hasPermission("greattitle.admin")) {
                    handleBuffCommand(sender, args);
                } else {
                    sender.sendMessage("§c你没有权限执行此命令!");
                }
                break;

            case "particle":
                if (sender.hasPermission("greattitle.admin")) {
                    handleParticleCommand(sender, args);
                } else {
                    sender.sendMessage("§c你没有权限执行此命令!");
                }
                break;

            case "clear":
                if (sender.hasPermission("greattitle.admin")) {
                    handleClearCommand(sender, args);
                } else {
                    sender.sendMessage("§c你没有权限执行此命令!");
                }
                break;

            case "convert":
                if (sender.hasPermission("greattitle.admin")) {
                    handleConvertCommand(sender, args);
                } else {
                    sender.sendMessage("§c你没有权限执行此命令!");
                }
                break;

            case "view":
                if (sender.hasPermission("greattitle.admin")) {
                    handleViewCommand(sender, args);
                } else {
                    sender.sendMessage("§c你没有权限执行此命令!");
                }
                break;

            default:
                if (sender instanceof Player) {
                    sendHelp((Player) sender);
                } else {
                    sendAdminHelp(sender);
                }
                break;
        }

        return true;
    }

    /**
     * 处理玩家相关命令
     */
    private void handlePlayerCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /grt player <give/givecard> <玩家> <参数...>");
            return;
        }

        String subCommand = args[1].toLowerCase();
        Player target = Bukkit.getPlayer(args[2]);

        if (target == null) {
            sender.sendMessage("§c玩家不存在或不在线!");
            return;
        }

        switch (subCommand) {
            case "give":
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /grt player give <玩家> <称号ID> [持续时间(天)]");
                    return;
                }
                String titleId = args[3];
                int duration = 0;

                if (args.length >= 5) {
                    try {
                        duration = Integer.parseInt(args[4]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§c持续时间必须是数字!");
                        return;
                    }
                }

                if (titleManager.giveTitle(target, titleId, duration)) {
                    sender.sendMessage("§a已给予玩家 " + target.getName() + " 称号: " + titleId);
                    target.sendMessage("§a你获得了新称号!");
                } else {
                    sender.sendMessage("§c给予称号失败! 请检查称号ID是否正确。");
                }
                break;

            case "givecard":
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /grt player givecard <玩家> <random|fixed|称号ID> [更多称号ID...]");
                    return;
                }

                String cardType = args[3].toLowerCase();
                switch (cardType) {
                    case "random":
                        giveRandomCard(target);
                        sender.sendMessage("§a已给予 " + target.getName() + " 随机称号卡");
                        break;
                    case "fixed":
                        giveFixedCard(target);
                        sender.sendMessage("§a已给予 " + target.getName() + " 固定称号卡");
                        break;
                    default:
                        // 处理自定义称号卡
                        List<String> titleIds = new ArrayList<>();
                        for (int i = 3; i < args.length; i++) {
                            String customTitleId = args[i];
                            if (titleManager.getTitle(customTitleId) == null) {
                                sender.sendMessage("§c称号不存在: " + customTitleId);
                                return;
                            }
                            titleIds.add(customTitleId);
                        }
                        if (titleIds.isEmpty()) {
                            sender.sendMessage("§c请指定至少一个称号ID!");
                            return;
                        }
                        ItemStack card = createCustomCard(titleIds);
                        giveItemToPlayer(target, card, "自定义称号卡");
                        if (titleIds.size() == 1) {
                            sender.sendMessage("§a已给予 " + target.getName() + " 称号卡: " + titleIds.get(0));
                        } else {
                            sender.sendMessage("§a已给予 " + target.getName() + " 多称号卡 (" + titleIds.size() + "个称号)");
                        }
                        break;
                }
                break;

            default:
                sender.sendMessage("§c无效的子命令! 可用: give, givecard");
                break;
        }
    }

    /**
     * 处理称号管理命令
     */
    private void handleTitleCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /grt title <add/delete> [参数...]");
            return;
        }

        String subCommand = args[1].toLowerCase();
        switch (subCommand) {
            case "add":
                handleTitleAdd(sender, args);
                break;
            case "delete":
            case "del":
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /grt title delete <称号ID>");
                    return;
                }
                String titleId = args[2];
                if (titleManager.deleteTitle(titleId)) {
                    sender.sendMessage("§a已删除称号: " + titleId);
                } else {
                    sender.sendMessage("§c删除称号失败! 请检查称号ID是否正确。");
                }
                break;
            default:
                sender.sendMessage("§c无效的子命令! 可用: add, delete");
                break;
        }
    }

    /**
     * 处理添加称号命令
     */
    private void handleTitleAdd(CommandSender sender, String[] args) {
        if (args.length < 9) {
            sender.sendMessage("§c用法: /grt title add <ID> <显示名称> <内容> <类型> <价格> <点券> <天数>");
            sender.sendMessage("§7类型: NOT, VAULT, PLAYER_POINTS, COIN, ITEM_STACK, PERMISSION, ACTIVITY");
            sender.sendMessage("§7注意: 使用 - 表示不需要该字段");
            sender.sendMessage("§7示例: /grt title add vip1 VIP称号 &6VIP玩家 VAULT 1000 500 30");
            return;
        }

        try {
            String id = args[2];

            // 检查ID是否已存在
            if (titleManager.getTitle(id) != null) {
                sender.sendMessage("§c称号ID已存在: " + id);
                return;
            }

            String displayName = args[3].replace("&", "§");
            String content = args[4].replace("&", "§");

            // 验证类型
            TitleType type;
            try {
                type = TitleType.valueOf(args[5].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§c无效的类型! 可用类型: NOT, VAULT, PLAYER_POINTS, COIN, ITEM_STACK, PERMISSION, ACTIVITY");
                return;
            }

            // 安全解析价格
            double price = 0;
            if (!args[6].equals("-")) {
                try {
                    price = Double.parseDouble(args[6]);
                    if (price < 0) {
                        sender.sendMessage("§c价格不能为负数!");
                        return;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c价格必须是有效的数字!");
                    return;
                }
            }

            // 安全解析点券
            int points = 0;
            if (!args[7].equals("-")) {
                try {
                    points = Integer.parseInt(args[7]);
                    if (points < 0) {
                        sender.sendMessage("§c点券不能为负数!");
                        return;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c点券必须是有效的整数!");
                    return;
                }
            }

            // 安全解析天数
            int duration = 0;
            if (!args[8].equals("-")) {
                try {
                    duration = Integer.parseInt(args[8]);
                    if (duration < 0) {
                        sender.sendMessage("§c天数不能为负数!");
                        return;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c天数必须是有效的整数!");
                    return;
                }
            }

            // 创建基础称号配置
            Map<String, Object> titleConfig = new HashMap<>();
            titleConfig.put("display-name", displayName);
            titleConfig.put("content", content);
            titleConfig.put("type", type.toString());

            if (price > 0) titleConfig.put("price", price);
            if (points > 0) titleConfig.put("points", points);
            if (duration > 0) titleConfig.put("duration", duration);

            // 设置默认值
            titleConfig.put("show-in-shop", true);
            titleConfig.put("rgb", false);

            // 保存到配置文件
            plugin.getConfigManager().getTitlesConfig().set(id, titleConfig);
            plugin.getConfigManager().saveTitlesConfig();

            // 重新加载称号
            titleManager.loadTitles();

            sender.sendMessage("§a成功创建称号: " + displayName);
            sender.sendMessage("§7ID: " + id);
            sender.sendMessage("§7类型: " + type);
            if (price > 0) sender.sendMessage("§7价格: " + price + " 金币");
            if (points > 0) sender.sendMessage("§7点券: " + points);
            if (duration > 0) sender.sendMessage("§7天数: " + duration);
            sender.sendMessage("§7使用 /grt buff 和 /grt particle 命令配置特效");

        } catch (Exception e) {
            sender.sendMessage("§c创建称号时发生错误: " + e.getMessage());
            plugin.getLogger().warning("创建称号时发生错误: " + e.getMessage());
        }
    }

    /**
     * 处理Buff配置命令
     */
    private void handleBuffCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c用法: /grt buff <addBuff/deleteBuff> <称号ID> <配置>");
            sender.sendMessage("§7配置格式: <效果类型>:<等级>:<持续时间>");
            sender.sendMessage("§7示例: /grt buff addBuff vip1 SPEED:1:3600");
            sender.sendMessage("§7效果类型: SPEED, JUMP, STRENGTH, REGENERATION 等");
            return;
        }

        String action = args[1].toLowerCase();
        String titleId = args[2];
        String config = args[3];

        Title title = titleManager.getTitle(titleId);
        if (title == null) {
            sender.sendMessage("§c称号不存在: " + titleId);
            return;
        }

        switch (action) {
            case "addbuff":
                // 添加Buff配置
                List<String> buffs = plugin.getConfigManager().getTitlesConfig().getStringList(titleId + ".buffs");
                buffs.add(config);
                plugin.getConfigManager().getTitlesConfig().set(titleId + ".buffs", buffs);
                plugin.getConfigManager().saveTitlesConfig();
                titleManager.loadTitles();
                sender.sendMessage("§a已为称号 " + title.getDisplayName() + " 添加Buff: " + config);
                break;

            case "deletebuff":
                // 删除Buff配置
                List<String> currentBuffs = plugin.getConfigManager().getTitlesConfig().getStringList(titleId + ".buffs");
                if (currentBuffs.remove(config)) {
                    plugin.getConfigManager().getTitlesConfig().set(titleId + ".buffs", currentBuffs);
                    plugin.getConfigManager().saveTitlesConfig();
                    titleManager.loadTitles();
                    sender.sendMessage("§a已从称号 " + title.getDisplayName() + " 删除Buff: " + config);
                } else {
                    sender.sendMessage("§c未找到匹配的Buff配置: " + config);
                }
                break;

            default:
                sender.sendMessage("§c无效的操作! 可用: addBuff, deleteBuff");
                break;
        }
    }

    /**
     * 处理粒子配置命令
     */
    private void handleParticleCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c用法: /grt particle <addParticle/deleteParticle> <称号ID> <配置>");
            sender.sendMessage("§7配置格式: <粒子类型>:<数量>:<范围>");
            sender.sendMessage("§7示例: /grt particle addParticle vip1 FLAME:5:1");
            sender.sendMessage("§7粒子类型: FLAME, HEART, CLOUD, REDSTONE 等");
            return;
        }

        String action = args[1].toLowerCase();
        String titleId = args[2];
        String config = args[3];

        Title title = titleManager.getTitle(titleId);
        if (title == null) {
            sender.sendMessage("§c称号不存在: " + titleId);
            return;
        }

        switch (action) {
            case "addparticle":
                // 添加粒子配置
                List<String> particles = plugin.getConfigManager().getTitlesConfig().getStringList(titleId + ".particle-effects");
                particles.add(config);
                plugin.getConfigManager().getTitlesConfig().set(titleId + ".particle-effects", particles);
                plugin.getConfigManager().saveTitlesConfig();
                titleManager.loadTitles();
                sender.sendMessage("§a已为称号 " + title.getDisplayName() + " 添加粒子: " + config);
                break;

            case "deleteparticle":
                // 删除粒子配置
                List<String> currentParticles = plugin.getConfigManager().getTitlesConfig().getStringList(titleId + ".particle-effects");
                if (currentParticles.remove(config)) {
                    plugin.getConfigManager().getTitlesConfig().set(titleId + ".particle-effects", currentParticles);
                    plugin.getConfigManager().saveTitlesConfig();
                    titleManager.loadTitles();
                    sender.sendMessage("§a已从称号 " + title.getDisplayName() + " 删除粒子: " + config);
                } else {
                    sender.sendMessage("§c未找到匹配的粒子配置: " + config);
                }
                break;

            default:
                sender.sendMessage("§c无效的操作! 可用: addParticle, deleteParticle");
                break;
        }
    }

    /**
     * 处理后台管理命令
     */
    private void handleViewCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /grt view <商店界面/奖励界面>");
            sender.sendMessage("§c用法: /grt view open <玩家名>");
            return;
        }

        if (args[1].equalsIgnoreCase("open")) {
            if (args.length < 3) {
                sender.sendMessage("§c用法: /grt view open <玩家名>");
                return;
            }

            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§c玩家不存在或不在线!");
                return;
            }

            if (sender instanceof Player) {
                plugin.getTitleGUI().openPlayerTitleManagement((Player) sender, target, 1);
            } else {
                sender.sendMessage("§c只有玩家才能打开GUI界面!");
            }
        } else {
            String viewType = args[1].toLowerCase();
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c只有玩家才能打开GUI界面!");
                return;
            }

            Player player = (Player) sender;
            switch (viewType) {
                case "商店界面":
                case "shop":
                    plugin.getTitleGUI().openShopManagement(player, 1);
                    break;
                case "奖励界面":
                case "reward":
                    plugin.getTitleGUI().openRewardManagement(player, 1);
                    break;
                default:
                    sender.sendMessage("§c无效的界面类型! 可用: 商店界面, 奖励界面");
                    break;
            }
        }
    }

    /**
     * 处理数据库转换命令
     */
    private void handleConvertCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /grt convert <mysql|sqlite>");
            sender.sendMessage("§7注意: 转换过程可能需要一些时间，请确保服务器已备份!");
            return;
        }

        String targetType = args[1].toLowerCase();
        if (!targetType.equals("mysql") && !targetType.equals("sqlite")) {
            sender.sendMessage("§c目标数据库类型必须是 mysql 或 sqlite!");
            return;
        }

        sender.sendMessage("§6开始转换数据库...");
        sender.sendMessage("§6当前数据库: " + plugin.getConfigManager().getDatabaseType());
        sender.sendMessage("§6目标数据库: " + targetType);
        sender.sendMessage("§e转换过程中请勿关闭服务器...");

        // 异步执行转换操作
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getDatabaseManager().convertDatabase(targetType, sender);

            if (success) {
                // 重新为在线玩家加载数据
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        plugin.getTitleManager().loadPlayerData(player);
                    });
                    sender.sendMessage("§a数据库转换完成! 所有在线玩家数据已重新加载。");
                });
            }
        });
    }

    private void handleShopCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 打开商店界面
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c只有玩家才能使用此命令!");
                return;
            }
            plugin.getTitleGUI().openTitleShop((Player) sender, 1);
        } else {
            // 商店管理命令
            if (!sender.hasPermission("greattitle.admin")) {
                sender.sendMessage("§c你没有权限执行此命令!");
                return;
            }

            if (args.length < 3) {
                sender.sendMessage("§c用法: /grt shop <add|remove> <称号ID> [时间]");
                return;
            }

            String action = args[1].toLowerCase();
            String titleId = args[2];

            Title title = titleManager.getTitle(titleId);
            if (title == null) {
                sender.sendMessage("§c称号不存在: " + titleId);
                return;
            }

            switch (action) {
                case "add":
                    if (args.length < 4) {
                        sender.sendMessage("§c用法: /grt shop add <称号ID> <时间>");
                        sender.sendMessage("§7时间格式: yyyy-MM-dd-HH-mm 或 天数d 或 0(永久)");
                        return;
                    }
                    handleShopAdd(sender, title, args[3]);
                    break;
                case "remove":
                    handleShopRemove(sender, title);
                    break;
                default:
                    sender.sendMessage("§c无效的操作! 可用: add, remove");
                    break;
            }
        }
    }

    private void handleShopAdd(CommandSender sender, Title title, String timeStr) {
        long expireTime = 0;

        if (timeStr.equalsIgnoreCase("0")) {
            // 永久上架
            expireTime = 0;
        } else if (timeStr.endsWith("d")) {
            // 按天数计算
            try {
                int days = Integer.parseInt(timeStr.substring(0, timeStr.length() - 1));
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, days);
                expireTime = calendar.getTimeInMillis();
            } catch (NumberFormatException e) {
                sender.sendMessage("§c无效的天数格式! 示例: 7d 表示7天");
                return;
            }
        } else {
            // 按具体时间计算
            try {
                Date date = dateFormat.parse(timeStr);
                expireTime = date.getTime();

                if (expireTime <= System.currentTimeMillis()) {
                    sender.sendMessage("§c上架时间必须大于当前时间!");
                    return;
                }
            } catch (ParseException e) {
                sender.sendMessage("§c无效的时间格式! 正确格式: yyyy-MM-dd-HH-mm");
                sender.sendMessage("§7示例: 2024-12-31-23-59");
                return;
            }
        }

        // 更新称号的上架状态
        updateTitleShopStatus(title, true, expireTime);

        if (expireTime == 0) {
            sender.sendMessage("§a已永久上架称号: " + title.getDisplayName());
        } else {
            String expireStr = dateFormat.format(new Date(expireTime));
            sender.sendMessage("§a已上架称号: " + title.getDisplayName() + " §7(下架时间: " + expireStr + ")");
        }
    }

    private void handleShopRemove(CommandSender sender, Title title) {
        updateTitleShopStatus(title, false, 0);
        sender.sendMessage("§a已下架称号: " + title.getDisplayName());
    }

    private void updateTitleShopStatus(Title title, boolean showInShop, long expireTime) {
        try {
            // 更新内存中的称号状态
            title.setShowInShop(showInShop);
            title.setShopExpireTime(expireTime);

            // 保存到配置文件
            plugin.getConfigManager().getTitlesConfig().set(title.getId() + ".show-in-shop", showInShop);
            if (expireTime > 0) {
                String expireStr = dateFormat.format(new Date(expireTime));
                plugin.getConfigManager().getTitlesConfig().set(title.getId() + ".shop-expire-time", expireStr);
            } else {
                plugin.getConfigManager().getTitlesConfig().set(title.getId() + ".shop-expire-time", "");
            }

            plugin.getConfigManager().saveTitlesConfig();

        } catch (Exception e) {
            plugin.getLogger().warning("更新称号商店状态失败: " + e.getMessage());
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

    private void giveItemToPlayer(Player player, ItemStack item, String itemName) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
            player.sendMessage("§a你获得了一张" + itemName + "! 右键点击使用。");
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage("§a你获得了一张" + itemName + "! 已掉落在地面上。");
        }
    }

    // 处理删除称号命令
    private void handleDeleteCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /grt del <称号ID>");
            return;
        }

        String titleId = args[1];
        if (titleManager.deleteTitle(titleId)) {
            sender.sendMessage("§a已删除称号: " + titleId);
        } else {
            sender.sendMessage("§c删除称号失败! 请检查称号ID是否正确。");
        }
    }

    private void handleUseCommand(Player player, String titleId) {
        if (titleManager.setActiveTitle(player, titleId)) {
            player.sendMessage("§a已设置称号: " + titleId);
        } else {
            player.sendMessage("§c设置称号失败! 请检查你是否拥有该称号或称号是否过期。");
        }
    }

    private void handleRemoveCommand(Player player) {
        if (titleManager.setActiveTitle(player, null)) {
            player.sendMessage("§a已移除当前称号");
        }
    }

    private void handleDisplayCommand(Player player, String type, String value) {
        boolean display = value.equalsIgnoreCase("on");

        switch (type.toLowerCase()) {
            case "head":
                titleManager.setHeadDisplay(player, display);
                player.sendMessage("§a已" + (display ? "开启" : "关闭") + "头顶称号显示");
                break;
            case "tab":
                titleManager.setTabListDisplay(player, display);
                player.sendMessage("§a已" + (display ? "开启" : "关闭") + "Tab列表称号显示");
                break;
            case "chat":
                titleManager.setChatDisplay(player, display);
                player.sendMessage("§a已" + (display ? "开启" : "关闭") + "聊天称号显示");
                break;
            default:
                player.sendMessage("§c无效的显示类型! 可用类型: head, tab, chat");
                break;
        }
    }

    private void handleClearCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /grt clear <title|player|all> [玩家]");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "title":
                // 清除所有称号数据
                plugin.getDatabaseManager().clearAllTitles();
                titleManager.loadTitles(); // 重新加载
                sender.sendMessage("§a已清除所有称号数据，重置为默认配置!");
                break;

            case "player":
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /grt clear player <玩家>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§c玩家不在线或不存在!");
                    return;
                }
                plugin.getDatabaseManager().clearPlayerTitles(target.getUniqueId());
                titleManager.loadPlayerData(target);
                sender.sendMessage("§a已清除玩家 " + target.getName() + " 的所有称号数据!");
                break;

            case "all":
                plugin.getDatabaseManager().clearAllData();
                // 重新为在线玩家加载数据
                Bukkit.getOnlinePlayers().forEach(player -> {
                    titleManager.loadPlayerData(player);
                });
                sender.sendMessage("§a已清除所有插件数据!");
                break;

            default:
                sender.sendMessage("§c无效的清除类型! 可用类型: title, player, all");
                break;
        }
    }

    /**
     * 创建称号卡 - 统一方法，支持随机称号卡和自定义称号卡
     */
    private ItemStack createTitleCard(List<String> titleIds, String cardType) {
        // 获取物品类型
        Material material = Material.PAPER;
        if ("random".equals(cardType)) {
            String itemType = plugin.getConfigManager().getRandomCardItem();
            Material randomMaterial = Material.getMaterial(itemType);
            if (randomMaterial != null) {
                material = randomMaterial;
            }
        } else if ("fixed".equals(cardType)) {
            String itemType = plugin.getConfigManager().getFixedCardItem();
            Material fixedMaterial = Material.getMaterial(itemType);
            if (fixedMaterial != null) {
                material = fixedMaterial;
            }
        }

        ItemStack card = new ItemStack(material);
        ItemMeta meta = card.getItemMeta();

        if (meta != null) {
            // 设置显示名称
            if ("random".equals(cardType)) {
                meta.setDisplayName(plugin.getConfigManager().getRandomCardName());
            } else if ("fixed".equals(cardType)) {
                meta.setDisplayName(plugin.getConfigManager().getFixedCardName());
            } else {
                // 自定义称号卡
                if (titleIds.size() == 1) {
                    Title title = titleManager.getTitle(titleIds.get(0));
                    String titleName = title != null ? title.getDisplayName() : titleIds.get(0);
                    meta.setDisplayName("§6" + titleName + " §e称号卡");
                } else {
                    meta.setDisplayName("§6多称号卡 §e(" + titleIds.size() + "个称号)");
                }
            }

            // 设置Lore
            List<String> lore = new ArrayList<>();

            if ("random".equals(cardType)) {
                lore.add("§7右键点击获得随机称号");

                // 显示可能的称号池
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
                lore.add("§e类型: 随机");
            }
            else if ("fixed".equals(cardType)) {
                lore.add("§7右键点击获得固定称号");

                String fixedTitle = plugin.getConfigManager().getFixedCardTitle();
                Title title = titleManager.getTitle(fixedTitle);
                String titleName = title != null ? title.getDisplayName() : fixedTitle;
                lore.add("§e包含称号: " + titleName);
                lore.add("§e类型: 固定");
            }
            else {
                // 自定义称号卡
                lore.add("§7右键点击获得称号");

                if (titleIds.size() == 1) {
                    Title title = titleManager.getTitle(titleIds.get(0));
                    String titleName = title != null ? title.getDisplayName() : titleIds.get(0);
                    lore.add("§e包含称号: " + titleName);
                } else {
                    lore.add("§e包含 " + titleIds.size() + " 个称号:");
                    // 只显示前5个称号，避免Lore过长
                    for (int i = 0; i < Math.min(titleIds.size(), 5); i++) {
                        Title title = titleManager.getTitle(titleIds.get(i));
                        String titleName = title != null ? title.getDisplayName() : titleIds.get(i);
                        lore.add("§7- " + titleName);
                    }
                    if (titleIds.size() > 5) {
                        lore.add("§7... 还有 " + (titleIds.size() - 5) + " 个称号");
                    }
                }
                lore.add("§e类型: 自定义");
            }

            meta.setLore(lore);
            card.setItemMeta(meta);
        }

        // 对于自定义称号卡，存储称号ID到NBT
        if (!"random".equals(cardType) && !"fixed".equals(cardType)) {
            card = storeTitleIdsInNBT(card, titleIds);
        }

        return card;
    }

    /**
     * 创建随机称号卡 - 使用统一方法
     */
    private ItemStack createRandomCard() {
        // 随机称号卡不需要传递具体的titleIds，因为会从配置的池中随机选择
        return createTitleCard(new ArrayList<>(), "random");
    }

    /**
     * 创建固定称号卡 - 使用统一方法
     */
    private ItemStack createFixedCard() {
        // 固定称号卡也不需要传递具体的titleIds
        return createTitleCard(new ArrayList<>(), "fixed");
    }

    /**
     * 创建自定义称号卡 - 使用统一方法
     */
    private ItemStack createCustomCard(List<String> titleIds) {
        return createTitleCard(titleIds, "custom");
    }

    /**
     * 将称号ID列表存储到NBT
     */
    private ItemStack storeTitleIdsInNBT(ItemStack item, List<String> titleIds) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "custom_title_ids");

        // 将称号ID列表转换为字符串存储
        String titleIdsString = String.join(",", titleIds);
        data.set(key, PersistentDataType.STRING, titleIdsString);

        item.setItemMeta(meta);
        return item;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== GreatTitle 称号系统 ===");
        player.sendMessage("§e/grt §7- 打开称号仓库");
        player.sendMessage("§e/grt shop §7- 打开称号商店");
        player.sendMessage("§e/grt use <称号ID> §7- 使用指定称号");
        player.sendMessage("§e/grt remove §7- 移除当前称号");
        player.sendMessage("§e/grt display <head|tab|chat> <on|off> §7- 设置显示选项");
        player.sendMessage("§e/grt settings §7- 打开显示设置界面");
        player.sendMessage("§e/grt check §7- 检查过期称号");
        player.sendMessage("§e/grt rewards §7- 打开奖励界面");

        if (player.hasPermission("greattitle.admin")) {
            player.sendMessage("§e/grt reload §7- 重载配置文件");
            player.sendMessage("§e/grt player give <玩家> <称号ID> [时间] §7- 给予玩家称号");
            player.sendMessage("§e/grt player givecard <玩家> <random|fixed|称号ID> [更多称号ID] §7- 给予称号卡");
            player.sendMessage("§e/grt title add <ID> <显示名称> <内容> <类型> <价格> <点券> <天数> §7- 创建称号");
            player.sendMessage("§e/grt title delete <称号ID> §7- 删除称号");
            player.sendMessage("§e/grt buff <addBuff/deleteBuff> <称号ID> <配置> §7- 管理Buff");
            player.sendMessage("§e/grt particle <addParticle/deleteParticle> <称号ID> <配置> §7- 管理粒子");
            player.sendMessage("§e/grt shop add <称号ID> <时间> §7- 上架称号");
            player.sendMessage("§e/grt shop remove <称号ID> §7- 下架称号");
            player.sendMessage("§e/grt clear <title|player|all> §7- 清除数据");
            player.sendMessage("§e/grt convert <mysql|sqlite> §7- 转换数据库类型");
            player.sendMessage("§e/grt view <商店界面/奖励界面> §7- 打开后台管理");
            player.sendMessage("§e/grt view open <玩家名> §7- 查看玩家称号仓库");
        }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§6=== GreatTitle 管理员命令 ===");
        sender.sendMessage("§e/grt reload §7- 重载配置文件");
        sender.sendMessage("§e/grt player give <玩家> <称号ID> [时间] §7- 给予玩家称号");
        sender.sendMessage("§e/grt player givecard <玩家> <random|fixed|称号ID> [更多称号ID] §7- 给予称号卡");
        sender.sendMessage("§e/grt title add <ID> <显示名称> <内容> <类型> <价格> <点券> <天数> §7- 创建称号");
        sender.sendMessage("§e/grt title delete <称号ID> §7- 删除称号");
        sender.sendMessage("§e/grt buff <addBuff/deleteBuff> <称号ID> <配置> §7- 管理Buff");
        sender.sendMessage("§e/grt particle <addParticle/deleteParticle> <称号ID> <配置> §7- 管理粒子");
        sender.sendMessage("§e/grt shop add <称号ID> <时间> §7- 上架称号");
        sender.sendMessage("§e/grt shop remove <称号ID> §7- 下架称号");
        sender.sendMessage("§e/grt clear <title|player|all> §7- 清除数据");
        sender.sendMessage("§e/grt convert <mysql|sqlite> §7- 转换数据库类型");
        sender.sendMessage("§e/grt view <商店界面/奖励界面> §7- 打开后台管理");
        sender.sendMessage("§e/grt view open <玩家名> §7- 查看玩家称号仓库");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("shop", "use", "remove", "display", "settings", "check", "reload",
                    "player", "title", "buff", "particle", "clear", "del", "delete", "rewards", "convert", "view"));

            // 过滤权限
            completions.removeIf(cmd ->
                    (cmd.equals("player") || cmd.equals("title") || cmd.equals("buff") || cmd.equals("particle") ||
                            cmd.equals("clear") || cmd.equals("del") || cmd.equals("delete") || cmd.equals("convert") ||
                            cmd.equals("view")) && !sender.hasPermission("greattitle.admin")
            );
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "use":
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        completions.addAll(titleManager.getPlayerTitles(player).keySet());
                    }
                    break;
                case "display":
                    completions.addAll(Arrays.asList("head", "tab", "chat"));
                    break;
                case "player":
                    if (sender.hasPermission("greattitle.admin")) {
                        completions.addAll(Arrays.asList("give", "givecard"));
                    }
                    break;
                case "title":
                    if (sender.hasPermission("greattitle.admin")) {
                        completions.addAll(Arrays.asList("add", "delete", "del"));
                    }
                    break;
                case "buff":
                    if (sender.hasPermission("greattitle.admin")) {
                        completions.addAll(Arrays.asList("addBuff", "deleteBuff"));
                    }
                    break;
                case "particle":
                    if (sender.hasPermission("greattitle.admin")) {
                        completions.addAll(Arrays.asList("addParticle", "deleteParticle"));
                    }
                    break;
                case "clear":
                    if (sender.hasPermission("greattitle.admin")) {
                        completions.addAll(Arrays.asList("title", "player", "all"));
                    }
                    break;
                case "del":
                case "delete":
                    if (sender.hasPermission("greattitle.admin")) {
                        titleManager.getAllTitles().forEach(title -> completions.add(title.getId()));
                    }
                    break;
                case "shop":
                    if (sender.hasPermission("greattitle.admin")) {
                        completions.addAll(Arrays.asList("add", "remove"));
                    }
                    break;
                case "convert":
                    if (sender.hasPermission("greattitle.admin")) {
                        completions.addAll(Arrays.asList("mysql", "sqlite"));
                    }
                    break;
                case "view":
                    if (sender.hasPermission("greattitle.admin")) {
                        completions.addAll(Arrays.asList("商店界面", "奖励界面", "open"));
                    }
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "display":
                    completions.addAll(Arrays.asList("on", "off"));
                    break;
                case "player":
                    if (sender.hasPermission("greattitle.admin")) {
                        if (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("givecard")) {
                            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                        }
                    }
                    break;
                case "title":
                    if (sender.hasPermission("greattitle.admin")) {
                        if (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("del")) {
                            titleManager.getAllTitles().forEach(title -> completions.add(title.getId()));
                        } else if (args[1].equalsIgnoreCase("add")) {
                            // 可以添加一些常用的称号类型提示
                            completions.addAll(Arrays.asList("vip", "mvp", "legend", "hero"));
                        }
                    }
                    break;
                case "buff":
                case "particle":
                    if (sender.hasPermission("greattitle.admin")) {
                        titleManager.getAllTitles().forEach(title -> completions.add(title.getId()));
                    }
                    break;
                case "clear":
                    if (sender.hasPermission("greattitle.admin") && args[1].equalsIgnoreCase("player")) {
                        Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                    }
                    break;
                case "shop":
                    if (sender.hasPermission("greattitle.admin")) {
                        if (args[1].equalsIgnoreCase("add")) {
                            titleManager.getAllTitles().forEach(title -> completions.add(title.getId()));
                        } else if (args[1].equalsIgnoreCase("remove")) {
                            for (Title title : titleManager.getAllTitles()) {
                                if (title.isShowInShop()) {
                                    completions.add(title.getId());
                                }
                            }
                        }
                    }
                    break;
                case "view":
                    if (sender.hasPermission("greattitle.admin") && args[1].equalsIgnoreCase("open")) {
                        Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                    }
                    break;
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "player":
                    if (sender.hasPermission("greattitle.admin")) {
                        if (args[1].equalsIgnoreCase("give")) {
                            titleManager.getAllTitles().forEach(title -> completions.add(title.getId()));
                        } else if (args[1].equalsIgnoreCase("givecard")) {
                            completions.addAll(Arrays.asList("random", "fixed"));
                            titleManager.getAllTitles().forEach(title -> completions.add(title.getId()));
                        }
                    }
                    break;
                case "title":
                    if (sender.hasPermission("greattitle.admin") && args[1].equalsIgnoreCase("add")) {
                        // 添加类型补全
                        for (TitleType type : TitleType.values()) {
                            completions.add(type.toString());
                        }
                    }
                    break;
                case "buff":
                    if (sender.hasPermission("greattitle.admin")) {
                        // 添加Buff类型补全
                        completions.addAll(Arrays.asList("SPEED:1:3600", "JUMP:1:3600", "STRENGTH:1:3600", "REGENERATION:1:3600"));
                    }
                    break;
                case "particle":
                    if (sender.hasPermission("greattitle.admin")) {
                        // 添加粒子类型补全
                        completions.addAll(Arrays.asList("FLAME:5:1", "HEART:3:1", "CLOUD:10:2", "REDSTONE:5:1"));
                    }
                    break;
                case "shop":
                    if (sender.hasPermission("greattitle.admin") && args[1].equalsIgnoreCase("add")) {
                        completions.add("0");
                        completions.add("7d");
                        completions.add("30d");
                        // 添加一些示例时间
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DAY_OF_YEAR, 7);
                        completions.add(dateFormat.format(cal.getTime()));
                        cal.add(Calendar.DAY_OF_YEAR, 23); // 总共30天
                        completions.add(dateFormat.format(cal.getTime()));
                    }
                    break;
            }
        } else if (args.length >= 5) {
            switch (args[0].toLowerCase()) {
                case "player":
                    if (sender.hasPermission("greattitle.admin") && args[1].equalsIgnoreCase("givecard")) {
                        // 从第四个参数开始可以补全其他称号ID
                        titleManager.getAllTitles().forEach(title -> completions.add(title.getId()));
                    }
                    break;
            }
        }

        return completions;
    }
}