package me.toaruuhc;

import org.bukkit.plugin.java.JavaPlugin;

public class ToaruMain extends JavaPlugin {

    @Override
    public void onEnable() {
        // Ce message s'affichera dans la console de ton serveur Minecraft
        getLogger().info("Le systeme de la Cite Scolaire est en ligne !");
    }

    @Override
    public void onDisable() {
        getLogger().info("Arret du systeme Toaru. ");
    }
}