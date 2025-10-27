package xyz.tolite.greatTitle.model;

public class PlayerTitle {

    private final String titleId;
    private final long obtainTime;
    private long expireTime; // 0表示永久

    public PlayerTitle(String titleId, long obtainTime, long expireTime) {
        this.titleId = titleId;
        this.obtainTime = obtainTime;
        this.expireTime = expireTime;
    }

    // Getters
    public String getTitleId() { return titleId; }
    public long getObtainTime() { return obtainTime; }
    public long getExpireTime() { return expireTime; }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public boolean isExpired() {
        if (expireTime == 0) {
            return false; // 永久称号
        }
        return System.currentTimeMillis() > expireTime;
    }

    public boolean isPermanent() {
        return expireTime == 0;
    }

    public long getRemainingTime() {
        if (isPermanent()) {
            return -1; // 永久
        }
        if (isExpired()) {
            return 0;
        }
        return expireTime - System.currentTimeMillis();
    }

    public String getRemainingTimeDisplay() {
        if (isPermanent()) {
            return "永久";
        }

        long remaining = getRemainingTime();
        if (remaining <= 0) {
            return "已过期";
        }

        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "天" + (hours % 24) + "小时";
        } else if (hours > 0) {
            return hours + "小时" + (minutes % 60) + "分钟";
        } else if (minutes > 0) {
            return minutes + "分钟" + (seconds % 60) + "秒";
        } else {
            return seconds + "秒";
        }
    }

    public String getStatusDisplay(boolean isActive) {
        if (isExpired()) {
            return "§c已过期";
        } else if (isActive) {
            return "§a使用中";
        } else {
            return "§7未使用";
        }
    }
}