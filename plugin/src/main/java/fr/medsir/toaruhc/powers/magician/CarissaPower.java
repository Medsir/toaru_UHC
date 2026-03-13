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
 * ⚔ CURTANA ORIGINAL - Carissa
 * AOE 80 blocs : Buff massif sur Carissa, Debuff massif sur tous les ennemis.
 * 20 éclairs décoratifs, aura pulsante pendant 10 secondes.
 */
public class CarissaPower extends Power {

    private static final double LIGHTNING_RADIUS = 10.0;
    private static final double ENEMY_RADIUS     = 80.0;
    private static final int    DURATION_TICKS   = 200; // 10 secondes

    public CarissaPower() {
        super("curtana_original", "§6⚔ Curtana Original §7(Carissa)",
              "Tranche la réalité — Buff massif + Debuff 80 blocs pendant 10s.",
              PowerType.MAGICIAN, 60, 45);
        setCustomModelId(16);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = player.getWorld();
        Location origin = player.getLocation();

        // Sons épiques
        world.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
        world.playSound(origin, Sound.ITEM_TOTEM_USE, 1.0f, 0.7f);

        // 20 éclairs décoratifs en cercle de 10 blocs
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI / 20) * i;
            double lx = Math.cos(angle) * LIGHTNING_RADIUS;
            double lz = Math.sin(angle) * LIGHTNING_RADIUS;
            Location lightningLoc = origin.clone().add(lx, 0, lz);
            // strikeLightningEffect = décoratif, pas de dégâts de foudre
            world.strikeLightningEffect(lightningLoc);
        }

        // Buff sur Carissa (10s)
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, DURATION_TICKS, 2)); // Strength III
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,           DURATION_TICKS, 1)); // Speed II
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, DURATION_TICKS, 1)); // Resistance II

        // Debuff sur tous les ennemis dans 80 blocs
        int enemyCount = 0;
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player enemy = u.getBukkitPlayer();
            if (enemy == null || !enemy.isOnline() || enemy.equals(player)) continue;
            if (enemy.getLocation().distance(origin) > ENEMY_RADIUS) continue;

            enemy.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,      DURATION_TICKS, 2)); // Weakness III
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,          DURATION_TICKS, 1)); // Slowness II
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,  DURATION_TICKS, 1)); // Mining Fatigue II
            enemy.sendMessage("§6⚔ §cCurtana Original de §b" + player.getName()
                    + " §c— Weakness + Slowness + Fatigue 10s !");
            enemy.sendTitle("§6⚔ CURTANA", "§7La réalité te soumet...", 5, 60, 15);
            enemyCount++;
        }

        player.sendMessage("§6⚔ §bCurtana Original §6— §c" + enemyCount
                + " ennemi(s) §7affaibli(s) dans 80 blocs ! §6Buff Carissa 10s !");
        player.sendTitle("§6⚔ CURTANA ORIGINAL", "§7La réalité se soumet à toi", 5, 60, 15);

        // Aura pulsante : anneaux de particules toutes les 2 secondes pendant 10s
        new BukkitRunnable() {
            int pulses = 0;

            @Override
            public void run() {
                if (!player.isOnline() || pulses >= 5) { cancel(); return; }
                Location playerLoc = player.getLocation();

                // Anneau de CRIT dorées se propageant vers l'extérieur
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double r = 3.0 + pulses * 1.0;
                    double rx = Math.cos(angle) * r;
                    double rz = Math.sin(angle) * r;
                    world.spawnParticle(Particle.CRIT, playerLoc.clone().add(rx, 0.5, rz),
                            2, 0.1, 0.1, 0.1, 0.02);
                    world.spawnParticle(Particle.FIREWORKS_SPARK, playerLoc.clone().add(rx, 0.5, rz),
                            1, 0.1, 0.1, 0.1, 0.03);
                }
                world.playSound(playerLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 1.5f);
                pulses++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 40L); // toutes les 2 secondes

        return true;
    }
}
