package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * 🌀 TELEPORT - Shirai Kuroko
 * Téléportation 15 blocs dans la direction du regard, sans contrainte de bloc.
 */
public class TeleportPower extends Power {

    private static final double MAX_DISTANCE = 15.0;
    private static final double STEP         = 0.5;

    public TeleportPower() {
        super("teleport", "§d🌀 Teleport §7(Shirai Kuroko)",
              "Téléportation instantanée 15 blocs devant soi.",
              PowerType.ESPER, 25, 6);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();

        Location origin = player.getLocation().clone();
        Vector   dir    = player.getLocation().getDirection().normalize();
        Location dest   = findDestination(player, origin, dir);

        consumeResources(uhcPlayer);

        // Effets à l'origine
        origin.getWorld().spawnParticle(Particle.PORTAL, origin.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.5);
        origin.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);

        player.teleport(dest);

        // Effets à la destination
        dest.getWorld().spawnParticle(Particle.PORTAL, dest.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.5);
        dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);

        player.sendMessage("§d🌀 §bTeleport §d— Téléportation !");
        return true;
    }

    /**
     * Avance pas à pas dans la direction du regard.
     * S'arrête avant un bloc solide ou au max (15 blocs).
     * Cherche toujours une position où le joueur peut tenir debout (2 blocs de hauteur).
     */
    private Location findDestination(Player player, Location origin, Vector dir) {
        Location current  = origin.clone().add(0, 0.1, 0);
        Location lastSafe = origin.clone();
        double dist = 0;

        while (dist < MAX_DISTANCE) {
            current.add(dir.clone().multiply(STEP));
            dist += STEP;

            Location feet = current.clone();
            Location head = current.clone().add(0, 1, 0);

            // Si le bloc des pieds ou de la tête est solide → on s'arrête
            if (feet.getBlock().getType().isSolid() || head.getBlock().getType().isSolid()) {
                break;
            }

            // Sinon c'est une position valide
            lastSafe = current.clone();
        }

        // Ajuster pour poser les pieds sur le sol
        lastSafe = adjustToGround(lastSafe);
        lastSafe.setYaw(player.getLocation().getYaw());
        lastSafe.setPitch(player.getLocation().getPitch());
        return lastSafe;
    }

    private Location adjustToGround(Location loc) {
        Location check = loc.clone();
        // Descendre jusqu'au sol (max 5 blocs)
        for (int i = 0; i < 5; i++) {
            if (check.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) return check;
            check.subtract(0, 1, 0);
        }
        return loc;
    }
}
