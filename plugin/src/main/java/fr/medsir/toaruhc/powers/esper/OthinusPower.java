package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Random;

/**
 * ✦ HALF-A-GOD - Othinus
 * 50/50 : victoire → 15 dégâts à l'ennemi, défaite → 15 dégâts sur Othinus.
 * Avec Gungnir dans l'inventaire : TOUJOURS victoire, 20 dégâts.
 */
public class OthinusPower extends Power {

    public static final String GUNGNIR_NAME = "§5✦ Gungnir §8— Lance des Dieux";
    public static final String GUNGNIR_LORE = "§7Appartient à Othinus. Supprime le 50/50.";

    private static final double NORMAL_DAMAGE  = 15.0;
    private static final double GUNGNIR_DAMAGE = 20.0;
    private static final double ENEMY_RADIUS   = 20.0;

    private static final Random RANDOM = new Random();

    public OthinusPower() {
        super("half_a_god", "§5✦ Half-a-God §7(Othinus)",
              "50/50 — Destin ou catastrophe. Gungnir supprime le 50/50.",
              PowerType.ESPER, 60, 25);
        setCustomModelId(21);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();

        // Trouver l'ennemi le plus proche dans 20 blocs
        Player target = null;
        double bestDist = ENEMY_RADIUS;
        for (UHCPlayer u : ToaruUHC.getInstance().getGameManager().getPlayers().values()) {
            if (!u.isAlive()) continue;
            Player other = u.getBukkitPlayer();
            if (other == null || !other.isOnline() || other.equals(player)) continue;
            double dist = other.getLocation().distance(player.getLocation());
            if (dist < bestDist) { bestDist = dist; target = other; }
        }

        if (target == null) {
            player.sendMessage("§5✦ §cAucun ennemi dans 20 blocs !");
            return false;
        }

        consumeResources(uhcPlayer);

        World world = player.getWorld();
        boolean hasGungnir = hasGungnir(player);

        if (hasGungnir) {
            // Gungnir : TOUJOURS victoire, 20 dégâts
            applyGungnirHit(player, target, world);
        } else {
            // 50/50
            if (RANDOM.nextBoolean()) {
                applyVictory(player, target, world);
            } else {
                applyDefeat(player, world);
            }
        }

        return true;
    }

    private void applyVictory(Player othinus, Player target, World world) {
        target.damage(NORMAL_DAMAGE, othinus);

        // Effets spectaculaires sur la cible
        for (int i = 0; i < 24; i++) {
            double angle = (2 * Math.PI / 24) * i;
            double rx = Math.cos(angle) * 1.0;
            double rz = Math.sin(angle) * 1.0;
            world.spawnParticle(Particle.SCULK_SOUL,  target.getLocation().add(rx, 0.5, rz), 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticle(Particle.CRIT_MAGIC,  target.getLocation().add(rx, 0.5, rz), 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticle(Particle.DRAGON_BREATH, target.getLocation().add(rx * 0.5, 1.0 + i * 0.05, rz * 0.5), 1, 0.0, 0.0, 0.0, 0.0);
        }

        world.playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH,        0.6f, 0.8f);
        world.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL,  0.5f, 1.5f);

        othinus.sendMessage("§5✦ §bHalf-a-God §5— §aVictoire ! §c" + target.getName()
                + " §7reçoit §c" + (int) NORMAL_DAMAGE + " §7dégâts !");
        othinus.sendTitle("§5✦ OTHINUS", "§aDestin favorable !", 5, 40, 10);
        target.sendMessage("§5✦ §cOthinus : Le destin t'a trahi — §c" + (int) NORMAL_DAMAGE + " dégâts !");
        target.sendTitle("§5✦ OTHINUS", "§cLe destin t'a trahi...", 5, 60, 15);
    }

    private void applyDefeat(Player othinus, World world) {
        othinus.damage(NORMAL_DAMAGE);

        // Effets sur Othinus
        world.spawnParticle(Particle.SCULK_SOUL, othinus.getLocation().add(0, 1, 0), 20, 0.5, 0.7, 0.5, 0.05);
        world.spawnParticle(Particle.CRIT,       othinus.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.05);
        world.playSound(othinus.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.8f);

        othinus.sendMessage("§5✦ §bHalf-a-God §5— §cDéfaite ! §7Le destin se retourne contre toi — §c"
                + (int) NORMAL_DAMAGE + " dégâts !");
        othinus.sendTitle("§c✖ RETOUR DE SORT", "§7Le destin t'a puni...", 5, 60, 15);
    }

    private void applyGungnirHit(Player othinus, Player target, World world) {
        target.damage(GUNGNIR_DAMAGE, othinus);

        // Effets encore plus dramatiques
        for (int i = 0; i < 32; i++) {
            double angle = (2 * Math.PI / 32) * i;
            double rx = Math.cos(angle) * 1.5;
            double rz = Math.sin(angle) * 1.5;
            world.spawnParticle(Particle.DRAGON_BREATH, target.getLocation().add(rx, 0.5, rz), 2, 0.0, 0.0, 0.0, 0.0);
        }
        world.createExplosion(target.getLocation().clone().add(0, 0.5, 0), 0f, false, false);
        world.spawnParticle(Particle.EXPLOSION_LARGE, target.getLocation().add(0, 1, 0), 8, 0.4, 0.5, 0.4, 0.0);
        world.spawnParticle(Particle.SCULK_SOUL,      target.getLocation().add(0, 1, 0), 30, 0.6, 0.8, 0.6, 0.06);

        world.playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH,       0.8f, 0.6f);
        world.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.0f);
        world.playSound(othinus.getLocation(), Sound.ITEM_TOTEM_USE,           0.8f, 0.7f);

        othinus.sendMessage("§5✦ §bGungnir §5— §aLance des Dieux ! §c" + target.getName()
                + " §7reçoit §c" + (int) GUNGNIR_DAMAGE + " §7dégâts — Aucun 50/50 !");
        othinus.sendTitle("§5✦ GUNGNIR", "§7La lance des dieux frappe !", 5, 50, 15);
        target.sendMessage("§5✦ §cGungnir d'§bOthinus §c— §c" + (int) GUNGNIR_DAMAGE + " dégâts !");
        target.sendTitle("§5✦ GUNGNIR", "§cLa lance des dieux frappe !", 5, 60, 15);
    }

    // ─── Gungnir item ─────────────────────────────────────────────────────────

    public static ItemStack createGungnir() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(GUNGNIR_NAME);
        meta.setLore(Arrays.asList(GUNGNIR_LORE, "§8Ne peut pas être jeté"));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.DAMAGE_ALL,  5, true); // Sharpness V
        meta.addEnchant(Enchantment.KNOCKBACK,   2, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        sword.setItemMeta(meta);
        return sword;
    }

    public static boolean hasGungnir(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isGungnir(item)) return true;
        }
        return false;
    }

    public static boolean isGungnir(ItemStack item) {
        return item != null
                && item.getType() == Material.NETHERITE_SWORD
                && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().equals(GUNGNIR_NAME);
    }

    /**
     * Fait apparaître le coffre contenant Gungnir entre 200 et 500 blocs du spawn.
     */
    public static void spawnGungnirChest(World world) {
        Random random = new Random();
        double angle    = random.nextDouble() * 2 * Math.PI;
        int    distance = 200 + random.nextInt(300);
        int    x        = (int) (Math.cos(angle) * distance);
        int    z        = (int) (Math.sin(angle) * distance);
        int    y        = world.getHighestBlockYAt(x, z) + 1;

        Location chestLoc = new Location(world, x + 0.5, y, z + 0.5);
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.CHEST);

        if (block.getState() instanceof org.bukkit.block.Chest chest) {
            chest.getInventory().setItem(13, createGungnir());
            chest.update();
        }

        Bukkit.broadcastMessage("§5✦ §7Gungnir est apparu quelque part dans le monde... (≤500 blocs du spawn)");
        Bukkit.broadcastMessage("§5✦ Coordonnées: §f" + x + ", " + y + ", " + z);
    }
}
