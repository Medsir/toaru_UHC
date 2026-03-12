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
 * La rune brûle 10s : tout joueur dans un rayon de 2.5 blocs prend 3 dégâts
 * et est enflammé 3s toutes les 0.5s.
 */
public class FlameRunePower extends Power {

    private static final double RUNE_RADIUS    = 2.5;
    private static final double RUNE_DAMAGE    = 3.0;
    private static final int    FIRE_TICKS     = 60;  // 3s de feu
    private static final int    RUNE_DURATION  = 20;  // 10s (20 pulses à 0.5s)
    private static final int    PULSE_INTERVAL = 10;  // ticks entre chaque pulse (0.5s)
    private static final int    TARGET_RANGE   = 20;  // blocs max pour cibler

    public FlameRunePower() {
        super("flame_rune", "§c🔥 Flame Rune §7(Stiyl Magnus)",
              "Pose une rune de feu Innocentius qui brûle les ennemis 10s.",
              PowerType.MAGICIAN, 35, 25);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        // Localiser la surface ciblée
        Location runeLoc = findRuneLocation(player);

        World world = runeLoc.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.6f);
        world.playSound(runeLoc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
        player.sendMessage("§c🔥 §bFlame Rune §c— Innocentius invoqué !");
        player.sendTitle("§c🔥 INNOCENTIUS", "§7La rune brûle pendant 10s", 5, 40, 10);

        // Rune persistante
        final int[] pulsesLeft = {RUNE_DURATION};
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pulsesLeft[0] <= 0 || !player.isOnline()) {
                    // Fin : particules d'extinction
                    world.spawnParticle(Particle.CLOUD, runeLoc, 15, 0.5, 0.3, 0.5, 0.05);
                    world.playSound(runeLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.2f);
                    cancel(); return;
                }

                pulsesLeft[0]--;

                // Dessin de la rune (anneau de flammes)
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double rx = Math.cos(angle) * RUNE_RADIUS * 0.6;
                    double rz = Math.sin(angle) * RUNE_RADIUS * 0.6;
                    Location rp = runeLoc.clone().add(rx, 0.1, rz);
                    world.spawnParticle(Particle.FLAME, rp, 1, 0.0, 0.1, 0.0, 0.0);
                }
                world.spawnParticle(Particle.CRIT_MAGIC, runeLoc, 4, 0.3, 0.2, 0.3, 0.05);

                // Dégâts aux ennemis proches
                for (Entity entity : world.getNearbyEntities(runeLoc, RUNE_RADIUS, RUNE_RADIUS, RUNE_RADIUS)) {
                    if (!(entity instanceof Player target)) continue;
                    if (target.equals(player)) continue;

                    UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
                    if (uTarget == null || !uTarget.isAlive()) continue;

                    target.damage(RUNE_DAMAGE, player);
                    target.setFireTicks(Math.max(target.getFireTicks(), FIRE_TICKS));
                    target.sendMessage("§c🔥 Tu brûles dans la §bFlame Rune §cde §b" + player.getName() + "§c !");
                }
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, PULSE_INTERVAL);

        return true;
    }

    /**
     * Trouve le bloc visé jusqu'à TARGET_RANGE blocs.
     * Si aucune surface solide, pose la rune 10 blocs devant.
     */
    private Location findRuneLocation(Player player) {
        Block target = player.getTargetBlock(null, TARGET_RANGE);
        if (target != null && target.getType().isSolid()) {
            return target.getLocation().add(0.5, 1.0, 0.5);
        }
        // Fallback : 10 blocs devant le joueur
        return player.getEyeLocation()
                .add(player.getLocation().getDirection().normalize().multiply(10));
    }
}
