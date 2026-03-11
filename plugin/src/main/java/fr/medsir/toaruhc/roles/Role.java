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
