package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 🔧 ITEM EXOSQUELETTE - Hamazura Shiage
 * Level 0 avec exosquelette militaire — Resistance + Strength + Speed + Absorption 12s.
 */
public class HamazuraPower extends Power {

    private static final int DURATION_TICKS = 240; // 12 secondes

    public HamazuraPower() {
        super("item_exoskeleton", "§7🔧 ITEM System §7(Hamazura Shiage)",
              "Exosquelette opérationnel — Resistance + Force pendant 12s.",
              PowerType.ESPER, 20, 35);
        setCustomModelId(19);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = player.getWorld();
        Location loc = player.getLocation();

        // Sons d'équipement
        world.playSound(loc, Sound.BLOCK_ANVIL_PLACE,         1.0f, 1.0f);
        world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_DIAMOND,  1.0f, 1.2f);
        world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_DIAMOND,  0.8f, 0.9f);
        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP,     0.6f, 1.5f);

        // Particules en cercle (étincelles métalliques)
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
            double rx = Math.cos(angle) * 0.8;
            double rz = Math.sin(angle) * 0.8;
            world.spawnParticle(Particle.CRIT, loc.clone().add(rx, 0.5, rz),
                    3, 0.05, 0.1, 0.05, 0.05);
        }
        // Fumée de démarrage des servos
        for (int h = 0; h <= 3; h++) {
            world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, h * 0.5, 0),
                    4, 0.3, 0.1, 0.3, 0.02);
        }
        // Circuits électriques
        world.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1, 0),
                30, 0.5, 0.7, 0.5, 0.06);

        // Effets actifs
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, DURATION_TICKS, 1)); // Resistance II
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,   DURATION_TICKS, 1)); // Strength II
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,             DURATION_TICKS, 0)); // Speed I
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,        DURATION_TICKS, 1)); // Absorption II (4 coeurs)

        player.sendMessage("§7🔧 §bExosquelette ITEM §7activé — Resistance II + Strength II + Speed I + Absorption !");
        player.sendTitle("§7🔧 EXOSQUELETTE ACTIF", "§7Resistance + Force + Speed — 12s", 5, 50, 15);

        // À expiration
        final Player finalPlayer = player;
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
            ToaruUHC.getInstance(), () -> {
                if (finalPlayer.isOnline()) {
                    finalPlayer.sendMessage("§7🔧 Exosquelette désactivé.");
                    finalPlayer.getWorld().playSound(finalPlayer.getLocation(),
                            Sound.BLOCK_ANVIL_DESTROY, 0.8f, 0.7f);
                    finalPlayer.getWorld().spawnParticle(Particle.SMOKE_NORMAL,
                            finalPlayer.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.03);
                }
            }, DURATION_TICKS
        );

        return true;
    }
}
