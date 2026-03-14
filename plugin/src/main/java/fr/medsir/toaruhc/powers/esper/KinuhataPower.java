package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * NITROGEN ARMOR - Kinuhata Saiai
 * Armor d'azote liquide — Resistance III + Absorption 6 + Strength I pendant 8s.
 * Contre-attaque automatique : repousse et affaiblit les attaquants (sans annuler les dégâts).
 */
public class KinuhataPower extends Power {

    private static final int DURATION_TICKS = 160; // 8 secondes

    public KinuhataPower() {
        super("nitrogen_armor", "§b🛡 Nitrogen Armor §7(Kinuhata Saiai)",
              "Armor d'azote — Resistance III + Absorption 6 + Strength I 8s. Contre-attaque auto.",
              PowerType.ESPER, 40, 25);
        setCustomModelId(22);
        this.ultimateCost = 0;
        this.ultimateCooldownSeconds = 180;
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = player.getWorld();
        Location loc = player.getLocation();

        // Sons d'activation
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP,  0.8f, 1.5f);

        // Particules d'activation
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
            double rx = Math.cos(angle) * 1.0;
            double rz = Math.sin(angle) * 1.0;
            world.spawnParticle(Particle.BUBBLE_COLUMN_UP,
                    loc.clone().add(rx, 0.2, rz), 3, 0.05, 0.2, 0.05, 0.01);
        }
        world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 25, 0.5, 0.7, 0.5, 0.04);
        world.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1, 0), 30, 0.6, 0.8, 0.6, 0.04);
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 6) {
            double rx = Math.cos(angle) * 0.7;
            double rz = Math.sin(angle) * 0.7;
            world.spawnParticle(Particle.CRIT, loc.clone().add(rx, 0.5, rz),
                    3, 0.05, 0.1, 0.05, 0.04);
        }

        // Effets actifs
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, DURATION_TICKS, 2)); // Resistance III
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,        DURATION_TICKS, 2)); // Absorption III (6 coeurs)
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,   DURATION_TICKS, 0)); // Strength I

        // Activer le flag
        uhcPlayer.setNitrogenArmorActive(true);

        player.sendMessage("§b🛡 §bNitrogen Armor §b— Resistance III + Absorption 6 + Strength I active !");
        player.sendTitle("§b🛡 NITROGEN ARMOR", "§7Azote liquide activé — 8s", 5, 50, 15);

        // Aura pulsante SNOWFLAKE toutes les 20 ticks
        new BukkitRunnable() {
            int elapsed = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !uhcPlayer.isNitrogenArmorActive() || elapsed >= DURATION_TICKS) {
                    cancel();
                    return;
                }
                Location pLoc = player.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, pLoc, 8, 0.5, 0.6, 0.5, 0.03);
                elapsed += 20;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 20L);

        // À expiration
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
            ToaruUHC.getInstance(), () -> {
                uhcPlayer.setNitrogenArmorActive(false);
                if (player.isOnline()) {
                    player.sendMessage("§b🛡 Nitrogen Armor expiré.");
                    player.getWorld().playSound(player.getLocation(),
                            Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.5f);
                    // Burst de particules à l'expiration
                    Location expLoc = player.getLocation().add(0, 1, 0);
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, expLoc, 40, 0.8, 0.9, 0.8, 0.08);
                    player.getWorld().spawnParticle(Particle.CLOUD,     expLoc, 25, 0.6, 0.7, 0.6, 0.05);
                }
            }, DURATION_TICKS
        );

        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        showUltimateIntro(player, "NITROGEN ABSOLUTE ZERO", "Gel instantané 20 blocs — explosion 5s !");
        consumeUltimateResources(uhcPlayer);

        World world = player.getWorld();
        Bukkit.broadcastMessage("§b❄ §fKinuhata §7active §bNITROGEN ABSOLUTE ZERO §7— Gel instantané + explosion 5s !");

        // Collect alive enemies in 20 blocks
        List<Player> frozenTargets = new ArrayList<>();
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player enemy = u.getBukkitPlayer();
            if (enemy == null || !enemy.isOnline() || enemy.equals(player)) continue;
            if (enemy.getLocation().distance(player.getLocation()) <= 20.0) {
                frozenTargets.add(enemy);
            }
        }

        // Apply freeze to each enemy
        for (Player target : frozenTargets) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 120, 9));          // can't move 6s
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 120, 9));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));      // 3s blindness
            world.spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0),
                    30, 0.5, 0.7, 0.5, 0.0);
            target.sendTitle("§b❄ NITROGEN ZERO", "§7Gelé — explosion imminente !", 3, 80, 10);
        }

        // Sounds
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);
        world.playSound(player.getLocation(), Sound.AMBIENT_BASALT_DELTAS_LOOP, 0.8f, 0.5f);

        // After 100 ticks (5s): detonate frozen enemies
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
                ToaruUHC.getInstance(), () -> {
                    // Invincibilité pour ne pas prendre ses propres explosions
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 255, false, false));
                    for (Player target : frozenTargets) {
                        if (!target.isOnline()) continue;
                        UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
                        if (uTarget == null || !uTarget.isAlive()) continue;

                        world.createExplosion(target.getLocation(), 3.0f, false, false, player);
                        target.setVelocity(new Vector(
                                (Math.random() - 0.5) * 1.5,
                                3.0,
                                (Math.random() - 0.5) * 1.5));
                        target.damage(20.0, player);
                        world.spawnParticle(Particle.SNOWFLAKE, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.05);
                        world.spawnParticle(Particle.CLOUD, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.05);
                    }

                    // CONSTRAINT
                    if (player.isOnline()) {
                        uhcPlayer.setNitrogenArmorActive(false);
                        player.damage(10.0);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 600, 2)); // Weakness III 30s
                        uhcPlayer.setCooldown("nitrogen_armor", 30);
                        Bukkit.broadcastMessage("§b❄ §fKinuhata §7— L'armure azote se fracasse — épuisée !");
                    }
                }, 100L);

        return true;
    }

    /**
     * Contre-attaque azote : repousse l'attaquant et lui applique Slowness III + Weakness I.
     * NE CANCELLE PAS les dégâts — s'utilise en complément de Resistance III.
     */
    public static void nitrogenCounter(Player victim, Player attacker) {
        // Pushback
        Vector push = attacker.getLocation().toVector()
                .subtract(victim.getLocation().toVector())
                .setY(0).normalize().multiply(2.2).add(new Vector(0, 0.4, 0));
        attacker.setVelocity(push);
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 2));     // Slowness III 2s
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0)); // Weakness I 2s
        // Particles
        World w = attacker.getWorld();
        w.spawnParticle(Particle.SNOWFLAKE, attacker.getLocation().add(0,1,0), 20, 0.4,0.5,0.4,0.05);
        w.spawnParticle(Particle.CRIT, attacker.getLocation().add(0,1,0), 10, 0.3,0.3,0.3,0.03);
        w.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.2f);
        attacker.sendTitle("§bNITROGEN", "§7Repoussé !", 3, 15, 4);
    }
}
