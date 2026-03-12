package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Collection;

/**
 * 🖐 IMAGINE BREAKER - Kamijou Touma
 * Active un bouclier 3s. Au prochain contact : annule les effets + slowness+weakness 5s sur la cible.
 */
public class ImagineBreaker extends Power {

    public ImagineBreaker() {
        super("imagine_breaker", "§f🖐 Imagine Breaker §7(Kamijou Touma)",
              "Annule tous les effets ennemis au contact + ralentit la cible 5s.",
              PowerType.ESPER, 20, 8);
        setCustomModelId(3);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        uhcPlayer.setImagineBreaker(true);

        player.getWorld().spawnParticle(
            Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0),
            20, 0.3, 0.3, 0.3, 0
        );
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        player.sendMessage("§f🖐 §bImagine Breaker §f— Actif pendant 3 secondes !");
        player.sendTitle("§f🖐", "§7Imagine Breaker actif", 5, 30, 10);

        // Auto-désactivation après 3s
        fr.medsir.toaruhc.ToaruUHC.getInstance().getServer().getScheduler()
            .runTaskLater(fr.medsir.toaruhc.ToaruUHC.getInstance(), () -> {
                if (uhcPlayer.hasImagineBreaker()) {
                    uhcPlayer.setImagineBreaker(false);
                    if (player.isOnline()) player.sendMessage("§7🖐 Imagine Breaker expiré.");
                }
            }, 60L);

        return true;
    }

    /**
     * Appliqué depuis PowerListener quand un joueur avec IB frappe quelqu'un.
     * - Annule tous les effets de la cible
     * - Applique Slowness II + Weakness II pendant 5s sur la cible
     */
    public static void applyNullification(Player user, Player target) {
        Collection<PotionEffect> effects = target.getActivePotionEffects();

        // Supprimer tous les effets
        for (PotionEffect e : effects) target.removePotionEffect(e.getType());

        // Appliquer slowness + weakness (cooldown de 5s)
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,        100, 1)); // 5s Slowness II
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,    100, 1)); // 5s Weakness II

        // Effets visuels
        target.getWorld().spawnParticle(
            Particle.ELECTRIC_SPARK,
            target.getLocation().add(0, 1, 0),
            40, 0.5, 0.8, 0.5, 0.1
        );
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 2.0f);

        user.sendMessage("§f🖐 §bImagine Breaker §f— " + (effects.isEmpty() ? "Cible ralentie !" : effects.size() + " effet(s) annulé(s) + cible ralentie !"));
        target.sendMessage("§cImagine Breaker de §b" + user.getName() + " §c— Tu es ralenti pendant 5s !");
        target.sendTitle("§c🖐 BRISÉ", "§7Ralenti 5 secondes...", 5, 50, 10);
    }
}
