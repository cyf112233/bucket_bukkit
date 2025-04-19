package top.cxkcxkckx.bucket.func;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import top.cxkcxkckx.bucket.bucket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class CustomBucket implements Listener {
    private static final String CUSTOM_BUCKET_TAG = "custom_bucket";
    private static final String CUSTOM_BUCKET_ID = "bucket_id";
    private static double damage = 4.0;
    private static double knockback = 0.4;
    private static double stunChance = 0.3;
    private static double stunDuration = 5.0;
    private static int potionLevel = 2;
    private static boolean stunMessageEnabled = true;
    private static String stunMessageText = "&e目标已眩晕";
    private static double stunMessageDuration = 3.0;
    private static String itemName = "&6锁分桶";
    private static List<String> itemLore = new ArrayList<>();
    private static final Map<UUID, Long> lastAttackTime = new HashMap<>();
    private static final Map<UUID, Integer> noDamageTicks = new HashMap<>();
    private static final Map<UUID, Long> stunEndTime = new HashMap<>();
    private static final Map<UUID, Double> originalSpeed = new HashMap<>();
    private static final Map<UUID, Double> originalAttackSpeed = new HashMap<>();
    private static final List<SoundConfig> sounds = new ArrayList<>();
    private static final Random random = new Random();
    private static int logLevel = 1;  // 默认日志级别为1

    private static class SoundConfig {
        private final Sound sound;
        private final float volume;
        private final float pitch;

        public SoundConfig(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public void play(Player player, org.bukkit.Location location) {
            player.playSound(location, sound, volume, pitch);
        }
    }

    /**
     * 记录日志
     * @param level 日志级别
     * @param message 日志消息
     */
    private static void log(int level, String message) {
        if (logLevel >= level) {
            bucket.getInstance().getLogger().info(message);
        }
    }

    /**
     * 记录调试日志
     * @param message 日志消息
     */
    private static void debug(String message) {
        if (logLevel >= 2) {
            bucket.getInstance().getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * 设置自定义桶的配置
     * @param config 配置文件
     */
    public static void loadConfig(FileConfiguration config) {
        // 加载物品名称和描述
        itemName = config.getString("bucket.item.name", "&6锁分通");
        itemLore.clear();
        itemLore.addAll(config.getStringList("bucket.item.lore"));
        
        // 如果没有配置描述，使用默认描述
        if (itemLore.isEmpty()) {
            itemLore.add("&7一把神奇的桶");
            itemLore.add("&7可以造成伤害并击退目标");
            itemLore.add("&7有概率使目标眩晕");
            itemLore.add("");
            itemLore.add("&e特殊效果:");
            itemLore.add("&7- 无攻击冷却");
            itemLore.add("&7- 移除目标无敌时间");
            itemLore.add("&7- 击退目标");
            itemLore.add("&7- 概率眩晕目标");
        }
        
        damage = config.getDouble("bucket.damage", 4.0);
        knockback = config.getDouble("bucket.knockback", 0.4);
        stunChance = config.getDouble("bucket.stun.chance", 0.3);
        stunDuration = config.getDouble("bucket.stun.duration", 5.0);
        potionLevel = config.getInt("bucket.stun.potion-level", 2);
        
        // 加载眩晕提示配置
        stunMessageEnabled = config.getBoolean("bucket.stun.message.enabled", true);
        stunMessageText = config.getString("bucket.stun.message.text", "&e目标已眩晕");
        stunMessageDuration = config.getDouble("bucket.stun.message.duration", 3.0);
        
        // 加载日志级别
        logLevel = config.getInt("bucket.log-level", 1);
        log(1, "日志级别已设置为: " + logLevel);
        
        // 加载音效配置
        sounds.clear();
        List<Map<?, ?>> soundList = config.getMapList("bucket.sounds");
        for (Map<?, ?> soundMap : soundList) {
            try {
                String soundName = (String) soundMap.get("sound");
                double volume = ((Number) soundMap.get("volume")).doubleValue();
                double pitch = ((Number) soundMap.get("pitch")).doubleValue();
                
                Sound sound = Sound.valueOf(soundName);
                sounds.add(new SoundConfig(sound, (float) volume, (float) pitch));
                debug("已加载音效: " + soundName + " (音量: " + volume + ", 音调: " + pitch + ")");
            } catch (Exception e) {
                log(1, "无法加载音效配置: " + soundMap);
            }
        }
        
        // 如果没有配置音效，使用默认音效
        if (sounds.isEmpty()) {
            sounds.add(new SoundConfig(Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f));
            debug("使用默认音效: ENTITY_ITEM_BREAK");
        }
    }

    /**
     * 创建一个自定义空桶
     * @return 带有自定义NBT标签的空桶
     */
    public static ItemStack createCustomBucket() {
        ItemStack bucket = new ItemStack(Material.BUCKET);
        
        // 设置物品名称和描述
        org.bukkit.inventory.meta.ItemMeta meta = bucket.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', itemName));
            
            // 转换描述中的颜色代码
            List<String> coloredLore = new ArrayList<>();
            for (String line : itemLore) {
                coloredLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
            
            bucket.setItemMeta(meta);
        }
        
        // 创建NBT物品并设置标签
        NBTItem nbtItem = new NBTItem(bucket);
        nbtItem.setBoolean(CUSTOM_BUCKET_TAG, true);
        nbtItem.setString(CUSTOM_BUCKET_ID, "custom_bucket_" + System.currentTimeMillis());
        
        // 返回带有NBT标签的物品
        return nbtItem.getItem();
    }

    /**
     * 检查物品是否是自定义桶
     * @param item 要检查的物品
     * @return 如果是自定义桶返回true，否则返回false
     */
    public static boolean isCustomBucket(ItemStack item) {
        if (item == null || item.getType() != Material.BUCKET) {
            return false;
        }
        
        try {
            NBTItem nbtItem = new NBTItem(item);
            boolean isCustom = nbtItem.getBoolean(CUSTOM_BUCKET_TAG);
            debug("检查物品是否是自定义桶: " + (isCustom ? "是" : "否"));
            return isCustom;
        } catch (Exception e) {
            log(1, "检查NBT标签时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取自定义桶的ID
     * @param item 自定义桶物品
     * @return 桶的ID，如果不是自定义桶则返回null
     */
    public static String getBucketId(ItemStack item) {
        if (!isCustomBucket(item)) {
            return null;
        }
        
        NBTItem nbtItem = new NBTItem(item);
        return nbtItem.getString(CUSTOM_BUCKET_ID);
    }

    /**
     * 播放所有配置的音效
     * @param player 玩家
     * @param location 位置
     */
    private void playSounds(Player player, org.bukkit.Location location) {
        for (SoundConfig soundConfig : sounds) {
            soundConfig.play(player, location);
        }
    }

    /**
     * 应用击退效果
     * @param target 目标实体
     * @param attacker 攻击者
     */
    private void applyKnockback(Entity target, Entity attacker) {
        if (!(target instanceof LivingEntity)) {
            return;
        }

        // 计算击退方向
        Vector direction = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        
        // 设置水平方向的击退
        Vector velocity = direction.multiply(knockback);
        
        // 添加一个小的向上的力,防止目标卡在地面
        velocity.setY(0.2);
        
        // 应用击退效果
        target.setVelocity(velocity);
    }

    /**
     * 显示眩晕提示
     * @param player 玩家
     */
    private void showStunMessage(Player player) {
        if (!stunMessageEnabled) {
            return;
        }

        // 发送物品栏上方的文字提示
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', stunMessageText)
            ));

        // 设置定时任务清除消息
        new BukkitRunnable() {
            @Override
            public void run() {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(""));
            }
        }.runTaskLater(bucket.getInstance(), (long)(stunMessageDuration * 20L));
    }

    /**
     * 应用眩晕效果
     * @param target 目标实体
     * @param attacker 攻击者
     */
    private void applyStun(org.bukkit.entity.Entity target, Player attacker) {
        if (!(target instanceof LivingEntity)) {
            return;
        }

        LivingEntity livingEntity = (LivingEntity) target;
        
        // 检查是否触发眩晕
        if (random.nextDouble() > stunChance) {
            return;
        }

        // 设置眩晕状态
        livingEntity.setGlowing(true);
        stunEndTime.put(target.getUniqueId(), System.currentTimeMillis() + (long)(stunDuration * 1000));

        // 添加药水效果
        // 缓慢效果 (移动速度降低)
        livingEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOW,
            (int)(stunDuration * 20),
            potionLevel - 1
        ));
        
        // 挖掘疲劳效果 (攻击速度降低)
        livingEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOW_DIGGING,
            (int)(stunDuration * 20),
            potionLevel - 1
        ));
        
        // 虚弱效果 (伤害降低)
        livingEntity.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.WEAKNESS,
            (int)(stunDuration * 20),
            potionLevel - 1
        ));

        // 播放眩晕音效
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.5f);

        // 显示眩晕提示
        showStunMessage(attacker);

        // 设置定时任务解除眩晕
        new BukkitRunnable() {
            @Override
            public void run() {
                if (target.isValid() && !target.isDead()) {
                    livingEntity.setGlowing(false);
                    // 移除药水效果
                    livingEntity.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOW);
                    livingEntity.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOW_DIGGING);
                    livingEntity.removePotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS);
                    stunEndTime.remove(target.getUniqueId());
                }
            }
        }.runTaskLater(bucket.getInstance(), (long)(stunDuration * 20L));
    }

    /**
     * 移除玩家的攻击冷却
     * @param player 玩家
     */
    private void removeAttackCooldown(Player player) {
        try {
            // 方法1：使用反射获取NMS玩家并设置攻击速度
            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object attributeInstance = craftPlayer.getClass().getMethod("getAttributeInstance", 
                Class.forName("net.minecraft.server.v1_16_R3.GenericAttributes")).invoke(craftPlayer, 
                Class.forName("net.minecraft.server.v1_16_R3.GenericAttributes").getField("ATTACK_SPEED").get(null));
            attributeInstance.getClass().getMethod("setValue", double.class).invoke(attributeInstance, 100.0);
            
            // 方法2：直接设置攻击冷却为0
            craftPlayer.getClass().getMethod("a", float.class).invoke(craftPlayer, 0.0f);
            
            // 方法3：设置攻击速度倍率
            craftPlayer.getClass().getMethod("setAttackSpeed", float.class).invoke(craftPlayer, 100.0f);
            
            // 方法4：设置攻击冷却时间
            craftPlayer.getClass().getMethod("setAttackCooldown", int.class).invoke(craftPlayer, 0);
            
            // 方法5：设置攻击速度属性
            craftPlayer.getClass().getMethod("setAttributeValue", 
                Class.forName("net.minecraft.server.v1_16_R3.GenericAttributes"), double.class)
                .invoke(craftPlayer, 
                    Class.forName("net.minecraft.server.v1_16_R3.GenericAttributes").getField("ATTACK_SPEED").get(null),
                    100.0);
        } catch (Exception ignored) {
            // 静默处理异常
        }
        lastAttackTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * 移除实体的无敌时间和冷却
     * @param entity 实体
     */
    private void removeEntityCooldown(LivingEntity entity) {
        try {
            // 方法1：使用Bukkit API
            entity.setNoDamageTicks(0);
            entity.setMaximumNoDamageTicks(0);
            entity.setLastDamage(0.0D);
            
            // 方法2：使用反射设置NMS实体的所有相关属性
            Object nmsEntity = entity.getClass().getMethod("getHandle").invoke(entity);
            
            // 设置无敌时间
            nmsEntity.getClass().getMethod("setNoDamageTicks", int.class).invoke(nmsEntity, 0);
            
            // 设置最后伤害时间
            nmsEntity.getClass().getMethod("setLastDamage", float.class).invoke(nmsEntity, 0.0f);
            
            // 设置最后攻击时间
            nmsEntity.getClass().getMethod("setLastAttackTime", long.class).invoke(nmsEntity, 0L);
            
            // 设置伤害免疫时间
            nmsEntity.getClass().getMethod("setDamageImmunity", int.class).invoke(nmsEntity, 0);
            
            // 设置伤害延迟
            nmsEntity.getClass().getMethod("setDamageDelay", int.class).invoke(nmsEntity, 0);
            
            // 设置伤害减免
            nmsEntity.getClass().getMethod("setDamageReduction", float.class).invoke(nmsEntity, 0.0f);
            
            // 设置伤害保护
            nmsEntity.getClass().getMethod("setDamageProtection", float.class).invoke(nmsEntity, 0.0f);
            
            // 设置伤害反射
            nmsEntity.getClass().getMethod("setDamageReflection", float.class).invoke(nmsEntity, 0.0f);
            
            // 设置伤害反射时间
            nmsEntity.getClass().getMethod("setDamageReflectionTime", int.class).invoke(nmsEntity, 0);
            
            // 设置伤害反射冷却
            nmsEntity.getClass().getMethod("setDamageReflectionCooldown", int.class).invoke(nmsEntity, 0);
            
            // 设置伤害反射免疫
            nmsEntity.getClass().getMethod("setDamageReflectionImmunity", float.class).invoke(nmsEntity, 0.0f);
            
            // 设置伤害反射免疫时间
            nmsEntity.getClass().getMethod("setDamageReflectionImmunityTime", int.class).invoke(nmsEntity, 0);
            
            // 设置伤害反射免疫冷却
            nmsEntity.getClass().getMethod("setDamageReflectionImmunityCooldown", int.class).invoke(nmsEntity, 0);
            
        } catch (Exception ignored) {
            // 静默处理异常
        }
    }

    /**
     * 处理实体伤害事件
     * @param event 实体伤害事件
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (isCustomBucket(item)) {
            debug("玩家 " + player.getName() + " 使用自定义桶攻击了 " + event.getEntity().getName());
            
            // 设置伤害值
            event.setDamage(damage);
            debug("设置伤害值为: " + damage);
            
            // 移除攻击者和目标的无敌时间
            removeEntityCooldown((LivingEntity) event.getEntity());
            removeEntityCooldown(player);
            debug("已移除无敌时间");
            
            // 移除攻击者的攻击冷却
            removeAttackCooldown(player);
            debug("已移除攻击冷却");

            // 播放音效
            playSounds(player, event.getEntity().getLocation());
            debug("已播放音效");
            
            // 应用击退效果
            applyKnockback(event.getEntity(), player);
            debug("已应用击退效果");
            
            // 应用眩晕效果
            applyStun(event.getEntity(), player);
            debug("已应用眩晕效果");
        }
    }

    /**
     * 处理物品耐久度事件
     * @param event 物品耐久度事件
     */
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (isCustomBucket(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /**
     * 处理实体伤害事件（用于移除无敌时间）
     * @param event 实体伤害事件
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Player attacker = null;
            
            // 获取攻击者
            if (event instanceof EntityDamageByEntityEvent) {
                if (((EntityDamageByEntityEvent) event).getDamager() instanceof Player) {
                    attacker = (Player) ((EntityDamageByEntityEvent) event).getDamager();
                }
            }
            
            // 如果是被自定义桶攻击，移除无敌时间
            if (attacker != null && isCustomBucket(attacker.getInventory().getItemInMainHand())) {
                removeEntityCooldown((LivingEntity) player);
                noDamageTicks.put(player.getUniqueId(), 0);
            }
        }
    }

    /**
     * 处理玩家动画事件
     * @param event 玩家动画事件
     */
    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (isCustomBucket(item)) {
            removeAttackCooldown(player);
        }
    }

    /**
     * 处理玩家交互事件
     * @param event 玩家交互事件
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isCustomBucket(item)) {
            removeAttackCooldown(player);
        }
    }

    /**
     * 处理玩家切换物品事件
     * @param event 玩家切换物品事件
     */
    @EventHandler
    public void onPlayerItemHeldChange(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (isCustomBucket(item)) {
            removeAttackCooldown(player);
        }
    }

    /**
     * 处理玩家切换主副手事件
     * @param event 玩家切换主副手事件
     */
    @EventHandler
    public void onPlayerSwapHandItems(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (isCustomBucket(item)) {
            removeAttackCooldown(player);
        }
    }

    /**
     * 处理玩家移动事件
     * @param event 玩家移动事件
     */
    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (isCustomBucket(item)) {
            removeAttackCooldown(player);
        }
    }

    /**
     * 处理实体恢复生命值事件
     * @param event 实体恢复生命值事件
     */
    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (noDamageTicks.containsKey(player.getUniqueId())) {
                // 延迟恢复生命值
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            player.setNoDamageTicks(0);
                        }
                    }
                }.runTaskLater(bucket.getInstance(), 1L);
            }
        }
    }

    /**
     * 处理玩家退出事件
     * @param event 玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        lastAttackTime.remove(player.getUniqueId());
        noDamageTicks.remove(player.getUniqueId());
        stunEndTime.remove(player.getUniqueId());
        originalSpeed.remove(player.getUniqueId());
        originalAttackSpeed.remove(player.getUniqueId());
    }

    /**
     * 启动定时任务来持续移除无敌时间
     */
    public static void startNoDamageTicksTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Integer> entry : noDamageTicks.entrySet()) {
                    Player player = bucket.getInstance().getServer().getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        player.setNoDamageTicks(0);
                    }
                }
            }
        }.runTaskTimer(bucket.getInstance(), 0L, 1L);
    }

    /**
     * 启动定时任务来持续移除攻击冷却
     */
    public static void startAttackCooldownTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : bucket.getInstance().getServer().getOnlinePlayers()) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (isCustomBucket(item)) {
                        try {
                            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
                            craftPlayer.getClass().getMethod("a", float.class).invoke(craftPlayer, 0.0f);
                        } catch (Exception e) {
                            // 忽略异常
                        }
                    }
                }
            }
        }.runTaskTimer(bucket.getInstance(), 0L, 1L);
    }
} 