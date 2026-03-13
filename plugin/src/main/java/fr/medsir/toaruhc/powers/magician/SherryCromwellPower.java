package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 🗿 SHERRY CROMWELL - Invocatrice d'Ellis
 * Invoque un Warden qui cible l'ennemi le plus proche pendant 5 secondes.
 */
public class SherryCromwellPower extends Power {

    private static final int WARDEN_DURATION = 100; // 5 secondes

    public SherryCromwellPower() {
        super("sherry_cromwell", "§8🗿 Ellis Golem §7(Sherry Cromwell)",
              "Invoque Ellis le Golem de Pierre — attaque l'ennemi le plus proche 5s.",
              PowerType.MAGICIAN, 50, 30);
        setCustomModelId(15);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = player.getWorld();
        Location spawnLoc = player.getLocation();

        // Particules au spawn : spirale montante
        for (int i = 0; i < 30; i++) {
            double angle = i * Math.PI / 8;
            double h = i * 0.1;
            double sr = 0.8;
            double sx = Math.cos(angle) * sr;
            double sz = Math.sin(angle) * sr;
            world.spawnParticle(Particle.SCULK_SOUL, spawnLoc.clone().add(sx, h, sz),
                    1, 0.05, 0.05, 0.05, 0.0);
            world.spawnParticle(Particle.SMOKE_LARGE, spawnLoc.clone().add(sx, h, sz),
                    1, 0.05, 0.05, 0.05, 0.0);
        }

        // Invocation du Warden
        Warden warden = (Warden) world.spawnEntity(spawnLoc, EntityType.WARDEN);
        warden.setCustomName("§8Ellis - Golem de Sherry");
        warden.setCustomNameVisible(true);

        // Son d'émergence
        world.playSound(spawnLoc, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);

        // Trouver l'ennemi le plus proche (pas Sherry) et assigner la cible
        Player initialTarget = findNearestEnemy(player);
        if (initialTarget != null) {
            warden.setTarget(initialTarget);
        }

        player.sendMessage("§8🗿 §bEllis invoqué §8— Golem de Pierre actif 5 secondes !");
        player.sendTitle("§8🗿 ELLIS", "§7Le Golem attaque !", 5, 40, 10);

        // Tâche répétitive toutes les 10 ticks pendant 5 secondes
        new BukkitRunnable() {
            int pulses = 0;

            @Override
            public void run() {
                if (!warden.isValid() || warden.isDead()) {
                    cancel(); return;
                }
                if (pulses >= 5) {
                    // Fin : retirer le Warden
                    world.spawnParticle(Particle.CLOUD, warden.getLocation().add(0, 1, 0),
                            20, 0.5, 0.6, 0.5, 0.05);
                    world.spawnParticle(Particle.SMOKE_LARGE, warden.getLocation().add(0, 1, 0),
                            15, 0.4, 0.5, 0.4, 0.04);
                    world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_DEATH, 0.8f, 1.0f);
                    warden.remove();
                    if (player.isOnline()) player.sendMessage("§8🗿 Ellis disparaît...");
                    cancel(); return;
                }

                // Vérifier / recorriger la cible
                Player nearest = findNearestEnemy(player);
                if (nearest != null) {
                    LivingEntity currentTarget = warden.getTarget();
                    // Si la cible actuelle EST Sherry ou nulle, changer vers l'ennemi le plus proche
                    if (currentTarget == null || currentTarget.equals(player)) {
                        warden.setTarget(nearest);
                    }
                }

                pulses++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 10L);

        // Retrait forcé après 5 secondes si encore vivant
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
            ToaruUHC.getInstance(), () -> {
                if (warden.isValid() && !warden.isDead()) {
                    world.spawnParticle(Particle.CLOUD, warden.getLocation().add(0, 1, 0),
                            20, 0.5, 0.6, 0.5, 0.05);
                    world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_DEATH, 0.8f, 1.0f);
                    warden.remove();
                }
            }, WARDEN_DURATION
        );

        return true;
    }

    /** Trouve le joueur ennemi le plus proche de Sherry. */
    private Player findNearestEnemy(Player sherry) {
        Player nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(sherry)) continue;
            double dist = other.getLocation().distance(sherry.getLocation());
            if (dist < bestDist) { bestDist = dist; nearest = other; }
        }
        return nearest;
    }
}
