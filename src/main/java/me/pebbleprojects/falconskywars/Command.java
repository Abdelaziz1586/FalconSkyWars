package me.pebbleprojects.falconskywars;

import me.pebbleprojects.falconskywars.handlers.Handler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Command implements CommandExecutor {

    private final Handler handler;

    public Command(final Handler handler) {
        this.handler = handler;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final org.bukkit.command.Command command, final String label, final String[] args) {
        new Thread(() -> {
            if (sender instanceof Player) {
                final Player player = (Player) sender;

                if (args.length > 0) {
                    if (player.hasPermission("sw.admin")) {
                        if (args[0].equalsIgnoreCase("setLobby")) {
                            final Location location = player.getLocation();

                            handler.writeData("spawn.x", location.getX());
                            handler.writeData("spawn.y", location.getY());
                            handler.writeData("spawn.z", location.getZ());
                            handler.writeData("spawn.yaw", location.getYaw());
                            handler.writeData("spawn.pitch", location.getPitch());
                            handler.writeData("spawn.world", location.getWorld().getName());
                            player.sendMessage("§aSet main lobby to your current location!");
                            return;
                        }

                        if (args[0].equalsIgnoreCase("create")) {
                            if (args.length > 1) {
                                File file = new File(FalconSkyWars.INSTANCE.getDataFolder(), "games");

                                if (!file.exists()) file.mkdirs();

                                file = new File(FalconSkyWars.INSTANCE.getDataFolder(), "games/" + args[1] + ".yml");

                                if (file.exists()) {
                                    player.sendMessage("§cSorry, but it looks like there is already an arena with that name!");
                                    return;
                                }

                                try {
                                    file.createNewFile();
                                } catch (IOException e) {
                                    player.sendMessage("§cAn error has occurred while creating arena §e" + args[1]);
                                    return;
                                }

                                player.sendMessage("§aCreated game §e" + args[1]);

                                final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                                final Location location = player.getLocation();

                                yaml.set("spawn.x", location.getX());
                                yaml.set("spawn.y", location.getY());
                                yaml.set("spawn.z", location.getZ());
                                yaml.set("spawn.yaw", location.getYaw());
                                yaml.set("spawn.pitch", location.getPitch());
                                yaml.set("spawn.world", location.getWorld().getName());

                                try {
                                    yaml.save(file);
                                } catch (final IOException e) {
                                    player.sendMessage("§cAn error has occurred while setting waiting spawn for §e" + args[1]);
                                    e.printStackTrace();
                                    return;
                                }
                                player.sendMessage("§aSet waiting lobby spawn to your current position.");
                                return;
                            }
                            player.sendMessage("§cUncompleted arguments, /sw create <game name>");
                            return;
                        }

                        if (args[0].equalsIgnoreCase("addSpawn")) {
                            if (args.length > 1) {
                                final File file = new File(FalconSkyWars.INSTANCE.getDataFolder(), "games/" + args[1] + ".yml");

                                if (!file.exists()) {
                                    player.sendMessage("§cSorry, but it looks like there is no arena with that name!");
                                    return;
                                }

                                final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                                final ConfigurationSection section = yaml.getConfigurationSection("spawns");

                                final int i = section != null ? section.getKeys(false).size() : 0;

                                final Location location = player.getLocation();

                                yaml.set("spawns." + i + ".x", location.getX());
                                yaml.set("spawns." + i + ".y", location.getY());
                                yaml.set("spawns." + i + ".z", location.getZ());
                                yaml.set("spawns." + i + ".yaw", location.getYaw());
                                yaml.set("spawns." + i + ".pitch", location.getPitch());
                                yaml.set("spawns." + i + ".world", location.getWorld().getName());

                                try {
                                    yaml.save(file);
                                } catch (final IOException e) {
                                    player.sendMessage("§cAn error has occurred while adding spawn!");
                                    e.printStackTrace();
                                    return;
                                }

                                addChestsAutomatically(player, args[1]);

                                player.sendMessage("§aAdded spawn at your current location §7(total amount of locations => §8" + (i+1) + "§7)");
                                return;
                            }

                            player.sendMessage("§cUncompleted arguments, /sw addSpawn <game name>");
                            return;
                        }

                        if (args[0].equalsIgnoreCase("removeSpawn")) {
                            if (args.length > 2) {
                                final File file = new File(FalconSkyWars.INSTANCE.getDataFolder(), "games/" + args[1] + ".yml");

                                if (!file.exists()) {
                                    player.sendMessage("§cSorry, but it looks like there is no arena with that name!");
                                    return;
                                }

                                try {
                                    final int id = Integer.parseInt(args[2]);

                                    if (id <= 0) {
                                        player.sendMessage("§cID of spawn must be greater than 0!");
                                        return;
                                    }

                                    final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                                    final ConfigurationSection section = yaml.getConfigurationSection("spawns");

                                    if (section.getKeys(false).size() >= (id - 1)) {
                                        yaml.set("spawns." + (id - 1), null);

                                        try {
                                            yaml.save(file);
                                        } catch (final IOException e) {
                                            player.sendMessage("§cAn error has occurred while removing spawn!");
                                            e.printStackTrace();
                                            return;
                                        }
                                        player.sendMessage("§aRemoved spawn with ID §e" + id);
                                        return;
                                    }
                                    player.sendMessage("§cThere's no spawn with that ID! §7(Max spawn ID yet is " + section.getKeys(false).size() + "§7)");
                                } catch (final NumberFormatException ignored) {
                                    player.sendMessage("§cPlease enter a valid spawn ID");
                                }
                                return;
                            }

                            player.sendMessage("§cUncompleted arguments, /sw removeSpawn <game name> <ID>");
                            return;
                        }

                        if (args[0].equalsIgnoreCase("addMiddleChest")) {
                            if (args.length > 1) {
                                final File file = new File(FalconSkyWars.INSTANCE.getDataFolder(), "games/" + args[1] + ".yml");

                                if (!file.exists()) {
                                    player.sendMessage("§cSorry, but it looks like there is no arena with that name!");
                                    return;
                                }

                                final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

                                final Block block = player.getTargetBlock((Set<Material>) null, 5);

                                if (block.getState() instanceof Chest) {
                                    final ConfigurationSection section = yaml.getConfigurationSection("chests");

                                    int i = section == null ? 0 : section.getKeys(false).size();

                                    final Location location = block.getLocation();

                                    yaml.set("chests." + i + ".middle", true);
                                    yaml.set("chests." + i + ".location.x", location.getX());
                                    yaml.set("chests." + i + ".location.y", location.getY());
                                    yaml.set("chests." + i + ".location.z", location.getZ());
                                    yaml.set("chests." + i + ".location.yaw", location.getYaw());
                                    yaml.set("chests." + i + ".location.pitch", location.getPitch());
                                    yaml.set("chests." + i + ".location.world", location.getWorld().getName());

                                    try {
                                        yaml.save(file);
                                    } catch (final IOException e) {
                                        player.sendMessage("§cAn error has occurred while saving chest, please report this to the creator!");
                                        e.printStackTrace();
                                        return;
                                    }

                                    player.sendMessage("§aSet the chest that you're looking as a middle chest.");
                                    return;
                                }

                                player.sendMessage("§cYou're not looking at a chest!");
                                return;
                            }
                            player.sendMessage("§cUncompleted arguments, /sw addMiddleChest <game name>");
                            return;
                        }

                        if (args[0].equalsIgnoreCase("join")) {
                            handler.gameHandler.getPlayerReady(player, false, true);
                            return;
                        }

                        if (args[0].equalsIgnoreCase("leave")) {
                            handler.gameHandler.leave(player);
                            return;
                        }

                        if (args[0].equalsIgnoreCase("joinGame")) {
                            if (args.length > 1) {
                                handler.gameHandler.join(player, args[1]);
                                return;
                            }
                            handler.gameHandler.join(player, null);
                        }
                    }
                }
            }
        }).start();
        return false;
    }

    private void addChestsAutomatically(final Player player, final String game) {
        final File file = new File(FalconSkyWars.INSTANCE.getDataFolder(), "games/" + game + ".yml");

        if (!file.exists()) {
            player.sendMessage("§cLooks like there is no arena with that name!");
            return;
        }

        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        player.sendMessage("§eLooping chests in island...");

        Location location = player.getLocation();

        final World world = location.getWorld();
        int
                minX = location.getBlockX() - 9,
                minY = location.getBlockY() - 25,
                minZ = location.getBlockZ() - 9,
                maxX = location.getBlockX() + 9,
                maxY = location.getBlockY() + 25,
                maxZ = location.getBlockZ() + 9;

        Block block;

        final ConfigurationSection section = yaml.getConfigurationSection("chests");
        int i = section == null ? 0 : section.getKeys(false).size();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.CHEST) {
                        yaml.set("chests." + i + ".middle", false);

                        location = block.getLocation();

                        yaml.set("chests." + i + ".location.x", location.getX());
                        yaml.set("chests." + i + ".location.y", location.getY());
                        yaml.set("chests." + i + ".location.z", location.getZ());
                        yaml.set("chests." + i + ".location.yaw", location.getYaw());
                        yaml.set("chests." + i + ".location.pitch", location.getPitch());
                        yaml.set("chests." + i + ".location.world", location.getWorld().getName());

                        i++;

                        player.sendMessage("§aAutomatically detected and added chest at location §e" + block.getLocation().getBlockX() + ", " + block.getLocation().getBlockY() + ", " + block.getLocation().getBlockZ());
                    }
                }
            }
        }

        try {
            yaml.save(file);
        } catch (final IOException e) {
            player.sendMessage("§cAn error occurred while saving locations of added chests, please report this to the creator.");
            e.printStackTrace();
        }
    }

    private ArrayList<Location> getSpawns(final String game) {
        final Object o = handler.getData("locations.games." + game + ".spawns");

        final ArrayList<Location> locations = new ArrayList<>();

        if (o instanceof List) {
            for (final Object location : (List<?>) o) {
                if (location instanceof Location) {
                    locations.add((Location) location);
                }
            }
        }

        return locations;
    }
}
