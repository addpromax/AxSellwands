package com.artillexstudios.axsellwands.listeners;

import com.artillexstudios.axapi.items.NBTWrapper;
import com.artillexstudios.axapi.utils.ActionBar;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axapi.utils.Title;
import com.artillexstudios.axsellwands.api.events.AxSellwandsSellEvent;
import com.artillexstudios.axsellwands.hooks.HookManager;
import com.artillexstudios.axsellwands.hooks.container.ContainerHook;
import com.artillexstudios.axsellwands.hooks.shop.AdvancedPricesHook;
import com.artillexstudios.axsellwands.hooks.shop.PricesHook;
import com.artillexstudios.axsellwands.hooks.shop.SellResult;
import com.artillexstudios.axsellwands.sellwands.Sellwand;
import com.artillexstudios.axsellwands.sellwands.Sellwands;
import com.artillexstudios.axsellwands.utils.HistoryUtils;
import com.artillexstudios.axsellwands.utils.HologramUtils;
import com.artillexstudios.axsellwands.utils.NumberUtils;
import com.artillexstudios.axsellwands.utils.SellConfirmationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.artillexstudios.axsellwands.AxSellwands.CONFIG;
import static com.artillexstudios.axsellwands.AxSellwands.LANG;
import static com.artillexstudios.axsellwands.AxSellwands.MESSAGEUTILS;

public class SellwandUseListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        NBTWrapper wrapper = new NBTWrapper(event.getItem());
        String type = wrapper.getString("axsellwands-type");
        if (type == null) return;
        Sellwand sellwand = Sellwands.getSellwands().get(type);
        event.setCancelled(true);
        if (sellwand == null) return;
        Player player = event.getPlayer();

        ItemStack[] contents;
        ContainerHook containerHook = HookManager.getContainerAt(player, block);
        if (containerHook != null) {
            contents = containerHook.getItems(player, block).toArray(new ItemStack[0]);
        } else if (block.getState() instanceof Container) {
            contents = ((Container) block.getState()).getInventory().getContents();
        } else if (block.getType() == Material.ENDER_CHEST) {
            contents = player.getEnderChest().getContents();
        } else {
            return; // not a container
        }

        boolean hasBypass = player.hasPermission("axsellwands.admin");

        // 检查魔杖绑定
        if (sellwand.isBindable() && !hasBypass) {
            String boundPlayer = wrapper.getString("axsellwands-bound-player");
            if (boundPlayer != null && !boundPlayer.isEmpty() && !boundPlayer.equals(player.getName())) {
                MESSAGEUTILS.sendLang(player, "sellwand.not-bound-to-you");
                return;
            }
        }

        // 检查魔杖使用权限
        if (sellwand.isRequirePermission() && !hasBypass && !player.hasPermission(sellwand.getPermission())) {
            MESSAGEUTILS.sendLang(player, "no-sellwand-permission");
            return;
        }

        if (!hasBypass && !HookManager.canBuildAt(player, block.getLocation())) {
            MESSAGEUTILS.sendLang(player, "no-permission");
            return;
        }

        if (sellwand.getDisallowed().contains(block.getType()) || (!sellwand.getAllowed().isEmpty() && !sellwand.getAllowed().contains(block.getType()))) {
            MESSAGEUTILS.sendLang(player, "disallowed-container");
            return;
        }

        Long lastUsed = wrapper.getLong("axsellwands-lastused");
        if (lastUsed != null && System.currentTimeMillis() - lastUsed < sellwand.getCooldown() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            MESSAGEUTILS.sendLang(player, "cooldown", Collections.singletonMap("%time%", Long.toString(Math.round((sellwand.getCooldown() - System.currentTimeMillis() + lastUsed) / 1000D))));
            return;
        }

        UUID uuid = wrapper.getUUID("axsellwands-uuid");
        float multiplier = wrapper.getFloatOr("axsellwands-multiplier", 1);
        int uses = wrapper.getIntOr("axsellwands-uses", -1);
        int maxUses = wrapper.getIntOr("axsellwands-max-uses", -1);
        int soldAmount = wrapper.getIntOr("axsellwands-sold-amount", 0);
        double soldPrice = wrapper.getDoubleOr("axsellwands-sold-price", 0);

        int newSoldAmount = 0;
        double newSoldPrice = 0;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Map<Material, Integer> items = new HashMap<>();
            Map<ItemStack, Double> itemPrices = new HashMap<>();
            PricesHook pricesHook = HookManager.getShopPrices();
            boolean isAdvanced = pricesHook instanceof AdvancedPricesHook;
            
            // 第一遍：检查所有物品的价格和限制
            for (ItemStack it : contents) {
                if (it == null) continue;
                
                double price;
                if (isAdvanced) {
                    AdvancedPricesHook advancedHook = (AdvancedPricesHook) pricesHook;
                    SellResult result = advancedHook.canSell(player, it);
                    if (!result.canSell()) continue;
                    price = result.getPrice();
                } else {
                    price = pricesHook.getPrice(player, it);
                    if (price <= 0) continue;
                }
                
                price *= multiplier;
                itemPrices.put(it, price);
                
                newSoldPrice += price;
                newSoldAmount += it.getAmount();

                if (items.containsKey(it.getType()))
                    items.put(it.getType(), items.get(it.getType()) + it.getAmount());
                else
                    items.put(it.getType(), it.getAmount());
            }

            if (newSoldAmount <= 0 || newSoldPrice <= 0) {
                MESSAGEUTILS.sendLang(player, "nothing-sold");
                return;
            }
            
            // 二次确认功能
            boolean requireConfirmation = CONFIG.getBoolean("sell-confirmation.enabled", false);
            
            if (requireConfirmation) {
                // 检查是否有待确认的操作
                if (SellConfirmationManager.hasPendingConfirmation(player)) {
                    // 第二次点击，执行出售
                    SellConfirmationManager.ConfirmationData confirmData = SellConfirmationManager.confirmAndGet(player);
                    
                    if (confirmData == null) {
                        // 确认已超时
                        MESSAGEUTILS.sendLang(player, "sell-confirmation.timeout");
                        return;
                    }
                    
                    // 验证价格和数量是否一致（防止容器内容被修改）
                    if (Math.abs(confirmData.getTotalPrice() - newSoldPrice) > 0.01 || confirmData.getTotalAmount() != newSoldAmount) {
                        MESSAGEUTILS.sendLang(player, "sell-confirmation.changed");
                        return;
                    }
                    
                    // 继续执行出售逻辑
                } else {
                    // 第一次点击，创建确认请求
                    SellConfirmationManager.createConfirmation(player, newSoldPrice, newSoldAmount, items);
                    
                    // 发送确认消息
                    HashMap<String, String> confirmReplacements = new HashMap<>();
                    confirmReplacements.put("%amount%", "" + newSoldAmount);
                    confirmReplacements.put("%price%", NumberUtils.formatNumber(newSoldPrice));
                    confirmReplacements.put("%timeout%", "" + (SellConfirmationManager.getConfirmationTimeout() / 1000));
                    
                    // 发送多行确认消息
                    for (String line : LANG.getStringList("sell-confirmation.request")) {
                        player.sendMessage(StringUtils.formatToString(line, confirmReplacements));
                    }
                    
                    if (!LANG.getString("sounds.confirm-request", "").isEmpty()) {
                        player.playSound(player.getLocation(), Sound.valueOf(LANG.getString("sounds.confirm-request")), 1f, 1f);
                    }
                    
                    return;
                }
            }
            
            // 第二遍：确认出售并清空物品
            for (Map.Entry<ItemStack, Double> entry : itemPrices.entrySet()) {
                ItemStack it = entry.getKey();
                double price = entry.getValue();
                
                if (isAdvanced) {
                    AdvancedPricesHook advancedHook = (AdvancedPricesHook) pricesHook;
                    advancedHook.confirmSell(player, it, price);
                }
                
                it.setAmount(0);
            }

            AxSellwandsSellEvent apiEvent = new AxSellwandsSellEvent(player, newSoldPrice, newSoldAmount);
            Bukkit.getPluginManager().callEvent(apiEvent);
            if (apiEvent.isCancelled()) return;
            newSoldPrice = apiEvent.getMoneyMade();

            StringBuilder str = new StringBuilder("[");
            boolean first = true;
            for (Map.Entry<Material, Integer> e : items.entrySet()) {
                if (!first) str.append(", ");
                first = false;
                str.append(e.getValue()).append("x ").append(e.getKey().name());
            }
            str.append("]");
            HistoryUtils.writeToHistory(String.format("%s sold %dx items %s and earned %s (multiplier: %s, uses: %d)", player.getName(), newSoldAmount, str, newSoldPrice, multiplier, uses - 1));

            HashMap<String, String> replacements = new HashMap<>();
            replacements.put("%amount%", "" + newSoldAmount);
            replacements.put("%price%", NumberUtils.formatNumber(newSoldPrice));

            HookManager.getCurrency().giveBalance(player, newSoldPrice);

            if (CONFIG.getBoolean("hologram.enabled", true)) {
                HologramUtils.spawnHologram(player, block.getLocation().add(0.5, 0.5, 0.5), replacements);
            }

            MESSAGEUTILS.sendLang(player, "sell.chat", replacements);

            if (!LANG.getString("sell.actionbar", "").isBlank()) {
                ActionBar.create(StringUtils.format(LANG.getString("sell.actionbar"), replacements)).send(player);
            }

            if (LANG.getSection("sell.title") != null && !LANG.getString("sell.title.title").isBlank()) {
                Title.create(
                        StringUtils.format(LANG.getString("sell.title.title"), replacements),
                        StringUtils.format(LANG.getString("sell.title.subtitle"), replacements), 10, 40, 10
                ).send(player);
            }


            if (!LANG.getString("sounds.sell").isEmpty()) {
                player.playSound(player.getLocation(), Sound.valueOf(LANG.getString("sounds.sell")), 1f, 1f);
            }

            if (!LANG.getString("particles.sell").isEmpty()) {
                player.spawnParticle(Particle.valueOf(LANG.getString("particles.sell")), block.getLocation().add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5);
            }

            if (uses != -1) {
                uses--;

                if (uses < CONFIG.getInt("minimum-durability", 1)) {
                    // 检查是否需要充电而不是销毁
                    if (CONFIG.getBoolean("charger.require-recharge", false)) {
                        // 设置为0使用次数，需要充电
                        uses = 0;
                        MESSAGEUTILS.sendLang(player, "sellwand.requires-recharge");
                    } else {
                        // 销毁魔杖
                        event.getItem().setAmount(0);
                        return;
                    }
                }
            }

            replacements.clear();
            replacements.put("%multiplier%", "" + multiplier);
            replacements.put("%uses%", "" + (uses == -1 ? LANG.getString("unlimited", "∞") : uses));
            replacements.put("%max-uses%", "" + (maxUses == -1 ? LANG.getString("unlimited", "∞") : maxUses));
            replacements.put("%sold-amount%", "" + (soldAmount + newSoldAmount));
            replacements.put("%sold-price%", NumberUtils.formatNumber(soldPrice + newSoldPrice));
            
            // 添加绑定者变量
            String boundPlayer = wrapper.getString("axsellwands-bound-player");
            if (boundPlayer != null && !boundPlayer.isEmpty()) {
                replacements.put("%bound-player%", boundPlayer);
            } else {
                replacements.put("%bound-player%", LANG.getString("not-bound", "未绑定"));
            }

            Sellwand wand = Sellwands.getSellwands().get(type);
            ItemBuilder builder = ItemBuilder.create(wand.getItemSection(), replacements);

            event.getItem().setItemMeta(builder.get().getItemMeta());

            wrapper = new NBTWrapper(event.getItem());
            wrapper.set("axsellwands-uuid", uuid);
            wrapper.set("axsellwands-uses", uses);
            wrapper.set("axsellwands-lastused", System.currentTimeMillis());
            wrapper.set("axsellwands-sold-amount", soldAmount + newSoldAmount);
            wrapper.set("axsellwands-sold-price", soldPrice + newSoldPrice);
            wrapper.set("axsellwands-type", type);
            wrapper.set("axsellwands-multiplier", multiplier);
            wrapper.set("axsellwands-max-uses", maxUses);
            wrapper.build();

            if (block.getState() instanceof Container container) container.update();
        } else {
            PricesHook pricesHook = HookManager.getShopPrices();
            boolean isAdvanced = pricesHook instanceof AdvancedPricesHook;
            
            for (ItemStack it : contents) {
                if (it == null) continue;
                
                double price;
                if (isAdvanced) {
                    AdvancedPricesHook advancedHook = (AdvancedPricesHook) pricesHook;
                    SellResult result = advancedHook.canSell(player, it);
                    if (!result.canSell()) continue;
                    price = result.getPrice();
                } else {
                    price = pricesHook.getPrice(player, it);
                    if (price == -1.0D) continue;
                }
                
                price *= multiplier;

                newSoldPrice += price;
                newSoldAmount += it.getAmount();
            }

            if (newSoldAmount <= 0 || newSoldPrice <= 0) {
                MESSAGEUTILS.sendLang(player, "nothing-sold");
                return;
            }

            HashMap<String, String> replacements = new HashMap<>();
            replacements.put("%amount%", "" + newSoldAmount);
            replacements.put("%price%", NumberUtils.formatNumber(newSoldPrice));

            MESSAGEUTILS.sendLang(player, "inspect.chat", replacements);

            if (!LANG.getString("inspect.actionbar", "").isBlank()) {
                ActionBar.create(StringUtils.format(LANG.getString("inspect.actionbar"), replacements)).send(player);
            }

            if (LANG.getSection("inspect.title") != null && !LANG.getString("inspect.title.title").isBlank())
                Title.create(StringUtils.format(LANG.getString("inspect.title.title"), replacements),
                        StringUtils.format(LANG.getString("inspect.title.subtitle"), replacements), 10, 40, 10).send(player);

            if (!LANG.getString("sounds.inspect").isEmpty()) {
                player.playSound(player.getLocation(), Sound.valueOf(LANG.getString("sounds.inspect")), 1f, 1f);
            }

            if (!LANG.getString("particles.inspect").isEmpty()) {
                player.spawnParticle(Particle.valueOf(LANG.getString("particles.inspect")), block.getLocation().add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5);
            }
        }
    }
}
