package com.artillexstudios.axsellwands.hooks.shop;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 高级价格钩子接口，支持限制检查和出售结果
 */
public interface AdvancedPricesHook extends PricesHook {
    
    /**
     * 检查玩家是否可以出售物品并获取价格
     * 
     * @param player 玩家
     * @param it 物品
     * @return 出售结果，包含价格和是否可以出售
     */
    @NotNull
    default SellResult canSell(Player player, ItemStack it) {
        double price = getPrice(player, it);
        return price > 0 ? SellResult.success(price) : SellResult.noPrice();
    }
    
    /**
     * 确认出售物品（扣除限额等）
     * 
     * @param player 玩家
     * @param it 物品
     * @param price 出售价格
     * @return 是否成功确认
     */
    default boolean confirmSell(Player player, ItemStack it, double price) {
        return true;
    }
    
    /**
     * 获取优先级，数字越大优先级越高
     * 
     * @return 优先级
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * 检查是否可用
     * 
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }
}
