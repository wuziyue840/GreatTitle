package xyz.tolite.greatTitle.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.model.Buff;
import xyz.tolite.greatTitle.model.Title;

import java.util.*;

public class BuffManager {

    private final GreatTitle plugin;
    private final Map<UUID, Map<String, Buff>> activeBuffs;

    public BuffManager(GreatTitle plugin) {
        this.plugin = plugin;
        this.activeBuffs = new HashMap<>();
    }

    /**
     * 应用BUFF到玩家
     */
    public void applyBuffs(Player player, List<Buff> buffs) {
        UUID uuid = player.getUniqueId();
        Map<String, Buff> playerBuffs = activeBuffs.computeIfAbsent(uuid, k -> new HashMap<>());

        for (Buff buff : buffs) {
            if (buff.isExpired()) {
                continue;
            }

            PotionEffectType effectType = PotionEffectType.getByName(buff.getEffectType());
            if (effectType != null) {
                // 关键修复：正确处理持续时间
                int durationInTicks;
                if (buff.getDuration() == 0) {
                    // 永久效果，使用最大值（约24天）
                    durationInTicks = Integer.MAX_VALUE;
                } else {
                    // 转换为ticks（1秒 = 20 ticks）
                    durationInTicks = buff.getDuration() * 20;
                }

                PotionEffect effect = new PotionEffect(
                        effectType,
                        durationInTicks,
                        buff.getAmplifier(),
                        buff.isAmbient(),
                        buff.showParticles(),
                        buff.showIcon()
                );

                // 强制应用效果（覆盖现有效果）
                player.addPotionEffect(effect, true);
                playerBuffs.put(buff.getEffectType() + "_" + buff.getAmplifier(), buff);

                plugin.getLogger().info("为玩家 " + player.getName() + " 应用BUFF: " +
                        effectType.getName() + " " + (buff.getAmplifier() + 1));
            } else {
                plugin.getLogger().warning("无效的效果类型: " + buff.getEffectType());
            }
        }
    }

    /**
     * 移除玩家的BUFF
     */
    public void removeBuffs(Player player, List<Buff> buffs) {
        UUID uuid = player.getUniqueId();
        Map<String, Buff> playerBuffs = activeBuffs.get(uuid);

        if (playerBuffs == null) return;

        for (Buff buff : buffs) {
            PotionEffectType effectType = PotionEffectType.getByName(buff.getEffectType());
            if (effectType != null) {
                player.removePotionEffect(effectType);
                playerBuffs.remove(buff.getEffectType() + "_" + buff.getAmplifier());
                plugin.getLogger().info("移除玩家 " + player.getName() + " 的BUFF: " + effectType.getName());
            }
        }

        if (playerBuffs.isEmpty()) {
            activeBuffs.remove(uuid);
        }
    }

    /**
     * 移除玩家的所有BUFF
     */
    public void removeAllBuffs(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, Buff> playerBuffs = activeBuffs.get(uuid);

        if (playerBuffs != null) {
            for (Buff buff : playerBuffs.values()) {
                PotionEffectType effectType = PotionEffectType.getByName(buff.getEffectType());
                if (effectType != null) {
                    player.removePotionEffect(effectType);
                }
            }
            activeBuffs.remove(uuid);
            plugin.getLogger().info("移除玩家 " + player.getName() + " 的所有BUFF");
        }
    }

    /**
     * 为在线玩家重新应用所有BUFF（服务器重启后）
     */
    public void reapplyAllBuffs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String activeTitleId = plugin.getTitleManager().getActiveTitle(player);
            if (activeTitleId != null) {
                Title title = plugin.getTitleManager().getTitle(activeTitleId);
                if (title != null && title.hasBuffs()) {
                    applyBuffs(player, title.getBuffs());
                }
            }
        }
    }

    public boolean hasBuff(Player player, String effectType) {
        Map<String, Buff> playerBuffs = activeBuffs.get(player.getUniqueId());
        return playerBuffs != null && playerBuffs.containsKey(effectType);
    }

    public List<Buff> getActiveBuffs(Player player) {
        Map<String, Buff> playerBuffs = activeBuffs.get(player.getUniqueId());
        return playerBuffs != null ? new ArrayList<>(playerBuffs.values()) : new ArrayList<>();
    }

    public void checkAndRemoveExpiredBuffs(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, Buff> playerBuffs = activeBuffs.get(uuid);

        if (playerBuffs != null) {
            Iterator<Map.Entry<String, Buff>> iterator = playerBuffs.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Buff> entry = iterator.next();
                Buff buff = entry.getValue();

                if (buff.isExpired()) {
                    PotionEffectType effectType = PotionEffectType.getByName(buff.getEffectType());
                    if (effectType != null) {
                        player.removePotionEffect(effectType);
                    }
                    iterator.remove();
                }
            }

            if (playerBuffs.isEmpty()) {
                activeBuffs.remove(uuid);
            }
        }
    }
}