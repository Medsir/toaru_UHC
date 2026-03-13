package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
