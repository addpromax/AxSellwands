package com.artillexstudios.axsellwands.chargers;

import com.artillexstudios.axapi.config.Config;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.HashMap;

public class Chargers {
    private static final HashMap<String, Charger> chargers = new HashMap<>();

    public static void reload() {
        chargers.clear();
        File folder = new File("plugins/AxSellwands/chargers/");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        for (File file : folder.listFiles()) {
            if (!file.getName().endsWith(".yml")) continue;

            String name = file.getName().replace(".yml", "");
            Config config = new Config(file);
            Charger charger = new Charger(name, config);
            chargers.put(name, charger);
            Bukkit.getConsoleSender().sendMessage("§a[AxSellwands] Loaded charger: " + name);
        }
    }

    public static HashMap<String, Charger> getChargers() {
        return chargers;
    }
}
