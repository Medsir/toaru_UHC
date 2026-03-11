package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import java.util.Collection;

public class ImagineBreaker extends Power {
    public ImagineBreaker() {
        super("imagine_breaker", "§f🖐 Imagine Breaker §7(Kamijou Touma)",
              "Annule tous les effets de potion ennemis au contact. Actif 3s.",
              PowerType.ESPER, 20, 8);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);
        uhcPlayer.setImagineBreaker(true);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0,1,0), 20, 0.3, 0.3, 0.3, 0);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        player.sendMessage("§f🖐 §bImagine Breaker §f— Actif pendant 3 secondes !");
        fr.medsir.toaruhc.ToaruUHC.getInstance().getServer().getScheduler()
            .runTaskLater(fr.medsir.toaruhc.ToaruUHC.getInstance(), () -> {
                if (uhcPlayer.hasImagineBreaker()) {
                    uhcPlayer.setImagineBreaker(false);
                    if (player.isOnline()) player.sendMessage("§7🖐 Imagine Breaker désactivé.");
                }
            }, 60L);
        return true;
    }

    public static void applyNullification(Player user, Player target) {
        Collection<PotionEffect> effects = target.getActivePotionEffects();
        if (effects.isEmpty()) { user.sendMessage("§7Aucun effet à annuler sur " + target.getName()); return; }
        for (PotionEffect e : effects) target.removePotionEffect(e.getType());
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0,1,0), 40, 0.5, 0.8, 0.5, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        user.sendMessage("§f🖐 §b" + effects.size() + " effet(s) de " + target.getName() + " annulé(s) !");
        target.sendMessage("§cTes effets ont été annulés par §f🖐 Imagine Breaker §cde §b" + user.getName() + "§c!");
    }
}
