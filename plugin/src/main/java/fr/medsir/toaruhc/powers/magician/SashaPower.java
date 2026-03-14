package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * ARCHANGE GABRIEL — Sasha Kreutzev
 * Pluie divine : 3 éclairs sur chaque ennemi dans 20 blocs (espacés de 25 ticks).
 * Sasha reçoit Strength II + Resistance I pendant la durée.
 */
public class SashaPower extends Power {

    private static final int    RANGE       = 20;
    private static final int    BOLTS       = 3;
    private static final int    BOLT_DELAY  = 25; // ticks entre chaque éclair par cible
    private static final int    BUFF_TICKS  = 100;

    public SashaPower() {
        super("sasha", "§f👼 Gabriel §7(Sasha Kreutzev)",
              "Pluie divine — 3 éclairs sur chaque ennemi dans 20 blocs.",
              PowerType.MAGICIAN, 70, 35);
        setCustomModelId(28);
        this.ultimateCost = 0;
        this.ultimateCooldownSeconds = 240;
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player sasha = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = sasha.getWorld();

        // Buff de Sasha
        sasha.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,  BUFF_TICKS, 1));
        sasha.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, BUFF_TICKS, 0));

        // Collecter les cibles
        List<Player> targets = new ArrayList<>();
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(sasha)) continue;
            if (other.getLocation().distance(sasha.getLocation()) <= RANGE)
                targets.add(other);
        }

        if (targets.isEmpty()) {
            sasha.sendMessage("§fGabriel §7— Aucun ennemi à portée !");
            uhcPlayer.setMana(uhcPlayer.getMana() + 70);
            uhcPlayer.setCooldown("sasha", 0);
            sasha.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
            sasha.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            return false;
        }

        // Broadcast
        for (Player p : Bukkit.getOnlinePlayers())
            p.sendMessage("§f👼 §7Gabriel descend du ciel — §fSasha Kreutzev §7déchaîne la punition divine !");

        sasha.sendTitle("§f👼 GABRIEL", "§7Punition divine !", 5, 50, 10);
        world.playSound(sasha.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.5f);

        // Particules autour de Sasha (aura divine)
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= BUFF_TICKS) { cancel(); return; }
                double angle = t * 0.3;
                world.spawnParticle(Particle.TOTEM,
                        sasha.getLocation().clone().add(Math.cos(angle)*1.2, 1.5, Math.sin(angle)*1.2),
                        1, 0.05, 0.05, 0.05, 0.0);
                world.spawnParticle(Particle.END_ROD,
                        sasha.getLocation().clone().add(0, 2.2, 0),
                        1, 0.3, 0.1, 0.3, 0.01);
                t++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);

        // Éclairs sur chaque cible
        for (Player target : targets) {
            for (int bolt = 0; bolt < BOLTS; bolt++) {
                final int b = bolt;
                ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
                        ToaruUHC.getInstance(), () -> {
                            if (!target.isOnline()) return;
                            UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
                            if (uTarget == null || !uTarget.isAlive()) return;
                            world.strikeLightning(target.getLocation());
                            world.spawnParticle(Particle.TOTEM, target.getLocation().add(0,1,0),
                                    15, 0.4, 0.6, 0.4, 0.08);
                            if (b == 0) {
                                target.sendTitle("§f👼 GABRIEL", "§cPunition divine !", 3, 20, 5);
                                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                            }
                        }, (long)(bolt * BOLT_DELAY));
            }
        }

        sasha.sendMessage("§f👼 Gabriel — §7Punition divine sur §f" + targets.size() + " §7ennemi(s) !");
        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player sasha = uhcPlayer.getBukkitPlayer();
        if (sasha == null) return false;

        showUltimateIntro(sasha, "GABRIEL'S FULL MANIFESTATION", "L'Archange Gabriel en personne !");
        consumeUltimateResources(uhcPlayer);

        for (Player p : Bukkit.getOnlinePlayers())
            p.sendMessage("§f👼 §fGabriel §7se manifeste à travers §fSasha §7— Châtiment total dans 40 blocs !");

        World world = sasha.getWorld();
        Location loc = sasha.getLocation();

        // Pilier de particules
        for (double h = 0; h < 15; h += 0.5) {
            world.spawnParticle(Particle.TOTEM,   loc.clone().add(0, h, 0), 2, 0.2, 0.05, 0.2, 0.02);
            world.spawnParticle(Particle.END_ROD, loc.clone().add(0, h, 0), 1, 0.15, 0.02, 0.15, 0.01);
        }

        world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.8f);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.7f);

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= 100) {
                    cancel();
                    // Contrainte
                    if (sasha.isOnline()) {
                        sasha.damage(18.0);
                        sasha.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 300, 1));
                        uhcPlayer.setMana(0);
                        ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uhcPlayer);
                        for (Player p : Bukkit.getOnlinePlayers())
                            p.sendMessage("§f👼 §fGabriel §7quitte §fSasha §7— Elle s'effondre, consumée par la puissance divine !");
                    }
                    return;
                }

                // Anneau en expansion de END_ROD + TOTEM chaque tick
                double ringRadius = 1.0 + (tick / 100.0) * 5.0;
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double rx = Math.cos(angle + tick * 0.1) * ringRadius;
                    double rz = Math.sin(angle + tick * 0.1) * ringRadius;
                    world.spawnParticle(Particle.END_ROD, sasha.getLocation().clone().add(rx, 0.5, rz),
                            1, 0.05, 0.05, 0.05, 0.0);
                }

                // Toutes les 20 ticks: attaque
                if (tick % 20 == 0) {
                    for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
                        if (!u.isAlive()) continue;
                        Player target = u.getBukkitPlayer();
                        if (target == null || !target.isOnline() || target.equals(sasha)) continue;
                        if (target.getLocation().distance(sasha.getLocation()) > 40.0) continue;

                        world.strikeLightning(target.getLocation());
                        target.damage(6.0, sasha);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                        org.bukkit.util.Vector push = target.getVelocity().add(
                                new org.bukkit.util.Vector(
                                        (Math.random() - 0.5) * 0.5, 0.8, (Math.random() - 0.5) * 0.5));
                        target.setVelocity(push);
                        target.sendTitle("§f👼 GABRIEL", "§cChâtiment divin !", 2, 15, 3);
                    }
                }
                tick++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);

        return true;
    }
}
