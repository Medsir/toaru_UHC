package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * 💥 GUTS - Sogiita Gunha (Level 5)
 * Explosion AOE : repousse et blesse tous les ennemis proches.
 * Si HP ≤ 40% → GUTS MODE (Speed II + Strength II pendant 5s).
 */
public class GutsPower extends Power {

    private static final double DAMAGE      = 8.0;
    private static final double RADIUS      = 6.0;
    private static final double KB_SPEED    = 2.5;
    private static final double HP_THRESHOLD = 0.4; // 40% HP

    private static final Random RANDOM = new Random();

    public GutsPower() {
        super("guts", "§6💥 Guts §7(Sogiita Gunha)",
              "Explosion AOE + knockback. GUTS MODE si HP faibles !",
              PowerType.ESPER, 35, 20);
        setCustomModelId(2);
        this.ultimateCost = 0;
        this.ultimateCooldownSeconds = 86400; // once per game
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World    world = player.getWorld();
        Location loc   = player.getLocation();

        // Explosion visuelle (0 force = aucun dégât terrain)
        world.createExplosion(loc.clone().add(0, 0.5, 0), 0f, false, false);
        world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 8, 1.5, 0.5, 1.5, 0.0);
        world.spawnParticle(Particle.CRIT,            loc.clone().add(0, 1, 0), 30, 1.2, 1.0, 1.2, 0.2);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.7f);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.5f);

        // AOE sur les joueurs proches
        int hits = 0;
        for (Entity entity : world.getNearbyEntities(loc, RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(player)) continue;

            target.damage(DAMAGE, player);

            // Knockback radial
            Vector kb = target.getLocation().toVector()
                    .subtract(loc.toVector())
                    .normalize()
                    .multiply(KB_SPEED)
                    .add(new Vector(0, 0.7, 0));
            target.setVelocity(kb);

            world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0),
                    15, 0.3, 0.4, 0.3, 0.1);
            target.sendMessage("§6💥 §cTouché par le §bGuts §cde §b" + player.getName() + " §c!");
            hits++;
        }

        // GUTS MODE si HP ≤ 40%
        boolean gutsMode = player.getHealth() <= player.getMaxHealth() * HP_THRESHOLD;
        if (gutsMode) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,          100, 1)); // 5s Speed II
            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100, 1)); // 5s Strength II
            world.spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0), 50, 0.5, 0.7, 0.5, 0.3);
            world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
            player.sendTitle("§6💥 GUTS MODE !", "§7Il y a des choses qui ne peuvent être perdues !", 5, 80, 15);
        } else {
            player.sendTitle("§6💥 GUTS !", "§7" + hits + " cible(s) touchée(s)", 5, 40, 10);
        }

        player.sendMessage("§6💥 §bGuts §6— " + hits + " cible(s) touchée(s) !"
                + (gutsMode ? " §c§lGUTS MODE actif 5s !" : ""));
        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        // HP Check: must be at <= 4 hearts (8.0 HP)
        if (player.getHealth() > 8.0) {
            player.sendMessage("§6💥 §cNot enough GUTS ! (HP ≤ 4 coeurs requis)");
            return false;
        }

        showUltimateIntro(player, "NUMBER ONE MOVE: GREAT IMPACT", "À ≤4 coeurs — EXPLOSION TOTALE !");
        consumeUltimateResources(uhcPlayer);

        World world = player.getWorld();
        Bukkit.broadcastMessage("§6💥 §fGunha §7déclenche §6NUMBER ONE MOVE §7— Il lui reste des GUTS !");

        // Sounds x3 thunder + explode
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
        world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);

        // Launch Gunha into the air
        player.setVelocity(new Vector(0, 3.0, 0));

        // Invincible while flying up
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 40, 255, false, false));

        // Schedule shockwave after 30 ticks (landing time)
        ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(
                ToaruUHC.getInstance(), () -> {
                    if (!player.isOnline()) return;
                    Location landLoc = player.getLocation();
                    World w = player.getWorld();

                    // Multiple explosions in expanding radius
                    for (double radius = 0; radius <= 5; radius += 1.5) {
                        double offsetX = (RANDOM.nextDouble() - 0.5) * radius * 2;
                        double offsetZ = (RANDOM.nextDouble() - 0.5) * radius * 2;
                        Location expLoc = landLoc.clone().add(offsetX, 0, offsetZ);
                        w.createExplosion(expLoc, 4.0f, false, false, player);
                    }

                    // AOE on all alive enemies in 30 blocks
                    for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
                        if (!u.isAlive()) continue;
                        Player target = u.getBukkitPlayer();
                        if (target == null || !target.isOnline() || target.equals(player)) continue;
                        if (target.getLocation().distance(landLoc) > 30.0) continue;

                        target.damage(25.0, player);

                        // Random knockback direction
                        double angle = RANDOM.nextDouble() * Math.PI * 2;
                        Vector kb = new Vector(Math.cos(angle), 0, Math.sin(angle))
                                .normalize().multiply(2.5).add(new Vector(0, 1.5, 0));
                        target.setVelocity(kb);

                        target.sendTitle("§6💥 GREAT IMPACT", "§cGunha — GUTS !", 3, 25, 5);
                        w.strikeLightning(target.getLocation());
                    }

                    // 15 EXPLOSION_HUGE particles at landing
                    for (int i = 0; i < 15; i++) {
                        w.spawnParticle(Particle.EXPLOSION_HUGE, landLoc.clone().add(
                                (RANDOM.nextDouble() - 0.5) * 4,
                                RANDOM.nextDouble() * 2,
                                (RANDOM.nextDouble() - 0.5) * 4), 1, 0, 0, 0, 0);
                    }
                    w.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);

                    // CONSTRAINT
                    player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                    player.setHealth(1.0);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 300, 3)); // Slowness IV 15s
                    player.sendMessage("§6💥 §7Great Impact — §cHP réduit à 0.5 coeur§7, Slowness IV 15s — §6GUTS vaincus mais pas tombé !");
                    Bukkit.broadcastMessage("§6💥 §fGunha §7s'est transcendé — HP à 1, mais toujours debout !");
                }, 30L);

        return true;
    }
}
