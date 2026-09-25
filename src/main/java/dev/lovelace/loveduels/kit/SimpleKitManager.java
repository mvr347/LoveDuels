package dev.lovelace.loveduels.kit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class SimpleKitManager implements KitManager {

    private final Plugin plugin;
    private final File kitsFile;
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();

    public SimpleKitManager(Plugin plugin) {
        this.plugin = plugin;
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
    }

    @Override
    public Optional<Kit> getKit(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(kits.get(id.toLowerCase()));
    }

    @Override
    public Collection<Kit> getAllKits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    @Override
    public void registerKit(Kit kit) {
        kits.put(kit.id(), kit);
        saveKits();
    }

    @Override
    public boolean deleteKit(String id) {
        Kit removed = kits.remove(id.toLowerCase());
        if (removed != null) {
            saveKits();
            return true;
        }
        return false;
    }

    @Override
    public void loadKits() {
        kits.clear();
        if (!kitsFile.exists()) {
            createDefaultKits();
            saveKits();
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);
        ConfigurationSection root = config.getConfigurationSection("kits");
        if (root == null) {
            createDefaultKits();
            saveKits();
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;

            String name = sec.getString("displayName", id);
            ItemStack icon = sec.getItemStack("icon", new ItemStack(Material.IRON_SWORD));

            List<?> rawStorage = sec.getList("storage");
            ItemStack[] storage = rawStorage != null ? rawStorage.toArray(new ItemStack[0]) : new ItemStack[36];

            List<?> rawArmor = sec.getList("armor");
            ItemStack[] armor = rawArmor != null ? rawArmor.toArray(new ItemStack[0]) : new ItemStack[4];

            ItemStack offhand = sec.getItemStack("offhand");

            List<PotionEffect> effects = new ArrayList<>();
            List<Map<?, ?>> rawEffects = sec.getMapList("effects");
            for (Map<?, ?> effectMap : rawEffects) {
                try {
                    String typeStr = (String) effectMap.get("type");
                    int duration = (int) effectMap.get("duration");
                    int amplifier = (int) effectMap.get("amplifier");
                    org.bukkit.NamespacedKey key = (typeStr.contains(":"))
                            ? org.bukkit.NamespacedKey.fromString(typeStr.toLowerCase())
                            : org.bukkit.NamespacedKey.minecraft(typeStr.toLowerCase());
                    PotionEffectType pet = (key != null) ? org.bukkit.Registry.POTION_EFFECT_TYPE.get(key) : null;
                    if (pet != null) {
                        effects.add(new PotionEffect(pet, duration, amplifier));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse potion effect for kit " + id + ": " + e.getMessage());
                }
            }

            kits.put(id.toLowerCase(), new Kit(id.toLowerCase(), name, icon, storage, armor, offhand, effects));
        }

        if (kits.isEmpty()) {
            createDefaultKits();
            saveKits();
        }

        plugin.getLogger().info("Loaded " + kits.size() + " duel kit(s).");
    }

    private void createDefaultKits() {
        // 1. Knight
        ItemStack[] knightArmor = new ItemStack[]{
                new ItemStack(Material.IRON_BOOTS),
                new ItemStack(Material.IRON_LEGGINGS),
                new ItemStack(Material.IRON_CHESTPLATE),
                new ItemStack(Material.IRON_HELMET)
        };
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 1);
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemStack steak = new ItemStack(Material.COOKED_BEEF, 16);

        ItemStack[] knightStorage = new ItemStack[36];
        knightStorage[0] = sword;
        knightStorage[1] = steak;

        kits.put("knight", new Kit(
                "knight",
                "<gold>Рыцарь",
                sword,
                knightStorage,
                knightArmor,
                shield,
                List.of(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 0))
        ));

        // 2. Archer
        ItemStack[] archerArmor = new ItemStack[]{
                new ItemStack(Material.CHAINMAIL_BOOTS),
                new ItemStack(Material.CHAINMAIL_LEGGINGS),
                new ItemStack(Material.CHAINMAIL_CHESTPLATE),
                new ItemStack(Material.CHAINMAIL_HELMET)
        };
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.POWER, 2);
        ItemStack arrows = new ItemStack(Material.ARROW, 64);
        ItemStack dagger = new ItemStack(Material.STONE_SWORD);

        ItemStack[] archerStorage = new ItemStack[36];
        archerStorage[0] = bow;
        archerStorage[1] = dagger;
        archerStorage[2] = steak;
        archerStorage[9] = arrows;

        kits.put("archer", new Kit(
                "archer",
                "<green>Лучник",
                bow,
                archerStorage,
                archerArmor,
                null,
                List.of(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 0))
        ));
    }

    @Override
    public void saveKits() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("kits");

        for (Kit kit : kits.values()) {
            ConfigurationSection sec = root.createSection(kit.id());
            sec.set("displayName", kit.displayName());
            sec.set("icon", kit.icon());
            sec.set("storage", Arrays.asList(kit.storageContents()));
            sec.set("armor", Arrays.asList(kit.armorContents()));
            sec.set("offhand", kit.offhand());

            List<Map<String, Object>> effectMaps = new ArrayList<>();
            for (PotionEffect effect : kit.effects()) {
                Map<String, Object> m = new HashMap<>();
                m.put("type", effect.getType().getKey().getKey());
                m.put("duration", effect.getDuration());
                m.put("amplifier", effect.getAmplifier());
                effectMaps.add(m);
            }
            sec.set("effects", effectMaps);
        }

        try {
            config.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kits.yml", e);
        }
    }
}
