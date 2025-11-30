package com.artillexstudios.axsellwands.hooks.shop;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nexshop.ShopAPI;
import su.nightexpress.nexshop.api.shop.type.TradeType;
import su.nightexpress.nexshop.shop.virtual.impl.VirtualProduct;

public class ExcellentShopHook implements AdvancedPricesHook {

    @Override
    public void setup() {

    }

    @Override
    public double getPrice(ItemStack it) {
        VirtualProduct product = ShopAPI.getVirtualShop().getBestProductFor(it, TradeType.SELL);
        if (product == null || !product.isSellable()) return 0;
        return product.getPrice(TradeType.SELL) / product.getUnitAmount() * it.getAmount();
    }

    @Override
    public double getPrice(Player player, ItemStack it) {
        VirtualProduct product = ShopAPI.getVirtualShop().getBestProductFor(it, TradeType.SELL);
        if (product == null || !product.isSellable()) return 0;
        return product.getFinalSellPrice(player) / product.getUnitAmount() * it.getAmount();
    }

    @Override
    @NotNull
    public SellResult canSell(Player player, ItemStack it) {
        VirtualProduct product = ShopAPI.getVirtualShop().getBestProductFor(it, TradeType.SELL);
        
        if (product == null || !product.isSellable()) {
            return SellResult.noPrice();
        }
        
        // 检查玩家是否有权限访问该商品
        if (!product.hasAccess(player)) {
            return SellResult.failure("no_access");
        }
        
        // 检查库存限制（服务器全局和玩家个人）
        int availableAmount = product.getAvailableAmount(player, TradeType.SELL);
        if (availableAmount == 0) {
            return SellResult.failure("stock_limit");
        }
        
        // 计算价格
        double pricePerUnit = product.getFinalSellPrice(player) / product.getUnitAmount();
        int sellAmount = it.getAmount();
        
        // 如果库存有限制，调整出售数量
        if (availableAmount > 0 && sellAmount > availableAmount) {
            sellAmount = availableAmount;
        }
        
        double totalPrice = pricePerUnit * sellAmount;
        
        return SellResult.success(totalPrice);
    }

    @Override
    public boolean confirmSell(Player player, ItemStack it, double price) {
        VirtualProduct product = ShopAPI.getVirtualShop().getBestProductFor(it, TradeType.SELL);
        
        if (product == null || !product.isSellable()) {
            return false;
        }
        
        // 消耗库存
        int amount = it.getAmount();
        
        // 消耗全局库存
        product.consumeStock(TradeType.SELL, amount, null);
        
        // 消耗玩家个人限额
        product.consumeStock(TradeType.SELL, amount, player.getUniqueId());
        
        return true;
    }

    @Override
    public int getPriority() {
        // ExcellentShop 优先级为 50
        return 50;
    }

    @Override
    public boolean isAvailable() {
        try {
            return ShopAPI.getVirtualShop() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
