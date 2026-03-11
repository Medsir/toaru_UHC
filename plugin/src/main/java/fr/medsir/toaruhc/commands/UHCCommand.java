package fr.medsir.toaruhc.commands;

import fr.medsir.toaruhc.ToaruUHC;
import org.bukkit.command.*;
import java.util.*;

public class UHCCommand implements CommandExecutor, TabCompleter {
    private final ToaruUHC plugin;
    public UHCCommand(ToaruUHC plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§bAcademy City§8] §r");
        if (!sender.hasPermission("toaruhc.admin")) { sender.sendMessage(prefix + "§cPas la permission."); return true; }
        if (args.length == 0) { sender.sendMessage(prefix + "§7/uhc <start|stop|status|forcestart>"); return true; }
        switch (args[0].toLowerCase()) {
            case "start" -> { if (plugin.getGameManager().isRunning()) { sender.sendMessage(prefix + "§cPartie déjà en cours !"); return true; } plugin.getGameManager().startGame(false); }
            case "stop"  -> { if (!plugin.getGameManager().isRunning()) { sender.sendMessage(prefix + "§cAucune partie."); return true; } plugin.getGameManager().stopGame(); }
            case "status" -> { sender.sendMessage(prefix + "§7État : §e" + plugin.getGameManager().getState()); long alive = plugin.getGameManager().getPlayers().values().stream().filter(p->p.isAlive()).count(); sender.sendMessage(prefix + "§7Survivants : §a" + alive); }
            case "test" -> { if (plugin.getGameManager().isRunning()) { sender.sendMessage(prefix + "§cPartie déjà en cours !"); return true; } plugin.getGameManager().startGame(true); }
            case "forcestart" -> plugin.getGameManager().startGame(false);
            default -> sender.sendMessage(prefix + "§7/uhc <start|stop|status|forcestart>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        return args.length == 1 ? Arrays.asList("start", "stop", "status", "forcestart", "test") : List.of();
    }
}
