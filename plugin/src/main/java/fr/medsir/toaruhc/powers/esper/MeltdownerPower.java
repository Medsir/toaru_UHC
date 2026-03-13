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
 * MELTDOWNER - Mugino Shizuri (Level 5 N°4)
 * TOGGLE : clic droit = lance le beam, re-clic = l'arrête.
 * AIM drainé progressivement : DRAIN_BASE/s + DRAIN_INCREMENT par seconde (8, 11, 14...)
 * Plus tu maintiens, plus ça draine. Cooldown appliqué uniquement à l'arrêt.
 * Les dégâts augmentent aussi au fil du temps (beam qui chauffe).
 */
public class MeltdownerPower extends Power {

    private static final double DAMAGE_BASE     = 3.0;   // dégâts de base par hit
    private static final double MAX_DISTANCE    = 50.0;
    private static final double STEP            = 0.75;
    private static final double HIT_RADIUS      = 1.5;
    private static final int    HIT_COOLDOWN    = 10;    // ticks entre deux hits sur la même cible
    private static final int    DRAIN_BASE      = 8;     // AIM drainé à la 1ère seconde
    private static final int    DRAIN_INCREMENT = 3;     // +AIM par seconde supplémentaire
    private static final int    STOP_COOLDOWN   = 20;    // secondes de cooldown après arrêt

    public MeltdownerPower() {
        super("meltdowner", "§c🔴 Meltdowner §7(Mugino Shizuri)",
              "TOGGLE — re-clic pour arrêter. Drain AIM croissant. Dégâts augmentent.",
              PowerType.ESPER, 0, 0); // coût/cooldown gérés manuellement
        setCustomModelId(4);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null || !player.isOnline()) return false;

        // Mode actif → arrêter
        if (uhcPlayer.isMeltdownerActive()) {
            stopBeam(uhcPlayer, player);
            return true;
        }

        // Vérifier cooldown et AIM minimum
        if (uhcPlayer.isOnCooldown("meltdowner")) {
            player.sendMessage("§cMeltdowner en recharge ! ("
                    + uhcPlayer.getRemainingCooldown("meltdowner") + "s)");
            return false;
        }
        if (uhcPlayer.getAim() < DRAIN_BASE) {
            player.sendMessage("§cPas assez d'AIM ! (" + uhcPlayer.getAim() + "/" + DRAIN_BASE + ")");
            return false;
        }

        // Démarrage du beam
        uhcPlayer.setMeltdownerActive(true);
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.6f);
        world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
        player.sendMessage("§c🔴 §bMeltdowner §c— Feu ! §7(Re-clic pour arrêter — drain AIM croissant !)");
        player.sendTitle("§c🔴 MELTDOWNER", "§7Faisceau plasma actif", 5, 20, 5);

        final Map<UUID, Integer> lastHitTick = new HashMap<>();

        new BukkitRunnable() {
            int tick      = 0;
            int seconds   = 0;    // secondes complètes écoulées
            int nextDrain = 20;   // prochain tick où on draine l'AIM

            @Override
            public void run() {
                if (!player.isOnline() || !uhcPlayer.isMeltdownerActive()) {
                    cancel();
                    return;
                }

                // Drain AIM chaque seconde (montant croissant)
                if (tick >= nextDrain) {
                    int drain = DRAIN_BASE + seconds * DRAIN_INCREMENT;
                    if (uhcPlayer.getAim() < drain) {
                        player.sendMessage("§c🔴 §7Plus d'AIM — Meltdowner s'éteint automatiquement.");
                        stopBeam(uhcPlayer, player);
                        cancel();
                        return;
                    }
                    uhcPlayer.setAim(uhcPlayer.getAim() - drain);
                    ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uhcPlayer);
                    seconds++;
                    nextDrain += 20;
                    // Indicateur ActionBar du drain
                    int nextDrain2 = DRAIN_BASE + seconds * DRAIN_INCREMENT;
                    player.sendActionBar("§c🔴 §f" + seconds + "s §7— AIM drain §c" + drain
                            + "§7/s §8→ §c" + nextDrain2 + "/s");
                }

                // Tracer le beam
                Location start = player.getEyeLocation();
                Vector   dir   = player.getLocation().getDirection().normalize();
                Location cur   = start.clone();
                double   dist  = 0;

                while (dist < MAX_DISTANCE) {
                    cur.add(dir.clone().multiply(STEP));
                    dist += STEP;
                    World w = cur.getWorld();

                    // Particules (plus intenses avec le temps)
                    int extra = Math.min(seconds, 6);
                    w.spawnParticle(Particle.FLAME,      cur, 3 + extra, 0.05, 0.05, 0.05, 0.02);
                    w.spawnParticle(Particle.CRIT_MAGIC, cur, 2,         0.07, 0.07, 0.07, 0.00);
                    if ((int)(dist / STEP) % 3 == 0)
                        w.spawnParticle(Particle.SCULK_SOUL, cur, 1, 0.05, 0.05, 0.05, 0.0);
                    if ((int)(dist / STEP) % 5 == 0)
                        w.spawnParticle(Particle.SONIC_BOOM, cur, 1, 0.0, 0.0, 0.0, 0.0);

                    // Détection entités
                    for (Entity entity : w.getNearbyEntities(cur, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                        if (!(entity instanceof Player target)) continue;
                        if (target.equals(player)) continue;
                        UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager()
                                .getUHCPlayer(target);
                        if (uTarget == null || !uTarget.isAlive()) continue;

                        Integer lastHit = lastHitTick.get(target.getUniqueId());
                        if (lastHit != null && (tick - lastHit) < HIT_COOLDOWN) continue;
                        lastHitTick.put(target.getUniqueId(), tick);

                        double dmg = DAMAGE_BASE + seconds * 0.5; // dégâts croissants
                        target.damage(dmg, player);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));

                        Vector kb = dir.clone().multiply(0.8).add(new Vector(0, 0.3, 0));
                        target.setVelocity(target.getVelocity().add(kb));

                        w.spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0),
                                30 + extra * 5, 0.4, 0.6, 0.4, 0.08);
                        w.spawnParticle(Particle.CRIT,  target.getLocation().add(0, 1, 0),
                                20, 0.3, 0.4, 0.3, 0.05);
                        w.playSound(target.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT,
                                0.8f, 0.7f);

                        player.sendMessage("§c🔴 §fTouche §c" + target.getName()
                                + " §f(§c" + String.format("%.1f", dmg) + " dmg§f) !");
                        target.sendTitle("§c🔴 PLASMA", "§7Withering...", 2, 15, 3);
                    }

                    // Collision bloc
                    if (cur.getBlock().getType().isSolid()) {
                        w.spawnParticle(Particle.FLAME, cur, 10, 0.2, 0.2, 0.2, 0.03);
                        break;
                    }
                }
                tick++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);

        return true;
    }

    /** Arrête le beam et applique le cooldown. */
    private void stopBeam(UHCPlayer uhcPlayer, Player player) {
        uhcPlayer.setMeltdownerActive(false);
        uhcPlayer.setCooldown("meltdowner", STOP_COOLDOWN);
        if (player.isOnline()) {
            player.sendMessage("§7🔴 Meltdowner arrêté — Recharge §e" + STOP_COOLDOWN + "s§7.");
            player.sendActionBar("§7🔴 Arrêté — Recharge " + STOP_COOLDOWN + "s");
            player.getWorld().spawnParticle(Particle.SMOKE_NORMAL,
                    player.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.02);
            player.getWorld().playSound(player.getLocation(),
                    Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.2f);
        }
    }
}
