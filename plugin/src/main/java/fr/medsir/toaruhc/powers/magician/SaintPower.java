package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;

/**
 * ⚔ SAINT'S POWER - Kanzaki Kaori
 * Force II + Regen II + Résistance I pendant 8s. Activation dans le vide.
 */
public class SaintPower extends Power {

    public SaintPower() {
        super("saint_power", "§6⚔ Saint's Power §7(Kanzaki Kaori)",
              "Force II + Regen II + Résistance I pendant 8s.",
              PowerType.MAGICIAN, 40, 20);
        setCustomModelId(10);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,    100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 160, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 160, 0));

        // Effets visuels dorés
        player.getWorld().spawnParticle(
            Particle.CRIT_MAGIC,
            player.getLocation().add(0, 1, 0),
            40, 0.5, 0.5, 0.5, 0.3
        );
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.2f);

        player.sendMessage("§6⚔ §bSaint's Power §6— La force d'un Saint t'envahit !");
        player.sendTitle("§6⚔ SAINT", "§71/7 000 000 000", 5, 50, 10);
        return true;
    }
}
