package com.artillexstudios.axsellwands.chargers;

/**
 * 放电器类型包装类，用于命令参数区分
 */
public class DischargerType {
    private final Charger charger;

    public DischargerType(Charger charger) {
        this.charger = charger;
    }

    public Charger getCharger() {
        return charger;
    }
}
