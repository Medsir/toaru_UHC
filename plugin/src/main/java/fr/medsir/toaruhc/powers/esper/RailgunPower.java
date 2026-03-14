package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * ⚡ RAILGUN - Misaka Mikoto
 * Trait de particules animé + foudre infligeant des dégâts sur impact.
 */
public class RailgunPower extends Power {

    private static final double DAMAGE        = 16.0;
    private static final double MAX_DISTANCE  = 60.0;
    private static final double STEP          = 0.5;   // blocs par itération
    private static final int    STEPS_PER_TICK = 4;    // itérations par tick → 2 blocs/tick = 40 blocs/s

    public RailgunPower() {
        super("railgun", "§e⚡ Railgun §7(Misaka Mikoto)",
              "Lance un trait électrique dévastateur devant soi.",
              PowerType.ESPER, 30, 12);
        setCustomModelId(5);
        this.ultimateCost = 80;
        this.ultimateCooldownSeconds = 180;
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        Location start     = player.getEyeLocation();
        Vector   direction = player.getLocation().getDirection().normalize();

        // Son de départ
        player.getWorld().playSound(start, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 2.0f);
        player.sendMessage("§e⚡ §bRailgun §e— §fFeu !");

        fireRailgun(player, start, direction);
        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        showUltimateIntro(player, "RAILGUN MAX OUTPUT", "Pièce au maximum — traverse la carte !");
        consumeUltimateResources(uhcPlayer);

        World world = player.getWorld();
        Bukkit.broadcastMessage("§e⚡ §fMisaka §7tire §eRAILGUN MAX OUTPUT §7— Pièce à puissance maximale !");

        // Sounds
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
        world.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 2.0f, 1.0f);
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.8f);

        // Fire massive railgun raycast
        Location start = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection().normalize();
        final double ULT_STEP = 0.3;
        final double ULT_MAX = 300.0;

        new BukkitRunnable() {
            final Location cur = start.clone();
            double dist = 0;
            int stepCount = 0;

            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }

                // Process multiple steps per tick for speed
                for (int i = 0; i < 12; i++) {
                    if (dist >= ULT_MAX) { cancel(); return; }

                    cur.add(direction.clone().multiply(ULT_STEP));
                    dist += ULT_STEP;
                    stepCount++;

                    World w = cur.getWorld();

                    // Particles every step
                    w.spawnParticle(Particle.CRIT_MAGIC, cur, 3, 0.05, 0.05, 0.05, 0.02);
                    w.spawnParticle(Particle.END_ROD, cur, 1, 0.02, 0.02, 0.02, 0.0);
                    w.spawnParticle(Particle.FIREWORKS_SPARK, cur, 1, 0.05, 0.05, 0.05, 0.0);

                    // Terrain shake every 30 steps
                    if (stepCount % 30 == 0) {
                        w.createExplosion(cur.clone(), 1.5f, false, false);
                    }

                    // Block collision
                    if (cur.getBlock().getType().isSolid()) {
                        w.spawnParticle(Particle.EXPLOSION_LARGE, cur, 5, 0.3, 0.3, 0.3, 0.0);
                        cancel();
                        return;
                    }

                    // Hit detection
                    for (Entity entity : w.getNearbyEntities(cur, 1.2, 1.2, 1.2)) {
                        if (!(entity instanceof Player target)) continue;
                        if (target.equals(player)) continue;
                        UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
                        if (uTarget == null || !uTarget.isAlive()) continue;

                        // Massive damage
                        target.damage(40.0, player);

                        // Horizontal knockback in beam direction
                        Vector kb = direction.clone().setY(0).normalize().multiply(2.5);
                        target.setVelocity(target.getVelocity().add(kb));

                        w.strikeLightning(target.getLocation());
                        w.spawnParticle(Particle.EXPLOSION_HUGE, target.getLocation(), 5, 0.3, 0.3, 0.3, 0.0);
                        target.sendTitle("§e⚡ RAILGUN", "§cPièce à pleine puissance !", 3, 30, 5);
                        player.sendMessage("§e⚡ §7Railgun — Touche §f" + target.getName() + " §c(40 dmg)");
                        // Do NOT stop — railgun pierces through
                    }
                }
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);

        // CONSTRAINT: applied after beam fired (schedule 1 tick later to ensure it fires after initial setup)
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(ToaruUHC.getInstance(), () -> {
            if (!player.isOnline()) return;
            uhcPlayer.setAim(0);
            ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uhcPlayer);
            uhcPlayer.setCooldown("railgun", 30);
            player.damage(8.0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            player.sendMessage("§e⚡ §7Railgun Max Output — §c-8 HP§7, AIM vidé, Weakness III 10s, Blindness 5s");
        }, 2L);

        return true;
    }

    private void fireRailgun(Player shooter, Location start, Vector dir) {
        final Location current = start.clone();
        final double[] dist    = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                // Sécurité : shooter déconnecté
                if (!shooter.isOnline()) { cancel(); return; }

                for (int i = 0; i < STEPS_PER_TICK; i++) {
                    if (dist[0] >= MAX_DISTANCE) { cancel(); return; }

                    current.add(dir.clone().multiply(STEP));
                    dist[0] += STEP;

                    World world = current.getWorld();

                    // ── Trait de particules ─────────────────────────────────────
                    // Noyau blanc brillant (END_ROD = traînée de lumière)
                    world.spawnParticle(Particle.END_ROD,
                            current, 2, 0.02, 0.02, 0.02, 0.0);
                    // Étincelles électriques autour du faisceau
                    world.spawnParticle(Particle.ELECTRIC_SPARK,
                            current, 4, 0.08, 0.08, 0.08, 0.04);

                    // ── Détection d'entités ──────────────────────────────────────
                    for (Entity entity : world.getNearbyEntities(current, 0.8, 0.8, 0.8)) {
                        if (!(entity instanceof Player target)) continue;
                        if (target == shooter) continue;

                        // Foudre réelle → dégâts + visuel
                        world.strikeLightning(target.getLocation());
                        target.damage(DAMAGE, shooter);

                        // Explosion de particules à l'impact
                        world.spawnParticle(Particle.ELECTRIC_SPARK,
                                target.getLocation().add(0, 1, 0),
                                50, 0.5, 0.6, 0.5, 0.15);
                        world.playSound(target.getLocation(),
                                Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 1.5f);

                        shooter.sendMessage("§e⚡ §fTouche §c" + target.getName() + "§f !");
                        target.sendMessage("§cTouché par le §eRailgun §cde §b"
                                + shooter.getName() + "§c!");
                        cancel(); return;
                    }

                    // ── Collision bloc solide ────────────────────────────────────
                    if (current.getBlock().getType().isSolid()) {
                        // Visuel seulement sur le mur (pas de dégâts terrain)
                        world.strikeLightningEffect(current.clone());
                        world.spawnParticle(Particle.ELECTRIC_SPARK,
                                current, 20, 0.3, 0.3, 0.3, 0.05);
                        cancel(); return;
                    }
                }
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);
    }
}
