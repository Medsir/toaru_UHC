package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;

/**
 * ☣ VECTOR MANIPULATION - Accelerator
 * Le N°1 d'Academy City réfléchit TOUTES les attaques reçues pendant 5 secondes :
 * les dégâts sont renvoyés ×1.5, l'attaquant est propulsé, et les effets de sort
 * sont aussi transférés de la victime vers l'attaquant.
 */
public class AcceleratorPower extends Power {

    private static final int    DURATION_TICKS = 100; // 5 secondes
    private static final double REFLECT_MULT   = 1.5;
    private static final double LAUNCH_SPEED   = 2.5;

    public AcceleratorPower() {
        super("vector_manipulation", "§f☣ Vector Manipulation §7(Accelerator)",
              "Réfléchit toutes les attaques ×1.5 pendant 5 secondes. Renvoie aussi les sorts.",
              PowerType.ESPER, 35, 15);
        setCustomModelId(1);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);
        uhcPlayer.setAcceleratorMode(true);

        World world = player.getWorld();
        world.spawnParticle(Particle.CLOUD,      player.getLocation().add(0, 1, 0), 40, 0.6, 0.7, 0.6, 0.06);
        world.spawnParticle(Particle.CRIT_MAGIC,  player.getLocation().add(0, 1, 0), 30, 0.5, 0.7, 0.5, 0.07);
        world.spawnParticle(Particle.SONIC_BOOM,  player.getLocation().add(0, 1, 0),  5, 0.3, 0.3, 0.3, 0.0);
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.5f);
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,    1.0f, 1.8f);

        player.sendMessage("§f☣ §bVector Manipulation §f— Réflexion active §e5s §f! Toutes les attaques sont renvoyées !");
        player.sendTitle("§f☣", "§7Vecteurs redirigés — 5s", 5, 40, 10);

        // Auto-expiration après 5 secondes — le mode peut avoir déjà été désactivé manuellement
        ToaruUHC.getInstance().getServer().getScheduler()
            .runTaskLater(ToaruUHC.getInstance(), () -> {
                if (uhcPlayer.hasAcceleratorMode()) {
                    uhcPlayer.setAcceleratorMode(false);
                    if (player.isOnline()) {
                        player.sendMessage("§7☣ Vector Manipulation expiré.");
                        player.getWorld().spawnParticle(Particle.CLOUD,
                                player.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.04);
                    }
                }
            }, DURATION_TICKS);

        return true;
    }

    /**
     * Réfléchit l'attaque : renvoie les dégâts amplifiés ×1.5, knockback sur l'attaquant,
     * et transfère TOUS les effets de potion de la victime vers l'attaquant (sort inversé).
     * NE désactive PAS le mode accelerator — reste actif jusqu'à expiration.
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

        // Dégâts réfléchis ×1.5
        attacker.damage(incomingDamage * REFLECT_MULT, defender);

        // Transfert des effets de potion : retirer de la victime, appliquer sur l'attaquant
        Collection<PotionEffect> defenderEffects = new ArrayList<>(defender.getActivePotionEffects());
        for (PotionEffect effect : defenderEffects) {
            defender.removePotionEffect(effect.getType());
            attacker.addPotionEffect(new PotionEffect(
                effect.getType(),
                effect.getDuration(),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.hasParticles()
            ));
        }

        // Effets visuels dramatiques sur le défenseur
        world.spawnParticle(Particle.SONIC_BOOM,  defender.getLocation().add(0, 1, 0),  3, 0.2, 0.2, 0.2, 0.0);
        world.spawnParticle(Particle.CRIT_MAGIC,   defender.getLocation().add(0, 1, 0), 30, 0.6, 0.7, 0.6, 0.09);
        world.spawnParticle(Particle.CLOUD,        defender.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.06);

        // Effets visuels sur l'attaquant
        world.spawnParticle(Particle.CRIT_MAGIC,   attacker.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.06);

        world.playSound(defender.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 1.8f);
        world.playSound(defender.getLocation(), Sound.ENTITY_GENERIC_EXPLODE,   0.5f, 2.0f);

        String effectMsg = defenderEffects.isEmpty()
                ? ""
                : " §7(+" + defenderEffects.size() + " effets transférés)";

        defender.sendMessage("§f☣ §fAttaque réfléchie sur §c" + attacker.getName()
                + " §f(×1.5)" + effectMsg + " §7— Mode toujours actif !");
        attacker.sendMessage("§cTon attaque a été réfléchie par §bAccelerator §c!"
                + (defenderEffects.isEmpty() ? "" : " §7(effets inversés !)"));
        attacker.sendTitle("§c✖ RÉFLÉCHI", "§7Vecteur retourné !", 5, 30, 10);
    }
}
