package com.artillexstudios.axsellwands.commands.subcommands;

import com.artillexstudios.axapi.items.NBTWrapper;
import com.artillexstudios.axapi.utils.ContainerUtils;
import com.artillexstudios.axapi.utils.ItemBuilder;
import com.artillexstudios.axsellwands.chargers.Charger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.artillexstudios.axsellwands.AxSellwands.MESSAGEUTILS;

public enum GiveCharger {
    INSTANCE;

    public void execute(CommandSender sender, Player player, @NotNull Charger charger, @Nullable Integer amount, boolean isDischarger) {
        // 添加变量
        HashMap<String, String> chargerReplacements = new HashMap<>();
        if (isDischarger) {
            chargerReplacements.put("%uses%", "" + charger.getDischargerUses());
            chargerReplacements.put("%max-uses%", "" + charger.getDischargerUses());
        } else {
            chargerReplacements.put("%power%", "" + charger.getTotalPower());
            chargerReplacements.put("%total-power%", "" + charger.getTotalPower());
        }
        
        ItemBuilder builder = ItemBuilder.create(charger.getItemSection(), chargerReplacements);
        ItemStack it = builder.get();

        NBTWrapper wrapper = new NBTWrapper(it);
        
        if (isDischarger) {
            wrapper.set("axsellwands-discharger-type", charger.getId());
            wrapper.set("axsellwands-discharger-uses", charger.getDischargerUses());
        } else {
            wrapper.set("axsellwands-charger-type", charger.getId());
            wrapper.set("axsellwands-charger-power", charger.getTotalPower());
        }

        int am = 1;
        if (amount != null) am = amount;

        for (int i = 0; i < am; i++) {
            // 为每个物品设置唯一UUID，防止堆叠
            wrapper.set("axsellwands-uuid", UUID.randomUUID());
            wrapper.build();
            ContainerUtils.INSTANCE.addOrDrop(player.getInventory(), List.of(it.clone()), player.getLocation());
        }

        Map<String, String> replacements = new HashMap<>();
        replacements.put("%amount%", "" + am);
        replacements.put("%charger%", charger.getName());
        replacements.put("%player%", player.getName());

        MESSAGEUTILS.sendLang(sender, "charger-give", replacements);
        MESSAGEUTILS.sendLang(player, "charger-got", replacements);
    }
}
