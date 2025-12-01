package com.artillexstudios.axsellwands.listeners;

import com.artillexstudios.axapi.items.NBTWrapper;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axsellwands.chargers.Charger;
import com.artillexstudios.axsellwands.chargers.Chargers;
import com.artillexstudios.axsellwands.chargers.Dischargers;
import com.artillexstudios.axsellwands.sellwands.Sellwand;
import com.artillexstudios.axsellwands.sellwands.Sellwands;
import com.artillexstudios.axsellwands.utils.NumberUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static com.artillexstudios.axsellwands.AxSellwands.CONFIG;
import static com.artillexstudios.axsellwands.AxSellwands.LANG;
import static com.artillexstudios.axsellwands.AxSellwands.MESSAGEUTILS;

import org.bukkit.Bukkit;

public class ChargerUseListener implements Listener {

    @EventHandler
    public void onChargerUse(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        NBTWrapper wrapper = new NBTWrapper(item);
        String chargerType = wrapper.getString("axsellwands-charger-type");
        if (chargerType == null) return;

        event.setCancelled(true);

        Charger charger = Chargers.getChargers().get(chargerType);
        if (charger == null) {
            MESSAGEUTILS.sendLang(player, "charger.config-error");
            return;
        }

        // 检查副手是否有魔杖
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand == null || offHand.getType() == Material.AIR) {
            MESSAGEUTILS.sendLang(player, "charger.no-sellwand");
            return;
        }

        NBTWrapper sellwandWrapper = new NBTWrapper(offHand);
        String sellwandType = sellwandWrapper.getString("axsellwands-type");
        if (sellwandType == null) {
            MESSAGEUTILS.sendLang(player, "charger.no-sellwand");
            return;
        }

        Sellwand sellwand = Sellwands.getSellwands().get(sellwandType);
        if (sellwand == null) {
            MESSAGEUTILS.sendLang(player, "charger.sellwand-config-error");
            return;
        }

        // 检查魔杖是否绑定到其他玩家
        String boundPlayer = sellwandWrapper.getString("axsellwands-bound-player");
        if (boundPlayer != null && !boundPlayer.isEmpty() && !boundPlayer.equals(player.getName())) {
            MESSAGEUTILS.sendLang(player, "charger.not-bound-to-you");
            return;
        }

        int currentUses = sellwandWrapper.getIntOr("axsellwands-uses", -1);
        int maxUses = sellwandWrapper.getIntOr("axsellwands-max-uses", -1);
        float multiplier = sellwandWrapper.getFloatOr("axsellwands-multiplier", 1.0f);

        // 检查是否可以充电
        if (!charger.canCharge(sellwandType, multiplier, currentUses, maxUses)) {
            MESSAGEUTILS.sendLang(player, "charger.cannot-charge");
            return;
        }

        // 检查是否已满
        if (maxUses != -1 && currentUses >= maxUses) {
            MESSAGEUTILS.sendLang(player, "charger.already-full");
            return;
        }

        // 计算需要充电的次数（只充到满为止）
        int neededUses = maxUses - currentUses;
        int actualChargeUses = Math.min(neededUses, charger.getUsesPerCharge());
        int newUses = currentUses + actualChargeUses;

        // 更新魔杖
        sellwandWrapper.set("axsellwands-uses", newUses);
        
        // 更新魔杖显示
        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("%multiplier%", "" + multiplier);
        replacements.put("%uses%", "" + (newUses == -1 ? LANG.getString("unlimited", "∞") : newUses));
        replacements.put("%max-uses%", "" + (maxUses == -1 ? LANG.getString("unlimited", "∞") : maxUses));
        
        int soldAmount = sellwandWrapper.getIntOr("axsellwands-sold-amount", 0);
        double soldPrice = sellwandWrapper.getDoubleOr("axsellwands-sold-price", 0);
        replacements.put("%sold-amount%", "" + soldAmount);
        replacements.put("%sold-price%", NumberUtils.formatNumber(soldPrice));
        
        // 添加绑定者变量（使用之前检查时获取的boundPlayer变量）
        if (boundPlayer != null && !boundPlayer.isEmpty()) {
            replacements.put("%bound-player%", boundPlayer);
        } else {
            replacements.put("%bound-player%", LANG.getString("not-bound", "未绑定"));
        }

        ItemBuilder builder = ItemBuilder.create(sellwand.getItemSection(), replacements);
        offHand.setItemMeta(builder.get().getItemMeta());
        
        sellwandWrapper.build();

        // 更新充电器电量（消耗实际充入的电量）
        int currentPower = wrapper.getIntOr("axsellwands-charger-power", charger.getTotalPower());
        int newPower = currentPower - actualChargeUses;
        
        // 发送消息
        HashMap<String, String> msgReplacements = new HashMap<>();
        msgReplacements.put("%uses%", "" + actualChargeUses);
        msgReplacements.put("%total%", "" + newUses);
        
        if (newPower <= 0) {
            // 充电器电量耗尽，移除物品
            msgReplacements.put("%power%", "0");
            MESSAGEUTILS.sendLang(player, "charger.success", msgReplacements);
            MESSAGEUTILS.sendLang(player, "charger.depleted");
            item.setAmount(item.getAmount() - 1);
        } else {
            // 更新充电器剩余电量
            wrapper.set("axsellwands-charger-power", newPower);
            
            // 更新充电器显示
            HashMap<String, String> chargerReplacements = new HashMap<>();
            chargerReplacements.put("%power%", "" + newPower);
            chargerReplacements.put("%total-power%", "" + charger.getTotalPower());
            
            ItemBuilder chargerBuilder = ItemBuilder.create(charger.getItemSection(), chargerReplacements);
            item.setItemMeta(chargerBuilder.get().getItemMeta());
            
            wrapper.build();
            
            // 添加剩余电量到消息
            msgReplacements.put("%power%", "" + newPower);
            MESSAGEUTILS.sendLang(player, "charger.success", msgReplacements);
        }
    }

    @EventHandler
    public void onDischargerUse(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        NBTWrapper wrapper = new NBTWrapper(item);
        String dischargerType = wrapper.getString("axsellwands-discharger-type");
        if (dischargerType == null) return;

        boolean debug = CONFIG.getBoolean("debug", false);
        if (debug) {
            Bukkit.getConsoleSender().sendMessage("§e[AxSellwands Debug] §7玩家 §f" + player.getName() + "§7 使用放电器: §f" + dischargerType);
        }

        event.setCancelled(true);

        Charger discharger = Dischargers.getDischargers().get(dischargerType);
        if (discharger == null) {
            if (debug) {
                Bukkit.getConsoleSender().sendMessage("§c[AxSellwands Debug] §7放电器类型不存在: §f" + dischargerType);
            }
            MESSAGEUTILS.sendLang(player, "discharger.config-error");
            return;
        }

        // 检查副手是否有魔杖
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand == null || offHand.getType() == Material.AIR) {
            if (debug) {
                Bukkit.getConsoleSender().sendMessage("§c[AxSellwands Debug] §7副手没有物品");
            }
            MESSAGEUTILS.sendLang(player, "discharger.no-sellwand");
            return;
        }

        NBTWrapper sellwandWrapper = new NBTWrapper(offHand);
        String sellwandType = sellwandWrapper.getString("axsellwands-type");
        if (sellwandType == null) {
            if (debug) {
                Bukkit.getConsoleSender().sendMessage("§c[AxSellwands Debug] §7副手物品不是魔杖（没有axsellwands-type标签）");
            }
            MESSAGEUTILS.sendLang(player, "discharger.no-sellwand");
            return;
        }

        Sellwand sellwand = Sellwands.getSellwands().get(sellwandType);
        if (sellwand == null) {
            if (debug) {
                Bukkit.getConsoleSender().sendMessage("§c[AxSellwands Debug] §7魔杖类型不存在: §f" + sellwandType);
            }
            MESSAGEUTILS.sendLang(player, "discharger.sellwand-config-error");
            return;
        }

        // 检查魔杖是否绑定到其他玩家
        String boundPlayer = sellwandWrapper.getString("axsellwands-bound-player");
        if (boundPlayer != null && !boundPlayer.isEmpty() && !boundPlayer.equals(player.getName())) {
            if (debug) {
                Bukkit.getConsoleSender().sendMessage("§c[AxSellwands Debug] §7魔杖绑定到其他玩家: §f" + boundPlayer);
            }
            MESSAGEUTILS.sendLang(player, "discharger.not-bound-to-you");
            return;
        }

        int currentUses = sellwandWrapper.getIntOr("axsellwands-uses", -1);
        float multiplier = sellwandWrapper.getFloatOr("axsellwands-multiplier", 1.0f);

        if (debug) {
            Bukkit.getConsoleSender().sendMessage("§e[AxSellwands Debug] §7魔杖信息:");
            Bukkit.getConsoleSender().sendMessage("§e  §7类型: §f" + sellwandType);
            Bukkit.getConsoleSender().sendMessage("§e  §7当前使用次数: §f" + (currentUses == -1 ? "无限" : currentUses));
            Bukkit.getConsoleSender().sendMessage("§e  §7倍率: §f" + multiplier);
        }

        // 检查是否允许提取无限魔杖
        if (currentUses == -1 && !discharger.isAllowUnlimited()) {
            if (debug) {
                Bukkit.getConsoleSender().sendMessage("§c[AxSellwands Debug] §7不允许从无限魔杖提取");
            }
            MESSAGEUTILS.sendLang(player, "discharger.unlimited-wand");
            return;
        }

        // 检查魔杖类型限制（使用专门的canExtract方法，不检查使用次数是否已满）
        if (debug) {
            Bukkit.getConsoleSender().sendMessage("§e[AxSellwands Debug] §7检查限制条件:");
            Bukkit.getConsoleSender().sendMessage("§e  §7魔杖类型限制: §f" + (discharger.getSellwandLimits().isEmpty() ? "无限制" : discharger.getSellwandLimits().keySet()));
            Bukkit.getConsoleSender().sendMessage("§e  §7倍率限制: §f" + (discharger.getMultiplierLimits().isEmpty() ? "无限制" : discharger.getMultiplierLimits().keySet()));
        }
        
        if (!discharger.canExtract(sellwandType, multiplier)) {
            if (debug) {
                Bukkit.getConsoleSender().sendMessage("§c[AxSellwands Debug] §7魔杖不符合放电器限制条件");
            }
            MESSAGEUTILS.sendLang(player, "discharger.cannot-extract");
            return;
        }

        int extractAmount = discharger.getExtractAmount();
        double extractTax = discharger.getExtractTax();
        
        // 计算实际消耗（提取量 + 税）
        int actualCost = (int) Math.ceil(extractAmount * (1 + extractTax));

        if (debug) {
            Bukkit.getConsoleSender().sendMessage("§e[AxSellwands Debug] §7提取信息:");
            Bukkit.getConsoleSender().sendMessage("§e  §7提取量: §f" + extractAmount);
            Bukkit.getConsoleSender().sendMessage("§e  §7税率: §f" + (extractTax * 100) + "%");
            Bukkit.getConsoleSender().sendMessage("§e  §7实际消耗: §f" + actualCost);
        }

        // 检查是否有足够的使用次数
        if (currentUses != -1 && currentUses < actualCost) {
            if (debug) {
                Bukkit.getConsoleSender().sendMessage("§c[AxSellwands Debug] §7魔杖使用次数不足: §f" + currentUses + " < " + actualCost);
            }
            HashMap<String, String> errorReplacements = new HashMap<>();
            errorReplacements.put("%cost%", "" + actualCost);
            MESSAGEUTILS.sendLang(player, "discharger.not-enough-uses", errorReplacements);
            return;
        }

        // 扣除使用次数
        int newUses = currentUses == -1 ? -1 : currentUses - actualCost;
        sellwandWrapper.set("axsellwands-uses", newUses);

        // 更新魔杖显示
        int maxUses = sellwandWrapper.getIntOr("axsellwands-max-uses", -1);
        
        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("%multiplier%", "" + multiplier);
        replacements.put("%uses%", "" + newUses);
        replacements.put("%max-uses%", "" + (maxUses == -1 ? LANG.getString("unlimited", "∞") : maxUses));
        
        int soldAmount = sellwandWrapper.getIntOr("axsellwands-sold-amount", 0);
        double soldPrice = sellwandWrapper.getDoubleOr("axsellwands-sold-price", 0);
        replacements.put("%sold-amount%", "" + soldAmount);
        replacements.put("%sold-price%", NumberUtils.formatNumber(soldPrice));
        
        // 添加绑定者变量（使用之前检查时获取的boundPlayer变量）
        if (boundPlayer != null && !boundPlayer.isEmpty()) {
            replacements.put("%bound-player%", boundPlayer);
        } else {
            replacements.put("%bound-player%", LANG.getString("not-bound", "未绑定"));
        }

        ItemBuilder builder = ItemBuilder.create(sellwand.getItemSection(), replacements);
        offHand.setItemMeta(builder.get().getItemMeta());
        
        sellwandWrapper.build();

        // 给予充电器（使用放电器配置的充电器类型）
        String chargerToGive = discharger.getChargerToGive();
        Charger charger = Chargers.getChargers().get(chargerToGive);
        if (debug) {
            Bukkit.getConsoleSender().sendMessage("§e[AxSellwands Debug] §7给予充电器: §f" + chargerToGive + " (" + (charger != null ? "找到" : "未找到") + ")");
        }
        if (charger == null) {
            HashMap<String, String> errorReplacements = new HashMap<>();
            errorReplacements.put("%charger%", chargerToGive);
            MESSAGEUTILS.sendLang(player, "discharger.charger-config-error", errorReplacements);
            if (debug) {
                Bukkit.getConsoleSender().sendMessage("§c[AxSellwands Debug] §7充电器类型不存在: §f" + chargerToGive);
            }
        } else {
            // 添加电量变量
            HashMap<String, String> chargerReplacements = new HashMap<>();
            chargerReplacements.put("%power%", "" + extractAmount);
            chargerReplacements.put("%total-power%", "" + charger.getTotalPower());
            
            ItemBuilder chargerBuilder = ItemBuilder.create(charger.getItemSection(), chargerReplacements);
            ItemStack chargerItem = chargerBuilder.get();
            
            NBTWrapper chargerWrapper = new NBTWrapper(chargerItem);
            // 为充电器设置唯一UUID，防止堆叠
            chargerWrapper.set("axsellwands-uuid", java.util.UUID.randomUUID());
            chargerWrapper.set("axsellwands-charger-type", chargerToGive);
            chargerWrapper.set("axsellwands-charger-power", extractAmount);
            chargerWrapper.build();
            
            player.getInventory().addItem(chargerItem);
        }

        // 更新放电器使用次数
        int currentDischargerUses = wrapper.getIntOr("axsellwands-discharger-uses", discharger.getDischargerUses());
        int newDischargerUses = currentDischargerUses - 1;

        if (debug) {
            Bukkit.getConsoleSender().sendMessage("§e[AxSellwands Debug] §7放电器使用次数: §f" + currentDischargerUses + " -> " + newDischargerUses);
        }
        
        // 发送消息
        HashMap<String, String> msgReplacements = new HashMap<>();
        msgReplacements.put("%extract%", "" + extractAmount);
        msgReplacements.put("%cost%", "" + actualCost);
        msgReplacements.put("%tax%", "" + (int)(extractTax * 100));
        
        if (newDischargerUses <= 0) {
            // 放电器使用次数耗尽，移除物品
            if (debug) {
                Bukkit.getConsoleSender().sendMessage("§e[AxSellwands Debug] §7放电器使用次数耗尽，已销毁");
            }
            msgReplacements.put("%remaining%", "0");
            MESSAGEUTILS.sendLang(player, "discharger.success", msgReplacements);
            MESSAGEUTILS.sendLang(player, "discharger.depleted");
            item.setAmount(item.getAmount() - 1);
        } else {
            // 更新放电器剩余使用次数
            wrapper.set("axsellwands-discharger-uses", newDischargerUses);
            
            // 更新放电器显示
            HashMap<String, String> dischargerReplacements = new HashMap<>();
            dischargerReplacements.put("%uses%", "" + newDischargerUses);
            dischargerReplacements.put("%max-uses%", "" + discharger.getDischargerUses());
            
            ItemBuilder dischargerBuilder = ItemBuilder.create(discharger.getItemSection(), dischargerReplacements);
            item.setItemMeta(dischargerBuilder.get().getItemMeta());
            
            wrapper.build();
            
            // 添加剩余次数到消息
            msgReplacements.put("%remaining%", "" + newDischargerUses);
            MESSAGEUTILS.sendLang(player, "discharger.success", msgReplacements);
        }

        if (debug) {
            Bukkit.getConsoleSender().sendMessage("§a[AxSellwands Debug] §7放电器使用成功！");
        }
    }
}
