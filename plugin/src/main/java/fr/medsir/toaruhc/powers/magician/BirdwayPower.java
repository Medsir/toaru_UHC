package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * DAWN-COLORED SUNLIGHT — Leivinia Birdway
 * Dessine un pentagramme (5 nœuds en étoile, rayon 5) centré 12 blocs devant.
 * Après 2s de "chargement", chaque nœud explose avec 0.3s de délai entre chacun.
 */
public class BirdwayPower extends Power {

    private static final double PENTA_RADIUS   = 5.0;
    private static final int    PENTA_POINTS   = 5;
    private static final double TARGET_DIST    = 12.0;
    private static final float  EXPLOSION_POW  = 2.5f;
    private static final int    CHARGE_TICKS   = 40; // 2s de chargement
    private static final int    DELAY_BETWEEN  = 6;  // 0.3s entre chaque explosion

    public BirdwayPower() {
        super("birdway", "§e⭐ Dawn-Colored §7(Birdway Leivinia)",
              "Pentagramme — 5 nœuds explosifs en étoile, détonation en chaîne.",
              PowerType.MAGICIAN, 80, 40);
        setCustomModelId(27);
        this.ultimateCost = 100;
        this.ultimateCooldownSeconds = 240;
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player leivinia = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = leivinia.getWorld();

        // Centre du pentagramme : 12 blocs devant
        Location center = leivinia.getEyeLocation()
                .add(leivinia.getLocation().getDirection().normalize().multiply(TARGET_DIST));
        center.setY(leivinia.getLocation().getY());

        // Calculer les 5 sommets du pentagramme
        List<Location> nodes = new ArrayList<>();
        for (int i = 0; i < PENTA_POINTS; i++) {
            // Pentagramme étoilé : angle de base + i * 144° (pas de 144° = étoile à 5 branches)
            double angle = Math.toRadians(-90 + i * 144.0);
            double nx = center.getX() + PENTA_RADIUS * Math.cos(angle);
            double nz = center.getZ() + PENTA_RADIUS * Math.sin(angle);
            nodes.add(new Location(world, nx, center.getY(), nz));
        }

        leivinia.sendTitle("§e⭐ PENTAGRAMME", "§7Chargement...", 5, CHARGE_TICKS + 20, 5);
        leivinia.sendMessage("§e⭐ Dawn-Colored §7— Pentagramme en chargement !");
        world.playSound(leivinia.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.7f);
        world.playSound(leivinia.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.8f, 1.2f);

        // Phase 1 : animation des nœuds pendant CHARGE_TICKS ticks
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= CHARGE_TICKS) { cancel(); triggerExplosions(leivinia, world, nodes); return; }
                // Particules pulsantes sur chaque nœud + lignes entre eux
                for (int i = 0; i < nodes.size(); i++) {
                    Location node = nodes.get(i);
                    world.spawnParticle(Particle.END_ROD,  node.clone().add(0,0.5,0), 4, 0.2,0.3,0.2,0.05);
                    world.spawnParticle(Particle.TOTEM,    node.clone().add(0,0.5,0), 2, 0.1,0.2,0.1,0.02);
                    // Ligne vers le nœud suivant (pas étoilé)
                    Location next = nodes.get((i + 2) % nodes.size()); // +2 = ordre étoilé
                    drawLine(world, node, next, tick);
                }
                // Son de chargement
                if (tick % 10 == 0)
                    world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f + tick * 0.03f);
                tick++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);

        return true;
    }

    private void triggerExplosions(Player leivinia, World world, List<Location> nodes) {
        world.playSound(leivinia.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
        leivinia.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30, 255, false, false)); // ADD THIS
        for (int i = 0; i < nodes.size(); i++) {
            final Location nodeLoc = nodes.get(i);
            final int idx = i;
            ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
                    ToaruUHC.getInstance(), () -> {
                        world.spawnParticle(Particle.TOTEM,   nodeLoc.clone().add(0,1,0), 40, 0.6,0.8,0.6,0.15);
                        world.spawnParticle(Particle.END_ROD, nodeLoc.clone().add(0,1,0), 20, 0.4,0.6,0.4,0.10);
                        world.playSound(nodeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f + idx * 0.1f);
                        world.createExplosion(nodeLoc, EXPLOSION_POW, false, true, leivinia);
                    }, (long)(i * DELAY_BETWEEN));
        }
        leivinia.sendMessage("§e⭐ Pentagramme déclenché — §f5 explosions §7en chaîne !");
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player leivinia = uhcPlayer.getBukkitPlayer();
        if (leivinia == null) return false;

        showUltimateIntro(leivinia, "GRAND PENTAGRAM", "Pentagramme de 50 blocs — implosion totale !");
        consumeUltimateResources(uhcPlayer);

        for (Player p : org.bukkit.Bukkit.getOnlinePlayers())
            p.sendMessage("§e⭐ §fBirdway §7déploie §eGRAND PENTAGRAM §7— Pentagramme de 50 blocs !");

        World world = leivinia.getWorld();
        Location center = leivinia.getLocation().clone();

        // 5 nœuds à rayon 25
        List<Location> nodes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(-90 + i * 144.0);
            double nx = center.getX() + 25.0 * Math.cos(angle);
            double nz = center.getZ() + 25.0 * Math.sin(angle);
            nodes.add(new Location(world, nx, center.getY(), nz));
        }

        leivinia.sendTitle("§e⭐ GRAND PENTAGRAM", "§7Chargement...", 5, 50, 5);
        world.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.5f);

        // Phase de charge: 40 ticks
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= 40) {
                    cancel();
                    // Attirer les ennemis proches
                    for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
                        if (!u.isAlive()) continue;
                        Player enemy = u.getBukkitPlayer();
                        if (enemy == null || !enemy.isOnline() || enemy.equals(leivinia)) continue;
                        if (enemy.getLocation().distance(center) > 30.0) continue;
                        enemy.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 0));
                        org.bukkit.util.Vector pull = center.toVector()
                                .subtract(enemy.getLocation().toVector())
                                .normalize().multiply(0.8);
                        enemy.setVelocity(pull);
                    }

                    // Birdway ne prend pas ses propres explosions
                    leivinia.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 255, false, false));
                    // Toutes les explosions simultanément
                    for (Location nodeLoc : nodes) {
                        world.createExplosion(nodeLoc, 5.0f, true, true, leivinia);
                        world.spawnParticle(Particle.TOTEM, nodeLoc.clone().add(0, 1, 0),
                                50, 0.8, 1.0, 0.8, 0.2);
                    }
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);

                    // Contraintes
                    leivinia.damage(15.0);
                    uhcPlayer.setMana(0);
                    ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uhcPlayer);
                    uhcPlayer.setCooldown("birdway", 90);
                    leivinia.sendMessage("§e⭐ §7Grand Pentagram — §c-15 HP§7, Mana vidé, pouvoir désactivé 90s");
                    return;
                }

                // Particules sur chaque nœud + lignes en étoile
                for (int i = 0; i < nodes.size(); i++) {
                    Location node = nodes.get(i);
                    world.spawnParticle(Particle.END_ROD, node.clone().add(0, 0.5, 0), 4, 0.2, 0.3, 0.2, 0.05);
                    world.spawnParticle(Particle.TOTEM,   node.clone().add(0, 0.5, 0), 2, 0.1, 0.2, 0.1, 0.02);
                    Location next = nodes.get((i + 2) % nodes.size());
                    drawLine(world, node, next, tick);
                }
                if (tick % 10 == 0)
                    world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f + tick * 0.03f);
                tick++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);

        return true;
    }

    private void drawLine(World world, Location from, Location to, int tick) {
        double dist = from.distance(to);
        int steps = (int)(dist / 0.7);
        double dx = (to.getX() - from.getX()) / steps;
        double dy = (to.getY() - from.getY()) / steps;
        double dz = (to.getZ() - from.getZ()) / steps;
        for (int s = 0; s < steps; s += 2) {
            Location pt = from.clone().add(dx * s, dy * s + 0.3, dz * s);
            world.spawnParticle(Particle.END_ROD, pt, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }
}
