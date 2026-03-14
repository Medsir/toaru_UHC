package fr.medsir.toaruhc.powers;

import fr.medsir.toaruhc.models.UHCPlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public abstract class Power {
    private final String id, name, description;
    private final PowerType type;
    private final int aimOrManaCost, cooldownSeconds;
    private int customModelId;

    public Power(String id, String name, String description,
                 PowerType type, int aimOrManaCost, int cooldownSeconds) {
        this.id = id; this.name = name; this.description = description;
        this.type = type; this.aimOrManaCost = aimOrManaCost;
        this.cooldownSeconds = cooldownSeconds;
        this.customModelId = 0;
    }

    public abstract boolean activate(UHCPlayer uhcPlayer);
    public void deactivate(UHCPlayer uhcPlayer) {}

    // ─── Ultimate ────────────────────────────────────────────────────────────────
    protected int ultimateCost            = 0;   // AIM ou Mana requis
    protected int ultimateCooldownSeconds = 300; // 5 minutes par défaut

    /**
     * Override this in each power to implement the ultimate ability.
     * Return true if the ultimate was successfully activated.
     */
    public boolean activateUltimate(UHCPlayer uhcPlayer) { return false; }

    /**
     * Checks cooldown + energy for ultimate use. Shows messages on failure.
     */
    public boolean canUseUltimate(UHCPlayer uhcPlayer) {
        Player player = uhcPlayer.getBukkitPlayer();
        if (player == null || !player.isOnline()) return false;
        if (uhcPlayer.isOnCooldown("ult_" + id)) {
            player.sendMessage("§c§l✦ ULTIMATE §ren recharge ! ("
                    + uhcPlayer.getRemainingCooldown("ult_" + id) + "s)");
            return false;
        }
        if (ultimateCost > 0) {
            int energy = (type == PowerType.ESPER) ? uhcPlayer.getAim() : uhcPlayer.getMana();
            if (energy < ultimateCost) {
                String e = (type == PowerType.ESPER) ? "AIM" : "Mana";
                player.sendMessage("§c§l✦ ULTIMATE §r§c — Pas assez de " + e
                        + " ! (" + energy + "/" + ultimateCost + ")");
                return false;
            }
        }
        return true;
    }

    /**
     * Deducts ultimate cost + sets ultimate cooldown.
     * Call this at the START of a successful activateUltimate().
     */
    public void consumeUltimateResources(UHCPlayer uhcPlayer) {
        if (ultimateCost > 0) {
            if (type == PowerType.ESPER) uhcPlayer.setAim(Math.max(0, uhcPlayer.getAim() - ultimateCost));
            else uhcPlayer.setMana(Math.max(0, uhcPlayer.getMana() - ultimateCost));
        }
        if (ultimateCooldownSeconds > 0)
            uhcPlayer.setCooldown("ult_" + id, ultimateCooldownSeconds);
        fr.medsir.toaruhc.ToaruUHC.getInstance().getPowerManager().updateEnergyBar(uhcPlayer);
    }

    /**
     * Helper: show dramatic title + play sounds for ultimate activation.
     */
    protected void showUltimateIntro(Player player, String title, String subtitle) {
        player.sendTitle("§c§l✦ ULTIMATE ✦", "§e" + title, 5, 70, 10);
        player.getWorld().playSound(player.getLocation(),
                org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 0.7f);
        player.getWorld().playSound(player.getLocation(),
                org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.4f);
        player.sendMessage("§c§l✦ ULTIMATE — §r§e" + title + " §8— §7" + subtitle);
    }

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

    public void setCustomModelId(int id){
        //Setter pour le modele custom
        //Pour chaque pouvoir, assigner un id de model custom pour lier la texture
        if(id >= 0) {
            customModelId = id;
        }
    }

    public int getCustomModelId(){
        return customModelId;
    }

    public String getId()           { return id; }
    public String getName()         { return name; }
    public String getDescription()  { return description; }
    public PowerType getType()      { return type; }
    public int getAimOrManaCost()   { return aimOrManaCost; }
    public int getCooldownSeconds() { return cooldownSeconds; }

    public enum PowerType { ESPER, MAGICIAN }
}
