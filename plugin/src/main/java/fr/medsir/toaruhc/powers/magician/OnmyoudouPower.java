package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

/**
 * 🌑 ONMYOUDOU - Tsuchimikado Motoharu
 * Lance une malédiction sur le joueur ennemi dans la ligne de mire (20 blocs).
 * Effet : Slowness III + Cécité + Nausée 5s sur la cible.
 * Contrepartie : 4 dégâts en retour sur Tsuchimikado (coût de l'Onmyoudou).
 */
public class OnmyoudouPower extends Power {

    private static final double SEARCH_RANGE   = 20.0;
    private static final double CONE_THRESHOLD = 0.75; // cos ~41°
    private static final double SELF_DAMAGE    = 4.0;

    public OnmyoudouPower() {
        super("onmyoudou", "§8🌑 Onmyoudou §7(Tsuchimikado Motoharu)",
              "Maudit un ennemi en vue — Slowness + Cécité + Nausée. Retour de dégâts.",
              PowerType.MAGICIAN, 30, 20);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();

        Player target = findTarget(player);
        if (target == null) {
            player.sendMessage("§8🌑 §cAucun ennemi dans ta ligne de mire !");
            return false;
        }

        consumeResources(uhcPlayer);

        World world = player.getWorld();

        // Application de la malédiction
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,      100, 2)); // 5s Slowness III
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,  100, 0)); // 5s Cécité
        target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION,  100, 0)); // 5s Nausée

        // Dégâts de retour sur Tsuchimikado (limitation de l'Onmyoudou)
        player.damage(SELF_DAMAGE);

        // Particules sombres à la cible
        world.spawnParticle(Particle.SMOKE_NORMAL, target.getLocation().add(0, 1, 0),
                30, 0.4, 0.7, 0.4, 0.04);
        world.spawnParticle(Particle.CRIT_MAGIC,   target.getLocation().add(0, 1, 0),
                15, 0.3, 0.5, 0.3, 0.05);
        world.playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.6f);

        // Particules sur Tsuchimikado (douleur du retour)
        world.spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0),
                10, 0.2, 0.3, 0.2, 0.05);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.6f, 0.8f);

        // Trait de particules entre les deux joueurs
        drawCurseBeam(world, player.getEyeLocation(), target.getLocation().add(0, 1, 0));

        player.sendMessage("§8🌑 §bOnmyoudou §8— Malédiction sur §c" + target.getName()
                + " §8! §7(§c-" + (int) SELF_DAMAGE + " HP§7 en retour)");
        target.sendMessage("§8🌑 §cMalédiction de §b" + player.getName()
                + " §c— Slowness + Cécité + Nausée 5s !");
        target.sendTitle("§8🌑 MAUDIT", "§7Onmyoudou de Tsuchimikado...", 5, 60, 15);

        return true;
    }

    /** Cherche le joueur ennemi le mieux aligné dans le cône de visée. */
    private Player findTarget(Player shooter) {
        Vector eyeDir = shooter.getLocation().getDirection().normalize();
        Location eye  = shooter.getEyeLocation();

        Player best      = null;
        double bestScore = 0;

        for (UHCPlayer uhcPlayer : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!uhcPlayer.isAlive()) continue;
            Player other = uhcPlayer.getBukkitPlayer();
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

    /** Dessine un trait de fumée entre deux points. */
    private void drawCurseBeam(World world, Location from, Location to) {
        Vector step = to.toVector().subtract(from.toVector()).normalize().multiply(0.5);
        Location cur = from.clone();
        int steps = (int) (from.distance(to) / 0.5);
        for (int i = 0; i < steps; i++) {
            cur.add(step);
            world.spawnParticle(Particle.SMOKE_NORMAL, cur, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
