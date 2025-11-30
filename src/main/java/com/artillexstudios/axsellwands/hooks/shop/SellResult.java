package com.artillexstudios.axsellwands.hooks.shop;

import org.jetbrains.annotations.NotNull;

/**
 * 出售结果类，包含价格和是否可以出售的信息
 */
public class SellResult {
    private final double price;
    private final boolean canSell;
    private final String reason;

    public SellResult(double price, boolean canSell, String reason) {
        this.price = price;
        this.canSell = canSell;
        this.reason = reason;
    }

    public static SellResult success(double price) {
        return new SellResult(price, true, null);
    }

    public static SellResult failure(String reason) {
        return new SellResult(0, false, reason);
    }

    public static SellResult noPrice() {
        return new SellResult(0, false, "no_price");
    }

    public double getPrice() {
        return price;
    }

    public boolean canSell() {
        return canSell;
    }

    @NotNull
    public String getReason() {
        return reason == null ? "" : reason;
    }
}
