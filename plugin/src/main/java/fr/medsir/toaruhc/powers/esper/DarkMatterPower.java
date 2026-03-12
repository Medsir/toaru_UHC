package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

/**
 * ⬛ DARK MATTER - Kakine Teitoku (Level 5 N°2)
 * Explosion de matière noire en AOE : Blindness + Wither + knockback sur les ennemis.
 * Bonus personnel : Night Vision + Speed pendant 3s.
 */
public class DarkMatterPower extends Power {

    private static final double DAMAGE   = 8.0;
    private static final double RADIUS   = 5.0;
    private static final double KB_SPEED = 2.0;

    public DarkMatterPower() {
        super("dark_matter", "§8⬛ Dark Matter §7(Kakine Teitoku)",
              "Explosion de matière noire — Cécité + Wither + knockback AOE.",
              PowerType.ESPER, 35, 22);
        setCustomModelId(11);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World    world = player.getWorld();
        Location loc   = player.getLocation();

        // Buffs personnels : Night Vision + Speed
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,        60, 0));

        // Explosion de matière noire
        world.spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1, 0),
                50, RADIUS * 0.5, RADIUS * 0.4, RADIUS * 0.5, 0.5);
        world.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0),
                30, RADIUS * 0.4, RADIUS * 0.4, RADIUS * 0.4, 0.1);
        world.spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0),
                25, RADIUS * 0.5, RADIUS * 0.3, RADIUS * 0.5, 0.2);
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP,  0.8f, 0.4f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,    1.0f, 0.5f);

        // AOE sur les ennemis proches
        int hits = 0;
        for (Entity entity : world.getNearbyEntities(loc, RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(player)) continue;

            target.damage(DAMAGE, player);
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0)); // 3s
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,    40, 0)); // 2s

            Vector kb = target.getLocation().toVector()
                    .subtract(loc.toVector()).normalize()
                    .multiply(KB_SPEED).add(new Vector(0, 0.5, 0));
            target.setVelocity(kb);

            world.spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0),
                    15, 0.3, 0.4, 0.3, 0.1);
            target.sendMessage("§8⬛ §cDark Matter de §b" + player.getName()
                    + " §c— Aveuglé + Wither !");
            hits++;
        }

        player.sendMessage("§8⬛ §bDark Matter §8— " + hits + " cible(s) !"
                + " §7Night Vision + Speed actifs 3s !");
        player.sendTitle("§8⬛ DARK MATTER", "§7Wings of the N°2", 5, 40, 10);
        return true;
    }
}
