package fr.medsir.toaruhc.managers;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.listeners.PowerListener;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.esper.*;
import fr.medsir.toaruhc.powers.magician.*;
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
            "L'une des rares Saintes au monde.",
            Role.RoleType.MAGICIAN, new SaintPower(),
            "1/7 000 000 000 — je suis une Sainte."));

        // ── Nouveaux personnages ─────────────────────────────────────────────

        availableRoles.add(new Role("accelerator", "Accelerator",
            "§f☣ Accelerator §8(Level 5 N°1)",
            "Le plus puissant Esper d'Academy City. Réfléchit tout vecteur.",
            Role.RoleType.ESPER, new AcceleratorPower(),
            "Je n'ai besoin d'aucune raison pour tuer. J'ai juste besoin d'une raison pour ne pas le faire."));

        availableRoles.add(new Role("mugino", "Mugino Shizuri",
            "§c🔴 Mugino Shizuri §8(Level 5 N°4)",
            "Meltdowner — destructrice de plasma.",
            Role.RoleType.ESPER, new MeltdownerPower(),
            "N°4, c'est tout ce que j'aurai jamais. Et ça suffit pour te détruire."));

        availableRoles.add(new Role("gunha", "Sogiita Gunha",
            "§6💥 Sogiita Gunha §8(Level 5 N°7)",
            "Guts indomptable — l'explosion de l'esprit.",
            Role.RoleType.ESPER, new GutsPower(),
            "Il y a des choses qui ne peuvent être défaites, et des choses qui ne peuvent être perdues !"));

        availableRoles.add(new Role("stiyl", "Stiyl Magnus",
            "§c🔥 Stiyl Magnus §8(Magicien)",
            "Invocateur de runes de feu. Maître d'Innocentius.",
            Role.RoleType.MAGICIAN, new FlameRunePower(),
            "Innocentius — le démon de feu ne peut être tué que par les flammes elles-mêmes."));

        availableRoles.add(new Role("tsuchimikado", "Tsuchimikado Motoharu",
            "§8🌑 Tsuchimikado Motoharu §8(Magicien)",
            "Onmyoudou — la magie au prix du sang.",
            Role.RoleType.MAGICIAN, new OnmyoudouPower(),
            "L'Onmyoudou me tue à chaque fois. Mais je recommencerai quand même."));

        availableRoles.add(new Role("index", "Index",
            "§e📖 Index §8(103 000 Grimoires)",
            "La bibliothèque vivante — 103 000 grimoires mémorisés.",
            Role.RoleType.MAGICIAN, new GrimoirePower(),
            "Je n'ai pas de super pouvoirs, mais j'ai 103 000 livres de magie !"));

        availableRoles.add(new Role("kakine", "Kakine Teitoku",
            "§8⬛ Kakine Teitoku §8(Level 5 N°2)",
            "Dark Matter — matière noire qui écrase tout.",
            Role.RoleType.ESPER, new DarkMatterPower(),
            "N°2 ? Ces chiffres ne signifient rien — ma Dark Matter détruit tout."));

        availableRoles.add(new Role("takitsubo", "Takitsubo Rikou",
            "§5🎯 Takitsubo Rikou §8(AIM Stalker)",
            "Traque et vole l'AIM field des Espers.",
            Role.RoleType.ESPER, new AIMStalkerPower(),
            "Je peux sentir ton AIM field... et te l'arracher."));

        availableRoles.add(new Role("fiamma", "Fiamma of the Right",
            "§e✝ Fiamma of the Right §8(La Droite Sacrée)",
            "La droite qui dépasse Dieu — pouvoir ultime.",
            Role.RoleType.MAGICIAN, new HolyRightPower(),
            "Ma droite transcende le divin. Rien ne peut l'arrêter."));

        availableRoles.add(new Role("terra", "Terra of the Left",
            "§6⚖ Terra of the Left §8(Magicien)",
            "Precedence — impose sa priorité sur l'ennemi.",
            Role.RoleType.MAGICIAN, new PrecedencePower(),
            "Le Precedence signifie que ma magie prime sur la tienne. Toujours."));

        availableRoles.add(new Role("sherry", "Sherry Cromwell",
            "§8🗿 Sherry Cromwell §8(Magicienne)",
            "Invocatrice d'Ellis, le Golem.",
            Role.RoleType.MAGICIAN, new SherryCromwellPower(),
            "Ellis, deviens leur terreur."));

        availableRoles.add(new Role("carissa", "Carissa",
            "§6⚔ Carissa §8(Curtana Original)",
            "La princesse qui tranche la réalité.",
            Role.RoleType.MAGICIAN, new CarissaPower(),
            "La lame de la princesse coupe même les anges."));

        availableRoles.add(new Role("thor", "Thor",
            "§e⚡ Thor §8(Gremlin)",
            "Le dieu du tonnerre de Gremlin.",
            Role.RoleType.MAGICIAN, new ThorPower(),
            "Je ne suis pas le Thor du mythe — je suis bien pire."));

        availableRoles.add(new Role("kamisato", "Kamisato Kakeru",
            "§b✋ Kamisato Kakeru §8(World Rejector)",
            "Sa main droite rejette ce qui n'appartient pas au monde.",
            Role.RoleType.ESPER, new KamisatoPower(),
            "World Rejector — toi, tu n'appartiens pas à ce monde."));

        availableRoles.add(new Role("hamazura", "Hamazura Shiage",
            "§7🔧 Hamazura Shiage §8(Level 0 / ITEM)",
            "Un Level 0 avec un exosquelette militaire.",
            Role.RoleType.ESPER, new HamazuraPower(),
            "Je suis Level 0. Et je vais te battre quand même."));

        availableRoles.add(new Role("acqua", "Acqua of the Back",
            "§9💧 Acqua of the Back §8(Saint / Droite de Dieu)",
            "Un des plus puissants Saints. Eau sainte et force brute.",
            Role.RoleType.MAGICIAN, new AcquaPower(),
            "Ma lance est une requête. Ma puissance est une prière."));

        availableRoles.add(new Role("othinus", "Othinus",
            "§5✦ Othinus §8(Magic God)",
            "La Déesse de la Magie. 50/50 — trouvez Gungnir.",
            Role.RoleType.MAGICIAN, new OthinusPower(),
            "Je peux recréer le monde. Ça ne me rend pas heureuse."));

        availableRoles.add(new Role("kinuhata", "Kinuhata Saiai",
            "§b🛡 Kinuhata Saiai §8(Level 4 - Offense Armor)",
            "Armor d'azote liquide. Absorbe tout.",
            Role.RoleType.ESPER, new KinuhataPower(),
            "Mon Offense Armor ? Il super-absorbe même tes gros pouvoirs."));

        availableRoles.add(new Role("orsola", "Orsola Aquinas",
            "§a📖 Orsola Aquinas §8(Magicienne)",
            "Déchiffre tous les pouvoirs. Révèle les secrets.",
            Role.RoleType.MAGICIAN, new OrsolaPower(),
            "Le Book of Law n'a aucun secret pour moi."));

        availableRoles.add(new Role("frenda", "Frenda Seivelun",
            "§e💣 Frenda Seivelun §8(ITEM n°4)",
            "Mines invisibles. Clic=poser, Sneak+clic=détoner.",
            Role.RoleType.ESPER, new FrendaPower(),
            "Tu peux pas voir mes mines ? C'est fait exprès."));

        plugin.getLogger().info("[RoleManager] " + availableRoles.size() + " rôles enregistrés.");
    }

    public void distributeRoles(List<UHCPlayer> players, String roleName1, String roleName2){
        if(players.size() > 1){
            assignRole(players.get(0), getRole(roleName1));
            assignRole(players.get(1), getRole(roleName2));
        }
    }

    public void distributeRoles(List<UHCPlayer> players, String roleName){
        //Pour le test : donne un rôle précis a tous les joueurs
        if(roleName == null){
            distributeRoles(players);
            return;
        }
        for(UHCPlayer p : players) assignRole(p, getRole(roleName));
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

        //Mise a jour de la meta de l'objet
        meta.setCustomModelData(role.getModelId());

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
        player.getInventory().setItem(8, item);
        player.getInventory().setHeldItemSlot(8);
    }

    public List<Role> getAvailableRoles() { return Collections.unmodifiableList(availableRoles); }

    public List<String> getRoleNames(){
        List<String> l = new ArrayList<>();
        for(Role r: getAvailableRoles()){
            l.add(r.getId());
        }
        return Collections.unmodifiableList(l);
    }

    public Role getRole(String id){
        for(Role r: getAvailableRoles()){
            if (r.getId().equals(id)) return r;
        }
        return null;
    }
}
