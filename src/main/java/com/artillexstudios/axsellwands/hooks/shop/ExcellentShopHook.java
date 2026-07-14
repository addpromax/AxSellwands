package com.artillexstudios.axsellwands.hooks.shop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentshop.ShopPlugin;
import su.nightexpress.excellentshop.api.BalanceHolder;
import su.nightexpress.excellentshop.api.product.TradeType;
import su.nightexpress.excellentshop.api.product.limit.LimitData;
import su.nightexpress.excellentshop.api.transaction.ECompletedTransaction;
import su.nightexpress.excellentshop.api.transaction.ETransactionResult;
import su.nightexpress.excellentshop.virtualshop.VirtualShopModule;
import su.nightexpress.excellentshop.virtualshop.product.VirtualProduct;

import java.util.List;

public class ExcellentShopHook implements AdvancedPricesHook {
    private VirtualShopModule module;

    @Override
    public void setup() {
        ShopPlugin plugin = (ShopPlugin) Bukkit.getPluginManager().getPlugin("ExcellentShop");
        if (plugin == null) return;
        module = plugin.getModuleRegistry().byType(VirtualShopModule.class).orElse(null);
    }

    @Override
    public double getPrice(ItemStack it) {
        VirtualProduct product = findProduct(it);
        if (product == null) return -1.0D;
        return product.getFinalPrice(TradeType.SELL, getUnitAmount(product, it));
    }

    @Override
    public double getPrice(Player player, ItemStack it) {
        VirtualProduct product = findProduct(it);
        if (product == null) return -1.0D;
        return product.getFinalPrice(TradeType.SELL, getUnitAmount(product, it), player);
    }

    @Override
    @NotNull
    public SellResult canSell(Player player, ItemStack it) {
        VirtualProduct product = findProduct(it);
        if (product == null || !product.isSellable()) return SellResult.noPrice();
        if (!product.canTrade(player)) return SellResult.failure("no_access");

        int units = getUnitAmount(product, it);
        if (units <= 0) return SellResult.noPrice();

        int space = product.getSpace();
        if (space >= 0 && units > space) return SellResult.failure("stock_limit");

        int sellLimit = product.getSellLimit();
        LimitData limitData = product.getLimitData(player);
        if (sellLimit >= 0 && units > sellLimit - limitData.getSales()) {
            return SellResult.failure("stock_limit");
        }

        return SellResult.success(product.getFinalPrice(TradeType.SELL, units, player));
    }

    @Override
    public boolean confirmSell(Player player, ItemStack it, double price) {
        VirtualProduct product = findProduct(it);
        if (product == null || !product.isSellable()) return false;

        ECompletedTransaction transaction = new ECompletedTransaction(
                player,
                TradeType.SELL,
                List.of(),
                List.of(),
                player.getInventory(),
                BalanceHolder.EMPTY,
                ETransactionResult.SUCCESS,
                true
        );
        product.handleSuccessfulTransaction(transaction, getUnitAmount(product, it));
        return true;
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean isAvailable() {
        return module != null;
    }

    private VirtualProduct findProduct(ItemStack item) {
        if (module == null) return null;
        return module.getBestProductFor(copy(item), TradeType.SELL);
    }

    private int getUnitAmount(VirtualProduct product, ItemStack item) {
        int unitSize = product.getUnitSize();
        return unitSize > 0 ? item.getAmount() / unitSize : 0;
    }

    private ItemStack copy(@NotNull ItemStack item) {
        ItemStack copy = item.clone();
        copy.setAmount(1);
        return copy;
    }
}
