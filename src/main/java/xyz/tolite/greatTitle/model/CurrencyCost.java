package xyz.tolite.greatTitle.model;

public class CurrencyCost {

    private final String currencyType;
    private final double amount;

    public CurrencyCost(String currencyType, double amount) {
        this.currencyType = currencyType;
        this.amount = amount;
    }

    // Getters
    public String getCurrencyType() { return currencyType; }
    public double getAmount() { return amount; }

    public String getDisplayName() {
        switch (currencyType.toUpperCase()) {
            case "VAULT": return "金币";
            case "PLAYER_POINTS": return "点券";
            case "COIN": return "称号币";
            case "TOKEN": return "代币";
            case "CRYSTAL": return "水晶";
            default: return currencyType;
        }
    }
}