package me.pebbleprojects.falconskywars.handlers;

import me.pebbleprojects.falconskywars.Command;
import me.pebbleprojects.falconskywars.FalconSkyWars;
import me.pebbleprojects.falconskywars.listeners.*;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;

public class Handler {

    private final File dataFile;
    public GameHandler gameHandler;
    public final FalconSkyWars main;
    private FileConfiguration config, data;
    public Handler() {
        main = FalconSkyWars.INSTANCE;

        main.getConfig().options().copyDefaults(true);
        main.saveDefaultConfig();
        updateConfig();

        dataFile = new File(main.getDataFolder().getPath(), "data.yml");

        if (!dataFile.exists()) {
            try {
                if (dataFile.createNewFile())
                    main.getServer().getConsoleSender().sendMessage("§aCreated data.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        updateData();

        new Thread(() -> {
            gameHandler = new GameHandler(this);

            final PluginManager pm = main.getServer().getPluginManager();

            pm.registerEvents(new AsyncPlayerChat(gameHandler), main);
            pm.registerEvents(new BlockPlace(gameHandler), main);
            pm.registerEvents(new BlockBreak(gameHandler), main);
            pm.registerEvents(new EntityDamage(this), main);
            pm.registerEvents(new FoodLevelChange(gameHandler), main);
            pm.registerEvents(new PlayerDeath(gameHandler), main);
            pm.registerEvents(new PlayerDropItem(gameHandler), main);
            pm.registerEvents(new PlayerQuit(gameHandler), main);

            main.getCommand("sw").setExecutor(new Command(this));
        }).start();
    }

    public void shutdown() {
        final File file = new File("swArenas");

        if (file.exists()) {
            final File[] files = file.listFiles();

            if (files != null) {

                World world;
                for (final File worldFile : files) {
                    world = Bukkit.getWorld("swArenas/" + worldFile.getName());

                    if (world != null) {
                        if (world.getPlayers().size() > 0) {
                            for (final Player player : world.getPlayers()) {
                                gameHandler.getPlayerReady(player, false, false);
                            }
                        }
                    }

                    Bukkit.unloadWorld(world, false);

                    try {
                        FileUtils.forceDelete(worldFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public void updateConfig() {
        main.reloadConfig();
        config = main.getConfig();
    }

    public void updateData() {
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void writeData(final String key, final Object value) {
        data.set(key, value);
        try {
            data.save(dataFile);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    public void runTask(final Runnable runnable) {
        if (!Bukkit.isPrimaryThread()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTask(main);
            return;
        }
        runnable.run();
    }

    public Object getConfig(final String key, final boolean translate) {
        return config.isSet(key) ? (translate ? ChatColor.translateAlternateColorCodes('&', config.getString(key)).replace("%nl%", "\n") : config.get(key)) : null;
    }

    public ConfigurationSection getConfigSection(final String key) {
        return config.isSet(key) ? config.getConfigurationSection(key) : null;
    }

    public ConfigurationSection getDataSection(final String key) {
        return data.isSet(key) ? data.getConfigurationSection(key) : null;
    }

    public Object getData(final String key) {
        return data.isSet(key) ? data.get(key) : null;
    }
}
