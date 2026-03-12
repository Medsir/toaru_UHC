package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * 📖 103 000 GRIMOIRES - Index
 * Déclenche un grimoire aléatoire parmi les 103 000 mémorisés :
 *   I.  Stigma       — Poison II + Slowness II sur le joueur le plus proche (5s)
 *   II. Necronomicon — Absorption II + Regen II + Résistance I pour soi (6s)
 *   III. Banishment  — Propulse et inflige 10 dégâts à tous dans un rayon de 5 blocs
 */
public class GrimoirePower extends Power {

    private static final double AOE_RADIUS   = 5.0;
    private static final double BAN_DAMAGE   = 10.0;
    private static final double BAN_KB_SPEED = 3.0;
    private static final double TARGET_RANGE = 15.0;

    private static final Random RNG = new Random();

    public GrimoirePower() {
        super("grimoire", "§e📖 103 000 Grimoires §7(Index)",
              "Active un grimoire aléatoire : malédiction, protection ou banissement.",
              PowerType.MAGICIAN, 30, 18);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        int roll = RNG.nextInt(3); // 0, 1, 2

        switch (roll) {
            case 0 -> activateStigma(player);
            case 1 -> activateNecronomicon(player);
            case 2 -> activateBanishment(player);
        }

        return true;
    }

    /** I. STIGMA — Poison II + Slowness II sur le joueur ennemi le plus proche. */
    private void activateStigma(Player player) {
        Player target = findNearestEnemy(player);
        if (target == null) {
            player.sendMessage("§e📖 §7Stigma — §cAucun ennemi proche !");
            return;
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON,  100, 1)); // 5s Poison II
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,    100, 1)); // 5s Slowness II

        World world = player.getWorld();
        world.spawnParticle(Particle.CRIT_MAGIC, target.getLocation().add(0, 1, 0),
                30, 0.4, 0.6, 0.4, 0.06);
        world.spawnParticle(Particle.ENCHANTMENT_TABLE, target.getLocation().add(0, 2, 0),
                20, 0.6, 0.4, 0.6, 0.1);
        world.playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.2f);

        player.sendMessage("§e📖 §bGrimoire I — §fStigma §esur §c" + target.getName() + " §e!");
        player.sendTitle("§e📖 STIGMA", "§7Grimoire I sur " + target.getName(), 5, 50, 10);
        target.sendMessage("§cStigma d'§bIndex §c— Poison + Ralentissement 5s !");
        target.sendTitle("§e📖 STIGMA", "§7Maudit par Index...", 5, 50, 10);
    }

    /** II. NECRONOMICON — Bouclier magique sur soi. */
    private void activateNecronomicon(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,     120, 1)); // 6s Absorption II
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,   120, 1)); // 6s Regen II
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 120, 0)); // 6s Résistance I

        World world = player.getWorld();
        world.spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, 1, 0),
                50, 0.7, 0.8, 0.7, 0.15);
        world.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0),
                20, 0.4, 0.6, 0.4, 0.05);
        world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.4f);

        player.sendMessage("§e📖 §bGrimoire II — §fNecronomicon §e— Protection 6s !");
        player.sendTitle("§e📖 NECRONOMICON", "§7Absorption + Regen + Résistance", 5, 60, 10);
    }

    /** III. BANISHMENT — Propulsion + dégâts à tous dans le rayon. */
    private void activateBanishment(Player player) {
        World    world = player.getWorld();
        Location loc   = player.getLocation();
        int hits = 0;

        for (Entity entity : world.getNearbyEntities(loc, AOE_RADIUS, AOE_RADIUS, AOE_RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(player)) continue;

            UHCPlayer uTarget = ToaruUHC.getInstance().getGameManager().getUHCPlayer(target);
            if (uTarget == null || !uTarget.isAlive()) continue;

            target.damage(BAN_DAMAGE, player);
            Vector kb = target.getLocation().toVector()
                    .subtract(loc.toVector())
                    .normalize()
                    .multiply(BAN_KB_SPEED)
                    .add(new Vector(0, 0.8, 0));
            target.setVelocity(kb);

            world.spawnParticle(Particle.ENCHANTMENT_TABLE,
                    target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);
            target.sendMessage("§eBanishment d'§bIndex §e— Propulsé !");
            hits++;
        }

        world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc.clone().add(0, 1, 0),
                60, AOE_RADIUS, 1.0, AOE_RADIUS, 0.2);
        world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0),
                30, AOE_RADIUS * 0.8, 0.5, AOE_RADIUS * 0.8, 0.1);
        world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.6f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 2.0f);

        player.sendMessage("§e📖 §bGrimoire III — §fBanishment §e— " + hits + " cible(s) !");
        player.sendTitle("§e📖 BANISHMENT", "§7Grimoire III", 5, 50, 10);
    }

    /** Trouve le joueur ennemi vivant le plus proche. */
    private Player findNearestEnemy(Player shooter) {
        Player nearest = null;
        double bestDist = TARGET_RANGE;

        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(shooter)) continue;
            double dist = other.getLocation().distance(shooter.getLocation());
            if (dist < bestDist) { bestDist = dist; nearest = other; }
        }
        return nearest;
    }
}
