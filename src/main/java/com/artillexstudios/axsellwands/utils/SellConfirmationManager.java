package com.artillexstudios.axsellwands.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理玩家的出售二次确认状态
 */
public class SellConfirmationManager {
    
    /**
     * 存储玩家的确认信息
     */
    private static final Map<UUID, ConfirmationData> confirmations = new ConcurrentHashMap<>();
    
    /**
     * 确认超时时间（毫秒）
     */
    private static long confirmationTimeout = 30000; // 默认30秒
    
    /**
     * 设置确认超时时间
     * @param timeout 超时时间（毫秒）
     */
    public static void setConfirmationTimeout(long timeout) {
        confirmationTimeout = timeout;
    }
    
    /**
     * 获取确认超时时间
     * @return 超时时间（毫秒）
     */
    public static long getConfirmationTimeout() {
        return confirmationTimeout;
    }
    
    /**
     * 检查玩家是否有待确认的出售操作
     * @param player 玩家
     * @return 如果有待确认的操作返回true
     */
    public static boolean hasPendingConfirmation(Player player) {
        ConfirmationData data = confirmations.get(player.getUniqueId());
        if (data == null) {
            return false;
        }
        
        // 检查是否超时
        if (System.currentTimeMillis() - data.timestamp > confirmationTimeout) {
            confirmations.remove(player.getUniqueId());
            return false;
        }
        
        return true;
    }
    
    /**
     * 创建新的确认请求
     * @param player 玩家
     * @param totalPrice 总价格
     * @param totalAmount 总数量
     * @param items 物品清单
     */
    public static void createConfirmation(Player player, double totalPrice, int totalAmount, Map<Material, Integer> items) {
        ConfirmationData data = new ConfirmationData(totalPrice, totalAmount, new HashMap<>(items));
        confirmations.put(player.getUniqueId(), data);
    }
    
    /**
     * 确认并获取确认数据
     * @param player 玩家
     * @return 确认数据，如果没有待确认的操作或已超时返回null
     */
    public static ConfirmationData confirmAndGet(Player player) {
        ConfirmationData data = confirmations.remove(player.getUniqueId());
        
        if (data == null) {
            return null;
        }
        
        // 检查是否超时
        if (System.currentTimeMillis() - data.timestamp > confirmationTimeout) {
            return null;
        }
        
        return data;
    }
    
    /**
     * 取消玩家的确认请求
     * @param player 玩家
     */
    public static void cancelConfirmation(Player player) {
        confirmations.remove(player.getUniqueId());
    }
    
    /**
     * 清除所有确认请求
     */
    public static void clearAll() {
        confirmations.clear();
    }
    
    /**
     * 清除过期的确认请求
     */
    public static void cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        confirmations.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().timestamp > confirmationTimeout
        );
    }
    
    /**
     * 确认数据类
     */
    public static class ConfirmationData {
        private final double totalPrice;
        private final int totalAmount;
        private final Map<Material, Integer> items;
        private final long timestamp;
        
        public ConfirmationData(double totalPrice, int totalAmount, Map<Material, Integer> items) {
            this.totalPrice = totalPrice;
            this.totalAmount = totalAmount;
            this.items = items;
            this.timestamp = System.currentTimeMillis();
        }
        
        public double getTotalPrice() {
            return totalPrice;
        }
        
        public int getTotalAmount() {
            return totalAmount;
        }
        
        public Map<Material, Integer> getItems() {
            return items;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * 获取剩余确认时间（秒）
         * @return 剩余秒数
         */
        public long getRemainingSeconds() {
            long elapsed = System.currentTimeMillis() - timestamp;
            long remaining = confirmationTimeout - elapsed;
            return Math.max(0, remaining / 1000);
        }
    }
}
