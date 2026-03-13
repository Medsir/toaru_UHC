package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * MENTAL OUT — Misaki Shokuhou (Level 5 #5)
 * Prend le contrôle mental de l'ennemi le plus proche (20 blocs).
 * Pendant 5s : aveugle, nause, ralentit, force son cooldown, et dirige ses pas vers ses ennemis.
 */
public class MentalOutPower extends Power {

    private static final int    CONTROL_DURATION = 100; // 5 secondes (ticks)
    private static final int    RANGE            = 20;

    public MentalOutPower() {
        super("mental_out", "§d🧠 Mental Out §7(Misaki Shokuhou)",
              "Contrôle mental 5s — aveugle, force cooldown, dirige vers les ennemis.",
              PowerType.ESPER, 60, 30);
        setCustomModelId(25);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player misaki = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        // Trouver l'ennemi le plus proche dans RANGE blocs
        Player target = null;
        double bestDist = RANGE;
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(misaki)) continue;
            double d = other.getLocation().distance(misaki.getLocation());
            if (d < bestDist) { bestDist = d; target = other; }
        }

        if (target == null) {
            misaki.sendMessage("§dMental Out §7— Aucun ennemi à portée !");
            // Remboursement
            uhcPlayer.setAim(uhcPlayer.getAim() + 60);
            uhcPlayer.setCooldown("mental_out", 0);
            return false;
        }

        final Player victim = target;
        UHCPlayer uVictim = ToaruUHC.getInstance().getGameManager().getUHCPlayer(victim);

        // Effets sur la victime
        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,  CONTROL_DURATION, 0));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION,  CONTROL_DURATION, 0));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,       CONTROL_DURATION, 2));

        // Force le cooldown de son pouvoir
        if (uVictim != null && uVictim.getPower() != null)
            uVictim.setCooldown(uVictim.getPower().getId(), 60);

        // Broadcast
        for (Player p : Bukkit.getOnlinePlayers())
            p.sendMessage("§d🧠 §f" + victim.getName() + " §7est sous contrôle mental §d(Misaki)§7 !");

        misaki.sendTitle("§d🧠 MENTAL OUT", "§7" + victim.getName() + " est sous contrôle !", 5, 40, 10);
        victim.sendTitle("§d🧠 CONTRÔLE", "§7Tu n'es plus maître de toi...", 5, 40, 10);
        misaki.getWorld().playSound(misaki.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);

        // Particules mentales autour de la victime + direction forcée chaque seconde
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= CONTROL_DURATION || !victim.isOnline()) { cancel(); return; }

                // Particules ENCHANT autour de la tête
                victim.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE,
                        victim.getLocation().add(0, 2, 0), 8, 0.5, 0.3, 0.5, 1.5);
                victim.getWorld().spawnParticle(Particle.SPELL_MOB,
                        victim.getLocation().add(0, 1, 0), 3, 0.3, 0.4, 0.3, 0.01);

                // Toutes les 20 ticks : diriger la victime vers l'ennemi le plus proche (autre que Misaki)
                if (ticks % 20 == 0) {
                    Player nearest = null;
                    double nd = Double.MAX_VALUE;
                    for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
                        if (!u.isAlive()) continue;
                        Player other = u.getBukkitPlayer();
                        if (other == null || !other.isOnline()
                                || other.equals(victim) || other.equals(misaki)) continue;
                        double d = other.getLocation().distance(victim.getLocation());
                        if (d < nd) { nd = d; nearest = other; }
                    }
                    if (nearest != null) {
                        Vector push = nearest.getLocation().toVector()
                                .subtract(victim.getLocation().toVector())
                                .setY(0).normalize().multiply(0.5);
                        victim.setVelocity(push);
                        victim.sendActionBar("§d🧠 §7Tu marches vers §f" + nearest.getName() + "§7...");
                    }
                }
                ticks++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);

        return true;
    }
}
