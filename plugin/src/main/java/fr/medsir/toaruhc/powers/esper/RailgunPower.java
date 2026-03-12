package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * ⚡ RAILGUN - Misaka Mikoto
 * Trait de particules animé + foudre infligeant des dégâts sur impact.
 */
public class RailgunPower extends Power {

    private static final double DAMAGE        = 16.0;
    private static final double MAX_DISTANCE  = 60.0;
    private static final double STEP          = 0.5;   // blocs par itération
    private static final int    STEPS_PER_TICK = 4;    // itérations par tick → 2 blocs/tick = 40 blocs/s

    public RailgunPower() {
        super("railgun", "§e⚡ Railgun §7(Misaka Mikoto)",
              "Lance un trait électrique dévastateur devant soi.",
              PowerType.ESPER, 30, 12);
        setCustomModelId(5);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        Location start     = player.getEyeLocation();
        Vector   direction = player.getLocation().getDirection().normalize();

        // Son de départ
        player.getWorld().playSound(start, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 2.0f);
        player.sendMessage("§e⚡ §bRailgun §e— §fFeu !");

        fireRailgun(player, start, direction);
        return true;
    }

    private void fireRailgun(Player shooter, Location start, Vector dir) {
        final Location current = start.clone();
        final double[] dist    = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                // Sécurité : shooter déconnecté
                if (!shooter.isOnline()) { cancel(); return; }

                for (int i = 0; i < STEPS_PER_TICK; i++) {
                    if (dist[0] >= MAX_DISTANCE) { cancel(); return; }

                    current.add(dir.clone().multiply(STEP));
                    dist[0] += STEP;

                    World world = current.getWorld();

                    // ── Trait de particules ─────────────────────────────────────
                    // Noyau blanc brillant (END_ROD = traînée de lumière)
                    world.spawnParticle(Particle.END_ROD,
                            current, 2, 0.02, 0.02, 0.02, 0.0);
                    // Étincelles électriques autour du faisceau
                    world.spawnParticle(Particle.ELECTRIC_SPARK,
                            current, 4, 0.08, 0.08, 0.08, 0.04);

                    // ── Détection d'entités ──────────────────────────────────────
                    for (Entity entity : world.getNearbyEntities(current, 0.8, 0.8, 0.8)) {
                        if (!(entity instanceof Player target)) continue;
                        if (target == shooter) continue;

                        // Foudre réelle → dégâts + visuel
                        world.strikeLightning(target.getLocation());
                        target.damage(DAMAGE, shooter);

                        // Explosion de particules à l'impact
                        world.spawnParticle(Particle.ELECTRIC_SPARK,
                                target.getLocation().add(0, 1, 0),
                                50, 0.5, 0.6, 0.5, 0.15);
                        world.playSound(target.getLocation(),
                                Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 1.5f);

                        shooter.sendMessage("§e⚡ §fTouche §c" + target.getName() + "§f !");
                        target.sendMessage("§cTouché par le §eRailgun §cde §b"
                                + shooter.getName() + "§c!");
                        cancel(); return;
                    }

                    // ── Collision bloc solide ────────────────────────────────────
                    if (current.getBlock().getType().isSolid()) {
                        // Visuel seulement sur le mur (pas de dégâts terrain)
                        world.strikeLightningEffect(current.clone());
                        world.spawnParticle(Particle.ELECTRIC_SPARK,
                                current, 20, 0.3, 0.3, 0.3, 0.05);
                        cancel(); return;
                    }
                }
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);
    }
}
