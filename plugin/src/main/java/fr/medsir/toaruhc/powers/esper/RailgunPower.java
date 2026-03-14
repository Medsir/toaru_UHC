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

        showUltimateIntro(player, "RAILGUN MAX OUTPUT", "Pièce à puissance maximale — traverse la carte !");
        consumeUltimateResources(uhcPlayer);

        World world = player.getWorld();
        Bukkit.broadcastMessage("§e⚡ §fMisaka §7tire §eRAILGUN MAX OUTPUT §7— La carte tremble !");

        // Sons dramatiques
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.5f);
        world.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 2.0f, 2.0f);

        // Invincibilité brève (0.5s) pendant le tir
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 10, 255, false, false));

        Location start = player.getEyeLocation();
        Vector dir = player.getLocation().getDirection().normalize();

        new org.bukkit.scheduler.BukkitRunnable() {
            final double STEP      = 0.4;
            final double MAX_RANGE = 300.0;
            final double HIT_RADIUS = 1.5;
            double dist = 0;
            Location cur = start.clone();

            @Override
            public void run() {
                // Avancer par lots de 15 steps par tick pour aller vite
                for (int s = 0; s < 15; s++) {
                    if (dist >= MAX_RANGE) { applyConstraint(player, uhcPlayer); cancel(); return; }

                    cur.add(dir.clone().multiply(STEP));
                    dist += STEP;

                    int step = (int)(dist / STEP);

                    // Particules continues
                    world.spawnParticle(Particle.CRIT_MAGIC,     cur, 2, 0.05, 0.05, 0.05, 0.0);
                    world.spawnParticle(Particle.END_ROD,        cur, 1, 0.02, 0.02, 0.02, 0.0);
                    world.spawnParticle(Particle.FIREWORKS_SPARK, cur, 1, 0.03, 0.03, 0.03, 0.0);
                    world.spawnParticle(Particle.FLAME,          cur, 2, 0.1,  0.1,  0.1,  0.02);

                    // Éclair cosmétique tous les 10 steps
                    if (step % 10 == 0) {
                        world.strikeLightningEffect(cur.clone());
                        world.playSound(cur, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1.5f);
                    }

                    // Explosion + feu + destruction de blocs tous les 25 steps
                    if (step % 25 == 0) {
                        world.createExplosion(cur.clone(), 2.0f, true, true, player);
                        world.spawnParticle(Particle.EXPLOSION_LARGE, cur, 3, 0.3, 0.3, 0.3, 0.0);
                    }

                    // Détruire les blocs dans un rayon de 2 autour du chemin
                    if (step % 5 == 0) {
                        for (int dx = -2; dx <= 2; dx++) {
                            for (int dy = -2; dy <= 2; dy++) {
                                for (int dz = -2; dz <= 2; dz++) {
                                    if (dx*dx + dy*dy + dz*dz > 4) continue;
                                    org.bukkit.block.Block b = cur.clone().add(dx,dy,dz).getBlock();
                                    if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK)
                                        b.setType(Material.AIR);
                                }
                            }
                        }
                    }

                    // Blocage (après destruction possible)
                    if (cur.getBlock().getType().isSolid()) {
                        world.createExplosion(cur.clone(), 3.0f, true, true, player);
                        world.spawnParticle(Particle.EXPLOSION_HUGE, cur, 5, 0.4, 0.4, 0.4, 0.0);
                        applyConstraint(player, uhcPlayer);
                        cancel();
                        return;
                    }

                    // Détection ennemis
                    for (org.bukkit.entity.Entity entity : world.getNearbyEntities(cur, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                        if (!(entity instanceof Player target)) continue;
                        if (target.equals(player)) continue;
                        fr.medsir.toaruhc.models.UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
                        if (uTarget == null || !uTarget.isAlive()) continue;

                        target.damage(40.0, player);
                        Vector kb = dir.clone().multiply(2.5);
                        target.setVelocity(target.getVelocity().add(kb));
                        world.strikeLightning(target.getLocation()); // dégâts éclair sur l'ennemi
                        world.spawnParticle(Particle.EXPLOSION_HUGE, target.getLocation().add(0,1,0), 6, 0.4,0.6,0.4,0.0);
                        target.sendTitle("§e⚡ RAILGUN", "§cPièce à pleine puissance !", 3, 30, 5);
                        player.sendMessage("§e⚡ §7Railgun — Touche §f" + target.getName() + " §c(40 dmg)");
                        // Le railgun perce — on n'arrête pas
                    }
                }
            }

            private void applyConstraint(Player player, UHCPlayer uhcPlayer) {
                uhcPlayer.setAim(0);
                ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uhcPlayer);
                uhcPlayer.setCooldown("railgun", 30);
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 2));
                player.sendMessage("§e⚡ §7Railgun Max Output — AIM vidé, Weakness III + Slowness III 10s");
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);

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
