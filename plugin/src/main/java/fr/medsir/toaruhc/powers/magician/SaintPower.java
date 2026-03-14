package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

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
        this.ultimateCost = 70;
        this.ultimateCooldownSeconds = 200;
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

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        showUltimateIntro(player, "NANASEN", "7 fils tranchants — touche tous les ennemis !");
        consumeUltimateResources(uhcPlayer);

        World world = player.getWorld();
        Bukkit.broadcastMessage("§b⚔ §fKanzaki §7déploie §bNANASEN §7— 7 fils tranchants sur 25 blocs !");

        // Collect alive enemies within 25 blocks
        List<Player> targets = new ArrayList<>();
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player enemy = u.getBukkitPlayer();
            if (enemy == null || !enemy.isOnline() || enemy.equals(player)) continue;
            if (enemy.getLocation().distance(player.getLocation()) <= 25.0) {
                targets.add(enemy);
            }
        }

        // Limit to 7 targets
        int max = Math.min(7, targets.size());
        for (int i = 0; i < max; i++) {
            Player target = targets.get(i);

            // Draw CRIT_MAGIC line from player toward target (wire beam)
            Location from = player.getEyeLocation();
            Location to = target.getLocation().add(0, 1, 0);
            Vector step = to.toVector().subtract(from.toVector()).normalize().multiply(0.5);
            Location cur = from.clone();
            double totalDist = from.distance(to);
            int steps = (int) (totalDist / 0.5);
            for (int s = 0; s < steps; s++) {
                cur.add(step);
                world.spawnParticle(Particle.CRIT_MAGIC, cur, 2, 0.05, 0.05, 0.05, 0.0);
            }

            // Apply wire effects
            target.damage(8.0, player);
            target.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            target.removePotionEffect(PotionEffectType.REGENERATION);
            target.removePotionEffect(PotionEffectType.ABSORPTION);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 2));

            world.spawnParticle(Particle.CRIT_MAGIC, target.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);
            world.playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);
            target.sendTitle("§b⚔ NANASEN", "§7Les fils de l'épée tranchent !", 3, 20, 5);
        }

        // Sounds at player
        world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.2f);

        // CONSTRAINT: Saint's burden
        player.damage(15.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 300, 3));    // Slowness IV 15s
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 2)); // Weakness III 10s
        player.sendMessage("§b⚔ §7Nanasen — §c-15 HP§7, muscles déchirés — Slowness IV 15s + Weakness III 10s");

        return true;
    }
}
