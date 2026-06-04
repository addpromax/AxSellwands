package com.artillexstudios.axsellwands.hooks.shop;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentshop.ShopAPI;
import su.nightexpress.excellentshop.api.product.Product;
import su.nightexpress.excellentshop.api.product.TradeType;
import su.nightexpress.excellentshop.api.product.limit.LimitData;
import su.nightexpress.excellentshop.api.product.stock.StockData;
import su.nightexpress.excellentshop.virtualshop.VirtualShopModule;
import su.nightexpress.excellentshop.virtualshop.shop.VirtualShop;

public class ExcellentShopHook implements AdvancedPricesHook {

    @Override
    public void setup() {

    }

    /**
     * 在所有虚拟商店中找到对指定物品出售价格最高的商品。
     */
    @Nullable
    private Product findBestSellProduct(ItemStack item) {
        VirtualShopModule module = ShopAPI.getVirtualShop();
        if (module == null) return null;

        Product best = null;
        for (VirtualShop shop : module.getShops()) {
            Product candidate = shop.getBestProduct(item, TradeType.SELL);
            if (candidate == null || !candidate.isSellable()) continue;
            if (best == null || candidate.isMoreProfitable(TradeType.SELL, best)) {
                best = candidate;
            }
        }
        return best;
    }

    @Override
    public double getPrice(ItemStack it) {
        Product product = findBestSellProduct(it);
        if (product == null) return 0;
        // getSellPrice() 返回每个 unitSize 的基础价格
        int unitSize = product.getUnitSize();
        if (unitSize <= 0) return 0;
        return product.getSellPrice() / unitSize * it.getAmount();
    }

    @Override
    public double getPrice(Player player, ItemStack it) {
        Product product = findBestSellProduct(it);
        if (product == null) return 0;
        int unitSize = product.getUnitSize();
        if (unitSize <= 0) return 0;
        // getFinalSellPrice(player) 返回含加成修正后每 unitSize 的价格
        return product.getFinalSellPrice(player) / unitSize * it.getAmount();
    }

    @Override
    @NotNull
    public SellResult canSell(Player player, ItemStack it) {
        Product product = findBestSellProduct(it);

        if (product == null) {
            return SellResult.noPrice();
        }

        // 检查玩家权限/rank/rotation 等条件
        if (!product.canTrade(player)) {
            return SellResult.failure("no_access");
        }

        int unitSize = product.getUnitSize();
        if (unitSize <= 0) return SellResult.noPrice();

        int sellAmount = it.getAmount();

        // 检查全局库存
        StockData stockData = product.getStockData();
        int globalStock = stockData.getStock(); // -1 表示无限制（UnlimitedStockData）
        if (globalStock == 0) {
            return SellResult.failure("stock_limit");
        }

        // 检查玩家个人限额
        LimitData limitData = product.getLimitData(player);
        int sellLimit = product.getSellLimit(); // -1 表示无限制
        if (sellLimit >= 0) {
            int usedSales = limitData.getSales();
            int remaining = sellLimit - usedSales;
            if (remaining <= 0) {
                return SellResult.failure("stock_limit");
            }
            // 计算单位数量（向下取整）
            int remainingItems = remaining * unitSize;
            if (sellAmount > remainingItems) {
                sellAmount = remainingItems;
            }
        }

        // 全局库存限制调整
        if (globalStock > 0) {
            // globalStock 是剩余可存入（sell 方向是 store）的容量
            int capacity = product.getCapacity();
            int currentStock = stockData.getStock();
            int spaceLeft = product.getSpace(); // capacity - stock，-1 表示无限
            if (spaceLeft >= 0) {
                int maxByStock = spaceLeft * unitSize;
                if (sellAmount > maxByStock) {
                    sellAmount = maxByStock;
                }
                if (sellAmount <= 0) {
                    return SellResult.failure("stock_limit");
                }
            }
        }

        double pricePerUnit = product.getFinalSellPrice(player) / unitSize;
        double totalPrice = pricePerUnit * sellAmount;

        return SellResult.success(totalPrice);
    }

    @Override
    public boolean confirmSell(Player player, ItemStack it, double price) {
        // 新版 ExcellentShop 通过 onSuccessfulTransaction 自动处理库存/限额消耗，
        // AxSellwands 自己负责扣除物品并给予货币，不需要在此手动消耗库存。
        Product product = findBestSellProduct(it);
        return product != null && product.isSellable();
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
