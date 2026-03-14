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
    private static final int    DRAIN     = 100;
    private static final int    SELF_GAIN = 40;

    public AIMStalkerPower() {
        super("aim_stalker", "§5🎯 AIM Stalker §7(Takitsubo Rikou)",
              "Vole §cTOUT §5l'AIM/Mana d'un ennemi proche + Faiblesse + Glowing 6s.",
              PowerType.ESPER, 20, 16);
        setCustomModelId(12);
        this.ultimateCost = 0;
        this.ultimateCooldownSeconds = 180;
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
        final Player finalTarget = target; // effectively final pour le lambda
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0)); // 5s
        target.setGlowing(true);
        ToaruUHC.getInstance().getServer().getScheduler()
            .runTaskLater(ToaruUHC.getInstance(), () -> {
                if (finalTarget.isOnline()) finalTarget.setGlowing(false);
            }, 120L); // 6s

        // Trait de particules de la cible vers soi — plus dramatique
        World world = player.getWorld();
        drawDrainBeam(world, target.getLocation().add(0, 1, 0), player.getLocation().add(0, 1, 0));
        world.spawnParticle(Particle.CRIT_MAGIC, target.getLocation().add(0, 1, 0), 40, 0.5, 0.7, 0.5, 0.08);
        world.spawnParticle(Particle.SMOKE_NORMAL, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.04);
        world.spawnParticle(Particle.CRIT_MAGIC, player.getLocation().add(0, 1, 0), 25, 0.4, 0.5, 0.4, 0.05);
        world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_STARE,  0.8f, 1.5f);
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT,   0.5f, 2.0f);
        world.playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT,    0.7f, 1.2f);

        player.sendMessage("§5🎯 §bAIM Stalker §5— §cTOUT §eDrainé §7(" + actualDrain
                + (isEsper ? " AIM" : " Mana") + ") §5à §c" + target.getName() + " !"
                + " §7(+§e" + SELF_GAIN + " AIM§7)");
        target.sendMessage("§5🎯 §c" + player.getName() + " §ca drainé §cTOUT §cton AIM field ! (-"
                + actualDrain + (isEsper ? " AIM" : " Mana") + " — VIDE TOTAL !!)");
        target.sendTitle("§5🎯 AIM DRAIN", "§7Énergie dérobée...", 5, 40, 10);
        return true;
    }

    @Override
    public boolean activateUltimate(UHCPlayer uhcPlayer) {
        if (!canUseUltimate(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return false;

        showUltimateIntro(player, "AIM BURST", "Vole tout l'AIM/Mana de tous — explose !");
        consumeUltimateResources(uhcPlayer);

        World world = player.getWorld();
        Bukkit.broadcastMessage("§e💫 §fTakitsubo §7active §eAIM BURST §7— Toute la magie du monde explose !");

        // Drain all alive players (not Takitsubo herself)
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player target = u.getBukkitPlayer();
            if (target == null || !target.isOnline() || target.equals(player)) continue;

            // Get their current energy
            Power p = u.getPower();
            int stolen = (p != null && p.getType() == PowerType.ESPER) ? u.getAim() : u.getMana();

            // Set their energy to 0
            u.setAim(0);
            u.setMana(0);
            ToaruUHC.getInstance().getPowerManager().updateEnergyBar(u);

            // Damage based on stolen energy
            double dmg = Math.min(stolen * 0.15, 25.0);
            target.damage(dmg, player);

            world.strikeLightning(target.getLocation());
            world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                    target.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0.05);
            target.sendTitle("§e💫 AIM BURST", "§cTon énergie explose !", 3, 25, 5);
        }

        // CONSTRAINT
        uhcPlayer.setAim(0);
        ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uhcPlayer);
        player.damage(10.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 400, 1));    // Wither II 20s
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0)); // Blindness 3s
        player.sendMessage("§e💫 §7AIM Burst — §c-10 HP§7, AIM vidé, Wither II 20s");

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
