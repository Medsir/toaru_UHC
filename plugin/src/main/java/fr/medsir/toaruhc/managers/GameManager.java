package fr.medsir.toaruhc.managers;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.core.GameState;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.esper.OthinusPower;
import fr.medsir.toaruhc.roles.Role;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class GameManager {
    private final ToaruUHC plugin;
    private GameState state = GameState.WAITING;
    private final Map<UUID, UHCPlayer> players = new HashMap<>();
    private BukkitTask countdownTask, pvpTask, borderTask;
    private final int miningDuration, borderSize, borderFinalSize, borderShrinkDuration, minPlayers, maxAim, maxMana;
    private boolean testing = false;
    private long phaseEndTime = -1;

    public GameManager(ToaruUHC plugin) {
        this.plugin = plugin;
        this.miningDuration       = plugin.getConfig().getInt("game.mining-phase-duration", 20);
        this.borderSize           = plugin.getConfig().getInt("game.border-size", 4000);
        this.borderFinalSize      = plugin.getConfig().getInt("game.border-final-size", 50);
        this.borderShrinkDuration = plugin.getConfig().getInt("game.border-shrink-duration", 30);
        this.minPlayers           = plugin.getConfig().getInt("game.min-players", 1);
        this.maxAim               = plugin.getConfig().getInt("powers.aim.max", 100);
        this.maxMana              = plugin.getConfig().getInt("powers.mana.max", 100);
    }


    public void startGame() {
        startGame(false);
    }


    public void startGame(boolean testing) {
        startGame(testing, null);
    }

    public void startGame(boolean testing, String roleName1){
        startGame(testing, roleName1, null);
    }
    public void startGame(boolean testing, String roleName, String roleName2) {
        if (state != GameState.WAITING) return;
        setPvP(false); // Désactiver le PvP dès le lancement
        this.testing = testing;
        for (Player p : Bukkit.getOnlinePlayers())
            players.put(p.getUniqueId(), new UHCPlayer(p.getUniqueId(), maxAim, maxMana));
        if (players.size() < 1 && !testing) {
            broadcastPrefix("§cPas assez de joueurs ! (" + players.size() + "/" + minPlayers + ")");
            players.clear(); return;
        }
        state = GameState.STARTING;
        // Faire apparaître le coffre Gungnir pour Othinus
        OthinusPower.spawnGungnirChest(Bukkit.getWorlds().get(0));
        broadcastPrefix("§aPartie dans §e10s §a! §7(" + players.size() + " joueurs)"
                + (testing ? " §8[TEST]" : ""));
        if (roleName2 != null) {plugin.getRoleManager().distributeRoles(new ArrayList<>(players.values()), roleName, roleName2);}
        else{plugin.getRoleManager().distributeRoles(new ArrayList<>(players.values()), roleName);}

        players.values().forEach(plugin.getPowerManager()::createEnergyBar);
        for (World w : Bukkit.getWorlds()) w.setGameRule(GameRule.NATURAL_REGENERATION, false);
        int effectiveMining = testing ? 0 : miningDuration;
        startCountdown(10, () -> {
            setupBorder();
            setPvP(false);
            // Téléportation aléatoire + freeze 5s, puis démarrage du minage
            teleportAndFreeze(() -> {
                state = GameState.MINING;
                phaseEndTime = System.currentTimeMillis() + effectiveMining * 60L * 1000L;
                plugin.getPowerManager().startRegen(players);
                broadcastTitle("§a§lLA PARTIE COMMENCE", "§7PvP dans " + effectiveMining + " min",
                        10, 80, 20);
                pvpTask = plugin.getServer().getScheduler()
                        .runTaskLater(plugin, this::enablePvP, 20L * 60 * effectiveMining);
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SPAWN : téléportation aléatoire + freeze 5s
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 1. Génère des positions de spawn espacées pour chaque joueur.
     * 2. Téléporte chaque joueur sur son spawn.
     * 3. Immobilise les joueurs (walkSpeed = 0) pendant 5s avec un countdown.
     * 4. Libère les joueurs et appelle onDone.
     */
    private void teleportAndFreeze(Runnable onDone) {
        World world = Bukkit.getWorlds().get(0);
        List<Player> alivePlayers = new ArrayList<>();
        for (UHCPlayer u : players.values()) {
            Player p = u.getBukkitPlayer();
            if (p != null && p.isOnline()) alivePlayers.add(p);
        }

        List<Location> spawns = generateSpawnPoints(world, alivePlayers.size());

        // Téléportation + freeze
        for (int i = 0; i < alivePlayers.size(); i++) {
            Player p = alivePlayers.get(i);
            p.teleport(spawns.get(i));
            p.setWalkSpeed(0f);
        }

        broadcastTitle("§6⚡ §eAcademy City", "§7Prépare-toi...", 5, 90, 5);

        // Countdown 5s
        new BukkitRunnable() {
            int count = 5;

            @Override
            public void run() {
                if (count <= 0) {
                    cancel();
                    // Libérer les joueurs
                    for (UHCPlayer u : players.values()) {
                        Player p = u.getBukkitPlayer();
                        if (p != null && p.isOnline()) p.setWalkSpeed(0.2f);
                    }
                    broadcastTitle("§a§l GO !", "§fBonne chance, Academy City !", 5, 40, 10);
                    for (Player p : Bukkit.getOnlinePlayers())
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.5f);
                    // 10 secondes d'invincibilité au lancement
                    for (UHCPlayer u : players.values()) {
                        Player p = u.getBukkitPlayer();
                        if (p != null && p.isOnline()) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 255, false, false));
                            p.sendMessage("§a🛡 §7Invincibilité §e10s §7active au lancement !");
                        }
                    }
                    onDone.run();
                    return;
                }
                // Affichage du compte à rebours (stay=22 ≈ 1.1s pour couvrir le prochain tick)
                String col = count <= 2 ? "§c" : "§e";
                broadcastTitle(col + "§l" + count, "§7Reste en place !", 0, 22, 0);
                for (Player p : Bukkit.getOnlinePlayers())
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f,
                            count == 1 ? 2.0f : 1.2f);
                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Génère {@code count} positions de spawn espacées dans la bordure.
     * Distance minimale entre deux spawns : max(40, borderHalf × 1.5 / √count).
     */
    private List<Location> generateSpawnPoints(World world, int count) {
        int half    = borderSize / 2 - 50; // marge 50 blocs par rapport à la bordure
        int minDist = Math.max(40, (int)(half * 1.5 / Math.max(1, Math.sqrt(count))));
        List<Location> result = new ArrayList<>();
        Random         rng    = new Random();
        int            maxTry = count * 150;

        while (result.size() < count && maxTry-- > 0) {
            double x = (rng.nextDouble() * 2 - 1) * half;
            double z = (rng.nextDouble() * 2 - 1) * half;

            boolean tooClose = false;
            for (Location existing : result) {
                if (Math.hypot(x - existing.getX(), z - existing.getZ()) < minDist) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) result.add(findSafeGround(world, x, z));
        }

        // Fallback : complète sans contrainte de distance si les essais sont épuisés
        while (result.size() < count) {
            double x = (rng.nextDouble() * 2 - 1) * half;
            double z = (rng.nextDouble() * 2 - 1) * half;
            result.add(findSafeGround(world, x, z));
        }

        return result;
    }

    /** Retourne le premier bloc solide au-dessus du sol à (x, z). */
    private Location findSafeGround(World world, double x, double z) {
        int y = world.getHighestBlockYAt((int) x, (int) z);
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void enablePvP() {
        state = GameState.PVP; setPvP(true);
        phaseEndTime = System.currentTimeMillis() + 5L * 60 * 1000;
        broadcastTitle("§c§l⚔ PvP ACTIVÉ", "§7Que le meilleur survive !", 10, 80, 20);
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        borderTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::startBorderShrink, 20L * 60 * 5);
    }

    private void startBorderShrink() {
        state = GameState.ENDGAME;
        phaseEndTime = System.currentTimeMillis() + borderShrinkDuration * 60L * 1000L;
        broadcastPrefix("§6⚠ La bordure rétrécit !");
        for (World w : Bukkit.getWorlds()) w.getWorldBorder().setSize(borderFinalSize, 60L * borderShrinkDuration);
    }

    public void handlePlayerDeath(Player victim, Player killer) {
        UHCPlayer uhcVictim = players.get(victim.getUniqueId());
        if (uhcVictim == null || !uhcVictim.isAlive()) return;
        uhcVictim.setAlive(false);
        if (uhcVictim.getPower() != null) uhcVictim.getPower().deactivate(uhcVictim);
        plugin.getPowerManager().removeEnergyBar(victim.getUniqueId());
        String killerName = (killer != null) ? killer.getName() : "l'environnement";
        broadcastPrefix("§c☠ " + victim.getName() + " §7éliminé par §c" + killerName + " §8(" + uhcVictim.getKills() + " kills)");
        if (killer != null) { UHCPlayer k = players.get(killer.getUniqueId()); if (k != null) k.addKill(); }
        checkWinCondition();
    }

    /**
     * Gère la déconnexion d'un joueur en cours de partie :
     * le retire silencieusement (pas de message de mort, pas de kill crédité).
     */
    public void handlePlayerQuit(Player player) {
        UHCPlayer u = players.get(player.getUniqueId());
        if (u == null || !u.isAlive()) return;
        u.setAlive(false);
        if (u.getPower() != null) u.getPower().deactivate(u);
        plugin.getPowerManager().removeEnergyBar(player.getUniqueId());
        broadcastPrefix("§7☁ " + player.getName() + " §7a quitté la partie.");
        checkWinCondition();
    }

    private void checkWinCondition() {
        List<UHCPlayer> alive = players.values().stream().filter(UHCPlayer::isAlive).toList();
        if (alive.size() <= 1) endGame(alive.isEmpty() ? null : alive.get(0).getBukkitPlayer());
    }

    public void endGame(Player winner) {
        state = GameState.FINISHED; cancelTasks(); plugin.getPowerManager().cleanup();
        if (winner != null) {
            broadcastTitle("§e§l✦ " + winner.getName() + " GAGNE ✦", "§7" + players.get(winner.getUniqueId()).getKills() + " kills", 10, 120, 20);
            for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, this::reset, 20L * 30);
    }

    public void stopGame() {
        broadcastPrefix("§cPartie arrêtée.");
        cancelTasks();
        plugin.getPowerManager().cleanup();
        // Remettre la vitesse au cas où le freeze était actif
        for (Player p : Bukkit.getOnlinePlayers()) p.setWalkSpeed(0.2f);
        reset();
    }

    private void reset() {
        players.clear(); state = GameState.WAITING;
        phaseEndTime = -1; testing = false;
        for (World w : Bukkit.getWorlds()) { w.setGameRule(GameRule.NATURAL_REGENERATION, true); w.getWorldBorder().setSize(borderSize); }
        setPvP(true);
    }

    private void startCountdown(int seconds, Runnable onFinish) {
        final int[] r = {seconds};
        countdownTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (r[0] <= 0) { countdownTask.cancel(); onFinish.run(); return; }
            if (r[0] <= 5 || r[0] == 10) broadcastPrefix("§eDémarrage dans §c" + r[0] + "§e secondes...");
            r[0]--;
        }, 0L, 20L);
    }

    private void setupBorder() {
        for (World w : Bukkit.getWorlds()) { WorldBorder wb = w.getWorldBorder(); wb.setCenter(0,0); wb.setSize(borderSize); wb.setDamageAmount(1.0); }
    }

    private void setPvP(boolean enabled) { for (World w : Bukkit.getWorlds()) w.setPVP(enabled); }
    private void cancelTasks() { if (countdownTask != null) countdownTask.cancel(); if (pvpTask != null) pvpTask.cancel(); if (borderTask != null) borderTask.cancel(); }

    public void broadcastPrefix(String msg) { Bukkit.broadcastMessage(plugin.getConfig().getString("messages.prefix", "§8[§bAcademy City§8] §r") + msg); }
    public void broadcastTitle(String t, String s, int i, int st, int o) { for (Player p : Bukkit.getOnlinePlayers()) p.sendTitle(t, s, i, st, o); }

    public long getRemainingPhaseSeconds() {
        if (phaseEndTime <= 0) return -1;
        return Math.max(0, (phaseEndTime - System.currentTimeMillis()) / 1000);
    }

    public GameState getState()              { return state; }
    public boolean isRunning()               { return state != GameState.WAITING && state != GameState.FINISHED; }
    public boolean isTesting()               { return testing; }
    public Map<UUID, UHCPlayer> getPlayers() { return Collections.unmodifiableMap(players); }
    public UHCPlayer getUHCPlayer(UUID uuid) { return players.get(uuid); }
    public UHCPlayer getUHCPlayer(Player p)  { return players.get(p.getUniqueId()); }
    public void setPvp(boolean state)                 { setPvP(state);}
}
