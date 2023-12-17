package dev.rgbmc.ferrum.commands;

import dev.rgbmc.ferrum.Ferrum;
import dev.rgbmc.ferrum.api.objects.ResultInfo;
import dev.rgbmc.ferrum.utils.BackupUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;

import static dev.rgbmc.ferrum.utils.Color.color;

public class FerrumCommand implements CommandExecutor {
    private static void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&6&lFerrum &8- &cHelp"));
        sender.sendMessage(color("&b/ferrum backup &7———— &eStart Backup Manually"));
        sender.sendMessage(color("&b/ferrum reload &7———— &eReload Configuration"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ferrum.admin")) return false;
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                Ferrum.instance.reloadConfig();
                Ferrum.instance.reloadCron();
                sender.sendMessage(color("&aSuccessful reload configuration"));
            } else if (args[0].equalsIgnoreCase("backup")) {
                CompletableFuture.runAsync(() -> BackupUtils.startBackup(true, sender));
                sender.sendMessage(color("&aBackup Task Started!"));
            } else if (args[0].equalsIgnoreCase("task")) {
                sender.sendMessage(color("&cForce executed tasks (Only Support for test none-argument task)"));
                CompletableFuture.runAsync(() -> Ferrum.taskManager.parseExpressions(Ferrum.instance.getConfig().getStringList("backups.finished-tasks"), new ResultInfo(null, null), null));
            } else {
                sendHelp(sender);
            }
        } else {
            sendHelp(sender);
        }
        return false;
    }
}
