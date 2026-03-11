package fr.medsir.toaruhc.listeners;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.core.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class PlayerListener implements Listener {
    private final ToaruUHC plugin;
    public PlayerListener(ToaruUHC plugin) { this.plugin = plugin; }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        GameState s = plugin.getGameManager().getState();
        if (s == GameState.WAITING || s == GameState.STARTING) event.setCancelled(true);
    }
}
