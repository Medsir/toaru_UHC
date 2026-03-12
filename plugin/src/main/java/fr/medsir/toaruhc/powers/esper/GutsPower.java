package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

/**
 * 💥 GUTS - Sogiita Gunha (Level 5)
 * Explosion AOE : repousse et blesse tous les ennemis proches.
 * Si HP ≤ 40% → GUTS MODE (Speed II + Strength II pendant 5s).
 */
public class GutsPower extends Power {

    private static final double DAMAGE      = 8.0;
    private static final double RADIUS      = 6.0;
    private static final double KB_SPEED    = 2.5;
    private static final double HP_THRESHOLD = 0.4; // 40% HP

    public GutsPower() {
        super("guts", "§6💥 Guts §7(Sogiita Gunha)",
              "Explosion AOE + knockback. GUTS MODE si HP faibles !",
              PowerType.ESPER, 35, 20);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World    world = player.getWorld();
        Location loc   = player.getLocation();

        // Explosion visuelle (0 force = aucun dégât terrain)
        world.createExplosion(loc.clone().add(0, 0.5, 0), 0f, false, false);
        world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 8, 1.5, 0.5, 1.5, 0.0);
        world.spawnParticle(Particle.CRIT,            loc.clone().add(0, 1, 0), 30, 1.2, 1.0, 1.2, 0.2);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.7f);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.5f);

        // AOE sur les joueurs proches
        int hits = 0;
        for (Entity entity : world.getNearbyEntities(loc, RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(player)) continue;

            target.damage(DAMAGE, player);

            // Knockback radial
            Vector kb = target.getLocation().toVector()
                    .subtract(loc.toVector())
                    .normalize()
                    .multiply(KB_SPEED)
                    .add(new Vector(0, 0.7, 0));
            target.setVelocity(kb);

            world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                    15, 0.3, 0.4, 0.3, 0.1);
            target.sendMessage("§6💥 §cTouché par le §bGuts §cde §b" + player.getName() + " §c!");
            hits++;
        }

        // GUTS MODE si HP ≤ 40%
        boolean gutsMode = player.getHealth() <= player.getMaxHealth() * HP_THRESHOLD;
        if (gutsMode) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,          100, 1)); // 5s Speed II
            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100, 1)); // 5s Strength II
            world.spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0), 50, 0.5, 0.7, 0.5, 0.3);
            world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
            player.sendTitle("§6💥 GUTS MODE !", "§7Il y a des choses qui ne peuvent être perdues !", 5, 80, 15);
        } else {
            player.sendTitle("§6💥 GUTS !", "§7" + hits + " cible(s) touchée(s)", 5, 40, 10);
        }

        player.sendMessage("§6💥 §bGuts §6— " + hits + " cible(s) touchée(s) !"
                + (gutsMode ? " §c§lGUTS MODE actif 5s !" : ""));
        return true;
    }
}
