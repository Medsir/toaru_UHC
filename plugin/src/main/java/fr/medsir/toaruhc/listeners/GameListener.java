package fr.medsir.toaruhc.listeners;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.core.GameState;
import fr.medsir.toaruhc.models.UHCPlayer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class GameListener implements Listener {
    private final ToaruUHC plugin;
    public GameListener(ToaruUHC plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        UHCPlayer uhcVictim = plugin.getGameManager().getUHCPlayer(victim);
        if (uhcVictim != null && uhcVictim.getRole() != null)
            event.setDeathMessage("§8[§cÉliminé§8] §r" + uhcVictim.getRole().getDisplayName() + (killer != null ? " §7par §c" + killer.getName() : " §7par l'environnement"));
        plugin.getGameManager().handlePlayerDeath(victim, killer);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().isRunning()) {
            player.sendMessage("§cPartie en cours. Mode spectateur.");
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        Player player = event.getPlayer();
        UHCPlayer u = plugin.getGameManager().getUHCPlayer(player);
        if (u != null && u.isAlive()) plugin.getGameManager().handlePlayerQuit(player);
        plugin.getPowerManager().removeEnergyBar(player.getUniqueId());
    }
}
