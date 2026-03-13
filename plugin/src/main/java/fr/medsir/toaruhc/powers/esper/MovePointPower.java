package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MOVE POINT — Musujime Awaki
 * Téléporte jusqu'à 3 ennemis proches (15 blocs) à 40-60 blocs en l'air.
 * Slow Fall accordé. Backlash : 3 dégâts sur Awaki (trauma de l'auto-tp).
 */
public class MovePointPower extends Power {

    private static final int    RANGE        = 15;
    private static final int    MAX_TARGETS  = 3;
    private static final int    HEIGHT_MIN   = 40;
    private static final int    HEIGHT_MAX   = 60;
    private static final int    SPREAD       = 20;   // offset X/Z max
    private static final double BACKLASH_DMG = 6.0;  // 3 coeurs

    public MovePointPower() {
        super("move_point", "§9📦 Move Point §7(Musujime Awaki)",
              "Téléporte 3 ennemis proches en l'air + Slow Fall. Backlash 3 coeurs.",
              PowerType.ESPER, 50, 25);
        setCustomModelId(26);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player awaki = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = awaki.getWorld();
        Random rng = new Random();

        // Collecter les cibles dans RANGE blocs
        List<Player> targets = new ArrayList<>();
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(awaki)) continue;
            if (other.getLocation().distance(awaki.getLocation()) <= RANGE)
                targets.add(other);
        }

        if (targets.isEmpty()) {
            awaki.sendMessage("§9Move Point §7— Aucun ennemi à portée !");
            uhcPlayer.setAim(uhcPlayer.getAim() + 50);
            uhcPlayer.setCooldown("move_point", 0);
            return false;
        }

        // Limiter à MAX_TARGETS
        while (targets.size() > MAX_TARGETS) targets.remove(rng.nextInt(targets.size()));

        awaki.sendTitle("§9MOVE POINT", "§7Téléportation forcée !", 5, 30, 8);
        world.playSound(awaki.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);

        for (Player victim : targets) {
            // Particules de départ
            world.spawnParticle(Particle.PORTAL, victim.getLocation().add(0,1,0), 40, 0.5,1,0.5,0.5);
            world.spawnParticle(Particle.DRAGON_BREATH, victim.getLocation().add(0,1,0), 20, 0.4,0.8,0.4,0.1);

            // Calculer position d'arrivée
            int height = HEIGHT_MIN + rng.nextInt(HEIGHT_MAX - HEIGHT_MIN + 1);
            double ox = (rng.nextDouble() * 2 - 1) * SPREAD;
            double oz = (rng.nextDouble() * 2 - 1) * SPREAD;
            Location dest = awaki.getLocation().clone().add(ox, height, oz);
            // S'assurer que la destination est dans le monde (pas sous le bedrock)
            dest.setY(Math.max(dest.getY(), 64));

            victim.teleport(dest);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 160, 0)); // 8s
            victim.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION,    40, 0)); // 2s suspension

            // Particules à l'arrivée
            dest.getWorld().spawnParticle(Particle.PORTAL, dest.clone().add(0,1,0), 30, 0.4,0.8,0.4,0.4);
            dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

            victim.sendTitle("§9MOVE POINT", "§7Tu as été téléporté en l'air !", 5, 30, 8);
            victim.sendMessage("§9📦 §7Musujime Awaki t'a téléporté §f" + height + " blocs §7en l'air !");
            awaki.sendMessage("§9📦 §f" + victim.getName() + " §7projeté §f" + height + " blocs §7en l'air !");
        }

        // Backlash sur Awaki (trauma de l'utilisation sur des êtres vivants)
        awaki.damage(BACKLASH_DMG);
        awaki.sendMessage("§9⚡ §7Backlash — §c-3 coeurs §7(trauma du Move Point sur des êtres vivants)");
        world.spawnParticle(Particle.CRIT_MAGIC, awaki.getLocation().add(0,1,0), 20, 0.4,0.6,0.4,0.05);

        return true;
    }
}
