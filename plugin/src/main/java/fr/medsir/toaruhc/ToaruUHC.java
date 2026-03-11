package fr.medsir.toaruhc;

import fr.medsir.toaruhc.commands.RoleCommand;
import fr.medsir.toaruhc.commands.UHCCommand;
import fr.medsir.toaruhc.commands.PowerCommand;
import fr.medsir.toaruhc.listeners.GameListener;
import fr.medsir.toaruhc.listeners.PowerListener;
import fr.medsir.toaruhc.listeners.PlayerListener;
import fr.medsir.toaruhc.managers.GameManager;
import fr.medsir.toaruhc.managers.RoleManager;
import fr.medsir.toaruhc.managers.PowerManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ToaruUHC extends JavaPlugin {
    private static ToaruUHC instance;
    private GameManager gameManager;
    private RoleManager roleManager;
    private PowerManager powerManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.roleManager  = new RoleManager(this);
        this.powerManager = new PowerManager(this);
        this.gameManager  = new GameManager(this);
        getCommand("uhc").setExecutor(new UHCCommand(this));
        getCommand("role").setExecutor(new RoleCommand(this));
        getCommand("power").setExecutor(new PowerCommand(this));
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new PowerListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getLogger().info("ToaruUHC activé ! Bienvenue dans Academy City.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isRunning()) gameManager.stopGame();
    }

    public static ToaruUHC getInstance() { return instance; }
    public GameManager getGameManager()  { return gameManager; }
    public RoleManager getRoleManager()  { return roleManager; }
    public PowerManager getPowerManager(){ return powerManager; }
}
