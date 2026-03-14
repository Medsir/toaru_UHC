package fr.medsir.toaruhc.listeners;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.core.GameState;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import fr.medsir.toaruhc.powers.esper.AcceleratorPower;
import fr.medsir.toaruhc.powers.esper.ImagineBreaker;
import fr.medsir.toaruhc.powers.esper.KinuhataPower;
import fr.medsir.toaruhc.powers.esper.OthinusPower;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

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

        // Pouvoirs disponibles uniquement en phase PvP et Endgame
        GameState state = plugin.getGameManager().getState();
        if (state != GameState.PVP && state != GameState.ENDGAME) {
            player.sendMessage("§c⚠ Les pouvoirs ne sont disponibles qu'à partir du PvP !");
            return;
        }

        Power power = u.getPower();
        if (power == null) return;

        // SHIFT + Clic droit = ULTIMATE
        if (player.isSneaking()) {
            boolean used = power.activateUltimate(u);
            if (!used) player.sendMessage("§7§o[✦ ULTIMATE non disponible ou en recharge]");
            else plugin.getPowerManager().updateEnergyBar(u);
        } else {
            // Clic droit normal = pouvoir
            if (power.activate(u)) plugin.getPowerManager().updateEnergyBar(u);
        }
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

        if (isMovingPowerItem) {
            //event.setCancelled(true);
        }
    }

    /**
     * Empêche de dropper l'item de pouvoir ET Gungnir.
     */
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == POWER_ITEM && isPowerItem(item)) {
            event.setCancelled(true);
            return;
        }
        // Empêcher de dropper Gungnir
        if (OthinusPower.isGungnir(item)) {
            event.setCancelled(true);
        }
    }

    /**
     * Accelerator : réflexion des attaques physiques et projectiles.
     * Kinuhata : contre-attaque azote sur les attaquants.
     * Imagine Breaker : nullification au contact.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        UHCPlayer uVictim = plugin.getGameManager().getUHCPlayer(victim);
        if (uVictim == null || !uVictim.isAlive()) return;

        // ACCELERATOR — reflect projectiles
        if (uVictim.hasAcceleratorMode() && event.getDamager() instanceof Projectile proj) {
            event.setCancelled(true);
            proj.remove();
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player shooter && !shooter.equals(victim)) {
                AcceleratorPower.reflectProjectile(victim, shooter, event.getDamage());
            }
            return;
        }

        // ACCELERATOR — reflect physical attacks from players
        if (uVictim.hasAcceleratorMode() && event.getDamager() instanceof Player attacker) {
            event.setCancelled(true);
            AcceleratorPower.reflect(victim, attacker, event.getDamage());
            return;
        }

        // KINUHATA nitrogen armor counter
        if (uVictim.isNitrogenArmorActive() && event.getDamager() instanceof Player attacker) {
            // Does NOT cancel damage — just counter-effect the attacker
            KinuhataPower.nitrogenCounter(victim, attacker);
        }

        // IMAGINE BREAKER logic (Player-vs-Player only)
        if (event.getDamager() instanceof Player attacker) {
            UHCPlayer uA = plugin.getGameManager().getUHCPlayer(attacker);
            if (uA == null) return;

            if (uA.hasImagineBreaker()) {
                uA.setImagineBreaker(false);
                ImagineBreaker.applyNullification(attacker, victim);
                if (uVictim.getPower() != null) uVictim.setCooldown(uVictim.getPower().getId(), 15);
            }
            if (uVictim.hasImagineBreaker()) {
                uVictim.setImagineBreaker(false);
                ImagineBreaker.applyNullification(victim, attacker);
                if (uA.getPower() != null) uA.setCooldown(uA.getPower().getId(), 15);
            }
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
