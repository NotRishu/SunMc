package dev.sunmc.kit;

import dev.sunmc.SunMc;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {

    private final SunMc plugin;
    private final Map<String, Kit> kits = new HashMap<>();
    private final Map<UUID, Map<String, List<KitLoadout>>> playerLoadouts = new HashMap<>();
    private final Map<UUID, Integer> activeLoadoutSlot = new HashMap<>();
    private final Map<UUID, String> activeKit = new HashMap<>();

    private File kitsFile;
    private FileConfiguration kitsConfig;
    private File loadoutsFile;
    private FileConfiguration loadoutsConfig;

    public KitManager(SunMc plugin) {
        this.plugin = plugin;
        loadKits();
        loadLoadouts();
    }

    private void loadKits() {
        kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        if (!kitsFile.exists()) {
            try { kitsFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
        for (String name : kitsConfig.getKeys(false)) {
            Kit kit = new Kit(name);
            kit.setDisplayName(kitsConfig.getString(name + ".display_name", name));
            kit.setExtraItemsEnabled(kitsConfig.getBoolean(name + ".extra_items", false));
            List<?> rawExtras = kitsConfig.getList(name + ".extra_item_list");
            if (rawExtras != null) {
                List<ItemStack> extras = new ArrayList<>();
                for (Object o : rawExtras) {
                    if (o instanceof ItemStack) extras.add((ItemStack) o);
                }
                kit.setExtraItems(extras);
            }
            if (kitsConfig.contains(name + ".icon")) {
                kit.setIcon(kitsConfig.getItemStack(name + ".icon"));
            }
            kits.put(name.toLowerCase(), kit);
        }
        plugin.getLogger().info("Loaded " + kits.size() + " kits.");
    }

    private void loadLoadouts() {
        loadoutsFile = new File(plugin.getDataFolder(), "loadouts.yml");
        if (!loadoutsFile.exists()) {
            try { loadoutsFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        loadoutsConfig = YamlConfiguration.loadConfiguration(loadoutsFile);
        int maxLoadouts = plugin.getConfig().getInt("kit.default-loadouts", 3);

        for (String uuidStr : loadoutsConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            Map<String, List<KitLoadout>> kitLoadouts = new HashMap<>();
            for (String kitName : loadoutsConfig.getConfigurationSection(uuidStr).getKeys(false)) {
                List<KitLoadout> loadouts = new ArrayList<>();
                for (int i = 1; i <= maxLoadouts; i++) {
                    String path = uuidStr + "." + kitName + ".loadout" + i;
                    if (!loadoutsConfig.contains(path)) continue;
                    KitLoadout loadout = new KitLoadout(kitName, i);
                    ItemStack[] armor = new ItemStack[4];
                    for (int j = 0; j < 4; j++) {
                        armor[j] = loadoutsConfig.getItemStack(path + ".armor." + j);
                    }
                    loadout.setArmor(armor);
                    List<?> rawContents = loadoutsConfig.getList(path + ".contents");
                    if (rawContents != null) {
                        ItemStack[] contents = new ItemStack[36];
                        for (int j = 0; j < rawContents.size() && j < 36; j++) {
                            if (rawContents.get(j) instanceof ItemStack) contents[j] = (ItemStack) rawContents.get(j);
                        }
                        loadout.setContents(contents);
                    }
                    loadout.setOffhand(loadoutsConfig.getItemStack(path + ".offhand"));
                    loadouts.add(loadout);
                }
                if (!loadouts.isEmpty()) kitLoadouts.put(kitName.toLowerCase(), loadouts);
            }
            playerLoadouts.put(uuid, kitLoadouts);
        }
    }

    public void reload() {
        kits.clear();
        loadKits();
    }

    public Kit createKit(String name) {
        Kit kit = new Kit(name.toLowerCase());
        kits.put(name.toLowerCase(), kit);
        saveKits();
        return kit;
    }

    public boolean deleteKit(String name) {
        if (kits.remove(name.toLowerCase()) != null) {
            saveKits();
            return true;
        }
        return false;
    }

    public Optional<Kit> getKit(String name) {
        return Optional.ofNullable(kits.get(name.toLowerCase()));
    }

    public Collection<Kit> getAllKits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public void saveKits() {
        kitsConfig = new YamlConfiguration();
        for (Kit kit : kits.values()) {
            String n = kit.getName();
            kitsConfig.set(n + ".display_name", kit.getDisplayName());
            kitsConfig.set(n + ".extra_items", kit.isExtraItemsEnabled());
            kitsConfig.set(n + ".extra_item_list", kit.getExtraItems());
            if (kit.getIcon() != null) kitsConfig.set(n + ".icon", kit.getIcon());
        }
        try { kitsConfig.save(kitsFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void saveLoadout(UUID uuid, String kitName, int slot, PlayerInventory inv) {
        Map<String, List<KitLoadout>> kitMap = playerLoadouts.computeIfAbsent(uuid, k -> new HashMap<>());
        List<KitLoadout> loadouts = kitMap.computeIfAbsent(kitName.toLowerCase(), k -> new ArrayList<>());

        KitLoadout loadout = loadouts.stream()
                .filter(l -> l.getSlot() == slot)
                .findFirst()
                .orElseGet(() -> {
                    KitLoadout l = new KitLoadout(kitName, slot);
                    loadouts.add(l);
                    return l;
                });

        loadout.setArmor(inv.getArmorContents().clone());
        ItemStack[] rawContents = inv.getStorageContents();
        loadout.setContents(Arrays.copyOf(rawContents, Math.min(rawContents.length, 36)));
        loadout.setOffhand(inv.getItemInOffHand().clone());

        persistLoadout(uuid, kitName, loadout);
        activeLoadoutSlot.put(uuid, slot);
    }

    private void persistLoadout(UUID uuid, String kitName, KitLoadout loadout) {
        String path = uuid + "." + kitName + ".loadout" + loadout.getSlot();
        ItemStack[] armor = loadout.getArmor();
        for (int i = 0; i < armor.length; i++) {
            loadoutsConfig.set(path + ".armor." + i, armor[i]);
        }
        loadoutsConfig.set(path + ".contents", Arrays.asList(loadout.getContents()));
        loadoutsConfig.set(path + ".offhand", loadout.getOffhand());
        try { loadoutsConfig.save(loadoutsFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void applyLoadout(Player player, String kitName, int slot) {
        UUID uuid = player.getUniqueId();
        List<KitLoadout> loadouts = playerLoadouts
                .getOrDefault(uuid, Collections.emptyMap())
                .getOrDefault(kitName.toLowerCase(), Collections.emptyList());

        Optional<KitLoadout> match = loadouts.stream().filter(l -> l.getSlot() == slot).findFirst();
        if (match.isEmpty()) return;

        KitLoadout loadout = match.get();
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(loadout.getArmor());
        inv.setStorageContents(loadout.getContents());
        inv.setItemInOffHand(loadout.getOffhand());
        activeLoadoutSlot.put(uuid, slot);
        activeKit.put(uuid, kitName.toLowerCase());
    }

    public List<KitLoadout> getPlayerLoadouts(UUID uuid, String kitName) {
        return playerLoadouts.getOrDefault(uuid, Collections.emptyMap())
                .getOrDefault(kitName.toLowerCase(), Collections.emptyList());
    }

    public int getActiveLoadoutSlot(UUID uuid) {
        return activeLoadoutSlot.getOrDefault(uuid, 1);
    }

    public void setActiveKit(UUID uuid, String kit) {
        activeKit.put(uuid, kit.toLowerCase());
    }

    public Optional<String> getActiveKit(UUID uuid) {
        return Optional.ofNullable(activeKit.get(uuid));
    }
}
