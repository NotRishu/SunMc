package dev.sunmc.kit;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Kit {

    private final String name;
    private String displayName;
    private boolean extraItemsEnabled;
    private List<ItemStack> extraItems;
    private ItemStack icon;

    public Kit(String name) {
        this.name = name;
        this.displayName = name;
        this.extraItemsEnabled = false;
        this.extraItems = new ArrayList<>();
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isExtraItemsEnabled() { return extraItemsEnabled; }
    public void setExtraItemsEnabled(boolean extraItemsEnabled) { this.extraItemsEnabled = extraItemsEnabled; }
    public List<ItemStack> getExtraItems() { return extraItems; }
    public void setExtraItems(List<ItemStack> extraItems) { this.extraItems = extraItems; }
    public ItemStack getIcon() { return icon; }
    public void setIcon(ItemStack icon) { this.icon = icon; }
}
