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
    private boolean imagineBreaker  = false;
    private boolean acceleratorMode = false;

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
    public boolean hasImagineBreaker()  { return imagineBreaker; }
    public boolean hasAcceleratorMode() { return acceleratorMode; }
    public void setRole(Role r)    { this.role  = r; }
    public void setPower(Power p)  { this.power = p; }
    public void setAlive(boolean v){ this.alive = v; }
    public void setImagineBreaker(boolean v)  { this.imagineBreaker  = v; }
    public void setAcceleratorMode(boolean v) { this.acceleratorMode = v; }
    public void addKill() { this.kills++; }
}
