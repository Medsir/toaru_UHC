package fr.medsir.toaruhc.commands;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class RoleCommand implements CommandExecutor {
    private final ToaruUHC plugin;
    public RoleCommand(ToaruUHC plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Joueurs uniquement."); return true; }
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§bAcademy City§8] §r");
        if (!plugin.getGameManager().isRunning()) { player.sendMessage(prefix + "§cAucune partie en cours."); return true; }
        UHCPlayer u = plugin.getGameManager().getUHCPlayer(player);
        if (u == null || u.getRole() == null) { player.sendMessage(prefix + "§cPas encore de rôle."); return true; }
        for (String line : u.getRole().getFullDescription()) player.sendMessage(line);
        return true;
    }
}
