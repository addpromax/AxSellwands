package com.artillexstudios.axsellwands.chargers;

import com.artillexstudios.axapi.config.Config;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.HashMap;

public class Dischargers {
    private static final HashMap<String, Charger> dischargers = new HashMap<>();

    public static void reload() {
        dischargers.clear();
        File folder = new File("plugins/AxSellwands/dischargers/");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (File file : folder.listFiles()) {
            if (!file.getName().endsWith(".yml")) continue;

            String name = file.getName().replace(".yml", "");
            Config config = new Config(file);
            Charger discharger = new Charger(name, config);
            dischargers.put(name, discharger);
            Bukkit.getConsoleSender().sendMessage("§a[AxSellwands] Loaded discharger: " + name);
        }
    }

    public static HashMap<String, Charger> getDischargers() {
        return dischargers;
    }
}
