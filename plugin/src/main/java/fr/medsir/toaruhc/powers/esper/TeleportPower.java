package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 🌀 TELEPORT - Shirai Kuroko
 * Téléportation 15 blocs dans la direction du regard, sans contrainte de bloc.
 */
public class TeleportPower extends Power {

    private static final double MAX_DISTANCE = 15.0;
    private static final double STEP         = 0.5;

    public TeleportPower() {
        super("teleport", "§d🌀 Teleport §7(Shirai Kuroko)",
              "Téléportation instantanée 15 blocs devant soi.",
              PowerType.ESPER, 25, 6);
        setCustomModelId(6);
        this.ultimateCost = 50;
        this.ultimateCooldownSeconds = 120;
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();

        Location origin = player.getLocation().clone();
        Vector   dir    = player.getLocation().getDirection().normalize();
        Location dest   = findDestination(player, origin, dir);

        consumeResources(uhcPlayer);

        // Effets à l'origine
        origin.getWorld().spawnParticle(Particle.PORTAL, origin.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.5);
        origin.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);

        player.teleport(dest);

        // Effets à la destination
        dest.getWorld().spawnParticle(Particle.PORTAL, dest.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.5);
        dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);

        player.sendMessage("§d🌀 §bTeleport §d— Téléportation !");
        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        // Collect alive enemies sorted by distance (up to 5)
        List<Player> targets = new ArrayList<>();
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player enemy = u.getBukkitPlayer();
            if (enemy == null || !enemy.isOnline() || enemy.equals(player)) continue;
            targets.add(enemy);
        }
        targets.sort(Comparator.comparingDouble(p -> p.getLocation().distance(player.getLocation())));
        if (targets.size() > 5) targets = targets.subList(0, 5);

        if (targets.isEmpty()) {
            player.sendMessage("§a🎯 §cAucun ennemi disponible pour l'assaut !");
            return false;
        }

        showUltimateIntro(player, "IRON SPIKE ASSAULT", "TP vers 5 ennemis — 12 dégâts + Poison !");
        consumeUltimateResources(uhcPlayer);

        Bukkit.broadcastMessage("§a🎯 §fKuroko §7déclenche §aIRON SPIKE ASSAULT §7— Téléportation de combat !");

        final List<Player> finalTargets = targets;
        final int count = finalTargets.size();

        for (int i = 0; i < count; i++) {
            final int idx = i;
            ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
                    ToaruUHC.getInstance(), () -> {
                        if (!player.isOnline()) return;
                        Player target = finalTargets.get(idx);
                        if (!target.isOnline()) return;

                        World world = player.getWorld();

                        // PORTAL particles at player current location
                        world.spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0),
                                25, 0.4, 0.6, 0.4, 0.2);

                        // Teleport player behind target
                        Location dest = target.getLocation().clone()
                                .subtract(target.getLocation().getDirection().multiply(1.0));
                        dest.setYaw(target.getLocation().getYaw());
                        dest.setPitch(target.getLocation().getPitch());
                        player.teleport(dest);

                        // Damage and effects on target
                        target.damage(12.0, player);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120, 1));  // Poison II 6s
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 2));     // Slowness III 3s

                        world.spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0),
                                25, 0.4, 0.6, 0.4, 0.2);
                        world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

                        // Internal injury per teleport
                        player.damage(2.0);
                        target.sendTitle("§a🎯 KUROKO", "§cAiguille d'acier !", 2, 15, 3);
                    }, (long) (idx * 15L));
        }

        // CONSTRAINT after all teleports
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
                ToaruUHC.getInstance(), () -> {
                    if (!player.isOnline()) return;
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0)); // Poison 5s
                    uhcPlayer.setCooldown("teleport", 30);
                    player.sendMessage("§a🎯 §7Iron Spike Assault — §c" + count
                            + "×2 HP backlash§7, Poison 5s, téléportation désactivée 30s");
                }, (long) (count * 15L + 20L));

        return true;
    }

    /**
     * Avance pas à pas dans la direction du regard.
     * S'arrête avant un bloc solide ou au max (15 blocs).
     * Cherche toujours une position où le joueur peut tenir debout (2 blocs de hauteur).
     */
    private Location findDestination(Player player, Location origin, Vector dir) {
        Location current  = origin.clone().add(0, 0.1, 0);
        Location lastSafe = origin.clone();
        double dist = 0;

        while (dist < MAX_DISTANCE) {
            current.add(dir.clone().multiply(STEP));
            dist += STEP;

            Location feet = current.clone();
            Location head = current.clone().add(0, 1, 0);

            // Si le bloc des pieds ou de la tête est solide → on s'arrête
            if (feet.getBlock().getType().isSolid() || head.getBlock().getType().isSolid()) {
                break;
            }

            // Sinon c'est une position valide
            lastSafe = current.clone();
        }

        // Ajuster pour poser les pieds sur le sol
        lastSafe = adjustToGround(lastSafe);
        lastSafe.setYaw(player.getLocation().getYaw());
        lastSafe.setPitch(player.getLocation().getPitch());
        return lastSafe;
    }

    private Location adjustToGround(Location loc) {
        Location check = loc.clone();
        // Descendre jusqu'au sol (max 5 blocs)
        for (int i = 0; i < 5; i++) {
            if (check.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) return check;
            check.subtract(0, 1, 0);
        }
        return loc;
    }
}
