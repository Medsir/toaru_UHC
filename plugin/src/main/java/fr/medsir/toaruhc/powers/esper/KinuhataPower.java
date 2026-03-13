package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * NITROGEN ARMOR - Kinuhata Saiai
 * Armor d'azote liquide — Resistance III + Absorption 6 + Strength I pendant 8s.
 * Contre-attaque automatique : repousse et affaiblit les attaquants (sans annuler les dégâts).
 */
public class KinuhataPower extends Power {

    private static final int DURATION_TICKS = 160; // 8 secondes

    public KinuhataPower() {
        super("nitrogen_armor", "§b🛡 Nitrogen Armor §7(Kinuhata Saiai)",
              "Armor d'azote — Resistance III + Absorption 6 + Strength I 8s. Contre-attaque auto.",
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

        // Sons d'activation
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP,  0.8f, 1.5f);

        // Particules d'activation
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
            double rx = Math.cos(angle) * 1.0;
            double rz = Math.sin(angle) * 1.0;
            world.spawnParticle(Particle.BUBBLE_COLUMN_UP,
                    loc.clone().add(rx, 0.2, rz), 3, 0.05, 0.2, 0.05, 0.01);
        }
        world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 25, 0.5, 0.7, 0.5, 0.04);
        world.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1, 0), 30, 0.6, 0.8, 0.6, 0.04);
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 6) {
            double rx = Math.cos(angle) * 0.7;
            double rz = Math.sin(angle) * 0.7;
            world.spawnParticle(Particle.CRIT, loc.clone().add(rx, 0.5, rz),
                    3, 0.05, 0.1, 0.05, 0.04);
        }

        // Effets actifs
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, DURATION_TICKS, 2)); // Resistance III
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,        DURATION_TICKS, 2)); // Absorption III (6 coeurs)
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,   DURATION_TICKS, 0)); // Strength I

        // Activer le flag
        uhcPlayer.setNitrogenArmorActive(true);

        player.sendMessage("§b🛡 §bNitrogen Armor §b— Resistance III + Absorption 6 + Strength I active !");
        player.sendTitle("§b🛡 NITROGEN ARMOR", "§7Azote liquide activé — 8s", 5, 50, 15);

        // Aura pulsante SNOWFLAKE toutes les 20 ticks
        new BukkitRunnable() {
            int elapsed = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !uhcPlayer.isNitrogenArmorActive() || elapsed >= DURATION_TICKS) {
                    cancel();
                    return;
                }
                Location pLoc = player.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, pLoc, 8, 0.5, 0.6, 0.5, 0.03);
                elapsed += 20;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 20L);

        // À expiration
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
            ToaruUHC.getInstance(), () -> {
                uhcPlayer.setNitrogenArmorActive(false);
                if (player.isOnline()) {
                    player.sendMessage("§b🛡 Nitrogen Armor expiré.");
                    player.getWorld().playSound(player.getLocation(),
                            Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.5f);
                    // Burst de particules à l'expiration
                    Location expLoc = player.getLocation().add(0, 1, 0);
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, expLoc, 40, 0.8, 0.9, 0.8, 0.08);
                    player.getWorld().spawnParticle(Particle.CLOUD,     expLoc, 25, 0.6, 0.7, 0.6, 0.05);
                }
            }, DURATION_TICKS
        );

        return true;
    }

    /**
     * Contre-attaque azote : repousse l'attaquant et lui applique Slowness III + Weakness I.
     * NE CANCELLE PAS les dégâts — s'utilise en complément de Resistance III.
     */
    public static void nitrogenCounter(Player victim, Player attacker) {
        // Pushback
        Vector push = attacker.getLocation().toVector()
                .subtract(victim.getLocation().toVector())
                .setY(0).normalize().multiply(2.2).add(new Vector(0, 0.4, 0));
        attacker.setVelocity(push);
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 2));     // Slowness III 2s
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0)); // Weakness I 2s
        // Particles
        World w = attacker.getWorld();
        w.spawnParticle(Particle.SNOWFLAKE, attacker.getLocation().add(0,1,0), 20, 0.4,0.5,0.4,0.05);
        w.spawnParticle(Particle.CRIT, attacker.getLocation().add(0,1,0), 10, 0.3,0.3,0.3,0.03);
        w.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.2f);
        attacker.sendTitle("§bNITROGEN", "§7Repoussé !", 3, 15, 4);
    }
}
