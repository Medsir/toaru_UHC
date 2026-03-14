package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * ✋ WORLD REJECTOR - Kamisato Kakeru
 * Bannit l'ennemi dans un monde alternatif : 60 blocs en l'air + Slow Falling + Blindness + Nausée.
 */
public class KamisatoPower extends Power {

    private static final double SEARCH_RANGE   = 30.0;
    private static final double CONE_THRESHOLD = 0.75;
    private static final int    BANISH_HEIGHT  = 60;

    public KamisatoPower() {
        super("world_rejector", "§b✋ World Rejector §7(Kamisato Kakeru)",
              "Bannit l'ennemi visé dans un monde alternatif — 60 blocs en l'air.",
              PowerType.ESPER, 45, 30);
        setCustomModelId(18);
        this.ultimateCost = 0;
        this.ultimateCooldownSeconds = 86400; // effectively once per game
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();

        Player target = findTarget(player);
        if (target == null) {
            player.sendMessage("§b✋ §cAucun ennemi dans ta ligne de mire (30 blocs) !");
            return false;
        }

        consumeResources(uhcPlayer);

        World world = player.getWorld();
        Location disappearLoc = target.getLocation().clone();
        Location arriveLoc    = target.getLocation().clone().add(0, BANISH_HEIGHT, 0);

        // Particules sur Kamisato
        for (int i = 0; i < 20; i++) {
            double angle = i * Math.PI / 5;
            double h = i * 0.15;
            world.spawnParticle(Particle.PORTAL,          player.getLocation().add(Math.cos(angle)*0.5, h, Math.sin(angle)*0.5), 2, 0.05, 0.05, 0.05, 0.0);
            world.spawnParticle(Particle.ELECTRIC_SPARK,  player.getLocation().add(Math.cos(angle)*0.5, h, Math.sin(angle)*0.5), 1, 0.03, 0.03, 0.03, 0.0);
        }

        // Téléporter la cible 60 blocs en l'air
        target.teleport(arriveLoc);

        // Appliquer effets
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 160, 0)); // 8s Slow Falling
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,     80, 0)); // 4s Blindness
        target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION,     80, 0)); // 4s Nausée

        // Particules à la position de disparition
        world.spawnParticle(Particle.EXPLOSION_LARGE, disappearLoc.clone().add(0, 1, 0), 5, 0.3, 0.4, 0.3, 0.0);
        for (int i = 0; i < 20; i++) {
            double angle = i * Math.PI / 5;
            double h = i * 0.1;
            world.spawnParticle(Particle.PORTAL, disappearLoc.clone().add(Math.cos(angle)*0.8, h, Math.sin(angle)*0.8), 2, 0.05, 0.05, 0.05, 0.0);
        }

        // Particules à la position d'arrivée (60 blocs en haut)
        world.spawnParticle(Particle.SCULK_SOUL, arriveLoc.clone().add(0, 1, 0), 20, 0.5, 0.4, 0.5, 0.04);
        world.spawnParticle(Particle.CLOUD,       arriveLoc.clone().add(0, 1, 0), 15, 0.4, 0.3, 0.4, 0.03);

        // Sons
        world.playSound(player.getLocation(),    Sound.ENTITY_ENDERMAN_TELEPORT,     1.0f, 0.8f);
        world.playSound(player.getLocation(),    Sound.BLOCK_PORTAL_TRAVEL,          0.4f, 1.5f);
        world.playSound(player.getLocation(),    Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.2f);

        player.sendMessage("§b✋ §bWorld Rejector §b— §c" + target.getName()
                + " §bbanni dans le monde alternatif !");
        target.sendMessage("§cKamisato Kakeru t'a banni dans un monde alternatif... §78s de Slow Fall !");
        target.sendTitle("§b✋ BANNI", "§7Monde alternatif...", 5, 80, 15);

        // Après 8 secondes : particules de re-matérialisation
        final Player finalTarget = target;
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
            ToaruUHC.getInstance(), () -> {
                if (finalTarget.isOnline()) {
                    world.spawnParticle(Particle.PORTAL,         finalTarget.getLocation().add(0, 1, 0), 25, 0.5, 0.7, 0.5, 0.06);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, finalTarget.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);
                    world.playSound(finalTarget.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                    finalTarget.sendMessage("§7✋ Tu re-matérialises dans ce monde...");
                }
            }, 160L
        );

        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        // HP Check: must have taken damage (≤ 8 hearts = 16.0 HP)
        if (player.getHealth() > 16.0) {
            player.sendMessage("§5🌀 §cWorld Rejecter — Impossible si HP max !");
            return false;
        }

        // Find nearest alive enemy (any range)
        Player target = null;
        double bestDist = Double.MAX_VALUE;
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(player)) continue;
            double dist = other.getLocation().distance(player.getLocation());
            if (dist < bestDist) { bestDist = dist; target = other; }
        }

        if (target == null) {
            player.sendMessage("§5🌀 §cAucun ennemi en vie !");
            return false;
        }

        showUltimateIntro(player, "WORLD REJECTER", "Envoie l'ennemi le plus proche dans un autre monde !");
        consumeUltimateResources(uhcPlayer);

        World world = player.getWorld();
        final Player finalTarget = target;
        Bukkit.broadcastMessage("§5🌀 §fKamisato §7active §5WORLD REJECTER §7— §f"
                + target.getName() + " §7est rejeté vers un autre monde !");

        // Dramatic effects on target
        world.spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0),
                40, 0.6, 0.8, 0.6, 0.2);
        world.spawnParticle(Particle.DRAGON_BREATH, target.getLocation().add(0, 1, 0),
                30, 0.5, 0.7, 0.5, 0.05);
        world.strikeLightning(target.getLocation());
        target.sendTitle("§5🌀 WORLD REJECTER", "§7Tu es rejeté vers un autre monde...", 5, 60, 15);
        world.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 1.0f, 0.8f);
        world.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.7f);

        // Schedule death after 40 ticks (dramatic buildup)
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
                ToaruUHC.getInstance(), () -> {
                    if (finalTarget.isOnline()) {
                        finalTarget.damage(1000.0, player);
                    }
                }, 40L);

        // CONSTRAINT: immediately
        player.damage(15.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 400, 1)); // Wither II 20s
        uhcPlayer.setCooldown("kamisato", 60);
        player.sendMessage("§5🌀 §7World Rejecter — §c-15 HP§7, Wither II 20s, pouvoir désactivé 60s — Ne peut être utilisé qu'une fois !");

        return true;
    }

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
}
