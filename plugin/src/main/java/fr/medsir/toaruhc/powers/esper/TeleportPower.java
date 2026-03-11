package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class TeleportPower extends Power {
    public TeleportPower() {
        super("teleport", "§d🌀 Teleport §7(Shirai Kuroko)",
              "Téléportation instantanée jusqu'à 15 blocs devant soi.",
              PowerType.ESPER, 25, 6);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        Location origin = player.getLocation().clone();
        Vector dir = player.getLocation().getDirection().normalize();
        Location dest = calculateDestination(player, dir, origin);
        if (dest == null) { player.sendMessage("§cImpossible de se téléporter ici !"); return false; }
        consumeResources(uhcPlayer);
        origin.getWorld().spawnParticle(Particle.PORTAL, origin.add(0,1,0), 30, 0.3, 0.5, 0.3, 0.5);
        origin.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        player.teleport(dest);
        dest.getWorld().spawnParticle(Particle.PORTAL, dest.add(0,1,0), 30, 0.3, 0.5, 0.3, 0.5);
        dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);
        player.sendMessage("§d🌀 §bTeleport §d— Téléportation effectuée !");
        return true;
    }

    private Location calculateDestination(Player player, Vector dir, Location origin) {
        Location current = origin.clone().add(0, 0.1, 0);
        double dist = 0;
        while (dist < 15.0) {
            current.add(dir.clone().multiply(0.5));
            dist += 0.5;
            if (current.getBlock().getType().isSolid()) { current.subtract(dir.clone().multiply(0.5)); break; }
        }
        current.setYaw(player.getLocation().getYaw());
        current.setPitch(player.getLocation().getPitch());
        return current;
    }
}
