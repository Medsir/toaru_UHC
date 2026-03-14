package fr.medsir.toaruhc.powers.magician;

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
 * ✝ THE HOLY RIGHT - Fiamma of the Right
 * Faisceau saint lent (0.5 blocs/tick) sur 80 blocs — dégâts massifs (18) + foudre.
 * Coût très élevé (60 Mana), cooldown long (40s). Pouvoir ultime.
 */
public class HolyRightPower extends Power {

    private static final double DAMAGE        = 18.0;
    private static final double MAX_DISTANCE  = 80.0;
    private static final double STEP          = 0.5;
    private static final int    STEPS_PER_TICK = 1;   // 0.5 blocs/tick = ~10 blocs/s (lent mais imparable)

    public HolyRightPower() {
        super("holy_right", "§e✝ The Holy Right §7(Fiamma of the Right)",
              "Faisceau saint ultime — 18 dégâts + foudre sur 80 blocs.",
              PowerType.MAGICIAN, 60, 40);
        setCustomModelId(13);
        this.ultimateCost = 100;
        this.ultimateCooldownSeconds = 240;
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        Location start    = player.getEyeLocation();
        Vector   direction = player.getLocation().getDirection().normalize();

        player.getWorld().playSound(start, Sound.BLOCK_BEACON_ACTIVATE,  1.5f, 0.4f);
        player.getWorld().playSound(start, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.3f);
        player.sendMessage("§e✝ §bThe Holy Right §e— Que Dieu se détourne !");
        player.sendTitle("§e✝ HOLY RIGHT", "§7La droite qui dépasse Dieu...", 5, 50, 10);

        fireHolyBeam(player, start, direction);
        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        showUltimateIntro(player, "HOLY RIGHT OVERLOAD", "La lumière sacrée frappe toute la carte !");
        consumeUltimateResources(uhcPlayer);

        for (Player p : org.bukkit.Bukkit.getOnlinePlayers())
            p.sendMessage("§e☀ §fFiamma §7déchaîne §eHOLY RIGHT OVERLOAD §7— Toute la carte est frappée !");

        World world = player.getWorld();
        Location loc = player.getLocation();

        // Fiamma lévite + invincible pendant le cast
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 255, false, false));

        // Colonne de lumière
        for (double h = 0; h < 15; h += 0.5) {
            world.spawnParticle(Particle.END_ROD, loc.clone().add(0, h, 0), 3, 0.2, 0.05, 0.2, 0.02);
        }

        // Sons
        world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.7f);

        // Frappe tous les ennemis vivants
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player target = u.getBukkitPlayer();
            if (target == null || !target.isOnline() || target.equals(player)) continue;

            target.damage(20.0, player);
            target.setFireTicks(100);
            // Supprimer effets positifs
            for (PotionEffect pe : target.getActivePotionEffects()) {
                PotionEffectType t = pe.getType();
                if (t == PotionEffectType.SPEED || t == PotionEffectType.INCREASE_DAMAGE
                        || t == PotionEffectType.DAMAGE_RESISTANCE || t == PotionEffectType.REGENERATION
                        || t == PotionEffectType.ABSORPTION || t == PotionEffectType.JUMP
                        || t == PotionEffectType.FIRE_RESISTANCE || t == PotionEffectType.INVISIBILITY) {
                    target.removePotionEffect(t);
                }
            }
            world.strikeLightning(target.getLocation());
            world.spawnParticle(Particle.TOTEM, target.getLocation().add(0, 1, 0), 25, 0.5, 0.7, 0.5, 0.1);
            target.sendTitle("§e☀ HOLY RIGHT", "§cLumière sacrée !", 3, 25, 5);
        }

        // Contraintes
        uhcPlayer.setMana(0);
        ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uhcPlayer);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 400, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 1));
        player.damage(10.0);
        uhcPlayer.setCooldown("holy_right", 90);
        player.sendMessage("§e☀ §7Holy Right Overload — §c-10 HP§7, Mana vidé, Weakness III, désactivé 90s");

        return true;
    }

    private void fireHolyBeam(Player shooter, Location start, Vector dir) {
        final Location current = start.clone();
        final double[] dist    = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!shooter.isOnline()) { cancel(); return; }

                for (int i = 0; i < STEPS_PER_TICK; i++) {
                    if (dist[0] >= MAX_DISTANCE) { cancel(); return; }

                    current.add(dir.clone().multiply(STEP));
                    dist[0] += STEP;

                    World world = current.getWorld();

                    // Faisceau saint — lumière blanche et runes
                    world.spawnParticle(Particle.END_ROD,           current, 4, 0.08, 0.08, 0.08, 0.0);
                    world.spawnParticle(Particle.ENCHANTMENT_TABLE, current, 3, 0.12, 0.12, 0.12, 0.0);
                    if ((int)(dist[0] / STEP) % 3 == 0)
                        world.spawnParticle(Particle.CRIT_MAGIC,    current, 2, 0.05, 0.05, 0.05, 0.0);

                    // Détection entités
                    for (Entity entity : world.getNearbyEntities(current, 1.0, 1.0, 1.0)) {
                        if (!(entity instanceof Player target)) continue;
                        if (target.equals(shooter)) continue;

                        // Impact : foudre réelle + dégâts
                        world.strikeLightning(target.getLocation());
                        target.damage(DAMAGE, shooter);

                        world.spawnParticle(Particle.END_ROD,
                                target.getLocation().add(0, 1, 0), 60, 0.8, 1.0, 0.8, 0.2);
                        world.spawnParticle(Particle.ENCHANTMENT_TABLE,
                                target.getLocation().add(0, 2, 0), 40, 1.0, 0.5, 1.0, 0.3);
                        world.playSound(target.getLocation(), Sound.ENTITY_EVOKER_DEATH,         1.0f, 0.5f);
                        world.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 1.0f);

                        shooter.sendMessage("§e✝ §fHoly Right frappe §c" + target.getName()
                                + " §e— " + (int)DAMAGE + " dégâts !");
                        target.sendMessage("§e§l✝ LA DROITE SACRÉE §cde §b" + shooter.getName()
                                + " §c— " + (int)DAMAGE + " dégâts !");
                        target.sendTitle("§e✝ HOLY RIGHT", "§cFrappé par Fiamma !", 5, 60, 10);
                        cancel(); return;
                    }

                    // Collision mur
                    if (current.getBlock().getType().isSolid()) {
                        world.spawnParticle(Particle.END_ROD, current, 30, 0.5, 0.5, 0.5, 0.1);
                        world.playSound(current, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
                        cancel(); return;
                    }
                }
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 0L, 1L);
    }
}
