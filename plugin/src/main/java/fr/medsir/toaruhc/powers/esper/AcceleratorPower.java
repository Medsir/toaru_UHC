package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * ☣ VECTOR MANIPULATION - Accelerator
 * Le N°1 d'Academy City réfléchit la prochaine attaque physique reçue :
 * les dégâts sont renvoyés ×1.5 et l'attaquant est propulsé en arrière.
 */
public class AcceleratorPower extends Power {

    private static final int    DURATION_TICKS = 80; // 4 secondes
    private static final double REFLECT_MULT   = 1.5;
    private static final double LAUNCH_SPEED   = 2.5;

    public AcceleratorPower() {
        super("vector_manipulation", "§f☣ Vector Manipulation §7(Accelerator)",
              "Réfléchit la prochaine attaque ×1.5 + propulsion.",
              PowerType.ESPER, 35, 15);
        setCustomModelId(1);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        uhcPlayer.setAcceleratorMode(true);

        // Halo blanc tourbillonnant
        World world = player.getWorld();
        world.spawnParticle(Particle.CLOUD,     player.getLocation().add(0, 1, 0), 40, 0.6, 0.7, 0.6, 0.06);
        world.spawnParticle(Particle.CRIT_MAGIC, player.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);

        player.sendMessage("§f☣ §bVector Manipulation §f— Réflexion active §e4s §f!");
        player.sendTitle("§f☣", "§7Vecteurs redirigés", 5, 40, 10);

        // Auto-expiration
        ToaruUHC.getInstance().getServer().getScheduler()
            .runTaskLater(ToaruUHC.getInstance(), () -> {
                if (uhcPlayer.hasAcceleratorMode()) {
                    uhcPlayer.setAcceleratorMode(false);
                    if (player.isOnline()) player.sendMessage("§7☣ Vector Manipulation expiré.");
                }
            }, DURATION_TICKS);

        return true;
    }

    /**
     * Appelé depuis PowerListener quand Accelerator (victim) reçoit un coup.
     * Annule les dégâts reçus, les renvoie × 1.5 et propulse l'attaquant.
     */
    public static void reflect(Player defender, Player attacker, double incomingDamage) {
        World world = defender.getWorld();

        // Propulsion radiale de l'attaquant
        Vector dir = attacker.getLocation().toVector()
                .subtract(defender.getLocation().toVector())
                .normalize()
                .multiply(LAUNCH_SPEED)
                .add(new Vector(0, 1.0, 0));
        attacker.setVelocity(dir);

        // Dégâts réfléchis
        attacker.damage(incomingDamage * REFLECT_MULT, defender);

        // Effets
        world.spawnParticle(Particle.CLOUD, defender.getLocation().add(0, 1, 0), 25, 0.5, 0.6, 0.5, 0.08);
        world.playSound(defender.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 2.0f);

        defender.sendMessage("§f☣ §fAttaque réfléchie sur §c" + attacker.getName() + " §f(×1.5) !");
        attacker.sendMessage("§cTon attaque a été réfléchie par §bAccelerator §c!");
        attacker.sendTitle("§c✖ RÉFLÉCHI", "§7Vecteur retourné !", 5, 30, 10);
    }
}
