package com.artillexstudios.axsellwands.commands.subcommands;

import com.artillexstudios.axapi.items.NBTWrapper;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axsellwands.sellwands.Sellwand;
import com.artillexstudios.axsellwands.sellwands.Sellwands;
import com.artillexstudios.axsellwands.utils.NumberUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

import static com.artillexstudios.axsellwands.AxSellwands.LANG;
import static com.artillexstudios.axsellwands.AxSellwands.MESSAGEUTILS;

public enum Bind {
    INSTANCE;

    public void execute(Player player) {
        // 获取主手物品
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            MESSAGEUTILS.sendLang(player, "bind.no-item");
            return;
        }

        // 检查是否是魔杖
        NBTWrapper wrapper = new NBTWrapper(item);
        String sellwandType = wrapper.getString("axsellwands-type");
        if (sellwandType == null) {
            MESSAGEUTILS.sendLang(player, "bind.not-sellwand");
            return;
        }

        // 获取魔杖配置
        Sellwand sellwand = Sellwands.getSellwands().get(sellwandType);
        if (sellwand == null) {
            MESSAGEUTILS.sendLang(player, "bind.config-error");
            return;
        }

        // 检查魔杖是否支持绑定
        if (!sellwand.isBindable()) {
            MESSAGEUTILS.sendLang(player, "bind.not-bindable");
            return;
        }

        // 检查是否已经绑定
        String boundPlayer = wrapper.getString("axsellwands-bound-player");
        if (boundPlayer != null && !boundPlayer.isEmpty()) {
            HashMap<String, String> replacements = new HashMap<>();
            replacements.put("%bound-player%", boundPlayer);
            MESSAGEUTILS.sendLang(player, "bind.already-bound", replacements);
            return;
        }

        // 绑定到玩家
        wrapper.set("axsellwands-bound-player", player.getName());

        // 更新物品显示
        float multiplier = wrapper.getFloatOr("axsellwands-multiplier", 1.0f);
        int uses = wrapper.getIntOr("axsellwands-uses", -1);
        int maxUses = wrapper.getIntOr("axsellwands-max-uses", -1);
        int soldAmount = wrapper.getIntOr("axsellwands-sold-amount", 0);
        double soldPrice = wrapper.getDoubleOr("axsellwands-sold-price", 0);

        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("%multiplier%", "" + multiplier);
        replacements.put("%uses%", "" + (uses == -1 ? LANG.getString("unlimited", "∞") : uses));
        replacements.put("%max-uses%", "" + (maxUses == -1 ? LANG.getString("unlimited", "∞") : maxUses));
        replacements.put("%sold-amount%", "" + soldAmount);
        replacements.put("%sold-price%", NumberUtils.formatNumber(soldPrice));
        replacements.put("%bound-player%", player.getName());

        ItemBuilder builder = ItemBuilder.create(sellwand.getItemSection(), replacements);
        item.setItemMeta(builder.get().getItemMeta());

        wrapper.build();

        // 发送成功消息
        replacements.put("%sellwand%", sellwand.getName());
        MESSAGEUTILS.sendLang(player, "bind.success", replacements);
    }
}
