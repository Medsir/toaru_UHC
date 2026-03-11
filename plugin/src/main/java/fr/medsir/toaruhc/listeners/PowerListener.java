package fr.medsir.toaruhc.listeners;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import fr.medsir.toaruhc.powers.esper.ImagineBreaker;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PowerListener implements Listener {
    private final ToaruUHC plugin;
    public PowerListener(ToaruUHC plugin) { this.plugin = plugin; }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType().isEdible()) return;
        UHCPlayer u = plugin.getGameManager().getUHCPlayer(player);
        if (u == null || !u.isAlive()) return;
        Power power = u.getPower();
        if (power == null) return;
        if (power.activate(u)) plugin.getPowerManager().updateEnergyBar(u);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        UHCPlayer uA = plugin.getGameManager().getUHCPlayer(attacker);
        UHCPlayer uV = plugin.getGameManager().getUHCPlayer(victim);
        if (uA == null || uV == null) return;
        if (uA.hasImagineBreaker()) { uA.setImagineBreaker(false); ImagineBreaker.applyNullification(attacker, victim); }
        if (uV.hasImagineBreaker()) { uV.setImagineBreaker(false); ImagineBreaker.applyNullification(victim, attacker); }
    }
}
