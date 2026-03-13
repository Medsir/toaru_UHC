package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 🔴 MELTDOWNER - Mugino Shizuri (Level 5 N°4)
 * Faisceau de plasma continu pendant 3 secondes (60 ticks).
 * Direction recalculée à chaque tick selon le regard du joueur.
 * Dégâts par tick hit: 3.0 + Wither I 2s + knockback léger.
 * Chaque cible ne peut être touchée que toutes les 10 ticks.
 */
public class MeltdownerPower extends Power {

    private static final double DAMAGE_PER_HIT = 3.0;
    private static final double MAX_DISTANCE   = 50.0;
    private static final double STEP           = 0.75;
    private static final double HIT_RADIUS     = 1.5;
    private static final int    BEAM_DURATION  = 60;  // 3 secondes
    private static final int    HIT_COOLDOWN   = 10;  // ticks entre hits sur la même cible

    public MeltdownerPower() {
        super("meltdowner", "§c🔴 Meltdowner §7(Mugino Shizuri)",
              "Faisceau plasma 3s continu — Wither I + knockback par hit.",
              PowerType.ESPER, 40, 20);
        setCustomModelId(4);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        uhcPlayer.setMeltdownerActive(true);

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.6f);
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
        player.sendMessage("§c🔴 §bMeltdowner §c— Feu ! §7(3 secondes)");
        player.sendTitle("§c🔴 MELTDOWNER", "§7Faisceau plasma actif — 3s", 5, 40, 10);

        // Suivi du cooldown par cible : UUID -> dernier tick touché
        final Map<UUID, Integer> lastHitTick = new HashMap<>();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !uhcPlayer.isMeltdownerActive()) {
                    uhcPlayer.setMeltdownerActive(false);
                    cancel(); return;
                }
                if (tick >= BEAM_DURATION) {
                    uhcPlayer.setMeltdownerActive(false);
                    if (player.isOnline()) {
                        player.sendMessage("§7🔴 Meltdowner expiré.");
                        player.getWorld().spawnParticle(Particle.SMOKE_NORMAL,
                                player.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.02);
                    }
                    cancel(); return;
                }

                // Direction recalculée chaque tick selon le regard du joueur
                Location start = player.getEyeLocation();
                Vector dir = player.getLocation().getDirection().normalize();
                Location current = start.clone();
                double dist = 0;

                while (dist < MAX_DISTANCE) {
                    current.add(dir.clone().multiply(STEP));
                    dist += STEP;
                    World w = current.getWorld();

                    // Effets visuels style Warden beam
                    w.spawnParticle(Particle.FLAME,      current, 3, 0.05, 0.05, 0.05, 0.02);
                    w.spawnParticle(Particle.CRIT_MAGIC,  current, 2, 0.07, 0.07, 0.07, 0.0);
                    if ((int)(dist / STEP) % 3 == 0) {
                        w.spawnParticle(Particle.SCULK_SOUL, current, 1, 0.05, 0.05, 0.05, 0.0);
                    }
                    if ((int)(dist / STEP) % 5 == 0) {
                        w.spawnParticle(Particle.SONIC_BOOM, current, 1, 0.0, 0.0, 0.0, 0.0);
                    }

                    // Détection entités (hitbox large)
                    for (Entity entity : w.getNearbyEntities(current, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                        if (!(entity instanceof Player target)) continue;
                        if (target.equals(player)) continue;

                        UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
                        if (uTarget == null || !uTarget.isAlive()) continue;

                        // Vérifier cooldown de hit sur cette cible
                        Integer lastHit = lastHitTick.get(target.getUniqueId());
                        if (lastHit != null && (tick - lastHit) < HIT_COOLDOWN) continue;

                        lastHitTick.put(target.getUniqueId(), tick);

                        target.damage(DAMAGE_PER_HIT, player);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0)); // 2s Wither I

                        // Knockback léger vers l'arrière
                        Vector kb = dir.clone().multiply(0.8).add(new Vector(0, 0.3, 0));
                        target.setVelocity(target.getVelocity().add(kb));

                        w.spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0),
                                30, 0.4, 0.6, 0.4, 0.08);
                        w.spawnParticle(Particle.CRIT,  target.getLocation().add(0, 1, 0),
                                20, 0.3, 0.4, 0.3, 0.05);
                        w.playSound(target.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0.8f, 0.7f);

                        player.sendMessage("§c🔴 §fMeltdowner touche §c" + target.getName()
                                + "§f — Withering !");
                        target.sendMessage("§cMeltdowner de §b" + player.getName()
                                + " §c— Wither + Knockback !");
                        target.sendTitle("§c🔴 PLASMA", "§7Withering...", 5, 20, 5);

                        // Ne pas arrêter le faisceau — continuer sur 3s entières
                    }

                    // Collision mur
                    if (current.getBlock().getType().isSolid()) {
                        w.createExplosion(current.clone(), 0f, false, false);
                        w.spawnParticle(Particle.FLAME, current, 15, 0.2, 0.2, 0.2, 0.03);
                        break; // Arrêter ce tick à ce bloc, mais continuer les autres ticks
                    }
                }

                tick++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);

        return true;
    }
}
