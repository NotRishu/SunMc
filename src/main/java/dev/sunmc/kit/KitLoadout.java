package dev.sunmc.kit;

import org.bukkit.inventory.ItemStack;

public class KitLoadout {

    private final String kitName;
    private final int slot;
    private ItemStack[] armor;    // [helmet, chestplate, leggings, boots]
    private ItemStack[] contents; // 36 inventory slots
    private ItemStack offhand;

    public KitLoadout(String kitName, int slot) {
        this.kitName = kitName;
        this.slot = slot;
        this.armor = new ItemStack[4];
        this.contents = new ItemStack[36];
    }

    public String getKitName() { return kitName; }
    public int getSlot() { return slot; }
    public ItemStack[] getArmor() { return armor; }
    public void setArmor(ItemStack[] armor) { this.armor = armor; }
    public ItemStack[] getContents() { return contents; }
    public void setContents(ItemStack[] contents) { this.contents = contents; }
    public ItemStack getOffhand() { return offhand; }
    public void setOffhand(ItemStack offhand) { this.offhand = offhand; }
}
