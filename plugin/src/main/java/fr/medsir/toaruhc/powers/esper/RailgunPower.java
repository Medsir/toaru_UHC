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
