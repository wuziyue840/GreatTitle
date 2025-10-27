package xyz.tolite.greatTitle.model;

public class PlayerReward {

    private final String rewardId;
    private final long claimTime;
    private final int claimCount;

    public PlayerReward(String rewardId, long claimTime, int claimCount) {
        this.rewardId = rewardId;
        this.claimTime = claimTime;
        this.claimCount = claimCount;
    }

    // Getters
    public String getRewardId() { return rewardId; }
    public long getClaimTime() { return claimTime; }
    public int getClaimCount() { return claimCount; }

    public PlayerReward incrementClaim() {
        return new PlayerReward(rewardId, System.currentTimeMillis(), claimCount + 1);
    }
}