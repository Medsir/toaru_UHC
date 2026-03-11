package fr.medsir.toaruhc.managers;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.roles.Role;
import org.bukkit.Bukkit;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class PowerManager {
    private final ToaruUHC plugin;
    private final Map<UUID, BossBar> energyBars = new HashMap<>();
    private BukkitTask regenTask;
    private final int aimRegen, manaRegen;

    public PowerManager(ToaruUHC plugin) {
        this.plugin = plugin;
        this.aimRegen  = plugin.getConfig().getInt("powers.aim.regen-per-second", 2);
        this.manaRegen = plugin.getConfig().getInt("powers.mana.regen-per-second", 1);
    }

    public void startRegen(Map<UUID, UHCPlayer> players) {
        stopRegen();
        regenTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (UHCPlayer u : players.values()) {
                if (!u.isAlive()) continue;
                Player p = u.getBukkitPlayer();
                if (p == null || !p.isOnline()) continue;
                Role role = u.getRole();
                if (role == null) continue;
                if (role.getType() == Role.RoleType.ESPER) u.regenAim(aimRegen);
                else u.regenMana(manaRegen);
                updateEnergyBar(u);
            }
        }, 20L, 20L);
    }

    public void stopRegen() {
        if (regenTask != null && !regenTask.isCancelled()) regenTask.cancel();
    }

    public void createEnergyBar(UHCPlayer uhcPlayer) {
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null) return;
        Role role = uhcPlayer.getRole();
        boolean isEsper = (role != null && role.getType() == Role.RoleType.ESPER);
        BossBar bar = Bukkit.createBossBar(
            isEsper ? "§b⚡ AIM : 100/100" : "§5✦ Mana : 100/100",
            isEsper ? BarColor.BLUE : BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bar.setProgress(1.0);
        bar.addPlayer(player);
        energyBars.put(player.getUniqueId(), bar);
    }

    public void updateEnergyBar(UHCPlayer uhcPlayer) {
        BossBar bar = energyBars.get(uhcPlayer.getUuid());
        if (bar == null) return;
        Role role = uhcPlayer.getRole();
        boolean isEsper = (role != null && role.getType() == Role.RoleType.ESPER);
        int current = isEsper ? uhcPlayer.getAim()    : uhcPlayer.getMana();
        int max     = isEsper ? uhcPlayer.getMaxAim() : uhcPlayer.getMaxMana();
        double progress = (double) current / max;
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        bar.setTitle((isEsper ? "§b⚡ AIM : §f" : "§5✦ Mana : §f") + current + "§7/§f" + max);
        bar.setColor(progress < 0.25 ? BarColor.RED : (isEsper ? BarColor.BLUE : BarColor.PURPLE));
    }

    public void removeEnergyBar(UUID uuid) {
        BossBar bar = energyBars.remove(uuid);
        if (bar != null) bar.removeAll();
    }

    public void cleanup() {
        stopRegen();
        energyBars.values().forEach(BossBar::removeAll);
        energyBars.clear();
    }
}
