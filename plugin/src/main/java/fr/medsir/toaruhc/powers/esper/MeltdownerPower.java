package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * 🔴 MELTDOWNER - Mugino Shizuri (Level 5)
 * Faisceau de plasma animé. Impact : dégâts + Wither II + Cécité.
 * Hitbox plus large que le Railgun, portée plus courte.
 */
public class MeltdownerPower extends Power {

    private static final double DAMAGE         = 12.0;
    private static final double MAX_DISTANCE   = 40.0;
    private static final double STEP           = 0.5;
    private static final int    STEPS_PER_TICK = 3;    // 1.5 blocs/tick = 30 blocs/s
    private static final double HIT_RADIUS     = 1.2;

    public MeltdownerPower() {
        super("meltdowner", "§c🔴 Meltdowner §7(Mugino Shizuri)",
              "Faisceau plasma — Wither II + Cécité à l'impact.",
              PowerType.ESPER, 40, 18);
        setCustomModelId(4);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        Location start    = player.getEyeLocation();
        Vector   direction = player.getLocation().getDirection().normalize();

        player.getWorld().playSound(start, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
        player.sendMessage("§c🔴 §bMeltdowner §c— Feu !");

        fireMeltdowner(player, start, direction);
        return true;
    }

    private void fireMeltdowner(Player shooter, Location start, Vector dir) {
        final Location current = start.clone();
        final double[] dist    = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!shooter.isOnline()) { cancel(); return; }

                for (int i = 0; i < STEPS_PER_TICK; i++) {
                    if (dist[0] >= MAX_DISTANCE) { cancel(); return; }

                    current.add(dir.clone().multiply(STEP));
                    dist[0] += STEP;

                    World world = current.getWorld();

                    // Faisceau plasma rouge-orangé
                    world.spawnParticle(Particle.FLAME,      current, 4, 0.06, 0.06, 0.06, 0.03);
                    world.spawnParticle(Particle.CRIT,       current, 3, 0.08, 0.08, 0.08, 0.0);
                    if ((int)(dist[0] / STEP) % 4 == 0)
                        world.spawnParticle(Particle.CLOUD,  current, 1, 0.05, 0.05, 0.05, 0.0);

                    // Détection entités (hitbox large)
                    for (Entity entity : world.getNearbyEntities(current, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                        if (!(entity instanceof Player target)) continue;
                        if (target == shooter) continue;

                        target.damage(DAMAGE, shooter);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,    60, 1)); // 3s Wither II
                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0)); // 2s Cécité

                        world.createExplosion(target.getLocation().add(0, 0.5, 0), 0f, false, false);
                        world.spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0),
                                50, 0.5, 0.7, 0.5, 0.12);
                        world.spawnParticle(Particle.CRIT,  target.getLocation().add(0, 1, 0),
                                30, 0.3, 0.5, 0.3, 0.05);
                        world.playSound(target.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1f, 0.5f);

                        shooter.sendMessage("§c🔴 §fMeltdowner touche §c" + target.getName() + "§f — Withering !");
                        target.sendMessage("§cMeltdowner de §b" + shooter.getName()
                                + " §c— Wither + Cécité !");
                        target.sendTitle("§c🔴 PLASMA", "§7Withering...", 5, 50, 10);
                        cancel(); return;
                    }

                    // Collision mur
                    if (current.getBlock().getType().isSolid()) {
                        world.createExplosion(current.clone(), 0f, false, false);
                        world.spawnParticle(Particle.FLAME,  current, 20, 0.3, 0.3, 0.3, 0.05);
                        cancel(); return;
                    }
                }
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);
    }
}
