package com.artillexstudios.axsellwands.hooks.shop;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 多价格提供商管理器
 * 支持按优先级选择最佳价格提供商
 */
public class MultiPricesHook implements AdvancedPricesHook {
    
    private final List<AdvancedPricesHook> hooks = new ArrayList<>();
    
    public void addHook(@NotNull AdvancedPricesHook hook) {
        hooks.add(hook);
        // 按优先级排序，优先级高的在前
        hooks.sort(Comparator.comparingInt(AdvancedPricesHook::getPriority).reversed());
    }
    
    public void clearHooks() {
        hooks.clear();
    }
    
    public List<AdvancedPricesHook> getHooks() {
        return new ArrayList<>(hooks);
    }

    @Override
    public void setup() {
        // 由各个子 hook 自己 setup
    }

    @Override
    public double getPrice(ItemStack it) {
        for (AdvancedPricesHook hook : hooks) {
            if (!hook.isAvailable()) continue;
            
            double price = hook.getPrice(it);
            if (price > 0) {
                return price;
            }
        }
        return 0;
    }

    @Override
    public double getPrice(Player player, ItemStack it) {
        double maxPrice = 0;
        AdvancedPricesHook bestHook = null;
        
        // 找到价格最高的可用提供商
        for (AdvancedPricesHook hook : hooks) {
            if (!hook.isAvailable()) continue;
            
            SellResult result = hook.canSell(player, it);
            if (result.canSell() && result.getPrice() > maxPrice) {
                maxPrice = result.getPrice();
                bestHook = hook;
            }
        }
        
        return maxPrice;
    }

    @Override
    @NotNull
    public SellResult canSell(Player player, ItemStack it) {
        double maxPrice = 0;
        SellResult bestResult = SellResult.noPrice();
        
        // 找到价格最高的可用提供商
        for (AdvancedPricesHook hook : hooks) {
            if (!hook.isAvailable()) continue;
            
            SellResult result = hook.canSell(player, it);
            if (result.canSell() && result.getPrice() > maxPrice) {
                maxPrice = result.getPrice();
                bestResult = result;
            }
        }
        
        return bestResult;
    }

    @Override
    public boolean confirmSell(Player player, ItemStack it, double price) {
        // 找到对应价格的提供商并确认出售
        for (AdvancedPricesHook hook : hooks) {
            if (!hook.isAvailable()) continue;
            
            SellResult result = hook.canSell(player, it);
            if (result.canSell() && Math.abs(result.getPrice() - price) < 0.01) {
                return hook.confirmSell(player, it, price);
            }
        }
        return false;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean isAvailable() {
        return hooks.stream().anyMatch(AdvancedPricesHook::isAvailable);
    }
    
    /**
     * 获取用于出售指定物品的最佳提供商
     * 
     * @param player 玩家
     * @param it 物品
     * @return 最佳提供商，如果没有则返回 null
     */
    public AdvancedPricesHook getBestHookFor(Player player, ItemStack it) {
        double maxPrice = 0;
        AdvancedPricesHook bestHook = null;
        
        for (AdvancedPricesHook hook : hooks) {
            if (!hook.isAvailable()) continue;
            
            SellResult result = hook.canSell(player, it);
            if (result.canSell() && result.getPrice() > maxPrice) {
                maxPrice = result.getPrice();
                bestHook = hook;
            }
        }
        
        return bestHook;
    }
}
