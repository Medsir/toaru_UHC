package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 💣 DEMOLITION MINES - Frenda Seivelun
 * Clic droit normal: pose une mine invisible (max 5).
 * Clic droit SNEAK: détone toutes les mines (ignore cooldown).
 */
public class FrendaPower extends Power {

    private static final int MAX_MINES = 5;

    // Static map : UUID du joueur → liste de positions de mines
    private static final Map<UUID, List<Location>> MINES = new HashMap<>();

    public FrendaPower() {
        super("demolition_mines", "§e💣 Demolition Mines §7(Frenda Seivelun)",
              "Pose des mines invisibles (max 5). Sneak+clic = détonation.",
              PowerType.ESPER, 25, 5);
        setCustomModelId(24);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null || !player.isOnline()) return false;

        // Mode détonation (sneak) — ne pas vérifier cooldown
        if (player.isSneaking()) {
            detonateAll(player);
            return true;
        }

        // Mode pose — vérifier cooldown et ressources
        if (!canUse(uhcPlayer)) return false;
        consumeResources(uhcPlayer);

        UUID uuid = player.getUniqueId();
        List<Location> mines = MINES.computeIfAbsent(uuid, k -> new ArrayList<>());

        if (mines.size() >= MAX_MINES) {
            player.sendMessage("§e💣 §cMax mines atteint ! (" + MAX_MINES + "/" + MAX_MINES + ") — Sneak+clic pour détoner !");
            return false;
        }

        Location mineLoc = player.getLocation().getBlock().getLocation().add(0.5, 0.1, 0.5);
        mines.add(mineLoc);

        // Son discret de pose
        player.getWorld().playSound(mineLoc, Sound.ITEM_CROSSBOW_QUICK_CHARGE_1, 0.5f, 1.5f);

        // Particule visible UNIQUEMENT pour Frenda (indication de la mine)
        player.spawnParticle(Particle.FIREWORKS_SPARK, mineLoc.clone().add(0, 0.3, 0),
                5, 0.1, 0.05, 0.1, 0.01);

        player.sendMessage("§e💣 Mine posée §7(" + mines.size() + "/" + MAX_MINES
                + " mines) §8— §7Sneak+clic pour détoner !");

        return true;
    }

    private void detonateAll(Player player) {
        UUID uuid = player.getUniqueId();
        List<Location> mines = MINES.remove(uuid);

        if (mines == null || mines.isEmpty()) {
            player.sendMessage("§c💣 Aucune mine posée !");
            return;
        }

        int count = mines.size();
        player.sendMessage("§e💣 §c" + count + " mines §7ont explosé !");
        player.sendTitle("§e💣 DÉTONATION", "§c" + count + " mines explosent !", 5, 30, 10);

        // Déclencher chaque mine avec un délai de 2 ticks entre elles (chaîne dramatique)
        for (int i = 0; i < mines.size(); i++) {
            final Location mineLoc = mines.get(i);
            final int index = i;
            ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
                ToaruUHC.getInstance(), () -> {
                    World world = mineLoc.getWorld();
                    // Explosion avec dégâts de blocs
                    world.createExplosion(mineLoc, 4.0f, true, true);
                    world.spawnParticle(Particle.EXPLOSION_HUGE, mineLoc, 10, 0.5, 0.3, 0.5, 0.0);
                    world.spawnParticle(Particle.FLAME,           mineLoc, 50, 0.5, 0.5, 0.5, 0.15);
                    world.playSound(mineLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
                },
                (long) (i * 2)
            );
        }
    }
}
