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
