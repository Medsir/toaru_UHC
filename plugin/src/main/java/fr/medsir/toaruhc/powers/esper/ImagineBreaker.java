package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 🖐 IMAGINE BREAKER - Kamijou Touma
 * Activation : AOE immédiate sur 6 blocs — force cooldown + supprime tous les effets.
 * Garde aussi le bouclier de contact pendant 3 secondes.
 */
public class ImagineBreaker extends Power {

    private static final double AOE_RADIUS = 6.0;

    public ImagineBreaker() {
        super("imagine_breaker", "§f🖐 Imagine Breaker §7(Kamijou Touma)",
              "AOE 6 blocs : annule effets + cooldown forcé 15s. Bouclier contact 3s.",
              PowerType.ESPER, 20, 8);
        setCustomModelId(3);
        this.ultimateCost = 0;
        this.ultimateCooldownSeconds = 180;
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = player.getWorld();

        // AOE immédiate sur tous les ennemis dans le rayon
        int hitCount = 0;
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player enemy = u.getBukkitPlayer();
            if (enemy == null || !enemy.isOnline() || enemy.equals(player)) continue;

            double dist = enemy.getLocation().distance(player.getLocation());
            if (dist > AOE_RADIUS) continue;

            applyNullification(player, enemy);
            // Forcer cooldown sur le pouvoir de la cible
            Power enemyPower = u.getPower();
            if (enemyPower != null) {
                u.setCooldown(enemyPower.getId(), 15);
            }

            // Particules électriques du joueur vers la cible
            drawElectricBeam(world, player.getLocation().add(0, 1, 0), enemy.getLocation().add(0, 1, 0));
            hitCount++;
        }

        // Activer le bouclier de contact pendant 3 secondes
        uhcPlayer.setImagineBreaker(true);

        // Sons dramatiques
        world.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.9f, 1.2f);
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);

        // Particules autour du joueur
        world.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0),
                60, 0.7, 0.9, 0.7, 0.1);
        world.spawnParticle(Particle.CRIT_MAGIC, player.getLocation().add(0, 1, 0),
                20, 0.5, 0.6, 0.5, 0.05);

        String hitMsg = hitCount > 0
                ? "§f— §c" + hitCount + " ennemi(s) §fnullifiés !"
                : "§f— §7Aucun ennemi à portée.";
        player.sendMessage("§f🖐 §bImagine Breaker §f" + hitMsg + " §7(Bouclier contact 3s)");
        player.sendTitle("§f🖐", "§7Imagine Breaker — AOE " + AOE_RADIUS + " blocs", 5, 30, 10);

        // Auto-désactivation du bouclier de contact après 3s
        fr.medsir.toaruhc.ToaruUHC.getInstance().getServer().getScheduler()
            .runTaskLater(fr.medsir.toaruhc.ToaruUHC.getInstance(), () -> {
                if (uhcPlayer.hasImagineBreaker()) {
                    uhcPlayer.setImagineBreaker(false);
                    if (player.isOnline()) player.sendMessage("§7🖐 Bouclier Imagine Breaker expiré.");
                }
            }, 60L);

        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        showUltimateIntro(player, "DRAGON'S STRIKE", "Le bras obscur surgit — annule toute magie !");
        consumeUltimateResources(uhcPlayer);

        World world = player.getWorld();
        Bukkit.broadcastMessage("§0🐉 §fTouma §7déclenche §8DRAGON'S STRIKE §7— La Main des Ténèbres !");

        // Sounds
        world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);
        world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // Massive AOE 20-block explosion effect
        world.createExplosion(player.getLocation(), 3.5f, false, false, player);
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI / 20) * i;
            double rx = Math.cos(angle) * 2.0;
            double rz = Math.sin(angle) * 2.0;
            world.spawnParticle(Particle.EXPLOSION_HUGE,
                    player.getLocation().add(rx, 0.5, rz), 1, 0.1, 0.1, 0.1, 0.0);
        }
        world.strikeLightning(player.getLocation());

        // AOE on all alive enemies within 20 blocks
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player target = u.getBukkitPlayer();
            if (target == null || !target.isOnline() || target.equals(player)) continue;
            if (target.getLocation().distance(player.getLocation()) > 20.0) continue;

            target.damage(20.0, player);

            // Remove ALL active potion effects
            for (PotionEffect pe : new ArrayList<>(target.getActivePotionEffects())) {
                target.removePotionEffect(pe.getType());
            }

            // Force 30s cooldown on their power
            if (u.getPower() != null) {
                u.setCooldown(u.getPower().getId(), 30);
            }
            // Force ult cooldown
            if (u.getPower() != null) {
                u.setCooldown("ult_" + u.getPower().getId(), 60);
            }

            target.sendTitle("§0🐉 DRAGON'S STRIKE", "§7Imagination annihilée...", 3, 30, 8);
            world.spawnParticle(Particle.EXPLOSION_HUGE,
                    target.getLocation().add(0, 1, 0), 8, 0.5, 0.7, 0.5, 0.0);
        }

        // CONSTRAINT
        player.damage(10.0);
        uhcPlayer.setImagineBreaker(false);
        uhcPlayer.setCooldown("imagine_breaker", 60);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 200, 4)); // Mining Fatigue V 10s
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));     // Weakness II
        player.sendMessage("§8🐉 §7Dragon's Strike — §c-10 HP§7, IB brisé 60s, Mining Fatigue V 10s");

        return true;
    }

    /**
     * Appliqué depuis PowerListener quand un joueur avec IB frappe quelqu'un,
     * OU lors de l'AOE au lancement.
     * - Annule tous les effets de la cible
     * - Applique Slowness II + Weakness II pendant 5s
     * - Force le cooldown du pouvoir de la cible
     */
    public static void applyNullification(Player user, Player target) {
        Collection<PotionEffect> effects = target.getActivePotionEffects();

        // Supprimer tous les effets (potions de soin annulées aussi)
        for (PotionEffect e : effects) target.removePotionEffect(e.getType());

        // Appliquer slowness + weakness
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,     100, 1)); // 5s Slowness II
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1)); // 5s Weakness II

        // Effets visuels
        target.getWorld().spawnParticle(
            Particle.ELECTRIC_SPARK,
            target.getLocation().add(0, 1, 0),
            50, 0.6, 0.9, 0.6, 0.12
        );
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK,           1.0f, 0.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 2.0f);

        String effectMsg = effects.isEmpty()
                ? "Cible ralentie !"
                : effects.size() + " effet(s) annulé(s) + cible ralentie + cooldown forcé 15s !";
        user.sendMessage("§f🖐 §bImagine Breaker §f— " + effectMsg);
        target.sendMessage("§cImagine Breaker de §b" + user.getName()
                + " §c— Tous tes effets annulés ! Ralenti 5s + Cooldown 15s !");
        target.sendTitle("§c🖐 BRISÉ", "§7Annulation totale !", 5, 50, 10);
    }

    /** Dessine un trait de particules électriques entre deux points. */
    private void drawElectricBeam(World world, Location from, Location to) {
        org.bukkit.util.Vector step = to.toVector().subtract(from.toVector()).normalize().multiply(0.5);
        Location cur = from.clone();
        int steps = (int) (from.distance(to) / 0.5);
        for (int i = 0; i < steps; i++) {
            cur.add(step);
            world.spawnParticle(Particle.ELECTRIC_SPARK, cur, 2, 0.05, 0.05, 0.05, 0.0);
        }
    }
}
