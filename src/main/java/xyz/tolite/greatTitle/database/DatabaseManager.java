package xyz.tolite.greatTitle.database;

import xyz.tolite.greatTitle.GreatTitle;
import xyz.tolite.greatTitle.manager.ConfigManager;
import xyz.tolite.greatTitle.model.PlayerReward;
import xyz.tolite.greatTitle.model.PlayerTitle;
import org.bukkit.command.CommandSender;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final GreatTitle plugin;
    private Connection connection;

    public DatabaseManager(GreatTitle plugin) {
        this.plugin = plugin;
    }

    public boolean initDatabase() {
        ConfigManager config = plugin.getConfigManager();
        String dbType = config.getDatabaseType();

        try {
            if ("MySQL".equalsIgnoreCase(dbType)) {
                String host = config.getMySQLHost();
                int port = config.getMySQLPort();
                String database = config.getMySQLDatabase();
                String username = config.getMySQLUsername();
                String password = config.getMySQLPassword();

                // 加载MySQL驱动
                Class.forName("com.mysql.cj.jdbc.Driver");
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC&characterEncoding=utf8";
                connection = DriverManager.getConnection(url, username, password);
            } else {
                // SQLite - 修改为 GreatTitle.db
                Class.forName("org.sqlite.JDBC");
                String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/GreatTitle.db";
                connection = DriverManager.getConnection(url);
            }

            createTables();
            plugin.getLogger().info("数据库连接成功: " + dbType);
            return true;

        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "数据库驱动加载失败: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "数据库连接失败: " + e.getMessage());
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "数据库初始化异常: " + e.getMessage());
            return false;
        }
    }

    private void createTables() throws SQLException {
        // 为不同数据库类型调整SQL语法
        String playerTitlesTableSQL;
        String playerSettingsTableSQL;
        String playerRewardsTableSQL;
        String playerCoinsTableSQL; // 新增称号币表

        String dbType = plugin.getConfigManager().getDatabaseType();
        if ("MySQL".equalsIgnoreCase(dbType)) {
            playerTitlesTableSQL = """
                CREATE TABLE IF NOT EXISTS player_titles (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    title_id VARCHAR(64) NOT NULL,
                    obtain_time BIGINT NOT NULL,
                    expire_time BIGINT DEFAULT 0,
                    UNIQUE KEY unique_player_title (player_uuid, title_id)
                )
                """;

            playerSettingsTableSQL = """
                CREATE TABLE IF NOT EXISTS player_settings (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    active_title VARCHAR(64),
                    head_display BOOLEAN DEFAULT true,
                    tablist_display BOOLEAN DEFAULT true,
                    chat_display BOOLEAN DEFAULT true,
                    particle_display BOOLEAN DEFAULT true, -- 新增粒子显示设置字段
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    particle_display BOOLEAN DEFAULT 1
                )
                """;

            playerRewardsTableSQL = """
                CREATE TABLE IF NOT EXISTS player_rewards (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    reward_id VARCHAR(64) NOT NULL,
                    claim_time BIGINT NOT NULL,
                    claim_count INT DEFAULT 1,
                    UNIQUE KEY unique_player_reward (player_uuid, reward_id)
                )
                """;

            playerCoinsTableSQL = """
                CREATE TABLE IF NOT EXISTS player_coins (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    coins INT DEFAULT 0,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;
        } else {
            // SQLite
            playerTitlesTableSQL = """
                CREATE TABLE IF NOT EXISTS player_titles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid VARCHAR(36) NOT NULL,
                    title_id VARCHAR(64) NOT NULL,
                    obtain_time BIGINT NOT NULL,
                    expire_time BIGINT DEFAULT 0,
                    UNIQUE(player_uuid, title_id)
                )
                """;

            playerSettingsTableSQL = """
                CREATE TABLE IF NOT EXISTS player_settings (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    active_title VARCHAR(64),
                    head_display BOOLEAN DEFAULT 1,
                    tablist_display BOOLEAN DEFAULT 1,
                    chat_display BOOLEAN DEFAULT 1,
                    particle_display BOOLEAN DEFAULT 1, -- 新增粒子显示设置字段
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            playerRewardsTableSQL = """
                CREATE TABLE IF NOT EXISTS player_rewards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid VARCHAR(36) NOT NULL,
                    reward_id VARCHAR(64) NOT NULL,
                    claim_time BIGINT NOT NULL,
                    claim_count INTEGER DEFAULT 1,
                    UNIQUE(player_uuid, reward_id)
                )
                """;

            playerCoinsTableSQL = """
                CREATE TABLE IF NOT EXISTS player_coins (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    coins INTEGER DEFAULT 0,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(playerTitlesTableSQL);
            stmt.execute(playerSettingsTableSQL);
            stmt.execute(playerRewardsTableSQL);
            stmt.execute(playerCoinsTableSQL); // 执行创建称号币表
            plugin.getLogger().info("数据表创建/检查完成");
        }
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // 使用现有的 initDatabase 方法而不是 initializeDatabase
                if (!initDatabase()) {
                    plugin.getLogger().severe("无法初始化数据库连接");
                    return null;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("检查数据库连接时出错: " + e.getMessage());
            // 尝试重新初始化
            if (!initDatabase()) {
                plugin.getLogger().severe("重新初始化数据库连接失败");
                return null;
            }
        }
        return connection;
    }

    // ==================== 粒子显示设置方法 ====================

    /**
     * 获取粒子显示设置
     */
    public boolean getParticleDisplaySetting(UUID playerUUID) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法获取粒子显示设置");
            return true; // 默认开启
        }

        String sql = "SELECT particle_display FROM player_settings WHERE player_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("particle_display");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取玩家粒子显示设置失败: " + e.getMessage());
        }

        return true; // 默认开启
    }

    /**
     * 设置粒子显示设置
     */
    public void setParticleDisplaySetting(UUID playerUUID, boolean display) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法设置粒子显示");
            return;
        }

        String sql;
        String dbType = plugin.getConfigManager().getDatabaseType();

        if ("MySQL".equalsIgnoreCase(dbType)) {
            sql = """
                INSERT INTO player_settings (player_uuid, particle_display) 
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE 
                particle_display = VALUES(particle_display),
                updated_at = CURRENT_TIMESTAMP
                """;
        } else {
            sql = """
                INSERT OR REPLACE INTO player_settings (player_uuid, particle_display, updated_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """;
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setBoolean(2, display);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("设置玩家粒子显示失败: " + e.getMessage());
        }
    }

    // 以下是原有的所有方法保持不变...
    // 称号币相关方法
    public int getPlayerCoins(UUID playerUUID) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法获取玩家称号币");
            return 0;
        }

        String sql = "SELECT coins FROM player_coins WHERE player_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("coins");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取玩家称号币失败: " + e.getMessage());
        }

        return 0; // 默认0个称号币
    }

    public boolean setPlayerCoins(UUID playerUUID, int coins) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法设置玩家称号币");
            return false;
        }

        String sql;
        String dbType = plugin.getConfigManager().getDatabaseType();

        if ("MySQL".equalsIgnoreCase(dbType)) {
            sql = """
                INSERT INTO player_coins (player_uuid, coins) 
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE 
                coins = VALUES(coins),
                updated_at = CURRENT_TIMESTAMP
                """;
        } else {
            sql = """
                INSERT OR REPLACE INTO player_coins (player_uuid, coins, updated_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """;
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setInt(2, coins);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("设置玩家称号币失败: " + e.getMessage());
            return false;
        }
    }

    // 转换数据库时需要添加称号币数据的处理
    private Map<UUID, Integer> getAllPlayerCoins() {
        Map<UUID, Integer> allCoins = new HashMap<>();

        String sql = "SELECT player_uuid, coins FROM player_coins";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                int coins = rs.getInt("coins");
                allCoins.put(playerUUID, coins);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取所有玩家称号币数据失败: " + e.getMessage());
        }

        return allCoins;
    }

    /**
     * 获取所有玩家称号数据
     */
    private Map<UUID, Map<String, PlayerTitle>> getAllPlayerTitles() {
        Map<UUID, Map<String, PlayerTitle>> allTitles = new HashMap<>();

        String sql = "SELECT player_uuid, title_id, obtain_time, expire_time FROM player_titles";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                String titleId = rs.getString("title_id");
                long obtainTime = rs.getLong("obtain_time");
                long expireTime = rs.getLong("expire_time");

                allTitles.computeIfAbsent(playerUUID, k -> new HashMap<>())
                        .put(titleId, new PlayerTitle(titleId, obtainTime, expireTime));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取所有玩家称号数据失败: " + e.getMessage());
        }

        return allTitles;
    }

    /**
     * 获取所有玩家设置数据
     */
    private Map<UUID, Map<String, Object>> getAllPlayerSettings() {
        Map<UUID, Map<String, Object>> allSettings = new HashMap<>();

        String sql = "SELECT player_uuid, active_title, head_display, tablist_display, chat_display, particle_display FROM player_settings";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                Map<String, Object> settings = new HashMap<>();

                settings.put("active_title", rs.getString("active_title"));
                settings.put("head_display", rs.getBoolean("head_display"));
                settings.put("tablist_display", rs.getBoolean("tablist_display"));
                settings.put("chat_display", rs.getBoolean("chat_display"));
                settings.put("particle_display", rs.getBoolean("particle_display")); // 新增粒子显示设置

                allSettings.put(playerUUID, settings);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取所有玩家设置数据失败: " + e.getMessage());
        }

        return allSettings;
    }

    /**
     * 获取所有玩家奖励数据
     */
    private Map<UUID, Map<String, PlayerReward>> getAllPlayerRewards() {
        Map<UUID, Map<String, PlayerReward>> allRewards = new HashMap<>();

        String sql = "SELECT player_uuid, reward_id, claim_time, claim_count FROM player_rewards";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                String rewardId = rs.getString("reward_id");
                long claimTime = rs.getLong("claim_time");
                int claimCount = rs.getInt("claim_count");

                allRewards.computeIfAbsent(playerUUID, k -> new HashMap<>())
                        .put(rewardId, new PlayerReward(rewardId, claimTime, claimCount));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取所有玩家奖励数据失败: " + e.getMessage());
        }

        return allRewards;
    }

    /**
     * 转换数据库
     */
    public boolean convertDatabase(String targetType, CommandSender sender) {
        String currentType = plugin.getConfigManager().getDatabaseType();

        if (currentType.equalsIgnoreCase(targetType)) {
            if (sender != null) {
                sender.sendMessage("§c当前数据库已经是 " + targetType + "，无需转换!");
            }
            return false;
        }

        try {
            // 备份当前数据
            Map<UUID, Map<String, PlayerTitle>> allTitles = getAllPlayerTitles();
            Map<UUID, Map<String, Object>> allSettings = getAllPlayerSettings();
            Map<UUID, Map<String, PlayerReward>> allRewards = getAllPlayerRewards();
            Map<UUID, Integer> allCoins = getAllPlayerCoins(); // 新增称号币数据备份

            // 关闭当前连接
            closeConnection();

            // 更新配置
            plugin.getConfigManager().getConfig().set("database.type", targetType);
            plugin.getConfigManager().saveConfig();

            // 重新初始化数据库
            if (!initDatabase()) {
                // 转换失败，恢复原配置
                plugin.getConfigManager().getConfig().set("database.type", currentType);
                plugin.getConfigManager().saveConfig();
                initDatabase(); // 重新连接原数据库

                if (sender != null) {
                    sender.sendMessage("§c数据库转换失败! 已恢复原数据库配置。");
                }
                return false;
            }

            // 恢复数据到新数据库
            boolean success = restoreDataToNewDatabase(allTitles, allSettings, allRewards, allCoins);

            if (success) {
                if (sender != null) {
                    sender.sendMessage("§a数据库转换成功! 已从 " + currentType + " 转换为 " + targetType);
                }
                return true;
            } else {
                // 转换失败，恢复原配置
                plugin.getConfigManager().getConfig().set("database.type", currentType);
                plugin.getConfigManager().saveConfig();
                closeConnection();
                initDatabase(); // 重新连接原数据库

                if (sender != null) {
                    sender.sendMessage("§c数据恢复失败! 已恢复原数据库配置。");
                }
                return false;
            }

        } catch (Exception e) {
            plugin.getLogger().severe("数据库转换过程中发生错误: " + e.getMessage());

            // 恢复原配置
            plugin.getConfigManager().getConfig().set("database.type", currentType);
            plugin.getConfigManager().saveConfig();
            closeConnection();
            initDatabase(); // 重新连接原数据库

            if (sender != null) {
                sender.sendMessage("§c数据库转换失败: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * 将数据恢复到新数据库
     */
    private boolean restoreDataToNewDatabase(Map<UUID, Map<String, PlayerTitle>> allTitles,
                                             Map<UUID, Map<String, Object>> allSettings,
                                             Map<UUID, Map<String, PlayerReward>> allRewards,
                                             Map<UUID, Integer> allCoins) {
        try {
            // 恢复玩家称号数据
            for (Map.Entry<UUID, Map<String, PlayerTitle>> playerEntry : allTitles.entrySet()) {
                UUID playerUUID = playerEntry.getKey();
                for (PlayerTitle playerTitle : playerEntry.getValue().values()) {
                    addPlayerTitle(playerUUID, playerTitle.getTitleId(),
                            playerTitle.getObtainTime(), playerTitle.getExpireTime());
                }
            }

            // 恢复玩家设置数据
            for (Map.Entry<UUID, Map<String, Object>> playerEntry : allSettings.entrySet()) {
                UUID playerUUID = playerEntry.getKey();
                Map<String, Object> settings = playerEntry.getValue();

                String activeTitle = (String) settings.get("active_title");
                boolean headDisplay = (Boolean) settings.get("head_display");
                boolean tablistDisplay = (Boolean) settings.get("tablist_display");
                boolean chatDisplay = (Boolean) settings.get("chat_display");
                boolean particleDisplay = (Boolean) settings.get("particle_display"); // 新增粒子显示设置

                if (activeTitle != null) {
                    setActiveTitle(playerUUID, activeTitle);
                }
                setHeadDisplaySetting(playerUUID, headDisplay);
                setTabListDisplaySetting(playerUUID, tablistDisplay);
                setChatDisplaySetting(playerUUID, chatDisplay);
                setParticleDisplaySetting(playerUUID, particleDisplay); // 新增粒子显示设置恢复
            }

            // 恢复玩家奖励数据
            for (Map.Entry<UUID, Map<String, PlayerReward>> playerEntry : allRewards.entrySet()) {
                UUID playerUUID = playerEntry.getKey();
                for (PlayerReward playerReward : playerEntry.getValue().values()) {
                    savePlayerReward(playerUUID, playerReward.getRewardId(),
                            playerReward.getClaimTime(), playerReward.getClaimCount());
                }
            }

            // 恢复玩家称号币数据
            for (Map.Entry<UUID, Integer> coinEntry : allCoins.entrySet()) {
                setPlayerCoins(coinEntry.getKey(), coinEntry.getValue());
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("恢复数据到新数据库失败: " + e.getMessage());
            return false;
        }
    }

    // 以下为原有的数据库操作方法，保持不变...
    public Map<String, PlayerTitle> getPlayerTitles(UUID playerUUID) {
        Map<String, PlayerTitle> titles = new HashMap<>();
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法获取玩家称号");
            return titles;
        }

        String sql = "SELECT title_id, obtain_time, expire_time FROM player_titles WHERE player_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String titleId = rs.getString("title_id");
                long obtainTime = rs.getLong("obtain_time");
                long expireTime = rs.getLong("expire_time");
                titles.put(titleId, new PlayerTitle(titleId, obtainTime, expireTime));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取玩家称号失败: " + e.getMessage());
        }

        return titles;
    }

    public void addPlayerTitle(UUID playerUUID, String titleId, long obtainTime, long expireTime) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法添加玩家称号");
            return;
        }

        String sql = "INSERT OR REPLACE INTO player_titles (player_uuid, title_id, obtain_time, expire_time) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, titleId);
            stmt.setLong(3, obtainTime);
            stmt.setLong(4, expireTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("添加玩家称号失败: " + e.getMessage());
        }
    }

    public void updatePlayerTitle(UUID playerUUID, String titleId, long obtainTime, long expireTime) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法更新玩家称号");
            return;
        }

        String sql = "UPDATE player_titles SET obtain_time = ?, expire_time = ? WHERE player_uuid = ? AND title_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, obtainTime);
            stmt.setLong(2, expireTime);
            stmt.setString(3, playerUUID.toString());
            stmt.setString(4, titleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("更新玩家称号失败: " + e.getMessage());
        }
    }

    public void removePlayerTitle(UUID playerUUID, String titleId) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法移除玩家称号");
            return;
        }

        String sql = "DELETE FROM player_titles WHERE player_uuid = ? AND title_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, titleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("移除玩家称号失败: " + e.getMessage());
        }
    }

    public String getActiveTitle(UUID playerUUID) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法获取活跃称号");
            return null;
        }

        String sql = "SELECT active_title FROM player_settings WHERE player_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("active_title");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取玩家活跃称号失败: " + e.getMessage());
        }

        return null;
    }

    public void setActiveTitle(UUID playerUUID, String titleId) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法设置活跃称号");
            return;
        }

        String sql;
        String dbType = plugin.getConfigManager().getDatabaseType();

        if ("MySQL".equalsIgnoreCase(dbType)) {
            sql = """
                INSERT INTO player_settings (player_uuid, active_title) 
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE 
                active_title = VALUES(active_title),
                updated_at = CURRENT_TIMESTAMP
                """;
        } else {
            sql = """
                INSERT OR REPLACE INTO player_settings (player_uuid, active_title, updated_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """;
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, titleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("设置玩家活跃称号失败: " + e.getMessage());
        }
    }

    public boolean getHeadDisplaySetting(UUID playerUUID) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法获取头顶显示设置");
            return true;
        }

        String sql = "SELECT head_display FROM player_settings WHERE player_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("head_display");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取玩家头顶显示设置失败: " + e.getMessage());
        }

        return true; // 默认开启
    }

    public void setHeadDisplaySetting(UUID playerUUID, boolean display) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法设置头顶显示");
            return;
        }

        String sql;
        String dbType = plugin.getConfigManager().getDatabaseType();

        if ("MySQL".equalsIgnoreCase(dbType)) {
            sql = """
                INSERT INTO player_settings (player_uuid, head_display) 
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE 
                head_display = VALUES(head_display),
                updated_at = CURRENT_TIMESTAMP
                """;
        } else {
            sql = """
                INSERT OR REPLACE INTO player_settings (player_uuid, head_display, updated_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """;
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setBoolean(2, display);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("设置玩家头顶显示失败: " + e.getMessage());
        }
    }

    public boolean getTabListDisplaySetting(UUID playerUUID) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法获取Tab列表显示设置");
            return true;
        }

        String sql = "SELECT tablist_display FROM player_settings WHERE player_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("tablist_display");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取玩家Tab列表显示设置失败: " + e.getMessage());
        }

        return true; // 默认开启
    }

    public void setTabListDisplaySetting(UUID playerUUID, boolean display) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法设置Tab列表显示");
            return;
        }

        String sql;
        String dbType = plugin.getConfigManager().getDatabaseType();

        if ("MySQL".equalsIgnoreCase(dbType)) {
            sql = """
                INSERT INTO player_settings (player_uuid, tablist_display) 
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE 
                tablist_display = VALUES(tablist_display),
                updated_at = CURRENT_TIMESTAMP
                """;
        } else {
            sql = """
                INSERT OR REPLACE INTO player_settings (player_uuid, tablist_display, updated_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """;
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setBoolean(2, display);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("设置玩家Tab列表显示失败: " + e.getMessage());
        }
    }

    public boolean getChatDisplaySetting(UUID playerUUID) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法获取聊天显示设置");
            return true;
        }

        String sql = "SELECT chat_display FROM player_settings WHERE player_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("chat_display");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取玩家聊天显示设置失败: " + e.getMessage());
        }

        return true; // 默认开启
    }

    public void setChatDisplaySetting(UUID playerUUID, boolean display) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法设置聊天显示");
            return;
        }

        String sql;
        String dbType = plugin.getConfigManager().getDatabaseType();

        if ("MySQL".equalsIgnoreCase(dbType)) {
            sql = """
                INSERT INTO player_settings (player_uuid, chat_display) 
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE 
                chat_display = VALUES(chat_display),
                updated_at = CURRENT_TIMESTAMP
                """;
        } else {
            sql = """
                INSERT OR REPLACE INTO player_settings (player_uuid, chat_display, updated_at) 
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """;
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setBoolean(2, display);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("设置玩家聊天显示失败: " + e.getMessage());
        }
    }

    public Map<String, PlayerReward> getPlayerRewards(UUID playerUUID) {
        Map<String, PlayerReward> rewards = new HashMap<>();
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法获取玩家奖励");
            return rewards;
        }

        String sql = "SELECT reward_id, claim_time, claim_count FROM player_rewards WHERE player_uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String rewardId = rs.getString("reward_id");
                long claimTime = rs.getLong("claim_time");
                int claimCount = rs.getInt("claim_count");
                rewards.put(rewardId, new PlayerReward(rewardId, claimTime, claimCount));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取玩家奖励失败: " + e.getMessage());
        }

        return rewards;
    }

    public void savePlayerReward(UUID playerUUID, String rewardId, long claimTime, int claimCount) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法保存玩家奖励");
            return;
        }

        String sql;
        String dbType = plugin.getConfigManager().getDatabaseType();

        if ("MySQL".equalsIgnoreCase(dbType)) {
            sql = """
                INSERT INTO player_rewards (player_uuid, reward_id, claim_time, claim_count) 
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                claim_time = VALUES(claim_time),
                claim_count = VALUES(claim_count)
                """;
        } else {
            sql = """
                INSERT OR REPLACE INTO player_rewards (player_uuid, reward_id, claim_time, claim_count) 
                VALUES (?, ?, ?, ?)
                """;
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, rewardId);
            stmt.setLong(3, claimTime);
            stmt.setInt(4, claimCount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("保存玩家奖励失败: " + e.getMessage());
        }
    }

    /**
     * 清除所有称号数据（重置为默认）
     */
    public void clearAllTitles() {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法清除称号数据");
            return;
        }

        try {
            // 删除所有玩家称号数据
            String deletePlayerTitles = "DELETE FROM player_titles";
            String deletePlayerSettings = "DELETE FROM player_settings";

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(deletePlayerTitles);
                stmt.execute(deletePlayerSettings);
            }

            plugin.getLogger().info("已清除所有称号数据");
        } catch (SQLException e) {
            plugin.getLogger().warning("清除称号数据失败: " + e.getMessage());
        }
    }

    /**
     * 清除指定玩家的所有称号数据
     */
    public void clearPlayerTitles(UUID playerUUID) {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法清除玩家称号数据");
            return;
        }

        try {
            String deletePlayerTitles = "DELETE FROM player_titles WHERE player_uuid = ?";
            String deletePlayerSettings = "DELETE FROM player_settings WHERE player_uuid = ?";

            try (PreparedStatement stmt1 = connection.prepareStatement(deletePlayerTitles);
                 PreparedStatement stmt2 = connection.prepareStatement(deletePlayerSettings)) {

                stmt1.setString(1, playerUUID.toString());
                stmt2.setString(1, playerUUID.toString());

                stmt1.executeUpdate();
                stmt2.executeUpdate();
            }

            plugin.getLogger().info("已清除玩家 " + playerUUID + " 的称号数据");
        } catch (SQLException e) {
            plugin.getLogger().warning("清除玩家称号数据失败: " + e.getMessage());
        }
    }

    /**
     * 清除所有插件数据（完全重置）
     */
    public void clearAllData() {
        if (connection == null) {
            plugin.getLogger().warning("数据库连接为空，无法清除所有数据");
            return;
        }

        try {
            // 删除所有表数据
            String deletePlayerTitles = "DELETE FROM player_titles";
            String deletePlayerSettings = "DELETE FROM player_settings";
            String deletePlayerRewards = "DELETE FROM player_rewards";

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(deletePlayerTitles);
                stmt.execute(deletePlayerSettings);
                stmt.execute(deletePlayerRewards);
            }

            // 如果是SQLite，还可以选择删除表并重新创建
            String dbType = plugin.getConfigManager().getDatabaseType();
            if ("SQLite".equalsIgnoreCase(dbType)) {
                String dropPlayerTitles = "DROP TABLE IF EXISTS player_titles";
                String dropPlayerSettings = "DROP TABLE IF EXISTS player_settings";
                String dropPlayerRewards = "DROP TABLE IF EXISTS player_rewards";

                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(dropPlayerTitles);
                    stmt.execute(dropPlayerSettings);
                    stmt.execute(dropPlayerRewards);
                }

                // 重新创建表
                createTables();
            }

            plugin.getLogger().info("已清除所有插件数据");
        } catch (SQLException e) {
            plugin.getLogger().warning("清除所有数据失败: " + e.getMessage());
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("数据库连接已关闭");
            } catch (SQLException e) {
                plugin.getLogger().warning("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }
}