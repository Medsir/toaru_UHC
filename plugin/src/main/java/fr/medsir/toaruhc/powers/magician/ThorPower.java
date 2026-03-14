package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ⚡ THOR - Dieu du Tonnerre (Gremlin)
 * Phase 1: Lightning Strike + Active le mode Thor (drain mana + TP derrière ennemi toutes les 3s).
 * Phase 2 (clic droit actif): Désactive le mode Thor.
 */
public class ThorPower extends Power {

    private static final double  LIGHTNING_RADIUS = 6.0;
    private static final int     LIGHTNING_COUNT  = 8;
    private static final double  LIGHTNING_DAMAGE = 6.0;
    private static final int     MANA_DRAIN_RATE  = 5;    // toutes les 20 ticks
    private static final double  TP_RADIUS        = 30.0;
    private static final int     TP_INTERVAL      = 60;   // 3 secondes

    // State statique par joueur
    private static final Map<UUID, BukkitTask> thorTasks = new HashMap<>();

    public ThorPower() {
        super("thor_lightning", "§e⚡ Thunder God §7(Thor)",
              "Phase 1: Foudre + Mode Thor actif. Phase 2: Désactiver.",
              PowerType.MAGICIAN, 50, 25);
        setCustomModelId(17);
        this.ultimateCost = 0;
        this.ultimateCooldownSeconds = 180;
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null || !player.isOnline()) return false;

        // Phase 2 : si mode déjà actif, désactiver
        if (uhcPlayer.isThorMode()) {
            deactivateThorMode(uhcPlayer);
            return true;
        }

        // Phase 1 : lightning strike + activer le mode
        if (!canUse(uhcPlayer)) return false;
        consumeResources(uhcPlayer);

        World world = player.getWorld();
        Location origin = player.getLocation();

        // 8 éclairs réels autour de Thor (avec dégâts)
        for (int i = 0; i < LIGHTNING_COUNT; i++) {
            double angle = (2 * Math.PI / LIGHTNING_COUNT) * i;
            double lx = Math.cos(angle) * LIGHTNING_RADIUS;
            double lz = Math.sin(angle) * LIGHTNING_RADIUS;
            Location lightLoc = origin.clone().add(lx, 0, lz);
            world.strikeLightning(lightLoc);
        }

        // Dégâts + Slowness aux joueurs dans le rayon
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player enemy = u.getBukkitPlayer();
            if (enemy == null || !enemy.isOnline() || enemy.equals(player)) continue;
            if (enemy.getLocation().distance(origin) > LIGHTNING_RADIUS) continue;

            enemy.damage(LIGHTNING_DAMAGE, player);
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1)); // Slowness II 3s
            enemy.sendMessage("§e⚡ §cFoudre de §b" + player.getName() + " §c— 6 dégâts + Slowness 3s !");
        }

        // Particules massives
        world.spawnParticle(Particle.ELECTRIC_SPARK, origin.clone().add(0, 1, 0), 80, 1.0, 1.2, 1.0, 0.15);
        world.spawnParticle(Particle.FLASH,          origin.clone().add(0, 1, 0),  5, 0.5, 0.5, 0.5, 0.0);
        world.spawnParticle(Particle.SOUL,           origin.clone().add(0, 1, 0), 20, 0.8, 1.0, 0.8, 0.05);

        world.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.9f);
        world.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_IMPACT,  1.0f, 1.2f);

        // Activer le mode Thor
        uhcPlayer.setThorMode(true);
        player.sendMessage("§e⚡ §bMode Thor actif §e— Drain mana continu + TP ennemi toutes les 3s !");
        player.sendTitle("§e⚡ MODE THOR", "§7Drain mana — Reclic pour désactiver", 5, 60, 15);

        // Tâche de drain mana + TP
        BukkitTask task = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !uhcPlayer.isThorMode()) {
                    uhcPlayer.setThorMode(false);
                    thorTasks.remove(player.getUniqueId());
                    cancel(); return;
                }

                ticksElapsed++;

                // Drain mana toutes les 20 ticks (1 seconde)
                if (ticksElapsed % 20 == 0) {
                    int currentMana = uhcPlayer.getMana();
                    if (currentMana < MANA_DRAIN_RATE) {
                        // Plus de mana : désactiver automatiquement
                        deactivateThorMode(uhcPlayer);
                        cancel(); return;
                    }
                    uhcPlayer.setMana(currentMana - MANA_DRAIN_RATE);
                    ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uhcPlayer);
                }

                // TP derrière l'ennemi toutes les 60 ticks (3 secondes)
                if (ticksElapsed % TP_INTERVAL == 0) {
                    teleportBehindNearestEnemy(player);
                }
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);

        // Stocker la tâche pour pouvoir l'annuler
        BukkitTask old = thorTasks.put(player.getUniqueId(), task);
        if (old != null) old.cancel();

        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        showUltimateIntro(player, "THUNDER GOD MODE", "10s d'invincibilité + frappe chaque ennemi !");
        consumeUltimateResources(uhcPlayer);

        for (Player p : org.bukkit.Bukkit.getOnlinePlayers())
            p.sendMessage("§e⚡ §fThor §7active §eTHUNDER GOD MODE §7— Dieu du tonnerre pendant 10s !");

        // Désactiver mode thor si actif
        if (uhcPlayer.isThorMode()) uhcPlayer.setThorMode(false);

        World world = player.getWorld();

        // Invincible 200 ticks
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 255, false, false));

        // Collecter les ennemis
        java.util.List<Player> enemies = new ArrayList<>();
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(player)) continue;
            enemies.add(other);
        }

        // Visiter chaque ennemi avec 20 ticks de délai entre chacun
        for (int i = 0; i < enemies.size(); i++) {
            final Player target = enemies.get(i);
            final long delay = i * 20L;
            ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(ToaruUHC.getInstance(), () -> {
                if (!player.isOnline()) return;
                UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
                if (uTarget == null || !uTarget.isAlive() || !target.isOnline()) return;

                // Téléporter Thor sur la cible
                player.teleport(target.getLocation().clone().add(1, 0, 0));

                // 3 éclairs
                for (int j = 0; j < 3; j++)
                    world.strikeLightning(target.getLocation());

                target.damage(15.0, player);
                world.spawnParticle(Particle.FIREWORKS_SPARK, target.getLocation().add(0, 1, 0), 30, 0.5, 0.7, 0.5, 0.1);
                target.sendTitle("§e⚡ THUNDER GOD", "§cFrappe de Thor !", 2, 15, 3);
                world.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.0f);
            }, delay);
        }

        // Contrainte après 200 ticks
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                player.damage(10.0);
                uhcPlayer.setMana(0);
                ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uhcPlayer);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 9));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 200, 9));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 9));
                for (Player p : org.bukkit.Bukkit.getOnlinePlayers())
                    p.sendMessage("§e⚡ §fThor §7s'effondre — décharge totale !");
            }
        }.runTaskLater(ToaruUHC.getInstance(), 200L);

        return true;
    }

    private void deactivateThorMode(UHCPlayer uhcPlayer) {
        uhcPlayer.setThorMode(false);
        Player player = uhcPlayer.getBukkitPlayer();
        BukkitTask task = thorTasks.remove(uhcPlayer.getUuid());
        if (task != null) task.cancel();
        if (player != null && player.isOnline()) {
            player.sendMessage("§7⚡ Mode Thor désactivé.");
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.2f);
        }
    }

    private void teleportBehindNearestEnemy(Player thor) {
        Player nearest = null;
        double bestDist = TP_RADIUS;
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(thor)) continue;
            double dist = other.getLocation().distance(thor.getLocation());
            if (dist < bestDist) { bestDist = dist; nearest = other; }
        }
        if (nearest == null) return;

        // Position derrière la cible
        Location behind = nearest.getLocation().subtract(
                nearest.getLocation().getDirection().multiply(2)
        );
        behind.setY(nearest.getLocation().getY());

        World world = thor.getWorld();

        // Particules au départ
        world.spawnParticle(Particle.ELECTRIC_SPARK, thor.getLocation().add(0, 1, 0),
                25, 0.5, 0.7, 0.5, 0.08);

        thor.teleport(behind);

        // Particules à l'arrivée + éclairs décoratifs
        world.spawnParticle(Particle.ELECTRIC_SPARK, behind.clone().add(0, 1, 0),
                25, 0.5, 0.7, 0.5, 0.08);
        for (int i = 0; i < 3; i++) {
            double angle = (2 * Math.PI / 3) * i;
            Location lightLoc = behind.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
            world.strikeLightningEffect(lightLoc);
        }
        world.playSound(behind, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.5f);
        thor.sendMessage("§e⚡ §bTP derrière §c" + nearest.getName() + " §7!");
    }
}
