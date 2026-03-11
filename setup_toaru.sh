#!/bin/bash
# Script de setup ToaruUHC - colle ce script dans le terminal VSCode depuis la racine de ton repo

mkdir -p plugin/src/main/java/fr/medsir/toaruhc/{core,managers,models,roles,listeners,commands}
mkdir -p plugin/src/main/java/fr/medsir/toaruhc/powers/{esper,magician}
mkdir -p plugin/src/main/resources
mkdir -p .vscode

# ── plugin.yml ──────────────────────────────────────────────────────────────
cat > plugin/src/main/resources/plugin.yml << 'EOF'
name: ToaruUHC
version: '${project.version}'
main: fr.medsir.toaruhc.ToaruUHC
api-version: '1.20'
description: "UHC Plugin - Academy City | A Certain Magical Index x Railgun"
authors: [Medsir]
website: https://github.com/Medsir/toaru_UHC

commands:
  uhc:
    description: Commande principale du UHC
    usage: /uhc <start|stop|status|forcestart>
    permission: toaruhc.admin
  role:
    description: Voir son rôle/pouvoir
    usage: /role
  power:
    description: Activer son pouvoir
    usage: /power

permissions:
  toaruhc.admin:
    description: Accès aux commandes admin du UHC
    default: op
  toaruhc.play:
    description: Permission de jouer
    default: true
EOF

# ── config.yml ──────────────────────────────────────────────────────────────
cat > plugin/src/main/resources/config.yml << 'EOF'
game:
  mining-phase-duration: 20
  pvp-delay: 20
  border-size: 1000
  border-final-size: 50
  border-shrink-duration: 30
  min-players: 2
  natural-regen: false

powers:
  global-cooldown: 5
  aim:
    max: 100
    regen-per-second: 2
  mana:
    max: 100
    regen-per-second: 1

messages:
  prefix: "§8[§bAcademy City§8] §r"
  game-start: "§aLa partie commence !"
  pvp-enabled: "§cLe PvP est maintenant activé !"
  border-shrinking: "§6La bordure commence à rétrécir !"
  player-eliminated: "§c%player% §7a été éliminé ! §8(%kills% kills)"
  winner: "§e§l✦ %player% §r§egagne la partie ! ✦"
EOF

# ── pom.xml ─────────────────────────────────────────────────────────────────
cat > plugin/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>fr.medsir</groupId>
    <artifactId>ToaruUHC</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <repositories>
        <repository>
            <id>papermc</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
    </repositories>
    <dependencies>
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.20.1-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>
</project>
EOF

# ── .vscode/tasks.json ──────────────────────────────────────────────────────
cat > .vscode/tasks.json << 'EOF'
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Maven: Build Plugin",
      "type": "shell",
      "command": "cd plugin && mvn clean package",
      "group": { "kind": "build", "isDefault": true },
      "presentation": { "reveal": "always", "panel": "shared" }
    }
  ]
}
EOF

# ── ToaruUHC.java ───────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/ToaruUHC.java << 'EOF'
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
EOF

# ── GameState.java ──────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/core/GameState.java << 'EOF'
package fr.medsir.toaruhc.core;

public enum GameState {
    WAITING, STARTING, MINING, PVP, ENDGAME, FINISHED
}
EOF

# ── UHCPlayer.java ──────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/models/UHCPlayer.java << 'EOF'
package fr.medsir.toaruhc.models;

import fr.medsir.toaruhc.powers.Power;
import fr.medsir.toaruhc.roles.Role;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UHCPlayer {
    private final UUID uuid;
    private Role role;
    private Power power;
    private int aim, mana;
    private final int maxAim, maxMana;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private int kills = 0;
    private boolean alive = true;
    private boolean imagineBreaker = false;

    public UHCPlayer(UUID uuid, int maxAim, int maxMana) {
        this.uuid = uuid; this.maxAim = maxAim; this.maxMana = maxMana;
        this.aim = maxAim; this.mana = maxMana;
    }

    public Player getBukkitPlayer() {
        return fr.medsir.toaruhc.ToaruUHC.getInstance().getServer().getPlayer(uuid);
    }

    public boolean isOnCooldown(String powerId) {
        Long expiry = cooldowns.get(powerId);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) { cooldowns.remove(powerId); return false; }
        return true;
    }
    public int getRemainingCooldown(String powerId) {
        Long expiry = cooldowns.get(powerId);
        if (expiry == null) return 0;
        long r = expiry - System.currentTimeMillis();
        return r > 0 ? (int) Math.ceil(r / 1000.0) : 0;
    }
    public void setCooldown(String powerId, int seconds) {
        cooldowns.put(powerId, System.currentTimeMillis() + (seconds * 1000L));
    }

    public int getAim()  { return aim; }
    public int getMana() { return mana; }
    public int getMaxAim()  { return maxAim; }
    public int getMaxMana() { return maxMana; }
    public void setAim(int v)  { this.aim  = Math.max(0, Math.min(v, maxAim)); }
    public void setMana(int v) { this.mana = Math.max(0, Math.min(v, maxMana)); }
    public void regenAim(int v)  { setAim(aim + v); }
    public void regenMana(int v) { setMana(mana + v); }

    public UUID getUuid()   { return uuid; }
    public Role getRole()   { return role; }
    public Power getPower() { return power; }
    public int getKills()   { return kills; }
    public boolean isAlive(){ return alive; }
    public boolean hasImagineBreaker() { return imagineBreaker; }
    public void setRole(Role r)    { this.role  = r; }
    public void setPower(Power p)  { this.power = p; }
    public void setAlive(boolean v){ this.alive = v; }
    public void setImagineBreaker(boolean v) { this.imagineBreaker = v; }
    public void addKill() { this.kills++; }
}
EOF

# ── Role.java ────────────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/roles/Role.java << 'EOF'
package fr.medsir.toaruhc.roles;

import fr.medsir.toaruhc.powers.Power;

public class Role {
    private final String id, name, displayName, description, lore;
    private final RoleType type;
    private final Power power;

    public Role(String id, String name, String displayName, String description,
                RoleType type, Power power, String lore) {
        this.id = id; this.name = name; this.displayName = displayName;
        this.description = description; this.type = type;
        this.power = power; this.lore = lore;
    }

    public String[] getFullDescription() {
        return new String[]{
            "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "§r  " + displayName,
            "§7  " + description,
            "§r  Pouvoir : " + power.getName(),
            "§7  " + power.getDescription(),
            "§7  Type    : " + (type == RoleType.ESPER ? "§bEsper §7(AIM)" : "§5Magicien §7(Mana)"),
            "§7  Coût    : §e" + power.getAimOrManaCost() + (type == RoleType.ESPER ? " AIM" : " Mana"),
            "§7  Recharge: §e" + power.getCooldownSeconds() + "s",
            "§o  \"" + lore + "\"",
            "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        };
    }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public RoleType getType()      { return type; }
    public Power getPower()        { return power; }
    public String getLore()        { return lore; }

    public enum RoleType { ESPER, MAGICIAN }
}
EOF

# ── Power.java ───────────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/powers/Power.java << 'EOF'
package fr.medsir.toaruhc.powers;

import fr.medsir.toaruhc.models.UHCPlayer;
import org.bukkit.entity.Player;

public abstract class Power {
    private final String id, name, description;
    private final PowerType type;
    private final int aimOrManaCost, cooldownSeconds;

    public Power(String id, String name, String description,
                 PowerType type, int aimOrManaCost, int cooldownSeconds) {
        this.id = id; this.name = name; this.description = description;
        this.type = type; this.aimOrManaCost = aimOrManaCost;
        this.cooldownSeconds = cooldownSeconds;
    }

    public abstract boolean activate(UHCPlayer uhcPlayer);
    public void deactivate(UHCPlayer uhcPlayer) {}

    public boolean canUse(UHCPlayer uhcPlayer) {
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null || !player.isOnline()) return false;
        if (uhcPlayer.isOnCooldown(this.id)) {
            player.sendMessage("§cPouvoir en recharge ! (" + uhcPlayer.getRemainingCooldown(id) + "s)");
            return false;
        }
        int energy = (type == PowerType.ESPER) ? uhcPlayer.getAim() : uhcPlayer.getMana();
        if (energy < aimOrManaCost) {
            String e = (type == PowerType.ESPER) ? "AIM" : "Mana";
            player.sendMessage("§cPas assez de " + e + " ! (" + energy + "/" + aimOrManaCost + ")");
            return false;
        }
        return true;
    }

    public void consumeResources(UHCPlayer uhcPlayer) {
        if (type == PowerType.ESPER) uhcPlayer.setAim(uhcPlayer.getAim() - aimOrManaCost);
        else uhcPlayer.setMana(uhcPlayer.getMana() - aimOrManaCost);
        if (cooldownSeconds > 0) uhcPlayer.setCooldown(this.id, cooldownSeconds);
    }

    public String getId()           { return id; }
    public String getName()         { return name; }
    public String getDescription()  { return description; }
    public PowerType getType()      { return type; }
    public int getAimOrManaCost()   { return aimOrManaCost; }
    public int getCooldownSeconds() { return cooldownSeconds; }

    public enum PowerType { ESPER, MAGICIAN }
}
EOF

# ── RailgunPower.java ────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/powers/esper/RailgunPower.java << 'EOF'
package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

public class RailgunPower extends Power {
    public RailgunPower() {
        super("railgun", "§e⚡ Railgun §7(Misaka Mikoto)",
              "Lance une pièce à vitesse électromagnétique. Dégâts massifs.",
              PowerType.ESPER, 30, 12);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);
        Vector dir = player.getLocation().getDirection().normalize().multiply(6.0);
        Location start = player.getEyeLocation();
        fireRailgun(player, start, dir);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);
        player.sendMessage("§e⚡ §bRailgun §e— §fPièce propulsée !");
        return true;
    }

    private void fireRailgun(Player shooter, Location start, Vector direction) {
        Location current = start.clone();
        double dist = 0;
        while (dist < 80.0) {
            current.add(direction.clone().multiply(0.5));
            dist += direction.length() * 0.5;
            current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, current, 3, 0.05, 0.05, 0.05, 0);
            for (Entity e : current.getWorld().getNearbyEntities(current, 0.6, 0.6, 0.6)) {
                if (e instanceof Player target && e != shooter) {
                    target.damage(16.0, shooter);
                    target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0,1,0), 25, 0.3, 0.3, 0.3, 0.2);
                    shooter.sendMessage("§e⚡ §fTouche §c" + target.getName() + " §f!");
                    target.sendMessage("§cTouché par le §eRailgun §cde §b" + shooter.getName() + "§c!");
                    return;
                }
            }
            if (current.getBlock().getType().isSolid()) return;
        }
    }
}
EOF

# ── ImagineBreaker.java ──────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/powers/esper/ImagineBreaker.java << 'EOF'
package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import java.util.Collection;

public class ImagineBreaker extends Power {
    public ImagineBreaker() {
        super("imagine_breaker", "§f🖐 Imagine Breaker §7(Kamijou Touma)",
              "Annule tous les effets de potion ennemis au contact. Actif 3s.",
              PowerType.ESPER, 20, 8);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);
        uhcPlayer.setImagineBreaker(true);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0,1,0), 20, 0.3, 0.3, 0.3, 0);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        player.sendMessage("§f🖐 §bImagine Breaker §f— Actif pendant 3 secondes !");
        fr.medsir.toaruhc.ToaruUHC.getInstance().getServer().getScheduler()
            .runTaskLater(fr.medsir.toaruhc.ToaruUHC.getInstance(), () -> {
                if (uhcPlayer.hasImagineBreaker()) {
                    uhcPlayer.setImagineBreaker(false);
                    if (player.isOnline()) player.sendMessage("§7🖐 Imagine Breaker désactivé.");
                }
            }, 60L);
        return true;
    }

    public static void applyNullification(Player user, Player target) {
        Collection<PotionEffect> effects = target.getActivePotionEffects();
        if (effects.isEmpty()) { user.sendMessage("§7Aucun effet à annuler sur " + target.getName()); return; }
        for (PotionEffect e : effects) target.removePotionEffect(e.getType());
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0,1,0), 40, 0.5, 0.8, 0.5, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        user.sendMessage("§f🖐 §b" + effects.size() + " effet(s) de " + target.getName() + " annulé(s) !");
        target.sendMessage("§cTes effets ont été annulés par §f🖐 Imagine Breaker §cde §b" + user.getName() + "§c!");
    }
}
EOF

# ── TeleportPower.java ───────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/powers/esper/TeleportPower.java << 'EOF'
package fr.medsir.toaruhc.powers.esper;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class TeleportPower extends Power {
    public TeleportPower() {
        super("teleport", "§d🌀 Teleport §7(Shirai Kuroko)",
              "Téléportation instantanée jusqu'à 15 blocs devant soi.",
              PowerType.ESPER, 25, 6);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        Location origin = player.getLocation().clone();
        Vector dir = player.getLocation().getDirection().normalize();
        Location dest = calculateDestination(player, dir, origin);
        if (dest == null) { player.sendMessage("§cImpossible de se téléporter ici !"); return false; }
        consumeResources(uhcPlayer);
        origin.getWorld().spawnParticle(Particle.PORTAL, origin.add(0,1,0), 30, 0.3, 0.5, 0.3, 0.5);
        origin.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        player.teleport(dest);
        dest.getWorld().spawnParticle(Particle.PORTAL, dest.add(0,1,0), 30, 0.3, 0.5, 0.3, 0.5);
        dest.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.8f);
        player.sendMessage("§d🌀 §bTeleport §d— Téléportation effectuée !");
        return true;
    }

    private Location calculateDestination(Player player, Vector dir, Location origin) {
        Location current = origin.clone().add(0, 0.1, 0);
        double dist = 0;
        while (dist < 15.0) {
            current.add(dir.clone().multiply(0.5));
            dist += 0.5;
            if (current.getBlock().getType().isSolid()) { current.subtract(dir.clone().multiply(0.5)); break; }
        }
        current.setYaw(player.getLocation().getYaw());
        current.setPitch(player.getLocation().getPitch());
        return current;
    }
}
EOF

# ── SaintPower.java ──────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/powers/magician/SaintPower.java << 'EOF'
package fr.medsir.toaruhc.powers.magician;

import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;

public class SaintPower extends Power {
    public SaintPower() {
        super("saint_power", "§6⚔ Saint's Power §7(Kanzaki Kaori)",
              "Force II + Regen II + Résistance I pendant 8s. 1/7 000 000 000.",
              PowerType.MAGICIAN, 40, 20);
    }

    @Override
    public boolean activate(UHCPlayer uhcPlayer) {
        if (!canUse(uhcPlayer)) return false;
        Player player = uhcPlayer.getBukkitPlayer();
        consumeResources(uhcPlayer);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 160, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 160, 0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 0.8f);
        player.sendMessage("§6⚔ §bSaint's Power §6— La force d'un Saint t'envahit !");
        player.sendTitle("§6⚔ SAINT", "§71/7 000 000 000", 5, 50, 10);
        return true;
    }
}
EOF

# ── RoleManager.java ─────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/managers/RoleManager.java << 'EOF'
package fr.medsir.toaruhc.managers;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.esper.*;
import fr.medsir.toaruhc.powers.magician.SaintPower;
import fr.medsir.toaruhc.roles.Role;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.*;

public class RoleManager {
    private final ToaruUHC plugin;
    private final List<Role> availableRoles = new ArrayList<>();

    public RoleManager(ToaruUHC plugin) {
        this.plugin = plugin;
        registerRoles();
    }

    private void registerRoles() {
        availableRoles.add(new Role("misaka", "Misaka Mikoto", "§e⚡ Misaka Mikoto §8(Level 5)",
            "La Railgun d'Academy City.", Role.RoleType.ESPER, new RailgunPower(),
            "Je ne cours pas après les garçons qui tombent du ciel."));
        availableRoles.add(new Role("touma", "Kamijou Touma", "§f🖐 Kamijou Touma §8(Level 0)",
            "La main droite qui brise toute illusion.", Role.RoleType.ESPER, new ImagineBreaker(),
            "Je briserai cette illusion de mes mains !"));
        availableRoles.add(new Role("kuroko", "Shirai Kuroko", "§d🌀 Shirai Kuroko §8(Level 4)",
            "Téléportatrice de Judgement.", Role.RoleType.ESPER, new TeleportPower(),
            "Jugement vous arrête là !"));
        availableRoles.add(new Role("kanzaki", "Kanzaki Kaori", "§6⚔ Kanzaki Kaori §8(Saint)",
            "L'une des rares Saintes.", Role.RoleType.MAGICIAN, new SaintPower(),
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
        for (String line : role.getFullDescription()) player.sendMessage(line);
        player.sendTitle(role.getDisplayName(), "§7Clic droit pour activer ton pouvoir", 10, 80, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
    }

    public List<Role> getAvailableRoles() { return Collections.unmodifiableList(availableRoles); }
}
EOF

# ── PowerManager.java ────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/managers/PowerManager.java << 'EOF'
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
EOF

# ── GameManager.java ─────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/managers/GameManager.java << 'EOF'
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
EOF

# ── GameListener.java ────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/listeners/GameListener.java << 'EOF'
package fr.medsir.toaruhc.listeners;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.core.GameState;
import fr.medsir.toaruhc.models.UHCPlayer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class GameListener implements Listener {
    private final ToaruUHC plugin;
    public GameListener(ToaruUHC plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        UHCPlayer uhcVictim = plugin.getGameManager().getUHCPlayer(victim);
        if (uhcVictim != null && uhcVictim.getRole() != null)
            event.setDeathMessage("§8[§cÉliminé§8] §r" + uhcVictim.getRole().getDisplayName() + (killer != null ? " §7par §c" + killer.getName() : " §7par l'environnement"));
        plugin.getGameManager().handlePlayerDeath(victim, killer);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().isRunning()) {
            player.sendMessage("§cPartie en cours. Mode spectateur.");
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        Player player = event.getPlayer();
        UHCPlayer u = plugin.getGameManager().getUHCPlayer(player);
        if (u != null && u.isAlive()) plugin.getGameManager().handlePlayerDeath(player, null);
        plugin.getPowerManager().removeEnergyBar(player.getUniqueId());
    }
}
EOF

# ── PowerListener.java ───────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/listeners/PowerListener.java << 'EOF'
package fr.medsir.toaruhc.listeners;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import fr.medsir.toaruhc.powers.Power;
import fr.medsir.toaruhc.powers.esper.ImagineBreaker;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PowerListener implements Listener {
    private final ToaruUHC plugin;
    public PowerListener(ToaruUHC plugin) { this.plugin = plugin; }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType().isEdible()) return;
        UHCPlayer u = plugin.getGameManager().getUHCPlayer(player);
        if (u == null || !u.isAlive()) return;
        Power power = u.getPower();
        if (power == null) return;
        if (power.activate(u)) plugin.getPowerManager().updateEnergyBar(u);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getGameManager().isRunning()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        UHCPlayer uA = plugin.getGameManager().getUHCPlayer(attacker);
        UHCPlayer uV = plugin.getGameManager().getUHCPlayer(victim);
        if (uA == null || uV == null) return;
        if (uA.hasImagineBreaker()) { uA.setImagineBreaker(false); ImagineBreaker.applyNullification(attacker, victim); }
        if (uV.hasImagineBreaker()) { uV.setImagineBreaker(false); ImagineBreaker.applyNullification(victim, attacker); }
    }
}
EOF

# ── PlayerListener.java ──────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/listeners/PlayerListener.java << 'EOF'
package fr.medsir.toaruhc.listeners;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.core.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class PlayerListener implements Listener {
    private final ToaruUHC plugin;
    public PlayerListener(ToaruUHC plugin) { this.plugin = plugin; }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        GameState s = plugin.getGameManager().getState();
        if (s == GameState.WAITING || s == GameState.STARTING) event.setCancelled(true);
    }
}
EOF

# ── UHCCommand.java ──────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/commands/UHCCommand.java << 'EOF'
package fr.medsir.toaruhc.commands;

import fr.medsir.toaruhc.ToaruUHC;
import org.bukkit.command.*;
import java.util.*;

public class UHCCommand implements CommandExecutor, TabCompleter {
    private final ToaruUHC plugin;
    public UHCCommand(ToaruUHC plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§bAcademy City§8] §r");
        if (!sender.hasPermission("toaruhc.admin")) { sender.sendMessage(prefix + "§cPas la permission."); return true; }
        if (args.length == 0) { sender.sendMessage(prefix + "§7/uhc <start|stop|status|forcestart>"); return true; }
        switch (args[0].toLowerCase()) {
            case "start" -> { if (plugin.getGameManager().isRunning()) { sender.sendMessage(prefix + "§cPartie déjà en cours !"); return true; } plugin.getGameManager().startGame(); }
            case "stop"  -> { if (!plugin.getGameManager().isRunning()) { sender.sendMessage(prefix + "§cAucune partie."); return true; } plugin.getGameManager().stopGame(); }
            case "status" -> { sender.sendMessage(prefix + "§7État : §e" + plugin.getGameManager().getState()); long alive = plugin.getGameManager().getPlayers().values().stream().filter(p->p.isAlive()).count(); sender.sendMessage(prefix + "§7Survivants : §a" + alive); }
            case "forcestart" -> plugin.getGameManager().startGame();
            default -> sender.sendMessage(prefix + "§7/uhc <start|stop|status|forcestart>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        return args.length == 1 ? Arrays.asList("start", "stop", "status", "forcestart") : List.of();
    }
}
EOF

# ── RoleCommand.java ─────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/commands/RoleCommand.java << 'EOF'
package fr.medsir.toaruhc.commands;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class RoleCommand implements CommandExecutor {
    private final ToaruUHC plugin;
    public RoleCommand(ToaruUHC plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Joueurs uniquement."); return true; }
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§bAcademy City§8] §r");
        if (!plugin.getGameManager().isRunning()) { player.sendMessage(prefix + "§cAucune partie en cours."); return true; }
        UHCPlayer u = plugin.getGameManager().getUHCPlayer(player);
        if (u == null || u.getRole() == null) { player.sendMessage(prefix + "§cPas encore de rôle."); return true; }
        for (String line : u.getRole().getFullDescription()) player.sendMessage(line);
        return true;
    }
}
EOF

# ── PowerCommand.java ────────────────────────────────────────────────────────
cat > plugin/src/main/java/fr/medsir/toaruhc/commands/PowerCommand.java << 'EOF'
package fr.medsir.toaruhc.commands;

import fr.medsir.toaruhc.ToaruUHC;
import fr.medsir.toaruhc.models.UHCPlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class PowerCommand implements CommandExecutor {
    private final ToaruUHC plugin;
    public PowerCommand(ToaruUHC plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Joueurs uniquement."); return true; }
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§bAcademy City§8] §r");
        if (!plugin.getGameManager().isRunning()) { player.sendMessage(prefix + "§cAucune partie."); return true; }
        UHCPlayer u = plugin.getGameManager().getUHCPlayer(player);
        if (u == null || !u.isAlive() || u.getPower() == null) { player.sendMessage(prefix + "§cPas de pouvoir."); return true; }
        if (u.getPower().activate(u)) plugin.getPowerManager().updateEnergyBar(u);
        return true;
    }
}
EOF

echo ""
echo "✅ Tous les fichiers ont été créés !"
echo "📦 Lance maintenant : cd plugin && mvn clean package"
