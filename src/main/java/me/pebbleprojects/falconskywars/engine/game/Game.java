package me.pebbleprojects.falconskywars.engine.game;

import me.pebbleprojects.falconskywars.engine.SWLocation;
import me.pebbleprojects.falconskywars.handlers.Handler;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.io.IOException;
import java.util.*;

public class Game {

    public String name;
    public Status status;
    public Location spawn;
    private int minPlayers;
    private Timer startTask;
    private Handler handler;
    private ArrayList<Location> spawns;
    public HashMap<Chest, Boolean> chests;
    public HashMap<Player, Boolean> players;

    public Game(final String name, final Handler handler, World world, final int minPlayers, final YamlConfiguration section) {
        this.name = name;

        this.handler = handler;
        this.minPlayers = minPlayers;

        chests = new HashMap<>();
        players = new HashMap<>();

        spawns = new ArrayList<>();

        SWLocation swLocation = new SWLocation(section.getConfigurationSection("spawn"));

        if (swLocation.location == null) {
            status = Status.FAILED;
            return;
        }

        spawn = swLocation.location;

        if (world == null) world = spawn.getWorld();

        world.setAnimalSpawnLimit(0);
        world.setMonsterSpawnLimit(0);

        for (final Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) entity.remove();
        }

        spawn.setWorld(world);

        if (spawns == null) {
            status = Status.FAILED;
            return;
        }

        Location location;

        for (final String spawn : section.getConfigurationSection("spawns").getKeys(false)) {
            swLocation = new SWLocation(section.getConfigurationSection("spawns").getConfigurationSection(spawn));

            if (swLocation.location == null) continue;

            location = swLocation.location;

            location.setWorld(world);

            this.spawns.add(location);
        }

        Block block;
        for (final String key : section.getConfigurationSection("chests").getKeys(false)) {

            if (!section.isConfigurationSection("chests." + key + ".location")) continue;

            swLocation = new SWLocation(section.getConfigurationSection("chests." + key + ".location"));

            if (swLocation.location != null) {
                location = swLocation.location;

                location.setWorld(world);

                block = world.getBlockAt(location);

                if (block.getType() == Material.CHEST) {
                    chests.put((Chest) block.getState(), section.getBoolean("chests." + key + ".middle"));
                }
            }
        }

        handler.gameHandler.resetChests(this);

        startTask = null;

        status = Status.WAITING;
    }

    public void join(final Player player) {
        if (status == Status.WAITING || status == Status.STARTING) {
            if (!handler.gameHandler.players.contains(player)) {
                player.sendMessage("§cYou must be in the SkyWars lobby in order to execute this command.");
                return;
            }

            if (handler.gameHandler.inGamePlayers.containsKey(player.getUniqueId())) {
                player.sendMessage("§cYou're already in an another game");
                return;
            }

            if (players.containsKey(player)) {
                player.sendMessage("§cYou're already in this game!");
                return;
            }

            if (players.size() == spawns.size()) {
                player.sendMessage(players.size() + " - " + spawns.size());
                player.sendMessage("§cSorry, but this game is full!");
                return;
            }

            players.put(player, true);

            handler.gameHandler.getPlayerReady(player, true, false);
            handler.gameHandler.inGamePlayers.put(player.getUniqueId(), this);

            handler.runTask(() -> player.teleport(spawn));

            if (Boolean.parseBoolean(handler.getConfig("scoreboard.game.waiting.enabled", false).toString())) {
                final ConfigurationSection section = handler.getConfigSection("scoreboard.game.waiting");

                final List<String> list = section.getStringList("lines");

                int i = 0;

                for (final Player p : players.keySet()) if (players.get(p)) i++;

                String s;
                for (int l = 0; l < list.size(); l++) {
                    s = handler.gameHandler.replaceStringWithData(player.getUniqueId(), list.get(l)).replace("%player%", player.getName()).replace("%stats%", status.name()).replace("%playerCount%", String.valueOf(players.size())).replace("%alivePlayers%", String.valueOf(i));
                    list.set(l, ChatColor.translateAlternateColorCodes('&', s));
                }

                handler.gameHandler.newScoreboard(player, ChatColor.translateAlternateColorCodes('&', section.getString("title")), list);
            }

            broadcast("§e" + player.getDisplayName() + " §ahas joined §8(§7" + players.size() + "§8/§7" + spawns.size() + "§8)");

            startCountdown();
            return;
        }

        player.sendMessage("§cSorry, but it looks this game isn't available at the moment.");
    }

    public void leave(final Player player) {
        if (status == Status.ENDED) {
            handler.gameHandler.inGamePlayers.remove(player.getUniqueId());
            handler.gameHandler.getPlayerReady(player, false, false);

            players.remove(player);
            return;
        }

        if (status == Status.WAITING || status == Status.STARTING) {
            handler.gameHandler.inGamePlayers.remove(player.getUniqueId());
            handler.gameHandler.getPlayerReady(player, false, false);

            players.remove(player);

            if (status != Status.WARMUP)
                broadcast("§e" + player.getDisplayName() + " §chas left §8(§7" + players.size() + "§8/§7" + spawns.size() + "§8)");
            else
                checkForWin();
            player.sendMessage("§cYou have left the game");
            return;
        }

        if (players.get(player)) {
            final Player attacker = player.getLastDamageCause() instanceof EntityDamageByEntityEvent ? (Player) ((EntityDamageByEntityEvent) player.getLastDamageCause()).getDamager() : null;

            players.remove(player);

            death(player, attacker);

            handler.gameHandler.inGamePlayers.remove(player.getUniqueId());
            handler.gameHandler.getPlayerReady(player, false, false);

            player.sendMessage("§cYou have left the game");
            return;
        }

        handler.gameHandler.inGamePlayers.remove(player.getUniqueId());
        handler.gameHandler.getPlayerReady(player, false, false);

        players.remove(player);
    }

    public void death(final Player victim, final Player attacker) {
        handler.gameHandler.getPlayerReady(victim, true, false);

        handler.runTask(() -> {
            victim.setAllowFlight(true);
            victim.setFlying(true);
        });

        handler.gameHandler.setGhost(victim, true);

        for (final Player player : players.keySet()) {
            if (players.get(player)) handler.runTask(() -> player.hidePlayer(victim));
        }

        players.put(victim, false);

        broadcast(attacker != null ? "§e" + victim.getDisplayName() + " §chas been eliminated by §e" + attacker.getDisplayName() : "§e" + victim.getDisplayName() + " §cdied");

        if (attacker != null) {
            handler.gameHandler.addKill(attacker.getUniqueId());
            handler.gameHandler.addCoins(attacker.getUniqueId(), Integer.parseInt(handler.getConfig("coinsPerKill", false).toString()));
        }

        handler.gameHandler.addDeath(victim.getUniqueId());

        checkForWin();
    }

    public void broadcast(final String message) {
        for (final Player player : players.keySet()) {
            player.sendMessage(message);
        }
    }


    // Internal Functions

    private void checkForWin() {
        if (status != Status.PLAYING && status != Status.WARMUP) return;

        final ArrayList<Player> alive = new ArrayList<>();

        for (final Player player : players.keySet()) {
            if (players.get(player)) alive.add(player);
        }

        if (alive.size() == 1) {
            if (startTask != null) {
                startTask.cancel();
                startTask = null;
            }

            status = Status.ENDED;

            final Player winner = alive.get(0);

            handler.runTask(() -> {
                winner.setAllowFlight(true);
                winner.setFlying(true);
            });

            handler.gameHandler.addWin(winner.getUniqueId());

            broadcast("§e" + winner.getDisplayName() + " §ahas won!");

            final int[] i = {15};
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    i[0]--;
                    if (i[0] == 0) {

                        final ArrayList<Player> set = new ArrayList<>(players.keySet());

                        for (Player player : set) {
                            leave(player);
                        }

                        destroy();

                        this.cancel();

                        return;
                    }
                    handler.runTask(() -> winner.getWorld().spawnEntity(winner.getLocation(), EntityType.FIREWORK));
                }
            }, 0, 250);
        }
    }

    private void startCountdown() {
        if (players.size() >= minPlayers && status == Status.WAITING) {
            final int[] i = new int[2];

            status = Status.STARTING;

            if (Boolean.parseBoolean(handler.getConfig("scoreboard.game.waiting.enabled", false).toString())) {
                final ConfigurationSection section = handler.getConfigSection("scoreboard.game.waiting");

                final List<String> list = section.getStringList("lines");

                for (final Player p : players.keySet()) if (players.get(p)) i[0]++;

                String s;
                for (final Player player : players.keySet()) {
                    for (i[1] = 0; i[1] < list.size(); i[1]++) {
                        s = handler.gameHandler.replaceStringWithData(player.getUniqueId(), list.get(i[1])).replace("%player%", player.getName()).replace("%stats%", status.name()).replace("%playerCount%", String.valueOf(players.size())).replace("%aliveCount%", String.valueOf(i[0]));
                        list.set(i[1], ChatColor.translateAlternateColorCodes('&', s));
                    }

                    handler.gameHandler.newScoreboard(player, ChatColor.translateAlternateColorCodes('&', section.getString("title")), list);
                }
            }

            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (players.size() >= minPlayers) {
                        i[0]--;
                        if (i[0] == 0) {
                            start();
                            this.cancel();
                            return;
                        }
                        broadcast("§eGame starting in " + i[0]);
                        return;
                    }

                    status = Status.WAITING;
                    broadcast("§cNo enough players, stopping countdown");
                    this.cancel();
                }
            }, 0, 1000);
        }
    }

    private void start() {
        status = Status.WARMUP;

        final int[] i = {0};

        handler.runTask(() -> {
            for (final Player player : players.keySet()) {
                handler.gameHandler.addGamePlayed(player.getUniqueId());
                handler.runTask(() -> player.teleport(setCage(spawns.get(i[0]), Material.GLASS)));
                i[0]++;
            }
        });

        i[0] = 6;
        startTask = new Timer();

        startTask.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                i[0]--;
                if (i[0] == 0) {
                    for (final Location spawn : spawns) {
                        setCage(spawn, Material.AIR);
                    }

                    status = Status.PLAYING;

                    broadcast("§aGame has started!");

                    this.cancel();
                    return;
                }
                broadcast("§eStarting in " + i[0]);
            }
        }, 0, 1000);
    }
    
    private Location setCage(final Location location, final Material material) {
        final Location[] teleportation = new Location[1];

        handler.runTask(() -> {
            location.clone().add(-1, 0, 0).getBlock().setType(material); // Left down
            location.clone().add(-1, 1, 0).getBlock().setType(material); // Left middle
            location.clone().add(-1, 2, 0).getBlock().setType(material); // Left up
            location.clone().add(1, 0, 0).getBlock().setType(material); // Right down
            location.clone().add(1, 1, 0).getBlock().setType(material); // Right middle
            location.clone().add(1, 2, 0).getBlock().setType(material); // Right up
            location.clone().add(0, 0, -1).getBlock().setType(material); // Back down
            location.clone().add(0, 1, -1).getBlock().setType(material); // Back middle
            location.clone().add(0, 2, -1).getBlock().setType(material); // Back up
            location.clone().add(0, 0, 1).getBlock().setType(material); // In front of down
            location.clone().add(0, 1, 1).getBlock().setType(material); // In front of middle
            location.clone().add(0, 2, 1).getBlock().setType(material); // In front of up
            location.clone().add(0, 3, 0).getBlock().setType(material);

            final Block block = location.clone().add(0, -1, 0).getBlock();
            block.setType(material);
            teleportation[0] = block.getLocation().add(0.5, 1, 0.5);
        });

        return teleportation[0];
    }

    private void destroy() {
        World world = spawn.getWorld();

        if (world.getPlayers().size() > 0) {
            for (final Player player : world.getPlayers()) {
                handler.gameHandler.getPlayerReady(player, false, false);
            }
        }

        if (Bukkit.unloadWorld(world, false)) {

            try {
                FileUtils.forceDelete(world.getWorldFolder());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        List<Game> games = new LinkedList<>(handler.gameHandler.games.get(name));

        games.remove(this);
        handler.gameHandler.games.put(name, games);

        name = null;
        spawn = null;
        spawns = null;
        chests = null;
        players = null;
        handler = null;
        minPlayers = 0;
        startTask = null;

        System.runFinalization();
    }
}
