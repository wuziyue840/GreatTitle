package xyz.tolite.greatTitle.model;

public class Buff {

    private final String effectType;
    private final int amplifier;
    private final int duration; // 秒，0表示无限
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;
    private final long appliedTime;

    public Buff(String effectType, int amplifier, int duration, boolean ambient,
                boolean showParticles, boolean showIcon) {
        this.effectType = effectType;
        this.amplifier = amplifier;
        this.duration = duration;
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.showIcon = showIcon;
        this.appliedTime = System.currentTimeMillis();
    }

    // Getters
    public String getEffectType() { return effectType; }
    public int getAmplifier() { return amplifier; }
    public int getDuration() { return duration; }
    public boolean isAmbient() { return ambient; }
    public boolean showParticles() { return showParticles; }
    public boolean showIcon() { return showIcon; }
    public long getAppliedTime() { return appliedTime; }

    public boolean isExpired() {
        if (duration == 0) return false; // 永久效果
        return System.currentTimeMillis() > appliedTime + (duration * 1000L);
    }

    public long getRemainingTime() {
        if (duration == 0) return -1;
        long elapsed = System.currentTimeMillis() - appliedTime;
        long remaining = (duration * 1000L) - elapsed;
        return Math.max(0, remaining);
    }

    public String getRemainingTimeDisplay() {
        if (duration == 0) return "永久";
        long remaining = getRemainingTime() / 1000;
        if (remaining <= 0) return "已过期";

        long hours = remaining / 3600;
        long minutes = (remaining % 3600) / 60;
        long seconds = remaining % 60;

        if (hours > 0) {
            return hours + "时" + minutes + "分";
        } else if (minutes > 0) {
            return minutes + "分" + seconds + "秒";
        } else {
            return seconds + "秒";
        }
    }
}