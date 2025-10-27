package xyz.tolite.greatTitle.model;

import xyz.tolite.greatTitle.manager.*;

public class RewardStatus {
    private final boolean canClaim;
    private final boolean claimed;
    private final boolean canClaimAgain;
    private final PlayerReward playerReward;

    public RewardStatus(boolean canClaim, boolean claimed, boolean canClaimAgain, PlayerReward playerReward) {
        this.canClaim = canClaim;
        this.claimed = claimed;
        this.canClaimAgain = canClaimAgain;
        this.playerReward = playerReward;
    }

    // Getters
    public boolean canClaim() { return canClaim; }
    public boolean isClaimed() { return claimed; }
    public boolean canClaimAgain() { return canClaimAgain; }
    public PlayerReward getPlayerReward() { return playerReward; }
}