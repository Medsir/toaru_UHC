package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

/**
 * ⚖ PRECEDENCE - Terra of the Left
 * Marque l'ennemi dans la ligne de mire :
 *   - Glowing 8s (visible à travers les murs)
 *   - Weakness II + Slowness II pendant 6s
 */
public class PrecedencePower extends Power {

    private static final double SEARCH_RANGE   = 20.0;
    private static final double CONE_THRESHOLD = 0.75; // ~41°

    public PrecedencePower() {
        super("precedence", "§6⚖ Precedence §7(Terra of the Left)",
              "Marque un ennemi : Glowing 8s + Faiblesse II + Ralentissement II 6s.",
              PowerType.MAGICIAN, 35, 25);
        setCustomModelId(14);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();

        Player target = findTarget(player);
        if (target == null) {
            player.sendMessage("§6⚖ §cAucun ennemi dans ta ligne de mire !");
            return false;
        }

        consumeResources(uhcPlayer);

        World world = player.getWorld();

        // Application du Precedence
        target.setGlowing(true);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 1)); // 6s Weakness II
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,     120, 1)); // 6s Slowness II

        ToaruUHC.getInstance().getServer().getScheduler()
            .runTaskLater(ToaruUHC.getInstance(), () -> {
                if (target.isOnline()) target.setGlowing(false);
            }, 160L); // 8s

        // Effets visuels : particules dorées sur la cible
        world.spawnParticle(Particle.CRIT_MAGIC,       target.getLocation().add(0, 1, 0),
                40, 0.5, 0.8, 0.5, 0.15);
        world.spawnParticle(Particle.ENCHANTMENT_TABLE, target.getLocation().add(0, 2, 0),
                25, 0.6, 0.5, 0.6, 0.15);
        world.playSound(target.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.7f);

        // Trait de particules entre les deux joueurs
        drawPrecedenceBeam(world, player.getEyeLocation(), target.getLocation().add(0, 1, 0));

        player.sendMessage("§6⚖ §bPrecedence §6— §c" + target.getName()
                + " §6marqué 8s (Weakness II + Slowness II) !");
        target.sendMessage("§6⚖ §cPrecedence de §b" + player.getName()
                + " §c— Tu es marqué 8s !");
        target.sendTitle("§6⚖ PRECEDENCE", "§7Marqué par Terra of the Left...", 5, 70, 10);
        return true;
    }

    /** Cherche le joueur ennemi le mieux aligné dans le cône de visée. */
    private Player findTarget(Player shooter) {
        Vector   eyeDir = shooter.getLocation().getDirection().normalize();
        Location eye    = shooter.getEyeLocation();
        Player   best   = null;
        double   bestScore = 0;

        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(shooter)) continue;

            double dist = other.getLocation().distance(shooter.getLocation());
            if (dist > SEARCH_RANGE) continue;

            Vector toOther = other.getLocation().add(0, 1, 0).toVector()
                    .subtract(eye.toVector()).normalize();
            double dot = toOther.dot(eyeDir);
            if (dot >= CONE_THRESHOLD) {
                double score = dot / Math.max(1, dist);
                if (score > bestScore) { bestScore = score; best = other; }
            }
        }
        return best;
    }

    /** Trait de particules dorées entre deux points. */
    private void drawPrecedenceBeam(World world, Location from, Location to) {
        Vector step  = to.toVector().subtract(from.toVector()).normalize().multiply(0.5);
        Location cur = from.clone();
        int steps    = (int)(from.distance(to) / 0.5);
        for (int i = 0; i < steps; i++) {
            cur.add(step);
            world.spawnParticle(Particle.CRIT_MAGIC, cur, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
