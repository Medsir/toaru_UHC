package fr.medsir.toaruhc.commands;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class PowerCommand implements CommandExecutor {
    private final ToaruUHC plugin;
    public PowerCommand(ToaruUHC plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Joueurs uniquement."); return true; }
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§bAcademy City§8] §r");
        if (!plugin.getGameManager().isRunning()) { player.sendMessage(prefix + "§cAucune partie."); return true; }
        UHCPlayer u = plugin.getGameManager().getUHCPlayer(player);
        if (u == null || !u.isAlive() || u.getPower() == null) { player.sendMessage(prefix + "§cPas de pouvoir."); return true; }
        if (u.getPower().activate(u)) plugin.getPowerManager().updateEnergyBar(u);
        return true;
    }
}
