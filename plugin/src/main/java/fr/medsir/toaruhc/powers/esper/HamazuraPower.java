package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * DSYSTEM MACHINE PISTOL - Hamazura Shiage
 * Level 0 avec le pistolet D.S.ESPER — rafale de 5 balles.
 * Chaque balle force le cooldown du pouvoir ennemi + Weakness + Slowness.
 */
public class HamazuraPower extends Power {

    private static final int    SHOTS            = 5;
    private static final double SHOT_DAMAGE      = 4.0;
    private static final double SHOT_RANGE       = 20.0;
    private static final double SHOT_STEP        = 0.5;
    private static final double HIT_RADIUS       = 0.8;
    private static final int    DSYSTEM_COOLDOWN = 8; // secondes forcées sur la cible

    public HamazuraPower() {
        super("hamazura", "§6DSYSTEM §7(Hamazura Shiage)",
              "Mitraille 5 balles — force cooldown ennemi + ralentit.",
              PowerType.ESPER, 20, 10);
        setCustomModelId(19);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        // Self buffs
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 120, 0));

        player.sendMessage("§6DSYSTEM §7— Rafale de 5 balles !");
        player.sendTitle("§6D.S.ESPER", "§7Tire !", 3, 20, 5);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1.5f, 1.8f);

        for (int i = 0; i < SHOTS; i++) {
            final int shotNum = i;
            ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
                    ToaruUHC.getInstance(), () -> fireShot(player, uhcPlayer, shotNum), i * 2L);
        }
        return true;
    }

    private void fireShot(Player player, UHCPlayer uhcPlayer, int shotNum) {
        if (!player.isOnline()) return;
        World world = player.getWorld();
        Location start = player.getEyeLocation();
        Vector dir = player.getLocation().getDirection().normalize();
        // Légère dispersion
        dir.add(new Vector((Math.random()-0.5)*0.1, 0, (Math.random()-0.5)*0.1)).normalize();

        Location cur = start.clone();
        double dist = 0;
        world.playSound(start, Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.8f);

        while (dist < SHOT_RANGE) {
            cur.add(dir.clone().multiply(SHOT_STEP));
            dist += SHOT_STEP;
            world.spawnParticle(Particle.CRIT_MAGIC, cur, 1, 0.02, 0.02, 0.02, 0.0);

            if (cur.getBlock().getType().isSolid()) {
                world.spawnParticle(Particle.SMOKE_NORMAL, cur, 5, 0.1, 0.1, 0.1, 0.02);
                break;
            }

            for (Entity entity : world.getNearbyEntities(cur, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                if (!(entity instanceof Player target)) continue;
                if (target.equals(player)) continue;
                UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
                if (uTarget == null || !uTarget.isAlive()) continue;

                target.damage(SHOT_DAMAGE, player);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1));
                // Forcer le cooldown du pouvoir de la cible
                if (uTarget.getPower() != null) {
                    uTarget.setCooldown(uTarget.getPower().getId(), DSYSTEM_COOLDOWN);
                }
                world.spawnParticle(Particle.CRIT, target.getLocation().add(0,1,0), 12, 0.3,0.4,0.3,0.04);
                world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8f, 1.2f);
                player.sendMessage("§6DSYSTEM §7— Touche §6" + target.getName() + " §7(balle " + (shotNum+1) + "/5)");
                target.sendTitle("§6DSYSTEM", "§7Touché !", 2, 12, 3);
                return; // une cible par balle
            }
        }
    }
}
