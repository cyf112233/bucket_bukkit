package top.cxkcxkckx.bucket.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.cxkcxkckx.bucket.bucket;
import top.cxkcxkckx.bucket.func.CustomBucket;

public class GetBucketCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                bucket.getInstance().getConfig().getString("messages.player-only", "&c该命令只能由玩家执行")));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bucket.get")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                bucket.getInstance().getConfig().getString("messages.no-permission", "&c你没有权限使用该命令")));
            return true;
        }

        player.getInventory().addItem(CustomBucket.createCustomBucket());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            bucket.getInstance().getConfig().getString("messages.success", "&a成功获取锁分通")));
        return true;
    }
} 