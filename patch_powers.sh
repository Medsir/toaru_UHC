#!/bin/bash
# patch_powers.sh — Colle ce script à la racine de toaru_UHC et lance-le

# ── RailgunPower.java ────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/powers/esper/RailgunPower.java << 'EOF'
package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

/**
 * ⚡ RAILGUN - Misaka Mikoto
 * Trait de particules + foudre sur la trajectoire.
 */
public class RailgunPower extends Power {

    private static final double DAMAGE       = 16.0;
    private static final double MAX_DISTANCE = 60.0;
    private static final double STEP         = 0.5;

    public RailgunPower() {
        super("railgun", "§e⚡ Railgun §7(Misaka Mikoto)",
              "Lance un trait électrique dévastateur devant soi.",
              PowerType.ESPER, 30, 12);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        Location start    = player.getEyeLocation();
        Vector   direction = player.getLocation().getDirection().normalize();
        fireRailgun(player, start, direction);

        player.sendMessage("§e⚡ §bRailgun §e— §fFeu !");
        return true;
    }

    private void fireRailgun(Player shooter, Location start, Vector dir) {
        Location current = start.clone();
        double dist = 0;
        boolean hit = false;

        while (dist < MAX_DISTANCE && !hit) {
            current.add(dir.clone().multiply(STEP));
            dist += STEP;

            // Particules électriques sur toute la trajectoire
            shooter.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK, current, 4, 0.05, 0.05, 0.05, 0.02
            );

            // Foudre visuelle tous les 5 blocs
            if (dist % 5 < STEP) {
                shooter.getWorld().strikeLightningEffect(current.clone());
            }

            // Détection d'entités
            for (Entity entity : current.getWorld().getNearbyEntities(current, 0.8, 0.8, 0.8)) {
                if (!(entity instanceof Player target)) continue;
                if (target == shooter) continue;

                target.damage(DAMAGE, shooter);
                target.getWorld().strikeLightningEffect(target.getLocation());
                target.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    target.getLocation().add(0, 1, 0),
                    30, 0.4, 0.5, 0.4, 0.1
                );
                target.getWorld().playSound(
                    target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1.5f
                );

                shooter.sendMessage("§e⚡ §fTouche §c" + target.getName() + "§f !");
                target.sendMessage("§cTouché par le §eRailgun §cde §b" + shooter.getName() + "§c!");
                hit = true;
                break;
            }

            if (current.getBlock().getType().isSolid()) {
                current.getWorld().strikeLightningEffect(current.clone());
                hit = true;
            }
        }
    }
}
EOF

# ── ImagineBreaker.java ──────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/powers/esper/ImagineBreaker.java << 'EOF'
package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Collection;

/**
 * 🖐 IMAGINE BREAKER - Kamijou Touma
 * Active un bouclier 3s. Au prochain contact : annule les effets + slowness+weakness 5s sur la cible.
 */
public class ImagineBreaker extends Power {

    public ImagineBreaker() {
        super("imagine_breaker", "§f🖐 Imagine Breaker §7(Kamijou Touma)",
              "Annule tous les effets ennemis au contact + ralentit la cible 5s.",
              PowerType.ESPER, 20, 8);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        uhcPlayer.setImagineBreaker(true);

        player.getWorld().spawnParticle(
            Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0),
            20, 0.3, 0.3, 0.3, 0
        );
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        player.sendMessage("§f🖐 §bImagine Breaker §f— Actif pendant 3 secondes !");
        player.sendTitle("§f🖐", "§7Imagine Breaker actif", 5, 30, 10);

        // Auto-désactivation après 3s
        fr.medsir.toaruhc.ToaruUHC.getInstance().getServer().getScheduler()
            .runTaskLater(fr.medsir.toaruhc.ToaruUHC.getInstance(), () -> {
                if (uhcPlayer.hasImagineBreaker()) {
                    uhcPlayer.setImagineBreaker(false);
                    if (player.isOnline()) player.sendMessage("§7🖐 Imagine Breaker expiré.");
                }
            }, 60L);

        return true;
    }

    /**
     * Appliqué depuis PowerListener quand un joueur avec IB frappe quelqu'un.
     * - Annule tous les effets de la cible
     * - Applique Slowness II + Weakness II pendant 5s sur la cible
     */
    public static void applyNullification(Player user, Player target) {
        Collection<PotionEffect> effects = target.getActivePotionEffects();

        // Supprimer tous les effets
        for (PotionEffect e : effects) target.removePotionEffect(e.getType());

        // Appliquer slowness + weakness (cooldown de 5s)
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,        100, 1)); // 5s Slowness II
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,    100, 1)); // 5s Weakness II

        // Effets visuels
        target.getWorld().spawnParticle(
            Particle.ELECTRIC_SPARK,
            target.getLocation().add(0, 1, 0),
            40, 0.5, 0.8, 0.5, 0.1
        );
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 2.0f);

        user.sendMessage("§f🖐 §bImagine Breaker §f— " + (effects.isEmpty() ? "Cible ralentie !" : effects.size() + " effet(s) annulé(s) + cible ralentie !"));
        target.sendMessage("§cImagine Breaker de §b" + user.getName() + " §c— Tu es ralenti pendant 5s !");
        target.sendTitle("§c🖐 BRISÉ", "§7Ralenti 5 secondes...", 5, 50, 10);
    }
}
EOF

# ── TeleportPower.java ───────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/powers/esper/TeleportPower.java << 'EOF'
package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * 🌀 TELEPORT - Shirai Kuroko
 * Téléportation 15 blocs dans la direction du regard, sans contrainte de bloc.
 */
public class TeleportPower extends Power {

    private static final double MAX_DISTANCE = 15.0;
    private static final double STEP         = 0.5;

    public TeleportPower() {
        super("teleport", "§d🌀 Teleport §7(Shirai Kuroko)",
              "Téléportation instantanée 15 blocs devant soi.",
              PowerType.ESPER, 25, 6);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();

        Location origin = player.getLocation().clone();
        Vector   dir    = player.getLocation().getDirection().normalize();
        Location dest   = findDestination(player, origin, dir);

        consumeResources(uhcPlayer);

        // Effets à l'origine
        origin.getWorld().spawnParticle(Particle.PORTAL, origin.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.5);
        origin.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);

        player.teleport(dest);

        // Effets à la destination
        dest.getWorld().spawnParticle(Particle.PORTAL, dest.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.5);
        dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);

        player.sendMessage("§d🌀 §bTeleport §d— Téléportation !");
        return true;
    }

    /**
     * Avance pas à pas dans la direction du regard.
     * S'arrête avant un bloc solide ou au max (15 blocs).
     * Cherche toujours une position où le joueur peut tenir debout (2 blocs de hauteur).
     */
    private Location findDestination(Player player, Location origin, Vector dir) {
        Location current  = origin.clone().add(0, 0.1, 0);
        Location lastSafe = origin.clone();
        double dist = 0;

        while (dist < MAX_DISTANCE) {
            current.add(dir.clone().multiply(STEP));
            dist += STEP;

            Location feet = current.clone();
            Location head = current.clone().add(0, 1, 0);

            // Si le bloc des pieds ou de la tête est solide → on s'arrête
            if (feet.getBlock().getType().isSolid() || head.getBlock().getType().isSolid()) {
                break;
            }

            // Sinon c'est une position valide
            lastSafe = current.clone();
        }

        // Ajuster pour poser les pieds sur le sol
        lastSafe = adjustToGround(lastSafe);
        lastSafe.setYaw(player.getLocation().getYaw());
        lastSafe.setPitch(player.getLocation().getPitch());
        return lastSafe;
    }

    private Location adjustToGround(Location loc) {
        Location check = loc.clone();
        // Descendre jusqu'au sol (max 5 blocs)
        for (int i = 0; i < 5; i++) {
            if (check.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) return check;
            check.subtract(0, 1, 0);
        }
        return loc;
    }
}
EOF

# ── SaintPower.java ──────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/powers/magician/SaintPower.java << 'EOF'
package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;

/**
 * ⚔ SAINT'S POWER - Kanzaki Kaori
 * Force II + Regen II + Résistance I pendant 8s. Activation dans le vide.
 */
public class SaintPower extends Power {

    public SaintPower() {
        super("saint_power", "§6⚔ Saint's Power §7(Kanzaki Kaori)",
              "Force II + Regen II + Résistance I pendant 8s.",
              PowerType.MAGICIAN, 40, 20);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,    100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 160, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 160, 0));

        // Effets visuels dorés
        player.getWorld().spawnParticle(
            Particle.CRIT_MAGIC,
            player.getLocation().add(0, 1, 0),
            40, 0.5, 0.5, 0.5, 0.3
        );
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.2f);

        player.sendMessage("§6⚔ §bSaint's Power §6— La force d'un Saint t'envahit !");
        player.sendTitle("§6⚔ SAINT", "§71/7 000 000 000", 5, 50, 10);
        return true;
    }
}
EOF

# ── PowerListener.java — clic droit avec l'item de pouvoir ──────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/listeners/PowerListener.java << 'EOF'
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
EOF

# ── RoleManager.java — donne l'item de pouvoir à chaque joueur ──────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/managers/RoleManager.java << 'EOF'
package fr.medsir.toaruhc.managers;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.listeners.PowerListener;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.esper.*;
import fr.medsir.toaruhc.powers.magician.SaintPower;
import fr.medsir.toaruhc.roles.Role;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RoleManager {

    private final ToaruUHC plugin;
    private final List<Role> availableRoles = new ArrayList<>();

    public RoleManager(ToaruUHC plugin) {
        this.plugin = plugin;
        registerRoles();
    }

    private void registerRoles() {
        availableRoles.add(new Role("misaka", "Misaka Mikoto",
            "§e⚡ Misaka Mikoto §8(Level 5)",
            "La Railgun d'Academy City.",
            Role.RoleType.ESPER, new RailgunPower(),
            "Je ne cours pas après les garçons qui tombent du ciel."));

        availableRoles.add(new Role("touma", "Kamijou Touma",
            "§f🖐 Kamijou Touma §8(Level 0)",
            "La main droite qui brise toute illusion.",
            Role.RoleType.ESPER, new ImagineBreaker(),
            "Je briserai cette illusion de mes mains !"));

        availableRoles.add(new Role("kuroko", "Shirai Kuroko",
            "§d🌀 Shirai Kuroko §8(Level 4)",
            "Téléportatrice de Judgement.",
            Role.RoleType.ESPER, new TeleportPower(),
            "Jugement vous arrête là !"));

        availableRoles.add(new Role("kanzaki", "Kanzaki Kaori",
            "§6⚔ Kanzaki Kaori §8(Saint)",
            "L'une des rares Saintes.",
            Role.RoleType.MAGICIAN, new SaintPower(),
            "1/7 000 000 000 — je suis une Sainte."));

        plugin.getLogger().info("[RoleManager] " + availableRoles.size() + " rôles enregistrés.");
    }

    public void distributeRoles(List<UHCPlayer> players) {
        List<Role> pool = new ArrayList<>(availableRoles);
        while (pool.size() < players.size()) pool.addAll(availableRoles);
        Collections.shuffle(pool);
        for (int i = 0; i < players.size(); i++) assignRole(players.get(i), pool.get(i));
    }

    public void assignRole(UHCPlayer uhcPlayer, Role role) {
        uhcPlayer.setRole(role);
        uhcPlayer.setPower(role.getPower());

        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null || !player.isOnline()) return;

        // Afficher les infos du rôle
        for (String line : role.getFullDescription()) player.sendMessage(line);
        player.sendTitle(role.getDisplayName(), "§7Clic droit avec §6✦ §7pour activer", 10, 80, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);

        // Donner l'item de pouvoir au slot 0
        givePowerItem(player, role);
    }

    /**
     * Crée et donne l'item de pouvoir (Blaze Rod renommé) au joueur dans le slot 0.
     * Impossible à retirer grâce à PowerListener.
     */
    private void givePowerItem(Player player, Role role) {
        ItemStack item = new ItemStack(PowerListener.POWER_ITEM);
        ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName("§6✦ Pouvoir §8— " + role.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("§7" + role.getPower().getName());
        lore.add("§7" + role.getPower().getDescription());
        lore.add("");
        lore.add("§eClic droit §7pour activer");
        lore.add("§7Coût : §e" + role.getPower().getAimOrManaCost()
            + (role.getType() == Role.RoleType.ESPER ? " AIM" : " Mana"));
        lore.add("§7Recharge : §e" + role.getPower().getCooldownSeconds() + "s");

        meta.setLore(lore);
        // Rendre l'item indestructible
        meta.setUnbreakable(true);
        item.setItemMeta(meta);

        // Forcer le slot 0
        player.getInventory().setItem(0, item);
        player.getInventory().setHeldItemSlot(0);
    }

    public List<Role> getAvailableRoles() { return Collections.unmodifiableList(availableRoles); }
}
EOF

echo ""
echo "✅ Patch appliqué ! Lance : cd plugin && mvn clean package"
