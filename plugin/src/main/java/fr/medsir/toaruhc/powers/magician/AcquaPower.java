package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 💧 SAINT EXORCISTE - Acqua of the Back
 * Projette une vague massive dans la direction du regard (15 blocs, 10 steps de 1.5 blocs).
 * Frappe tous les ennemis avec dégâts 10.0 + knockback + Slowness + Weakness.
 * Chaque ennemi ne peut être frappé qu'une fois par activation.
 */
public class AcquaPower extends Power {

    private static final int    STEPS        = 10;
    private static final double STEP_SIZE    = 1.5;
    private static final double HIT_HALF     = 2.0; // demi-hitbox de 4x3x4
    private static final double HIT_HALF_Y   = 1.5;
    private static final double WAVE_DAMAGE  = 10.0;
    private static final double KNOCKBACK_MULT = 2.0;

    public AcquaPower() {
        super("saint_exorciste", "§9💧 Saint Exorciste §7(Acqua of the Back)",
              "Vague d'eau sainte — frappe massive en avant.",
              PowerType.MAGICIAN, 55, 20);
        setCustomModelId(20);
        this.ultimateCost = 80;
        this.ultimateCooldownSeconds = 200;
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.7f);

        player.sendTitle("§9💧 SAINT EXORCISTE", "§7Eau sainte !", 5, 30, 10);
        player.sendMessage("§9💧 §bSaint Exorciste §9— Vague sainte lancée !");

        Location start     = player.getEyeLocation();
        Vector   direction = player.getLocation().getDirection().normalize();
        final Set<UUID> hit = new HashSet<>();

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (!player.isOnline() || step >= STEPS) { cancel(); return; }

                Location wavePos = start.clone().add(direction.clone().multiply(step * STEP_SIZE + 1.0));
                wavePos.setY(player.getLocation().getY() + 1.0);

                // Particules de vague
                world.spawnParticle(Particle.WATER_WAKE,    wavePos, 15, HIT_HALF * 0.6, HIT_HALF_Y * 0.4, HIT_HALF * 0.6, 0.05);
                world.spawnParticle(Particle.DRIP_WATER, wavePos, 10, HIT_HALF * 0.5, HIT_HALF_Y * 0.3, HIT_HALF * 0.5, 0.02);
                world.spawnParticle(Particle.BUBBLE_POP,     wavePos, 12, HIT_HALF * 0.7, HIT_HALF_Y * 0.5, HIT_HALF * 0.7, 0.03);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, wavePos, 8, HIT_HALF * 0.4, HIT_HALF_Y * 0.6, HIT_HALF * 0.4, 0.04);
                world.playSound(wavePos, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.4f, 1.2f);
                world.playSound(wavePos, Sound.BLOCK_WATER_AMBIENT,           0.3f, 1.5f);

                // Hitbox 4x3x4
                for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
                    if (!u.isAlive()) continue;
                    Player enemy = u.getBukkitPlayer();
                    if (enemy == null || !enemy.isOnline() || enemy.equals(player)) continue;
                    if (hit.contains(enemy.getUniqueId())) continue;

                    Location eLoc = enemy.getLocation();
                    if (Math.abs(eLoc.getX() - wavePos.getX()) > HIT_HALF)  continue;
                    if (Math.abs(eLoc.getY() - wavePos.getY()) > HIT_HALF_Y * 2) continue;
                    if (Math.abs(eLoc.getZ() - wavePos.getZ()) > HIT_HALF)  continue;

                    hit.add(enemy.getUniqueId());
                    enemy.damage(WAVE_DAMAGE, player);

                    // Knockback massif vers l'arrière de la vague
                    Vector kb = direction.clone().multiply(KNOCKBACK_MULT).add(new Vector(0, 0.5, 0));
                    enemy.setVelocity(enemy.getVelocity().add(kb));

                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,    80, 1)); // Slowness II 4s
                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0)); // Weakness I 4s

                    world.spawnParticle(Particle.WATER_WAKE, enemy.getLocation().add(0, 1, 0),
                            30, 0.5, 0.7, 0.5, 0.08);
                    world.playSound(enemy.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.8f, 0.6f);

                    enemy.sendMessage("§9💧 §cVague sainte d'§bAcqua §cof the Back — 10 dégâts + Slowness !");
                    enemy.sendTitle("§9💧 EAU SAINTE", "§7Tu es frappé !", 5, 30, 10);
                    player.sendMessage("§9💧 §fVague touche §c" + enemy.getName() + " §f— 10 dégâts !");
                }

                // Collision avec bloc solide
                if (wavePos.getBlock().getType().isSolid()) { cancel(); return; }

                step++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 2L); // 5 blocs/tick ≈ rapide

        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        showUltimateIntro(player, "DIVINE PUNISHMENT IMPACT", "3 vagues divines — 30 dégâts chacune !");
        consumeUltimateResources(uhcPlayer);

        for (Player p : org.bukkit.Bukkit.getOnlinePlayers())
            p.sendMessage("§9🌊 §fAcqua §7déclenche §9DIVINE PUNISHMENT IMPACT §7— 3 vagues divines !");

        World world = player.getWorld();
        Location origin = player.getLocation();

        // 3 vagues avec 40 ticks de délai entre chacune
        for (int wave = 0; wave < 3; wave++) {
            final int waveNum = wave;
            ToaruUHC.getInstance().getServer().getScheduler().runTaskLater(ToaruUHC.getInstance(), () -> {
                if (!player.isOnline()) return;

                // Particules de vague (sphère de water_splash)
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double rx = Math.cos(angle) * (5.0 + waveNum * 3.0);
                    double rz = Math.sin(angle) * (5.0 + waveNum * 3.0);
                    world.spawnParticle(Particle.WATER_SPLASH, origin.clone().add(rx, 0.5, rz),
                            5, 0.5, 0.3, 0.5, 0.2);
                }
                world.playSound(origin, Sound.ENTITY_ELDER_GUARDIAN_HURT, 1.5f, 0.6f + waveNum * 0.1f);

                for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
                    if (!u.isAlive()) continue;
                    Player target = u.getBukkitPlayer();
                    if (target == null || !target.isOnline() || target.equals(player)) continue;
                    if (target.getLocation().distance(origin) > 40.0) continue;

                    target.damage(30.0, player);
                    target.setVelocity(new Vector(
                            (Math.random() - 0.5) * 0.5, 3.0, (Math.random() - 0.5) * 0.5));
                    world.spawnParticle(Particle.WATER_SPLASH, target.getLocation().add(0, 1, 0),
                            40, 1.0, 1.0, 1.0, 0.5);
                    world.strikeLightning(target.getLocation());
                }

                // Contrainte après la 3ème vague
                if (waveNum == 2) {
                    player.damage(20.0);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 600, 3));
                    uhcPlayer.setCooldown("saint_exorciste", 45);
                    player.sendMessage("§9🌊 §7Divine Punishment — §c-20 HP§7, Weakness IV 30s, pouvoir désactivé 45s");
                }
            }, wave * 40L);
        }

        return true;
    }
}
