package xyz.tolite.greatTitle.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.model.Title;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

public class ParticleManager {

    private final GreatTitle plugin;
    private final Map<UUID, Set<String>> activeParticles;
    private final Map<UUID, BukkitRunnable> particleTasks;
    private boolean superTrailsEnabled = false;
    private boolean superTrailsProEnabled = false;
    private boolean playerParticlesEnabled = false;
    private boolean nativeParticlesEnabled = true; // 总是启用原生粒子

    // 粒子效果映射表
    private final Map<String, String> particleEffectMapping = new HashMap<>();

    public ParticleManager(GreatTitle plugin) {
        this.plugin = plugin;
        this.activeParticles = new HashMap<>();
        this.particleTasks = new HashMap<>();
        initializeParticleMapping();
        checkParticlePlugins();
    }

    private void initializeParticleMapping() {
        // 基础粒子效果映射
        particleEffectMapping.put("flame", "FLAME");
        particleEffectMapping.put("heart", "HEART");
        particleEffectMapping.put("smoke", "SMOKE_NORMAL");
        particleEffectMapping.put("crit", "CRIT");
        particleEffectMapping.put("magic_crit", "CRIT_MAGIC");
        particleEffectMapping.put("cloud", "CLOUD");
        particleEffectMapping.put("reddust", "REDSTONE");
        particleEffectMapping.put("spell", "SPELL");
        particleEffectMapping.put("portal", "PORTAL");
        particleEffectMapping.put("water", "WATER_DROP");
        particleEffectMapping.put("lava", "LAVA");
        particleEffectMapping.put("spark", "FIREWORKS_SPARK");
        particleEffectMapping.put("snow", "SNOWBALL");
        particleEffectMapping.put("slime", "SLIME");
        particleEffectMapping.put("enchant", "ENCHANTMENT_TABLE");
        particleEffectMapping.put("music", "NOTE");
        particleEffectMapping.put("villager", "VILLAGER_HAPPY");
        particleEffectMapping.put("angry_villager", "VILLAGER_ANGRY");
        particleEffectMapping.put("witch", "SPELL_WITCH");
        particleEffectMapping.put("ender", "PORTAL");
        particleEffectMapping.put("dragon_breath", "DRAGON_BREATH");
        particleEffectMapping.put("firework", "FIREWORKS_SPARK");
        particleEffectMapping.put("happy_villager", "VILLAGER_HAPPY");

        // 现代粒子（1.13+）
        particleEffectMapping.put("soul", "SOUL_FIRE_FLAME");
        particleEffectMapping.put("soul_fire", "SOUL_FIRE_FLAME");
        particleEffectMapping.put("warped", "WARPED_SPORE");
        particleEffectMapping.put("crimson", "CRIMSON_SPORE");
        particleEffectMapping.put("glow", "GLOW");
        particleEffectMapping.put("glow_squid", "GLOW_SQUID_INK");
        particleEffectMapping.put("electric", "ELECTRIC_SPARK");
        particleEffectMapping.put("sculk", "SCULK_SOUL");
    }

    private void checkParticlePlugins() {
        // 检查支持的粒子插件
        if (Bukkit.getPluginManager().getPlugin("SuperTrails") != null) {
            superTrailsEnabled = true;
            plugin.getLogger().info("SuperTrails 支持已启用");
        }

        if (Bukkit.getPluginManager().getPlugin("SuperTrailsPro") != null) {
            superTrailsProEnabled = true;
            plugin.getLogger().info("SuperTrailsPro 支持已启用");
        }

        if (Bukkit.getPluginManager().getPlugin("PlayerParticles") != null) {
            playerParticlesEnabled = true;
            plugin.getLogger().info("PlayerParticles 支持已启用");
        }

        plugin.getLogger().info("原生粒子系统已启用");
    }

    /**
     * 应用粒子效果到玩家
     */
    public boolean applyParticles(Player player, List<String> particleEffects) {
        UUID uuid = player.getUniqueId();
        Set<String> playerParticles = activeParticles.computeIfAbsent(uuid, k -> new HashSet<>());

        // 先停止现有的粒子任务
        stopParticleTask(player);

        boolean success = true;
        for (String particle : particleEffects) {
            if (applyParticleEffect(player, particle)) {
                playerParticles.add(particle);
                plugin.getLogger().info("为玩家 " + player.getName() + " 应用粒子效果: " + particle);
            } else {
                success = false;
                plugin.getLogger().warning("为玩家 " + player.getName() + " 应用粒子效果失败: " + particle);
            }
        }

        // 启动原生粒子任务
        if (!particleEffects.isEmpty()) {
            startNativeParticleTask(player, particleEffects);
        }

        // 检查粒子显示设置
        if (!plugin.getTitleManager().isParticleDisplayEnabled(player)) {
            plugin.getLogger().info("玩家 " + player.getName() + " 的粒子显示已关闭，跳过应用粒子效果");
            return true; // 返回true表示"成功"，只是不显示而已
        }

        return success;
    }

    /**
     * 启动原生粒子任务
     */
    private void startNativeParticleTask(Player player, List<String> particleEffects) {
        UUID uuid = player.getUniqueId();

        // 停止现有任务
        stopParticleTask(player);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !activeParticles.containsKey(uuid)) {
                    this.cancel();
                    return;
                }

                // 为每个粒子效果生成粒子
                for (String particleName : particleEffects) {
                    spawnNativeParticle(player, particleName);
                }
            }
        };

        // 每5ticks执行一次（0.25秒）
        task.runTaskTimer(plugin, 0L, 5L);
        particleTasks.put(uuid, task);
    }

    /**
     * 生成原生粒子 - 兼容性版本
     */
    private void spawnNativeParticle(Player player, String particleName) {
        try {
            Location location = player.getLocation().add(0, 2, 0);
            String mappedName = particleEffectMapping.getOrDefault(particleName.toLowerCase(), particleName);

            Particle particle;
            try {
                particle = Particle.valueOf(mappedName.toUpperCase());
            } catch (IllegalArgumentException e) {
                particle = Particle.FLAME;
            }

            // 使用兼容性包装方法
            spawnCompatibleParticle(player, particle, location);

        } catch (Exception e) {
            plugin.getLogger().warning("生成粒子失败: " + e.getMessage());
            spawnSimpleParticle(player); // 降级到简单粒子
        }
    }

    /**
     * 兼容性粒子生成方法
     */
    private void spawnCompatibleParticle(Player player, Particle particle, Location location) {
        try {
            switch (particle.name()) {
                case "REDSTONE":
                    spawnRedstoneParticle(player, location);
                    break;
                case "NOTE":
                    spawnNoteParticle(player, location);
                    break;
                case "SPELL":
                case "SPELL_WITCH":
                    player.getWorld().spawnParticle(particle, location, 10, 0.5, 0.5, 0.5, 0.1);
                    break;
                case "DUST_COLOR_TRANSITION":
                    spawnDustTransitionParticle(player, location);
                    break;
                default:
                    // 标准粒子参数
                    player.getWorld().spawnParticle(particle, location, 5, 0.5, 0.5, 0.5, 0);
                    break;
            }
        } catch (Exception e) {
            // 如果特定粒子失败，使用通用方法
            plugin.getLogger().warning(particle.name() + " 粒子生成失败，使用通用方法: " + e.getMessage());
            player.getWorld().spawnParticle(Particle.FLAME, location, 3, 0.3, 0.3, 0.3, 0);
        }
    }

    /**
     * 生成REDSTONE粒子（兼容不同版本）
     */
    private void spawnRedstoneParticle(Player player, Location location) {
        try {
            // 使用反射来调用粒子，避免编译时依赖
            Class<?> particleClass = Class.forName("org.bukkit.Particle");
            Object particleEnum;

            try {
                particleEnum = particleClass.getField("DUST").get(null);
            } catch (NoSuchFieldException e) {
                try {
                    particleEnum = particleClass.getField("REDSTONE").get(null);
                } catch (NoSuchFieldException e2) {
                    particleEnum = particleClass.getField("FLAME").get(null);
                }
            }

            // 使用反射调用spawnParticle方法
            Method spawnParticleMethod = player.getWorld().getClass().getMethod(
                    "spawnParticle", particleClass, Location.class, int.class,
                    double.class, double.class, double.class, double.class, Object.class
            );

            org.bukkit.Particle.DustOptions dustOptions = new org.bukkit.Particle.DustOptions(
                    org.bukkit.Color.fromRGB(255, 0, 0), 1.0f
            );

            spawnParticleMethod.invoke(player.getWorld(), particleEnum, location,
                    5, 0.5, 0.5, 0.5, 0.1, dustOptions);

        } catch (Exception e) {
            // 最简单的回退方案
            player.getWorld().spawnParticle(Particle.FLAME, location, 3, 0.3, 0.3, 0.3, 0);
        }
    }

    /**
     * 生成NOTE粒子
     */
    private void spawnNoteParticle(Player player, Location location) {
        try {
            // NOTE粒子在较新版本中可能需要特殊处理
            player.getWorld().spawnParticle(Particle.NOTE, location, 1, 0.5, 0.5, 0.5, 0.5);
        } catch (Exception e) {
            // 备用方案
            player.getWorld().spawnParticle(Particle.NOTE, location, 1, 0.5, 0.5, 0.5, 0);
        }
    }

    /**
     * 生成颜色过渡粒子（1.16+）
     */
    private void spawnDustTransitionParticle(Player player, Location location) {
        try {
            org.bukkit.Particle.DustTransition dustTransition = new org.bukkit.Particle.DustTransition(
                    org.bukkit.Color.fromRGB(255, 0, 0),
                    org.bukkit.Color.fromRGB(0, 0, 255),
                    1.0f
            );
            player.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, location, 5, 0.5, 0.5, 0.5, dustTransition);
        } catch (NoClassDefFoundError e) {
            // 1.16以下版本不支持，使用REDSTONE替代
            spawnRedstoneParticle(player, location);
        }
    }

    /**
     * 简单粒子降级方案
     */
    private void spawnSimpleParticle(Player player) {
        try {
            Location location = player.getLocation().add(0, 2, 0);
            player.getWorld().spawnParticle(Particle.FLAME, location, 3, 0.3, 0.3, 0.3, 0);
        } catch (Exception e) {
            plugin.getLogger().severe("所有粒子生成方法都失败了: " + e.getMessage());
        }
    }

    /**
     * 停止粒子任务
     */
    private void stopParticleTask(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitRunnable task = particleTasks.get(uuid);
        if (task != null) {
            task.cancel();
            particleTasks.remove(uuid);
        }
    }

    /**
     * 移除玩家的粒子效果
     */
    public boolean removeParticles(Player player, List<String> particleEffects) {
        UUID uuid = player.getUniqueId();
        Set<String> playerParticles = activeParticles.get(uuid);

        if (playerParticles == null) return true;

        boolean success = true;
        for (String particle : particleEffects) {
            if (removeParticleEffect(player, particle)) {
                playerParticles.remove(particle);
                plugin.getLogger().info("移除玩家 " + player.getName() + " 的粒子效果: " + particle);
            } else {
                success = false;
            }
        }

        if (playerParticles.isEmpty()) {
            activeParticles.remove(uuid);
            stopParticleTask(player);
        }

        return success;
    }

    /**
     * 移除玩家的所有粒子效果
     */
    public void removeAllParticles(Player player) {
        UUID uuid = player.getUniqueId();
        Set<String> playerParticles = activeParticles.get(uuid);

        if (playerParticles != null) {
            for (String particle : playerParticles) {
                removeParticleEffect(player, particle);
            }
            activeParticles.remove(uuid);
            stopParticleTask(player);
            plugin.getLogger().info("已移除玩家 " + player.getName() + " 的所有粒子效果");
        }
    }

    /**
     * 应用单个粒子效果
     */
    private boolean applyParticleEffect(Player player, String particleEffect) {
        boolean success = false;

        try {
            // 总是尝试原生粒子
            success = true; // 原生粒子总是成功，因为会在任务中处理

            // 尝试第三方插件
            if (superTrailsEnabled) {
                success = applySuperTrailsEffect(player, particleEffect) || success;
            }

            if (superTrailsProEnabled) {
                success = applySuperTrailsProEffect(player, particleEffect) || success;
            }

            if (playerParticlesEnabled) {
                success = applyPlayerParticlesEffect(player, particleEffect) || success;
            }

            return success;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "应用粒子效果时出错: " + e.getMessage());
            return true; // 原生粒子总是可用
        }
    }

    /**
     * 移除单个粒子效果
     */
    private boolean removeParticleEffect(Player player, String particleEffect) {
        boolean success = false;

        try {
            if (superTrailsEnabled) {
                success = removeSuperTrailsEffect(player, particleEffect) || success;
            }

            if (superTrailsProEnabled) {
                success = removeSuperTrailsProEffect(player, particleEffect) || success;
            }

            if (playerParticlesEnabled) {
                success = removePlayerParticlesEffect(player, particleEffect) || success;
            }

            return success;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "移除粒子效果时出错: " + e.getMessage());
            return true;
        }
    }

    // ==================== 第三方插件支持 ====================

    private boolean applySuperTrailsEffect(Player player, String effect) {
        try {
            String command = String.format("trails set %s %s", player.getName(), effect);
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean removeSuperTrailsEffect(Player player, String effect) {
        try {
            String command = String.format("trails remove %s", player.getName());
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean applySuperTrailsProEffect(Player player, String effect) {
        try {
            String command = String.format("strails set %s %s", player.getName(), effect);
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean removeSuperTrailsProEffect(Player player, String effect) {
        try {
            String command = String.format("strails remove %s", player.getName());
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean applyPlayerParticlesEffect(Player player, String effect) {
        try {
            String command = String.format("pp set %s %s", player.getName(), effect);
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean removePlayerParticlesEffect(Player player, String effect) {
        try {
            String command = String.format("pp remove %s", player.getName());
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 工具方法 ====================

    public Set<String> getActiveParticles(Player player) {
        return activeParticles.getOrDefault(player.getUniqueId(), new HashSet<>());
    }

    public boolean hasActiveParticles(Player player) {
        Set<String> particles = activeParticles.get(player.getUniqueId());
        return particles != null && !particles.isEmpty();
    }

    /**
     * 重新为所有在线玩家应用粒子效果
     */
    public void reapplyAllParticles() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String activeTitleId = plugin.getTitleManager().getActiveTitle(player);
            if (activeTitleId != null) {
                Title title = plugin.getTitleManager().getTitle(activeTitleId);
                if (title != null && title.hasParticleEffects()) {
                    applyParticles(player, title.getParticleEffects());
                }
            }
        }
    }

    public void reload() {
        // 停止所有粒子任务
        for (BukkitRunnable task : particleTasks.values()) {
            task.cancel();
        }
        particleTasks.clear();

        activeParticles.clear();
        checkParticlePlugins();
        initializeParticleMapping();

        // 重新应用所有粒子效果
        reapplyAllParticles();
    }

    public Map<String, Boolean> getPluginStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("SuperTrails", superTrailsEnabled);
        status.put("SuperTrailsPro", superTrailsProEnabled);
        status.put("PlayerParticles", playerParticlesEnabled);
        status.put("NativeParticles", nativeParticlesEnabled);
        return status;
    }

    public List<String> getAvailableParticles() {
        List<String> particles = new ArrayList<>();
        particles.addAll(particleEffectMapping.keySet());
        return particles;
    }

    public boolean isAnyParticlePluginEnabled() {
        return superTrailsEnabled || superTrailsProEnabled || playerParticlesEnabled || nativeParticlesEnabled;
    }
}