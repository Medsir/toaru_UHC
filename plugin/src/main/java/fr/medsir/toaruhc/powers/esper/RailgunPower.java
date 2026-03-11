package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

public class RailgunPower extends Power {
    public RailgunPower() {
        super("railgun", "§e⚡ Railgun §7(Misaka Mikoto)",
              "Lance une pièce à vitesse électromagnétique. Dégâts massifs.",
              PowerType.ESPER, 30, 12);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);
        Vector dir = player.getLocation().getDirection().normalize().multiply(6.0);
        Location start = player.getEyeLocation();
        fireRailgun(player, start, dir);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);
        player.sendMessage("§e⚡ §bRailgun §e— §fPièce propulsée !");
        return true;
    }

    private void fireRailgun(Player shooter, Location start, Vector direction) {
        Location current = start.clone();
        double dist = 0;
        while (dist < 80.0) {
            current.add(direction.clone().multiply(0.5));
            dist += direction.length() * 0.5;
            current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, current, 3, 0.05, 0.05, 0.05, 0);
            for (Entity e : current.getWorld().getNearbyEntities(current, 0.6, 0.6, 0.6)) {
                if (e instanceof Player target && e != shooter) {
                    target.damage(16.0, shooter);
                    target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0,1,0), 25, 0.3, 0.3, 0.3, 0.2);
                    shooter.sendMessage("§e⚡ §fTouche §c" + target.getName() + " §f!");
                    target.sendMessage("§cTouché par le §eRailgun §cde §b" + shooter.getName() + "§c!");
                    return;
                }
            }
            if (current.getBlock().getType().isSolid()) return;
        }
    }
}
