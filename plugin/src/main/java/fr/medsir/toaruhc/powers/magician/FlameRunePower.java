package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 🔥 FLAME RUNE (Innocentius) - Stiyl Magnus
 * Place une rune de feu à la surface visée (jusqu'à 20 blocs).
 * La rune brûle 15s : 3 anneaux concentriques, spirale de feu, sons dramatiques.
 * RUNE_RADIUS: 5.0, RUNE_DAMAGE: 5.0, RUNE_DURATION: 30 pulses (15s)
 */
public class FlameRunePower extends Power {

    private static final double RUNE_RADIUS    = 5.0;
    private static final double RUNE_DAMAGE    = 5.0;
    private static final int    FIRE_TICKS     = 60;   // 3s de feu
    private static final int    RUNE_DURATION  = 30;   // 15s (30 pulses à 0.5s)
    private static final int    PULSE_INTERVAL = 10;   // ticks entre chaque pulse (0.5s)
    private static final int    TARGET_RANGE   = 20;   // blocs max pour cibler

    public FlameRunePower() {
        super("flame_rune", "§c🔥 Flame Rune §7(Stiyl Magnus)",
              "Pose une rune de feu Innocentius qui brûle les ennemis 15s.",
              PowerType.MAGICIAN, 35, 25);
        setCustomModelId(7);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        Location runeLoc = findRuneLocation(player);
        World world = runeLoc.getWorld();

        // Sons d'invocation
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT,      1.0f, 0.6f);
        world.playSound(runeLoc,              Sound.BLOCK_FIRE_AMBIENT,        1.0f, 0.8f);
        world.playSound(runeLoc,              Sound.ITEM_FLINTANDSTEEL_USE,    1.0f, 1.0f);

        // Explosion de feu au spawn (sans dégâts de blocs)
        world.createExplosion(runeLoc.clone().add(0, 0.5, 0), 0f, false, false);

        // Piliers de feu au spawn
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
            double rx = Math.cos(angle) * RUNE_RADIUS * 0.5;
            double rz = Math.sin(angle) * RUNE_RADIUS * 0.5;
            Location pillarBase = runeLoc.clone().add(rx, 0, rz);
            for (double h = 0; h <= 3; h += 0.5) {
                world.spawnParticle(Particle.FLAME, pillarBase.clone().add(0, h, 0), 3, 0.1, 0.05, 0.1, 0.02);
            }
        }

        // Grande explosion de particules initiale
        world.spawnParticle(Particle.FLAME,      runeLoc.clone().add(0, 0.5, 0), 80, 1.0, 0.5, 1.0, 0.12);
        world.spawnParticle(Particle.LAVA,       runeLoc.clone().add(0, 0.5, 0), 20, 0.8, 0.3, 0.8, 0.05);
        world.spawnParticle(Particle.CRIT_MAGIC, runeLoc.clone().add(0, 0.5, 0), 30, 0.7, 0.4, 0.7, 0.06);

        player.sendMessage("§c🔥 §bFlame Rune §c— Innocentius invoqué ! (15 secondes)");
        player.sendTitle("§c🔥 INNOCENTIUS", "§7La créature de feu brûle pendant 15s", 5, 40, 10);

        final int[] pulsesLeft = {RUNE_DURATION};
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pulsesLeft[0] <= 0 || !player.isOnline()) {
                    world.spawnParticle(Particle.CLOUD, runeLoc, 20, 0.6, 0.4, 0.6, 0.05);
                    world.playSound(runeLoc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);
                    cancel(); return;
                }

                int pulseIndex = RUNE_DURATION - pulsesLeft[0];
                pulsesLeft[0]--;

                // Son dramatique toutes les 5 pulses
                if (pulseIndex % 5 == 0) {
                    world.playSound(runeLoc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.6f);
                }

                // Anneau interne (rayon 1.5)
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double rx = Math.cos(angle) * 1.5;
                    double rz = Math.sin(angle) * 1.5;
                    Location rp = runeLoc.clone().add(rx, 0.1, rz);
                    world.spawnParticle(Particle.CRIT_MAGIC, rp, 1, 0.0, 0.1, 0.0, 0.0);
                    world.spawnParticle(Particle.FLAME,       rp, 1, 0.0, 0.1, 0.0, 0.02);
                }

                // Anneau milieu (rayon 3.0)
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 10) {
                    double rx = Math.cos(angle) * 3.0;
                    double rz = Math.sin(angle) * 3.0;
                    Location rp = runeLoc.clone().add(rx, 0.1, rz);
                    world.spawnParticle(Particle.FLAME, rp, 1, 0.0, 0.15, 0.0, 0.01);
                }

                // Anneau extérieur (rayon 5.0)
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
                    double rx = Math.cos(angle) * RUNE_RADIUS;
                    double rz = Math.sin(angle) * RUNE_RADIUS;
                    Location rp = runeLoc.clone().add(rx, 0.1, rz);
                    world.spawnParticle(Particle.FLAME, rp, 1, 0.0, 0.2, 0.0, 0.01);
                    // Particules LAVA aux cardinaux
                    if (Math.abs(rx) < 0.6 || Math.abs(rz) < 0.6) {
                        world.spawnParticle(Particle.LAVA, rp, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }

                // Spirale montante centrale
                for (int i = 0; i < 16; i++) {
                    double spiralAngle = (pulseIndex * 0.5 + i * Math.PI / 8);
                    double spiralH = i * 0.15;
                    double sr = 0.4;
                    double sx = Math.cos(spiralAngle) * sr;
                    double sz = Math.sin(spiralAngle) * sr;
                    world.spawnParticle(Particle.FLAME, runeLoc.clone().add(sx, spiralH, sz),
                            1, 0.04, 0.02, 0.04, 0.01);
                }

                // Particules centrales
                world.spawnParticle(Particle.CRIT_MAGIC, runeLoc, 5, 0.4, 0.3, 0.4, 0.06);

                // Dégâts aux ennemis proches
                for (Entity entity : world.getNearbyEntities(runeLoc, RUNE_RADIUS, RUNE_RADIUS, RUNE_RADIUS)) {
                    if (!(entity instanceof Player target)) continue;
                    if (target.equals(player)) continue;

                    UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
                    if (uTarget == null || !uTarget.isAlive()) continue;

                    target.damage(RUNE_DAMAGE, player);
                    target.setFireTicks(Math.max(target.getFireTicks(), FIRE_TICKS));
                    target.sendMessage("§c🔥 Tu brûles dans la §bFlame Rune §cd'Innocentius de §b"
                            + player.getName() + "§c !");
                }
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, PULSE_INTERVAL);

        return true;
    }

    private Location findRuneLocation(Player player) {
        Block target = player.getTargetBlock(null, TARGET_RANGE);
        if (target != null && target.getType().isSolid()) {
            return target.getLocation().add(0.5, 1.0, 0.5);
        }
        return player.getEyeLocation()
                .add(player.getLocation().getDirection().normalize().multiply(10));
    }
}
