package top.cxkcxkckx.bucket;

import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.EconomyHolder;
import top.cxkcxkckx.bucket.commands.GetBucketCommand;
import top.cxkcxkckx.bucket.func.CustomBucket;

public class bucket extends BukkitPlugin {
    public static bucket getInstance() {
        return (bucket) BukkitPlugin.getInstance();
    }

    public bucket() {
        super(options()
                .bungee(false)
                .adventure(false)
                .database(false)
                .reconnectDatabaseWhenReloadConfig(false)
                .vaultEconomy(false)
                .scanIgnore("top.mrxiaom.example.libs")
        );
    }

    @Override
    protected void afterEnable() {
        // 保存默认配置
        saveDefaultConfig();
        
        // 加载配置
        CustomBucket.loadConfig(getConfig());
        
        // 注册命令
        getCommand("getbucket").setExecutor(new GetBucketCommand());
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new CustomBucket(), this);
        
        // 启动定时任务
        CustomBucket.startNoDamageTicksTask();
        
        getLogger().info("锁分桶插件已启动！");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        CustomBucket.loadConfig(getConfig());
    }
}
