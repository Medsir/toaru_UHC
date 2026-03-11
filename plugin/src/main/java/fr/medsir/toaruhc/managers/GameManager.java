package fr.medsir.toaruhc.managers;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.core.GameState;
import fr.medsir.toaruhc.models.UHCPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class GameManager {
    private final ToaruUHC plugin;
    private GameState state = GameState.WAITING;
    private final Map<UUID, UHCPlayer> players = new HashMap<>();
    private BukkitTask countdownTask, pvpTask, borderTask;
    private final int miningDuration, borderSize, borderFinalSize, borderShrinkDuration, minPlayers, maxAim, maxMana;

    public GameManager(ToaruUHC plugin) {
        this.plugin = plugin;
        this.miningDuration       = plugin.getConfig().getInt("game.mining-phase-duration", 20);
        this.borderSize           = plugin.getConfig().getInt("game.border-size", 1000);
        this.borderFinalSize      = plugin.getConfig().getInt("game.border-final-size", 50);
        this.borderShrinkDuration = plugin.getConfig().getInt("game.border-shrink-duration", 30);
        this.minPlayers           = plugin.getConfig().getInt("game.min-players", 2);
        this.maxAim               = plugin.getConfig().getInt("powers.aim.max", 100);
        this.maxMana              = plugin.getConfig().getInt("powers.mana.max", 100);
    }

    public void startGame() {
        if (state != GameState.WAITING) return;
        for (Player p : Bukkit.getOnlinePlayers())
            players.put(p.getUniqueId(), new UHCPlayer(p.getUniqueId(), maxAim, maxMana));
        if (players.size() < minPlayers) {
            broadcastPrefix("§cPas assez de joueurs ! (" + players.size() + "/" + minPlayers + ")");
            players.clear(); return;
        }
        state = GameState.STARTING;
        broadcastPrefix("§aPartie dans §e10s §a! §7(" + players.size() + " joueurs)");
        plugin.getRoleManager().distributeRoles(new ArrayList<>(players.values()));
        players.values().forEach(plugin.getPowerManager()::createEnergyBar);
        for (World w : Bukkit.getWorlds()) w.setGameRule(GameRule.NATURAL_REGENERATION, false);
        startCountdown(10, () -> {
            state = GameState.MINING;
            setupBorder(); setPvP(false);
            plugin.getPowerManager().startRegen(players);
            broadcastTitle("§a§lLA PARTIE COMMENCE", "§7PvP dans " + miningDuration + " min", 10, 80, 20);
            pvpTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::enablePvP, 20L * 60 * miningDuration);
        });
    }

    private void enablePvP() {
        state = GameState.PVP; setPvP(true);
        broadcastTitle("§c§l⚔ PvP ACTIVÉ", "§7Que le meilleur survive !", 10, 80, 20);
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        borderTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::startBorderShrink, 20L * 60 * 5);
    }

    private void startBorderShrink() {
        state = GameState.ENDGAME;
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

    public void stopGame() { broadcastPrefix("§cPartie arrêtée."); cancelTasks(); plugin.getPowerManager().cleanup(); reset(); }

    private void reset() {
        players.clear(); state = GameState.WAITING;
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

    public GameState getState()              { return state; }
    public boolean isRunning()               { return state != GameState.WAITING && state != GameState.FINISHED; }
    public Map<UUID, UHCPlayer> getPlayers() { return Collections.unmodifiableMap(players); }
    public UHCPlayer getUHCPlayer(UUID uuid) { return players.get(uuid); }
    public UHCPlayer getUHCPlayer(Player p)  { return players.get(p.getUniqueId()); }
}
