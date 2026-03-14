package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * ⬛ DARK MATTER - Kakine Teitoku (Level 5 N°2)
 * Explosion de matière noire en AOE : Blindness + Wither + knockback sur les ennemis.
 * Bonus personnel : Night Vision + Speed pendant 3s.
 */
public class DarkMatterPower extends Power {

    private static final double DAMAGE   = 8.0;
    private static final double RADIUS   = 5.0;
    private static final double KB_SPEED = 2.0;

    private static final Random RANDOM = new Random();

    public DarkMatterPower() {
        super("dark_matter", "§8⬛ Dark Matter §7(Kakine Teitoku)",
              "Explosion de matière noire — Cécité + Wither + knockback AOE.",
              PowerType.ESPER, 35, 22);
        setCustomModelId(11);
        this.ultimateCost = 0;
        this.ultimateCooldownSeconds = 180;
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World    world = player.getWorld();
        Location loc   = player.getLocation();

        // Buffs personnels : Night Vision + Speed
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        60, 0));

        // Explosion de matière noire
        world.spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0),
                50, RADIUS * 0.5, RADIUS * 0.4, RADIUS * 0.5, 0.5);
        world.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0),
                30, RADIUS * 0.4, RADIUS * 0.4, RADIUS * 0.4, 0.1);
        world.spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0),
                25, RADIUS * 0.5, RADIUS * 0.3, RADIUS * 0.5, 0.2);
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP,  0.8f, 0.4f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,    1.0f, 0.5f);

        // AOE sur les ennemis proches
        int hits = 0;
        for (Entity entity : world.getNearbyEntities(loc, RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(player)) continue;

            target.damage(DAMAGE, player);
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0)); // 3s
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,    40, 0)); // 2s

            Vector kb = target.getLocation().toVector()
                    .subtract(loc.toVector()).normalize()
                    .multiply(KB_SPEED).add(new Vector(0, 0.5, 0));
            target.setVelocity(kb);

            world.spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0),
                    15, 0.3, 0.4, 0.3, 0.1);
            target.sendMessage("§8⬛ §cDark Matter de §b" + player.getName()
                    + " §c— Aveuglé + Wither !");
            hits++;
        }

        player.sendMessage("§8⬛ §bDark Matter §8— " + hits + " cible(s) !"
                + " §7Night Vision + Speed actifs 3s !");
        player.sendTitle("§8⬛ DARK MATTER", "§7Wings of the N°2", 5, 40, 10);
        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        showUltimateIntro(player, "DARK MATTER AWAKENING", "Sphère de Dark Matter — expansion 5s !");
        consumeUltimateResources(uhcPlayer);

        World world = player.getWorld();
        Bukkit.broadcastMessage("§8💀 §fKakine §7libère §8DARK MATTER AWAKENING §7— La matière noire dévore tout !");

        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);
        world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.6f);

        final int[] ticks = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || ticks[0] >= 100) {
                    // MASSIVE FINAL EXPLOSION
                    if (player.isOnline()) {
                        Location loc = player.getLocation();
                        World w = player.getWorld();
                        w.createExplosion(loc, 6.0f, false, false, player);

                        // 8 smaller explosions around
                        for (int i = 0; i < 8; i++) {
                            double angle = (Math.PI * 2 / 8) * i;
                            double r = 5 + RANDOM.nextDouble() * 10;
                            double rx = Math.cos(angle) * r;
                            double rz = Math.sin(angle) * r;
                            w.createExplosion(loc.clone().add(rx, 0, rz), 3.0f, false, false, player);
                        }

                        // EXPLOSION_HUGE x20
                        for (int i = 0; i < 20; i++) {
                            w.spawnParticle(Particle.EXPLOSION_HUGE, loc.clone().add(
                                    (RANDOM.nextDouble() - 0.5) * 10,
                                    RANDOM.nextDouble() * 3,
                                    (RANDOM.nextDouble() - 0.5) * 10), 1, 0, 0, 0, 0);
                        }
                        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
                    }
                    cancel();
                    return;
                }

                // Current radius grows from 5 to 25 over 5s
                double currentRadius = 5 + (ticks[0] / 5.0) * 2;
                Location playerLoc = player.getLocation();
                World w = player.getWorld();

                // Affect alive enemies within radius
                for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
                    if (!u.isAlive()) continue;
                    Player target = u.getBukkitPlayer();
                    if (target == null || !target.isOnline() || target.equals(player)) continue;
                    if (target.getLocation().distance(playerLoc) > currentRadius) continue;

                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 1));     // Wither II 2s
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                    target.damage(2.0, player);

                    // Pull toward center
                    Vector pull = playerLoc.toVector().subtract(target.getLocation().toVector())
                            .normalize().multiply(0.8);
                    target.setVelocity(target.getVelocity().add(pull));
                }

                // Sphere of SCULK_SOUL at radius surface
                for (double angle1 = 0; angle1 < Math.PI * 2; angle1 += Math.PI / 8) {
                    for (double angle2 = 0; angle2 < Math.PI; angle2 += Math.PI / 6) {
                        double rx = Math.cos(angle1) * Math.sin(angle2) * currentRadius;
                        double ry = Math.cos(angle2) * currentRadius;
                        double rz = Math.sin(angle1) * Math.sin(angle2) * currentRadius;
                        w.spawnParticle(Particle.SCULK_SOUL,
                                playerLoc.clone().add(rx, ry, rz), 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }

                // SMOKE_LARGE spiral
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 6) {
                    double r = 1.5;
                    double rx = Math.cos(angle + ticks[0] * 0.1) * r;
                    double rz = Math.sin(angle + ticks[0] * 0.1) * r;
                    w.spawnParticle(Particle.SMOKE_LARGE, playerLoc.clone().add(rx, 1, rz),
                            1, 0.0, 0.1, 0.0, 0.02);
                }

                ticks[0] += 5;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 5L);

        // CONSTRAINT: immediately
        player.damage(20.0);
        uhcPlayer.setCooldown("dark_matter", 90);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 300, 0)); // Wither I 15s on self
        player.sendMessage("§8💀 §7Dark Matter Awakening — §c-20 HP§7, Wither I 15s, pouvoir désactivé 90s");

        return true;
    }
}
