package top.cxkcxkckx.bucket.func;

import org.bukkit.event.Listener;
import top.cxkcxkckx.bucket.bucket;

@SuppressWarnings({"unused"})
public abstract class AbstractPluginHolder implements Listener {
    protected final bucket plugin;

    public AbstractPluginHolder() {
        this.plugin = bucket.getInstance();
    }

    public AbstractPluginHolder(boolean register) {
        this();
        if (register) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    public bucket getPlugin() {
        return plugin;
    }
}
