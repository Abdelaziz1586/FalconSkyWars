package me.pebbleprojects.falconskywars.engine;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class LootItem {

    private final double chance;
    private final Material material;
    private final int minAmount, maxAmount;
    private final Map<Enchantment, Integer> enchantmentToLevelMap;

    public LootItem(final ConfigurationSection section) {
        enchantmentToLevelMap = new HashMap<>();

        Material material;

        try {
            material = Material.valueOf(section.getString("material"));
        } catch (final Exception ignored) {
            material = Material.AIR;
        }

        this.material = material;

        final ConfigurationSection enchantmentsSection = section.getConfigurationSection("enchantments");
        if (enchantmentsSection != null) {
            int level;
            Enchantment enchantment;

            for (final String key : enchantmentsSection.getKeys(false)) {
                enchantment = Enchantment.getByName(key);

                if (enchantment != null) {
                    level = enchantmentsSection.getInt(key);

                    enchantmentToLevelMap.put(enchantment, level);
                }
            }
        }

        chance = section.getDouble("chance");
        minAmount = section.getInt("minAmount");
        maxAmount = section.getInt("maxAmount");
    }

    public boolean shouldFill(final ThreadLocalRandom random) {
        return random.nextDouble() < chance;
    }

    public final ItemStack make(final Random random) {
        final ItemStack itemStack = new ItemStack(material, random.nextInt(maxAmount + 1 - minAmount) + minAmount);
        final ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            if (!enchantmentToLevelMap.isEmpty()) {
                for (final Enchantment enchantment : enchantmentToLevelMap.keySet()) {
                    itemMeta.addEnchant(enchantment, enchantmentToLevelMap.get(enchantment), true);
                }
            }

            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }
}
