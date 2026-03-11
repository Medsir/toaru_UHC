package fr.medsir.toaruhc.managers;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.listeners.PowerListener;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.esper.*;
import fr.medsir.toaruhc.powers.magician.SaintPower;
import fr.medsir.toaruhc.roles.Role;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RoleManager {

    private final ToaruUHC plugin;
    private final List<Role> availableRoles = new ArrayList<>();

    public RoleManager(ToaruUHC plugin) {
        this.plugin = plugin;
        registerRoles();
    }

    private void registerRoles() {
        availableRoles.add(new Role("misaka", "Misaka Mikoto",
            "§e⚡ Misaka Mikoto §8(Level 5)",
            "La Railgun d'Academy City.",
            Role.RoleType.ESPER, new RailgunPower(),
            "Je ne cours pas après les garçons qui tombent du ciel."));

        availableRoles.add(new Role("touma", "Kamijou Touma",
            "§f🖐 Kamijou Touma §8(Level 0)",
            "La main droite qui brise toute illusion.",
            Role.RoleType.ESPER, new ImagineBreaker(),
            "Je briserai cette illusion de mes mains !"));

        availableRoles.add(new Role("kuroko", "Shirai Kuroko",
            "§d🌀 Shirai Kuroko §8(Level 4)",
            "Téléportatrice de Judgement.",
            Role.RoleType.ESPER, new TeleportPower(),
            "Jugement vous arrête là !"));

        availableRoles.add(new Role("kanzaki", "Kanzaki Kaori",
            "§6⚔ Kanzaki Kaori §8(Saint)",
            "L'une des rares Saintes.",
            Role.RoleType.MAGICIAN, new SaintPower(),
            "1/7 000 000 000 — je suis une Sainte."));

        plugin.getLogger().info("[RoleManager] " + availableRoles.size() + " rôles enregistrés.");
    }

    public void distributeRoles(List<UHCPlayer> players) {
        List<Role> pool = new ArrayList<>(availableRoles);
        while (pool.size() < players.size()) pool.addAll(availableRoles);
        Collections.shuffle(pool);
        for (int i = 0; i < players.size(); i++) assignRole(players.get(i), pool.get(i));
    }

    public void assignRole(UHCPlayer uhcPlayer, Role role) {
        uhcPlayer.setRole(role);
        uhcPlayer.setPower(role.getPower());

        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null || !player.isOnline()) return;

        // Afficher les infos du rôle
        for (String line : role.getFullDescription()) player.sendMessage(line);
        player.sendTitle(role.getDisplayName(), "§7Clic droit avec §6✦ §7pour activer", 10, 80, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);

        // Donner l'item de pouvoir au slot 0
        givePowerItem(player, role);
    }

    /**
     * Crée et donne l'item de pouvoir (Blaze Rod renommé) au joueur dans le slot 0.
     * Impossible à retirer grâce à PowerListener.
     */
    private void givePowerItem(Player player, Role role) {
        ItemStack item = new ItemStack(PowerListener.POWER_ITEM);
        ItemMeta meta  = item.getItemMeta();

        meta.setDisplayName("§6✦ Pouvoir §8— " + role.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("§7" + role.getPower().getName());
        lore.add("§7" + role.getPower().getDescription());
        lore.add("");
        lore.add("§eClic droit §7pour activer");
        lore.add("§7Coût : §e" + role.getPower().getAimOrManaCost()
            + (role.getType() == Role.RoleType.ESPER ? " AIM" : " Mana"));
        lore.add("§7Recharge : §e" + role.getPower().getCooldownSeconds() + "s");

        meta.setLore(lore);
        // Rendre l'item indestructible
        meta.setUnbreakable(true);
        item.setItemMeta(meta);

        // Forcer le slot 0
        player.getInventory().setItem(0, item);
        player.getInventory().setHeldItemSlot(0);
    }

    public List<Role> getAvailableRoles() { return Collections.unmodifiableList(availableRoles); }
}
