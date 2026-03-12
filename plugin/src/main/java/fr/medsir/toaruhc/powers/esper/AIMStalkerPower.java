package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import fr.medsir.toaruhc.roles.Role;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

/**
 * 🎯 AIM STALKER - Takitsubo Rikou
 * Vole 40 AIM/Mana à l'ennemi le plus proche (15 blocs).
 * La cible est affaiblie + rendue brillante (Glowing) 6s.
 */
public class AIMStalkerPower extends Power {

    private static final double RANGE     = 15.0;
    private static final int    DRAIN     = 40;
    private static final int    SELF_GAIN = 20;

    public AIMStalkerPower() {
        super("aim_stalker", "§5🎯 AIM Stalker §7(Takitsubo Rikou)",
              "Vole 40 AIM/Mana d'un ennemi proche + Faiblesse + Glowing 6s.",
              PowerType.ESPER, 20, 16);
        setCustomModelId(12);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();

        // Cherche l'ennemi le plus proche dans le rayon
        Player target    = null;
        double bestDist  = RANGE;
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(player)) continue;
            double dist = other.getLocation().distance(player.getLocation());
            if (dist < bestDist) { bestDist = dist; target = other; }
        }

        if (target == null) {
            player.sendMessage("§5🎯 §cAucun ennemi dans un rayon de 15 blocs !");
            return false;
        }

        consumeResources(uhcPlayer);

        UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
        if (uTarget == null) return false;

        // Drain de l'énergie selon le type du joueur ciblé
        Role targetRole   = uTarget.getRole();
        boolean isEsper   = targetRole != null && targetRole.getType() == Role.RoleType.ESPER;
        int actualDrain;
        if (isEsper) {
            actualDrain = Math.min(DRAIN, uTarget.getAim());
            uTarget.setAim(uTarget.getAim() - actualDrain);
        } else {
            actualDrain = Math.min(DRAIN, uTarget.getMana());
            uTarget.setMana(uTarget.getMana() - actualDrain);
        }
        uhcPlayer.regenAim(SELF_GAIN);

        // Mise à jour des barres d'énergie
        ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uTarget);
        ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uhcPlayer);

        // Effets sur la cible
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0)); // 5s
        target.setGlowing(true);
        ToaruUHC.getInstance().getServer().getScheduler()
            .runTaskLater(ToaruUHC.getInstance(), () -> {
                if (target.isOnline()) target.setGlowing(false);
            }, 120L); // 6s

        // Trait de particules de la cible vers soi
        World world = player.getWorld();
        drawDrainBeam(world, target.getLocation().add(0, 1, 0), player.getLocation().add(0, 1, 0));
        world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_STARE,  0.8f, 1.5f);
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT,   0.5f, 2.0f);

        player.sendMessage("§5🎯 §bAIM Stalker §5— Volé §e" + actualDrain
                + (isEsper ? " AIM" : " Mana") + " §5à §c" + target.getName() + " !"
                + " §7(+§e" + SELF_GAIN + " AIM§7)");
        target.sendMessage("§5🎯 §c" + player.getName() + " §ca piraté ton AIM field ! (-"
                + actualDrain + (isEsper ? " AIM" : " Mana") + ")");
        target.sendTitle("§5🎯 AIM DRAIN", "§7Énergie dérobée...", 5, 40, 10);
        return true;
    }

    /** Trait de particules violettes de la source vers la destination. */
    private void drawDrainBeam(World world, Location from, Location to) {
        Vector step  = to.toVector().subtract(from.toVector()).normalize().multiply(0.5);
        Location cur = from.clone();
        int steps    = (int)(from.distance(to) / 0.5);
        for (int i = 0; i < steps; i++) {
            cur.add(step);
            world.spawnParticle(Particle.CRIT_MAGIC, cur, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
