package com.artillexstudios.axsellwands.commands;

import com.artillexstudios.axsellwands.chargers.Charger;
import com.artillexstudios.axsellwands.chargers.DischargerType;
import com.artillexstudios.axsellwands.commands.subcommands.Bind;
import com.artillexstudios.axsellwands.commands.subcommands.Give;
import com.artillexstudios.axsellwands.commands.subcommands.GiveCharger;
import com.artillexstudios.axsellwands.commands.subcommands.Help;
import com.artillexstudios.axsellwands.commands.subcommands.Reload;
import com.artillexstudios.axsellwands.sellwands.Sellwand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Range;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"axsellwands", "axsellwand", "sellwands", "sellwand"})
@CommandPermission("axsellwands.admin")
public class Commands {

    @DefaultFor({"~", "~ help"})
    public void help(@NotNull CommandSender sender) {
        Help.INSTANCE.execute(sender);
    }

    @Subcommand("give")
    public void give(@NotNull CommandSender sender, Player player, @NotNull Sellwand sellwand, @Optional @Range(min = 1, max = 64) Integer amount, @Optional Player boundPlayer) {
        Give.INSTANCE.execute(sender, player, sellwand, amount, boundPlayer);
    }

    @Subcommand("givecharger")
    public void giveCharger(@NotNull CommandSender sender, Player player, @NotNull Charger charger, @Optional @Range(min = 1, max = 64) Integer amount) {
        GiveCharger.INSTANCE.execute(sender, player, charger, amount, false);
    }

    @Subcommand("givedischarger")
    public void giveDischarger(@NotNull CommandSender sender, Player player, @NotNull DischargerType dischargerType, @Optional @Range(min = 1, max = 64) Integer amount) {
        GiveCharger.INSTANCE.execute(sender, player, dischargerType.getCharger(), amount, true);
    }

    @Subcommand("reload")
    public void reload(@NotNull CommandSender sender) {
        Reload.INSTANCE.execute(sender);
    }

    @Subcommand("bind")
    public void bind(@NotNull Player player) {
        Bind.INSTANCE.execute(player);
    }
}