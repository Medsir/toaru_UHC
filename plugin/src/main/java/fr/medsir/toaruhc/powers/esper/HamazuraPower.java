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
        this.ultimateCost = 30;
        this.ultimateCooldownSeconds = 180;
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

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        // HP Check: must be at <= 6 hearts (12.0 HP)
        if (player.getHealth() > 12.0) {
            player.sendMessage("§6💣 §cItem 005 — Désespoir requis (≤6 coeurs) !");
            return false;
        }

        showUltimateIntro(player, "ITEM: 005 DETONATOR", "3s avant explosion — fuis !");
        consumeUltimateResources(uhcPlayer);

        World world = player.getWorld();
        Bukkit.broadcastMessage("§6💣 §fHamazura §7plante §6ITEM: 005 §7— Explosion dans 3 secondes !");

        // Planting stun (3 seconds)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 9));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 60, 9));

        // Save bomb location
        final Location bombLoc = player.getLocation().clone();

        // Spawn ArmorStand as visual marker
        ArmorStand stand = (ArmorStand) world.spawnEntity(bombLoc.clone().add(0, 0.5, 0), EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCustomName("§c💣 §e3...");
        stand.setCustomNameVisible(true);

        // Countdown at 20 ticks (1s)
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(ToaruUHC.getInstance(), () -> {
            if (stand.isValid()) stand.setCustomName("§c💣 §e2...");
            world.spawnParticle(Particle.FLAME, bombLoc.clone().add(0, 0.5, 0), 10, 0.2, 0.2, 0.2, 0.05);
            world.playSound(bombLoc, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.8f);
            // Warn all players in 50 blocks
            for (Entity e : world.getNearbyEntities(bombLoc, 50, 50, 50)) {
                if (e instanceof Player p) p.sendMessage("§6💣 §cBOMBE ITEM:005 — 2 secondes !");
            }
        }, 20L);

        // Countdown at 40 ticks (2s)
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(ToaruUHC.getInstance(), () -> {
            if (stand.isValid()) stand.setCustomName("§c💣 §e1...");
            world.spawnParticle(Particle.FLAME, bombLoc.clone().add(0, 0.5, 0), 15, 0.2, 0.2, 0.2, 0.08);
            world.playSound(bombLoc, Sound.ENTITY_ARROW_HIT_PLAYER, 1.5f, 0.6f);
        }, 40L);

        // Detonate at 60 ticks (3s)
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(ToaruUHC.getInstance(), () -> {
            // Remove ArmorStand
            if (stand.isValid()) stand.remove();

            // 5 explosions at bomb location and surrounding
            world.createExplosion(bombLoc, 5.0f, false, true, player);
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                double rx = Math.cos(angle) * (1 + Math.random() * 3);
                double rz = Math.sin(angle) * (1 + Math.random() * 3);
                world.createExplosion(bombLoc.clone().add(rx, 0, rz), 5.0f, false, true, player);
            }

            // Distance-based damage for all alive players in 30 blocks
            for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
                if (!u.isAlive()) continue;
                Player target = u.getBukkitPlayer();
                if (target == null || !target.isOnline()) continue;
                double dist = target.getLocation().distance(bombLoc);
                if (dist > 30.0) continue;
                double dmg = 30.0 * Math.max(0, (30 - dist) / 30.0);
                target.damage(dmg, player);
            }

            // EXPLOSION_HUGE x10
            for (int i = 0; i < 10; i++) {
                world.spawnParticle(Particle.EXPLOSION_HUGE, bombLoc.clone().add(
                        (Math.random() - 0.5) * 6, Math.random() * 3, (Math.random() - 0.5) * 6),
                        1, 0, 0, 0, 0);
            }
            world.playSound(bombLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.7f);
            Bukkit.broadcastMessage("§6💣 §7ITEM: 005 explose !");
        }, 60L);

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
