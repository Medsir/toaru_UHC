package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * SHERRY CROMWELL — Ellis le Golem de Pierre
 * Invoque un Warden avec IA désactivée, contrôlé manuellement tick par tick.
 * Se déplace vers l'ennemi le plus proche et frappe à portée.
 * Ne cible jamais Sherry.
 */
public class SherryCromwellPower extends Power {

    private static final int    DURATION_TICKS  = 100; // 5 secondes
    private static final double ATTACK_RANGE    = 2.5;
    private static final double ATTACK_DAMAGE   = 10.0;
    private static final int    ATTACK_COOLDOWN = 20;  // ticks entre frappes
    private static final double MOVE_SPEED      = 0.4; // blocs/tick

    public SherryCromwellPower() {
        super("sherry_cromwell", "§8Ellis Golem §7(Sherry Cromwell)",
              "Invoque Ellis — attaque l'ennemi le plus proche 5 secondes.",
              PowerType.MAGICIAN, 50, 30);
        setCustomModelId(15);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player sherry = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);

        World world = sherry.getWorld();
        // Spawn devant Sherry
        Vector fwd = sherry.getLocation().getDirection().clone().setY(0).normalize().multiply(1.5);
        Location spawnLoc = sherry.getLocation().clone().add(fwd);

        // Animation de spawn
        for (int i = 0; i < 40; i++) {
            double angle = i * Math.PI / 6.0;
            double h = i * 0.08;
            world.spawnParticle(Particle.SCULK_SOUL,
                    spawnLoc.clone().add(Math.cos(angle)*0.9, h, Math.sin(angle)*0.9),
                    1, 0.04, 0.04, 0.04, 0.0);
            world.spawnParticle(Particle.SMOKE_LARGE,
                    spawnLoc.clone().add(Math.cos(angle)*0.5, h, Math.sin(angle)*0.5),
                    1, 0.04, 0.04, 0.04, 0.0);
        }

        // Spawn Warden avec IA DESACTIVEE
        Warden warden = (Warden) world.spawnEntity(spawnLoc, EntityType.WARDEN);
        warden.setCustomName("§8Ellis §7— Golem de Sherry");
        warden.setCustomNameVisible(true);
        warden.setPersistent(false);
        warden.setAI(false); // IA native désactivée → contrôle manuel total

        world.playSound(spawnLoc, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);
        world.playSound(spawnLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.5f);
        sherry.sendMessage("§8Ellis invoqué §8— Golem actif §e5 secondes§8 !");
        sherry.sendTitle("§8ELLIS", "§7Le Golem charge !", 5, 40, 10);

        final int[] tick       = {0};
        final int[] lastAttack = {-ATTACK_COOLDOWN};

        new BukkitRunnable() {
            @Override
            public void run() {
                // Fin de durée ou warden mort
                if (tick[0] >= DURATION_TICKS || !warden.isValid() || warden.isDead()) {
                    despawn(world, warden, sherry);
                    cancel();
                    return;
                }

                Player target = findNearest(sherry);

                if (target != null) {
                    Location wLoc = warden.getLocation();
                    Location tLoc = target.getLocation();
                    double dist   = wLoc.distance(tLoc);

                    // Déplacement manuel vers la cible
                    if (dist > 1.5) {
                        Vector move = tLoc.toVector().subtract(wLoc.toVector()).setY(0).normalize()
                                .multiply(MOVE_SPEED);
                        double yDiff = tLoc.getY() - wLoc.getY();
                        if (yDiff > 0.3) move.setY(0.45); // saut
                        warden.setVelocity(move);
                    }

                    // Orienter le Warden vers la cible
                    Location look = wLoc.clone();
                    look.setDirection(tLoc.clone().subtract(wLoc).toVector());
                    warden.teleport(look);

                    // Frappe manuelle quand à portée
                    if (dist <= ATTACK_RANGE && (tick[0] - lastAttack[0]) >= ATTACK_COOLDOWN) {
                        lastAttack[0] = tick[0];
                        target.damage(ATTACK_DAMAGE, warden);
                        world.playSound(wLoc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.0f, 0.8f);
                        world.playSound(wLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 0.5f);
                        world.spawnParticle(Particle.SCULK_SOUL, tLoc.clone().add(0,1,0),
                                20, 0.5, 0.6, 0.5, 0.08);
                        world.spawnParticle(Particle.CRIT, tLoc.clone().add(0,1,0),
                                20, 0.3, 0.4, 0.3, 0.05);
                        target.sendMessage("§8Ellis te frappe ! (§c-" + (int)ATTACK_DAMAGE + " HP§8)");
                        target.sendTitle("§8ELLIS", "§cFrappe !", 3, 18, 4);
                    }
                }

                // Particules ambiantes toutes les 5 ticks
                if (tick[0] % 5 == 0) {
                    Location wLoc = warden.getLocation().clone().add(0, 1, 0);
                    world.spawnParticle(Particle.SCULK_SOUL,  wLoc, 3, 0.4, 0.5, 0.4, 0.03);
                    world.spawnParticle(Particle.SMOKE_LARGE,  wLoc, 2, 0.3, 0.3, 0.3, 0.02);
                }

                tick[0]++;
            }
        }.runTaskTimer(ToaruUHC.getInstance(), 2L, 1L);

        return true;
    }

    private void despawn(World world, Warden warden, Player sherry) {
        if (!warden.isValid() || warden.isDead()) return;
        Location loc = warden.getLocation().clone().add(0, 1, 0);
        world.spawnParticle(Particle.CLOUD,       loc, 25, 0.6, 0.7, 0.6, 0.06);
        world.spawnParticle(Particle.SCULK_SOUL,  loc, 15, 0.5, 0.6, 0.5, 0.05);
        world.spawnParticle(Particle.SMOKE_LARGE,  loc, 20, 0.4, 0.5, 0.4, 0.04);
        world.playSound(warden.getLocation(), Sound.ENTITY_WARDEN_DEATH, 0.9f, 1.0f);
        warden.remove();
        if (sherry.isOnline()) sherry.sendMessage("§8Ellis disparaît dans les ténèbres...");
    }

    private Player findNearest(Player sherry) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(sherry)) continue;
            double d = other.getLocation().distance(sherry.getLocation());
            if (d < bestDist) { bestDist = d; best = other; }
        }
        return best;
    }
}
