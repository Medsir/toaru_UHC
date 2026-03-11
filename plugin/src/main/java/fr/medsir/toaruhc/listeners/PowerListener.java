package fr.medsir.toaruhc.listeners;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import fr.medsir.toaruhc.powers.esper.ImagineBreaker;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PowerListener implements Listener {

    private final ToaruUHC plugin;

    // L'item de pouvoir : BLAZE_ROD avec un nom custom
    public static final Material POWER_ITEM = Material.BLAZE_ROD;

    public PowerListener(ToaruUHC plugin) { this.plugin = plugin; }

    /**
     * Clic droit avec le Blaze Rod (item de pouvoir) = activation.
     * Fonctionne dans le vide ET en regardant un bloc.
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        // Seulement clic droit (vide ou bloc)
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Vérifier que c'est bien l'item de pouvoir
        if (item.getType() != POWER_ITEM) return;
        if (!isPowerItem(item)) return;

        event.setCancelled(true); // Empêche toute interaction avec les blocs

        UHCPlayer u = plugin.getGameManager().getUHCPlayer(player);
        if (u == null || !u.isAlive()) return;

        Power power = u.getPower();
        if (power == null) return;

        if (power.activate(u)) plugin.getPowerManager().updateEnergyBar(u);
    }

    /**
     * Empêche le joueur de retirer l'item de pouvoir du slot 0.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getGameManager().isRunning()) return;

        UHCPlayer u = plugin.getGameManager().getUHCPlayer(player);
        if (u == null || !u.isAlive()) return;

        // Bloquer si c'est le slot 0 (hotbar slot 1) ou si l'item déplacé est l'item de pouvoir
        ItemStack current = event.getCurrentItem();
        ItemStack cursor  = event.getCursor();

        boolean isMovingPowerItem =
            (current != null && current.getType() == POWER_ITEM && isPowerItem(current)) ||
            (cursor  != null && cursor.getType()  == POWER_ITEM && isPowerItem(cursor));

        if (isMovingPowerItem || event.getSlot() == 0) {
            event.setCancelled(true);
        }
    }

    /**
     * Empêche de dropper l'item de pouvoir.
     */
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == POWER_ITEM && isPowerItem(item)) {
            event.setCancelled(true);
        }
    }

    /**
     * Imagine Breaker : si actif et que le joueur frappe quelqu'un → nullification.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity()  instanceof Player victim))   return;

        UHCPlayer uA = plugin.getGameManager().getUHCPlayer(attacker);
        UHCPlayer uV = plugin.getGameManager().getUHCPlayer(victim);
        if (uA == null || uV == null) return;

        if (uA.hasImagineBreaker()) {
            uA.setImagineBreaker(false);
            ImagineBreaker.applyNullification(attacker, victim);
        }
        if (uV.hasImagineBreaker()) {
            uV.setImagineBreaker(false);
            ImagineBreaker.applyNullification(victim, attacker);
        }
    }

    /**
     * Vérifie si un ItemStack est bien l'item de pouvoir (via son nom custom).
     */
    public static boolean isPowerItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (!item.getItemMeta().hasDisplayName()) return false;
        return item.getItemMeta().getDisplayName().contains("§6✦ Pouvoir");
    }
}
