# bucket_bukkit
把锁分桶搬到了bukkit
<br>
用命令/getbucket获取锁分桶
<br>
这是配置文件
```
# 自定义桶配置
bucket:
  # 物品名称和描述
  item:
    # 物品名称 (支持颜色代码)
    name: "&6锁分桶"
    # 物品描述 (支持多行和颜色代码)
    lore:
      - "&7一把神奇的桶"
      - "&7可以造成伤害并击退目标"
      - "&7有概率使目标眩晕"
      - ""
      - "&e特殊效果:"
      - "&7- 无攻击冷却"
      - "&7- 移除目标无敌时间"
      - "&7- 击退目标"
      - "&7- 概率眩晕目标"
  # 自定义桶造成的伤害值
  damage: 4.0
  # 击退距离 (单位：方块)
  knockback: 0.4
  # 眩晕效果配置
  stun:
    # 眩晕触发概率 (0.0-1.0)
    chance: 0.3
    # 眩晕持续时间 (秒)
    duration: 5.0
    # 药水效果等级 (1-5)
    potion-level: 2
    # 眩晕提示配置
    message:
      # 是否启用提示
      enabled: true
      # 提示文本
      text: "&e目标已眩晕"
      # 提示持续时间 (秒)
      duration: 3.0
  # 攻击音效配置
  sounds:
    # 音效列表，可以配置多个音效
    # 每个音效需要设置:
    # - sound: 音效名称 (使用Minecraft原版音效)
    # - volume: 音量 (0.0-1.0)
    # - pitch: 音调 (0.5-2.0)
    # 示例:
    # - sound: ENTITY_ITEM_BREAK
    #   volume: 1.0
    #   pitch: 1.0
    # - sound: BLOCK_ANVIL_LAND
    #   volume: 0.5
    #   pitch: 1.2
    # 默认音效
    - sound: ENTITY_ITEM_BREAK
      volume: 1.0
      pitch: 1.0
  # 日志级别 (0: 不输出日志, 1: 普通日志, 2: 调试日志)
  log-level: 1

# 命令提示消息配置
messages:
  # 获取锁分桶命令
  getbucket:
    # 成功获取消息
    success: "&a你获得了一个锁分桶！"
    # 无权限消息
    no-permission: "&c你没有权限使用该命令！"
    # 非玩家消息
    player-only: "&c该命令只能由玩家执行！" 
```
