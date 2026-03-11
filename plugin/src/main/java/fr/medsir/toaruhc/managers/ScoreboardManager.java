package fr.medsir.toaruhc.managers;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.core.GameState;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.roles.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gère le scoreboard affiché à droite de l'écran pour chaque joueur.
 *
 * Format :
 * ┌─────────────────────┐
 * │  § ⚡ TOARU UHC §   │
 * │                     │
 * │  Phase : Minage     │
 * │  Survivants : 4/8   │
 * │                     │
 * │  Ton rôle :         │
 * │  Misaka Mikoto      │
 * │  Pouvoir : Railgun  │
 * │                     │
 * │  AIM : 70/100       │
 * │  Kills : 2          │
 * └─────────────────────┘
 */
public class ScoreboardManager {

    private final ToaruUHC plugin;
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();
    private BukkitTask updateTask;

    // Couleurs des phases
    private static final Map<GameState, String> PHASE_LABELS = new HashMap<>();
    static {
        PHASE_LABELS.put(GameState.WAITING,  "§7En attente...");
        PHASE_LABELS.put(GameState.STARTING, "§eDémarrage...");
        PHASE_LABELS.put(GameState.MINING,   "§a⛏ Minage");
        PHASE_LABELS.put(GameState.PVP,      "§c⚔ PvP");
        PHASE_LABELS.put(GameState.ENDGAME,  "§6🔥 Finale");
        PHASE_LABELS.put(GameState.FINISHED, "§dTerminée");
    }

    public ScoreboardManager(ToaruUHC plugin) {
        this.plugin = plugin;
    }

    /**
     * Démarre la mise à jour du scoreboard toutes les secondes.
     */
    public void start() {
        stop();
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 0L, 20L);
    }

    public void stop() {
        if (updateTask != null && !updateTask.isCancelled()) updateTask.cancel();
    }

    /**
     * Met à jour le scoreboard d'un joueur spécifique.
     */
    public void updateScoreboard(Player player) {
        Scoreboard board = scoreboards.computeIfAbsent(player.getUniqueId(),
            k -> Bukkit.getScoreboardManager().getNewScoreboard());

        // Recréer l'objectif à chaque update (plus simple pour changer les lignes)
        Objective obj = board.getObjective("toaruhc");
        if (obj != null) obj.unregister();

        obj = board.registerNewObjective("toaruhc", "dummy",
            "§e§l⚡ §6§lTOARU UHC §e§l⚡");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // ── Données ──────────────────────────────────────────────────────────
        GameState state      = plugin.getGameManager().getState();
        int totalPlayers     = plugin.getGameManager().getPlayers().size();
        long alivePlayers    = plugin.getGameManager().getPlayers().values()
                                    .stream().filter(UHCPlayer::isAlive).count();
        UHCPlayer uhcPlayer  = plugin.getGameManager().getUHCPlayer(player);

        // ── Construction des lignes (score décroissant = ordre affiché) ──────
        int line = 15;

        // Séparateur haut
        setLine(board, obj, "§f§1", line--);

        // Phase
        setLine(board, obj, "§7Phase :", line--);
        setLine(board, obj, PHASE_LABELS.getOrDefault(state, "§7?"), line--);

        // Séparateur
        setLine(board, obj, "§f§2", line--);

        // Joueurs
        String aliveColor = alivePlayers <= 3 ? "§c" : "§a";
        setLine(board, obj, "§7Survivants :", line--);
        setLine(board, obj, aliveColor + alivePlayers + "§7/§f" + totalPlayers, line--);

        // Séparateur
        setLine(board, obj, "§f§3", line--);

        // Rôle & Pouvoir du joueur
        if (uhcPlayer != null && uhcPlayer.getRole() != null) {
            Role role = uhcPlayer.getRole();
            boolean isEsper = role.getType() == Role.RoleType.ESPER;

            setLine(board, obj, "§7Ton rôle :", line--);
            // Nom du rôle tronqué si trop long
            String roleName = role.getDisplayName();
            if (ChatColor.stripColor(roleName).length() > 16)
                roleName = roleName.substring(0, 20) + "§r";
            setLine(board, obj, roleName, line--);

            // Énergie
            int current = isEsper ? uhcPlayer.getAim()    : uhcPlayer.getMana();
            int max     = isEsper ? uhcPlayer.getMaxAim() : uhcPlayer.getMaxMana();
            String energyLabel = isEsper ? "§b⚡ AIM" : "§5✦ Mana";
            String energyColor = current < max * 0.25 ? "§c" : (current < max * 0.5 ? "§e" : "§a");
            setLine(board, obj, energyLabel + " §7: " + energyColor + current + "§7/§f" + max, line--);

            // Kills
            setLine(board, obj, "§7Kills : §e" + uhcPlayer.getKills(), line--);

            // Cooldown du pouvoir
            if (uhcPlayer.getPower() != null) {
                String cdLabel;
                if (uhcPlayer.isOnCooldown(uhcPlayer.getPower().getId())) {
                    int cd = uhcPlayer.getRemainingCooldown(uhcPlayer.getPower().getId());
                    cdLabel = "§cRecharge : §f" + cd + "s";
                } else {
                    cdLabel = "§aPouvoir prêt !";
                }
                setLine(board, obj, cdLabel, line--);
            }

        } else {
            setLine(board, obj, "§7Rôle : §8Non assigné", line--);
        }

        // Séparateur bas
        setLine(board, obj, "§f§4", line--);

        // Site / branding
        setLine(board, obj, "§8academy-city.fr", line--);

        player.setScoreboard(board);
    }

    /**
     * Définit une ligne du scoreboard.
     * Chaque entrée doit être unique → on pad avec des espaces invisibles si besoin.
     */
    private void setLine(Scoreboard board, Objective obj, String text, int score) {
        // Limiter à 40 chars (limite Scoreboard)
        if (text.length() > 40) text = text.substring(0, 40);

        // Rendre l'entrée unique si elle existe déjà
        String entry = text;
        int attempt  = 0;
        while (board.getEntries().contains(entry) && attempt < 10) {
            entry = text + "§r".repeat(++attempt);
        }

        Score s = obj.getScore(entry);
        s.setScore(score);
    }

    /**
     * Supprime le scoreboard d'un joueur (déconnexion, fin de partie).
     */
    public void removeScoreboard(Player player) {
        scoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public void cleanup() {
        stop();
        for (Player player : Bukkit.getOnlinePlayers()) removeScoreboard(player);
        scoreboards.clear();
    }
}
