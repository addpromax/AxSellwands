package com.artillexstudios.axsellwands.hooks.shop;

import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.mechanic.context.Context;
import net.momirealms.customfishing.api.mechanic.market.MarketManager;
import net.momirealms.customfishing.api.storage.user.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * CustomFishing 价格钩子
 * 支持每日出售限额
 */
public class CustomFishingHook implements AdvancedPricesHook {
    
    private MarketManager marketManager;
    private boolean available = false;

    @Override
    public void setup() {
        try {
            BukkitCustomFishingPlugin plugin = BukkitCustomFishingPlugin.getInstance();
            if (plugin != null) {
                this.marketManager = plugin.getMarketManager();
                this.available = true;
            }
        } catch (Exception e) {
            this.available = false;
        }
    }

    @Override
    public double getPrice(ItemStack it) {
        if (!available || marketManager == null) return 0;
        
        // CustomFishing 需要玩家上下文，这里返回 0
        return 0;
    }

    @Override
    public double getPrice(Player player, ItemStack it) {
        if (!available || marketManager == null) return 0;
        
        try {
            Context<Player> context = Context.player(player);
            double price = marketManager.getItemPrice(context, it);
            
            if (price > 0) {
                // 应用收益倍数
                price *= marketManager.earningsMultiplier(context);
            }
            
            return price;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    @NotNull
    public SellResult canSell(Player player, ItemStack it) {
        if (!available || marketManager == null) {
            return SellResult.noPrice();
        }
        
        try {
            Context<Player> context = Context.player(player);
            double price = marketManager.getItemPrice(context, it);
            
            if (price <= 0) {
                return SellResult.noPrice();
            }
            
            // 应用收益倍数
            price *= marketManager.earningsMultiplier(context);
            
            // 检查每日限额
            double earningLimit = marketManager.earningLimit(context);
            if (earningLimit > 0) {
                Optional<UserData> optionalUserData = BukkitCustomFishingPlugin.getInstance()
                    .getStorageManager()
                    .getOnlineUser(player.getUniqueId());
                
                if (optionalUserData.isPresent()) {
                    UserData userData = optionalUserData.get();
                    userData.earningData().refresh();
                    double currentEarnings = userData.earningData().earnings;
                    double remaining = earningLimit - currentEarnings;
                    
                    if (remaining <= 0) {
                        return SellResult.failure("daily_limit_reached");
                    }
                    
                    // 如果超过剩余额度，调整价格
                    if (price > remaining) {
                        price = remaining;
                    }
                }
            }
            
            return SellResult.success(price);
        } catch (Exception e) {
            return SellResult.noPrice();
        }
    }

    @Override
    public boolean confirmSell(Player player, ItemStack it, double price) {
        if (!available || marketManager == null) {
            return false;
        }
        
        try {
            Optional<UserData> optionalUserData = BukkitCustomFishingPlugin.getInstance()
                .getStorageManager()
                .getOnlineUser(player.getUniqueId());
            
            if (optionalUserData.isPresent()) {
                UserData userData = optionalUserData.get();
                userData.earningData().refresh();
                userData.earningData().earnings += price;
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getPriority() {
        // CustomFishing 优先级最高
        return 100;
    }

    @Override
    public boolean isAvailable() {
        return available && marketManager != null;
    }
}
