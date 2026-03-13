package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 📖 DÉCHIFFREMENT SACRÉ - Orsola Aquinas
 * Révèle les rôles de TOUS les joueurs en vie à Orsola uniquement.
 * Fait briller tous les joueurs pendant 10 secondes.
 */
public class OrsolaPower extends Power {

    private static final int GLOW_DURATION = 200; // 10 secondes

    public OrsolaPower() {
        super("sacred_decipherment", "§a📖 Déchiffrement Sacré §7(Orsola Aquinas)",
              "Révèle les rôles de tous les joueurs en vie.",
              PowerType.MAGICIAN, 30, 60);
        setCustomModelId(23);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = player.getWorld();
        Location loc = player.getLocation();

        // Sons de déchiffrement
        world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);
        world.playSound(loc, Sound.ITEM_BOOK_PAGE_TURN,          0.8f, 1.2f);
        world.playSound(loc, Sound.ENTITY_VILLAGER_WORK_LIBRARIAN, 0.7f, 1.0f);

        // Particules autour d'Orsola
        for (int i = 0; i < 30; i++) {
            double angle = i * Math.PI / 8;
            double h = i * 0.1;
            double r = 0.8;
            world.spawnParticle(Particle.ENCHANTMENT_TABLE,
                    loc.clone().add(Math.cos(angle) * r, h, Math.sin(angle) * r),
                    2, 0.05, 0.05, 0.05, 0.0);
            world.spawnParticle(Particle.CRIT_MAGIC,
                    loc.clone().add(Math.cos(angle) * r * 0.5, h * 0.5, Math.sin(angle) * r * 0.5),
                    1, 0.03, 0.03, 0.03, 0.0);
        }

        player.sendMessage("§a§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§a📖 §bDéchiffrement Sacré §a— Pouvoirs révélés :");
        player.sendTitle("§a📖 DÉCHIFFREMENT", "§7Les pouvoirs se révèlent...", 5, 50, 15);

        int count = 0;
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline()) continue;

            String roleName  = u.getRole()  != null ? u.getRole().getDisplayName()  : "§7Inconnu";
            String powerName = u.getPower() != null ? u.getPower().getName()        : "§7Aucun";
            String powerDesc = u.getPower() != null ? u.getPower().getDescription() : "";

            // Envoyer info uniquement à Orsola
            player.sendMessage("§a• §f" + other.getName() + "§7: " + roleName + " §8— §7" + powerDesc);

            // Faire briller tous les joueurs (Orsola incluse)
            other.setGlowing(true);

            // Particules ENCHANTMENT_TABLE au-dessus de leur tête
            world.spawnParticle(Particle.ENCHANTMENT_TABLE,
                    other.getLocation().add(0, 2.5, 0), 10, 0.3, 0.3, 0.3, 1.5);

            count++;
        }

        player.sendMessage("§a§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§a📖 §7" + count + " joueur(s) déchiffrés. Glowing 10s.");

        // Désactiver le Glowing après 10 secondes
        fr.medsir.toaruhc.ToaruUHC.getInstance().getServer().getScheduler()
            .runTaskLater(fr.medsir.toaruhc.ToaruUHC.getInstance(), () -> {
                for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
                    Player other = u.getBukkitPlayer();
                    if (other != null && other.isOnline()) other.setGlowing(false);
                }
                if (player.isOnline()) player.sendMessage("§7📖 Déchiffrement expiré — Glowing désactivé.");
            }, GLOW_DURATION);

        // Tracking du joueur le plus proche pendant 8 secondes
        Player nearest = findNearestPlayer(player);
        if (nearest != null) {
            UHCPlayer uNearest = ToaruUHC.getInstance().getGameManager().getUHCPlayer(nearest);
            final int[] remaining = {8};
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || remaining[0] <= 0) { cancel(); return; }
                    Player tracked = nearest; // re-get live location
                    // Check still alive
                    UHCPlayer uTracked = ToaruUHC.getInstance().getGameManager().getUHCPlayer(tracked);
                    if (uTracked == null || !uTracked.isAlive()) { cancel(); return; }

                    String roleName = uTracked.getRole() != null ? uTracked.getRole().getDisplayName() : "?";
                    String powName  = uTracked.getPower() != null ? uTracked.getPower().getName() : "?";
                    String dir = compassDir(player.getLocation(), tracked.getLocation());
                    int dist = (int) player.getLocation().distance(tracked.getLocation());

                    player.sendActionBar("§a📖 §f" + tracked.getName() + " §8[" + roleName + "§8]"
                            + " §7" + dir + " §8@ §f" + dist + "m | §7" + powName + " §8| §e" + remaining[0] + "s");
                    remaining[0]--;
                }
            }.runTaskTimer(ToaruUHC.getInstance(), 0L, 20L);
        }

        return true;
    }

    private Player findNearestPlayer(Player orsola) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(orsola)) continue;
            double d = other.getLocation().distance(orsola.getLocation());
            if (d < bestDist) { bestDist = d; best = other; }
        }
        return best;
    }

    private String compassDir(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        // Convert: Minecraft Z+ = South, X+ = East
        // atan2(dz, dx): 0=East, 90=South, -90=North, 180/-180=West
        if (angle < 0) angle += 360;
        // Map to 8 directions
        if (angle < 22.5 || angle >= 337.5) return "§7→ E";
        if (angle < 67.5)  return "§7↘ SE";
        if (angle < 112.5) return "§7↓ S";
        if (angle < 157.5) return "§7↙ SO";
        if (angle < 202.5) return "§7← O";
        if (angle < 247.5) return "§7↖ NO";
        if (angle < 292.5) return "§7↑ N";
        return "§7↗ NE";
    }
}
