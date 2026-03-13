package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * CROCE DI PIETRO — Lidvia Lorenzetti
 * Plante une croix invisible (15 blocs devant). Pendant 15s :
 * toutes les 2s, tous les ennemis dans 30 blocs de la croix prennent 1 dégât
 * + Weakness I + 5s de cooldown forcé sur leur pouvoir.
 * La croix pulse de particules END_ROD. Explosion visuelle à l'expiration.
 */
public class LidviaPower extends Power {

    private static final double PLACE_RANGE    = 15.0;
    private static final double ZONE_RADIUS    = 30.0;
    private static final int    DURATION_TICKS = 300; // 15 secondes
    private static final int    PULSE_INTERVAL = 40;  // toutes les 2s
    private static final double TICK_DAMAGE    = 2.0; // 1 coeur par pulse
    private static final int    FORCED_CD      = 5;

    public LidviaPower() {
        super("lidvia", "§f✝ Croce di Pietro §7(Lidvia Lorenzetti)",
              "Plante la Croix — zone 30 blocs, malédictions & dégâts pendant 15s.",
              PowerType.MAGICIAN, 80, 50);
        setCustomModelId(30);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player lidvia = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = lidvia.getWorld();

        // Trouver le bloc visé (max PLACE_RANGE blocs)
        Location crossLoc = null;
        Location cur = lidvia.getEyeLocation();
        Vector dir = lidvia.getLocation().getDirection().normalize();
        for (double d = 0; d < PLACE_RANGE; d += 0.5) {
            cur.add(dir.clone().multiply(0.5));
            if (cur.getBlock().getType().isSolid()) {
                crossLoc = cur.clone().subtract(dir.clone().multiply(0.5));
                crossLoc.setY(Math.floor(crossLoc.getY()) + 1);
                break;
            }
        }
        if (crossLoc == null) crossLoc = lidvia.getLocation().add(0, 0, 0);

        // Spawn ARMOR_STAND invisible comme marqueur
        final Location finalCrossLoc = crossLoc;
        ArmorStand marker = (ArmorStand) world.spawnEntity(finalCrossLoc, EntityType.ARMOR_STAND);
        marker.setVisible(false);
        marker.setGravity(false);
        marker.setInvulnerable(true);
        marker.setCustomName("§f✝ Croix de Pierre");
        marker.setCustomNameVisible(true);

        // Animations de pose
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5;
            world.spawnParticle(Particle.END_ROD,
                    finalCrossLoc.clone().add(Math.cos(a)*2, 0.5, Math.sin(a)*2),
                    5, 0.1, 0.2, 0.1, 0.05);
        }
        world.playSound(finalCrossLoc, Sound.BLOCK_BELL_USE, 1.5f, 0.5f);
        world.playSound(finalCrossLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.6f);

        for (Player p : Bukkit.getOnlinePlayers())
            p.sendMessage("§f✝ §7Lidvia plante la §fCroce di Pietro §7— zone maudite active §e15s §7!");

        lidvia.sendTitle("§f✝ CROCE DI PIETRO", "§7Zone sacrée active !", 5, 50, 10);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= DURATION_TICKS || !marker.isValid()) {
                    explodeCross(world, finalCrossLoc, marker);
                    cancel();
                    return;
                }

                // Particules END_ROD pulsantes autour de la croix
                if (tick % 5 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4 + tick * 0.05;
                        double radius = 1.0 + 0.3 * Math.sin(tick * 0.1);
                        world.spawnParticle(Particle.END_ROD,
                                finalCrossLoc.clone().add(Math.cos(a)*radius, 0.8, Math.sin(a)*radius),
                                1, 0.02, 0.02, 0.02, 0.0);
                    }
                    // Pilier vertical
                    for (double h = 0; h < 3; h += 0.5)
                        world.spawnParticle(Particle.END_ROD,
                                finalCrossLoc.clone().add(0, h, 0), 1, 0.05, 0.02, 0.05, 0.0);
                }

                // Pulse de malédiction toutes les PULSE_INTERVAL ticks
                if (tick % PULSE_INTERVAL == 0 && tick > 0) {
                    world.playSound(finalCrossLoc, Sound.BLOCK_BELL_RESONATE, 0.8f, 0.7f);
                    for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
                        if (!u.isAlive()) continue;
                        Player p = u.getBukkitPlayer();
                        if (p == null || !p.isOnline() || p.equals(lidvia)) continue;
                        if (p.getLocation().distance(finalCrossLoc) > ZONE_RADIUS) continue;

                        p.damage(TICK_DAMAGE, lidvia);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, PULSE_INTERVAL + 5, 0));
                        if (u.getPower() != null) u.setCooldown(u.getPower().getId(), FORCED_CD);
                        world.spawnParticle(Particle.SPELL_MOB, p.getLocation().add(0,1,0),
                                10, 0.4, 0.5, 0.4, 0.01);
                        p.sendActionBar("§f✝ §7Zone maudite — §c-1 HP §7+ §eweak §7+ cooldown !");
                    }
                }
                tick++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);

        return true;
    }

    private void explodeCross(World world, Location loc, ArmorStand marker) {
        marker.remove();
        world.spawnParticle(Particle.END_ROD,   loc.clone().add(0,1,0), 60, 1.0, 1.5, 1.0, 0.15);
        world.spawnParticle(Particle.EXPLOSION_LARGE, loc.clone().add(0,1,0), 5, 0.5,0.5,0.5,0.1);
        world.spawnParticle(Particle.TOTEM,     loc.clone().add(0,1,0), 30, 0.8, 1.2, 0.8, 0.12);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,      0.8f, 1.5f);
        world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.8f);
        for (Player p : Bukkit.getOnlinePlayers())
            p.sendMessage("§f✝ §7La Croce di Pietro s'effondre...");
    }
}
