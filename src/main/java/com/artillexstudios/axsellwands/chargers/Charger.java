package com.artillexstudios.axsellwands.chargers;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Charger {
    private final String id;
    private final Config file;
    private final String name;
    private final int usesPerCharge;
    private final int totalPower;
    private final Section itemSection;
    private final Map<String, Integer> sellwandLimits = new HashMap<>();
    private final Map<Float, Integer> multiplierLimits = new HashMap<>();
    
    // 放电器专用配置
    private final int extractAmount;
    private final double extractTax;
    private final String chargerToGive;
    private final boolean allowUnlimited;
    private final int dischargerUses;

    public Charger(String id, @NotNull Config file) {
        this.id = id;
        this.file = file;
        this.name = file.getString("name", "Charger");
        this.usesPerCharge = file.getInt("uses-per-charge", 10);
        this.totalPower = file.getInt("total-power", 100);
        this.itemSection = file.getSection("item");
        
        // 放电器配置
        this.extractAmount = file.getInt("extract-amount", 10);
        this.extractTax = file.getDouble("extract-tax", 0.1);
        this.chargerToGive = file.getString("charger-to-give", "basic-charger");
        this.allowUnlimited = file.getBoolean("allow-unlimited", false);
        this.dischargerUses = file.getInt("discharger-uses", 10);

        // 加载魔杖类型限制
        Section sellwandSection = file.getSection("sellwand-limits");
        if (sellwandSection != null) {
            for (String key : sellwandSection.getRoutesAsStrings(false)) {
                sellwandLimits.put(key, sellwandSection.getInt(key));
            }
        }

        // 加载倍率限制
        Section multiplierSection = file.getSection("multiplier-limits");
        if (multiplierSection != null) {
            for (String key : multiplierSection.getRoutesAsStrings(false)) {
                try {
                    float multiplier = Float.parseFloat(key);
                    multiplierLimits.put(multiplier, multiplierSection.getInt(key));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public Config getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public int getUsesPerCharge() {
        return usesPerCharge;
    }

    public int getTotalPower() {
        return totalPower;
    }

    public Section getItemSection() {
        return itemSection;
    }

    public Map<String, Integer> getSellwandLimits() {
        return sellwandLimits;
    }

    public Map<Float, Integer> getMultiplierLimits() {
        return multiplierLimits;
    }

    public int getExtractAmount() {
        return extractAmount;
    }

    public double getExtractTax() {
        return extractTax;
    }

    public String getChargerToGive() {
        return chargerToGive;
    }

    public boolean isAllowUnlimited() {
        return allowUnlimited;
    }

    public int getDischargerUses() {
        return dischargerUses;
    }

    /**
     * 检查是否可以为指定魔杖充电
     */
    public boolean canCharge(String sellwandType, float multiplier, int currentUses, int maxUses) {
        // 检查魔杖类型限制
        if (!sellwandLimits.isEmpty() && !sellwandLimits.containsKey(sellwandType)) {
            return false;
        }

        // 检查倍率限制
        if (!multiplierLimits.isEmpty()) {
            boolean foundMatch = false;
            for (Float limitMultiplier : multiplierLimits.keySet()) {
                if (Math.abs(multiplier - limitMultiplier) < 0.01f) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                return false;
            }
        }

        // 检查是否已达到最大使用次数
        if (maxUses != -1 && currentUses >= maxUses) {
            return false;
        }

        return true;
    }

    /**
     * 获取充电后的使用次数
     */
    public int getChargedUses(int currentUses, int maxUses) {
        int newUses = currentUses + usesPerCharge;
        if (maxUses != -1 && newUses > maxUses) {
            return maxUses;
        }
        return newUses;
    }

    /**
     * 检查是否可以从指定魔杖提取使用次数（放电器专用）
     * 不检查当前使用次数是否已满，只检查类型和倍率限制
     */
    public boolean canExtract(String sellwandType, float multiplier) {
        // 检查魔杖类型限制
        if (!sellwandLimits.isEmpty() && !sellwandLimits.containsKey(sellwandType)) {
            return false;
        }

        // 检查倍率限制
        if (!multiplierLimits.isEmpty()) {
            boolean foundMatch = false;
            for (Float limitMultiplier : multiplierLimits.keySet()) {
                if (Math.abs(multiplier - limitMultiplier) < 0.01f) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                return false;
            }
        }

        return true;
    }
}
