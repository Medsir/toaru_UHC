package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 🛡 NITROGEN ARMOR - Kinuhata Saiai
 * Armor d'azote liquide — Resistance III + Absorption + Strength I pendant 10s.
 * N'envoie pas les attaques en retour, absorbe seulement.
 */
public class KinuhataPower extends Power {

    private static final int DURATION_TICKS = 200; // 10 secondes

    public KinuhataPower() {
        super("nitrogen_armor", "§b🛡 Nitrogen Armor §7(Kinuhata Saiai)",
              "Armor d'azote liquide — Resistance III + Absorption 6 + Strength I 10s.",
              PowerType.ESPER, 40, 25);
        setCustomModelId(22);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = player.getWorld();
        Location loc = player.getLocation();

        // Sons de coating d'azote
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP,  0.8f, 1.5f);

        // Particules BUBBLE_COLUMN en cercle complet (azote)
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
            double rx = Math.cos(angle) * 1.0;
            double rz = Math.sin(angle) * 1.0;
            world.spawnParticle(Particle.BUBBLE_COLUMN_UP,
                    loc.clone().add(rx, 0.2, rz), 3, 0.05, 0.2, 0.05, 0.01);
        }
        // Particules CLOUD (vapeur d'azote)
        world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 25, 0.5, 0.7, 0.5, 0.04);
        // Particules CRIT en cercle (étincelles)
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 6) {
            double rx = Math.cos(angle) * 0.7;
            double rz = Math.sin(angle) * 0.7;
            world.spawnParticle(Particle.CRIT, loc.clone().add(rx, 0.5, rz),
                    3, 0.05, 0.1, 0.05, 0.04);
        }
        // Flocons de neige (effet froid)
        world.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1, 0), 30, 0.6, 0.8, 0.6, 0.04);

        // Effets actifs
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, DURATION_TICKS, 2)); // Resistance III
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,        DURATION_TICKS, 2)); // Absorption III (6 coeurs)
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,   DURATION_TICKS, 0)); // Strength I

        player.sendMessage("§b🛡 §bNitrogen Armor §b— Resistance III + Absorption 6 + Strength I active !");
        player.sendTitle("§b🛡 NITROGEN ARMOR", "§7Azote liquide activé — 10s", 5, 50, 15);

        // À expiration
        final Player finalPlayer = player;
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
            ToaruUHC.getInstance(), () -> {
                if (finalPlayer.isOnline()) {
                    finalPlayer.sendMessage("§b🛡 Nitrogen Armor expiré.");
                    finalPlayer.getWorld().playSound(finalPlayer.getLocation(),
                            Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.5f);
                    finalPlayer.getWorld().spawnParticle(Particle.CLOUD,
                            finalPlayer.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.03);
                }
            }, DURATION_TICKS
        );

        return true;
    }
}
