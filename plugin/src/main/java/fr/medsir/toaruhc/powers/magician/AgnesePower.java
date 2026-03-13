package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * LOTUS WAND — Agnese Sanctis
 * Rayon précis (30 blocs). Sur touche : 18 dégâts + vole effets positifs + efface effets cible + force 30s cooldown.
 */
public class AgnesePower extends Power {

    private static final double RAYCAST_RANGE   = 30.0;
    private static final double RAYCAST_STEP    = 0.5;
    private static final double HIT_RADIUS      = 1.2;
    private static final double DAMAGE          = 18.0;
    private static final int    FORCED_COOLDOWN = 30;

    // Effets considérés comme "positifs" à voler
    private static final List<PotionEffectType> POSITIVE_EFFECTS = List.of(
            PotionEffectType.SPEED, PotionEffectType.INCREASE_DAMAGE, PotionEffectType.DAMAGE_RESISTANCE,
            PotionEffectType.REGENERATION, PotionEffectType.ABSORPTION, PotionEffectType.INCREASE_DAMAGE,
            PotionEffectType.JUMP, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.INVISIBILITY,
            PotionEffectType.SATURATION, PotionEffectType.HEALTH_BOOST, PotionEffectType.HERO_OF_THE_VILLAGE
    );

    public AgnesePower() {
        super("agnese", "§6🪷 Lotus Wand §7(Agnese Sanctis)",
              "Rayon 30 blocs — 18 dmg, vole effets positifs, force 30s cooldown.",
              PowerType.MAGICIAN, 60, 20);
        setCustomModelId(29);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player agnese = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = agnese.getWorld();
        Location cur = agnese.getEyeLocation();
        Vector dir = agnese.getLocation().getDirection().normalize();
        double dist = 0;

        // Raycast
        while (dist < RAYCAST_RANGE) {
            cur.add(dir.clone().multiply(RAYCAST_STEP));
            dist += RAYCAST_STEP;

            // Particules le long du rayon
            world.spawnParticle(Particle.FLAME, cur, 1, 0.02, 0.02, 0.02, 0.0);
            if ((int)(dist / RAYCAST_STEP) % 3 == 0)
                world.spawnParticle(Particle.CRIT_MAGIC, cur, 1, 0.05, 0.05, 0.05, 0.0);

            if (cur.getBlock().getType().isSolid()) {
                world.spawnParticle(Particle.SMOKE_NORMAL, cur, 8, 0.2, 0.2, 0.2, 0.02);
                break;
            }

            for (Entity entity : world.getNearbyEntities(cur, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                if (!(entity instanceof Player target)) continue;
                if (target.equals(agnese)) continue;
                UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
                if (uTarget == null || !uTarget.isAlive()) continue;

                // 1. Voler les effets positifs
                List<PotionEffect> stolen = new ArrayList<>();
                for (PotionEffectType type : POSITIVE_EFFECTS) {
                    PotionEffect effect = target.getPotionEffect(type);
                    if (effect != null) {
                        stolen.add(effect);
                        agnese.addPotionEffect(effect);
                        target.removePotionEffect(type);
                    }
                }

                // 2. Effacer tous les effets restants de la cible
                for (PotionEffect pe : new ArrayList<>(target.getActivePotionEffects()))
                    target.removePotionEffect(pe.getType());

                // 3. Dégâts
                target.damage(DAMAGE, agnese);

                // 4. Force cooldown sur le pouvoir de la cible
                if (uTarget.getPower() != null)
                    uTarget.setCooldown(uTarget.getPower().getId(), FORCED_COOLDOWN);

                // Effets visuels
                world.spawnParticle(Particle.FLAME,  target.getLocation().add(0,1,0), 30, 0.4,0.6,0.4,0.08);
                world.spawnParticle(Particle.CRIT,   target.getLocation().add(0,1,0), 25, 0.3,0.5,0.3,0.06);
                world.spawnParticle(Particle.HEART,  target.getLocation().add(0,2,0), stolen.size()+1, 0.5,0.2,0.5,0.1);
                world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT,        1.0f, 0.6f);
                world.playSound(target.getLocation(), Sound.ENTITY_WITCH_DRINK,        0.8f, 1.3f);

                agnese.sendMessage("§6🪷 Lotus Wand §7— §f" + target.getName()
                        + " §7touché (§c-18 HP§7) ! §a"
                        + stolen.size() + " effet(s) volé(s)§7, §eCooldown forcé " + FORCED_COOLDOWN + "s§7 !");
                target.sendTitle("§6LOTUS WAND", "§7Effets volés & cooldown forcé !", 3, 20, 5);
                target.sendMessage("§6🪷 §7Agnese t'a frappé — §c-18 HP§7, §ceffets effacés§7, §ecooldown forcé " + FORCED_COOLDOWN + "s§7 !");

                agnese.sendTitle("§6🪷 LOTUS WAND", "§7Touché !", 3, 20, 5);
                world.playSound(agnese.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                return true;
            }
        }

        // Raté
        agnese.sendMessage("§6🪷 Lotus Wand §7— §cRaté !");
        return true;
    }
}
