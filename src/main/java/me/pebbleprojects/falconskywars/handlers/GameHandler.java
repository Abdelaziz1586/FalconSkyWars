package me.pebbleprojects.falconskywars.handlers;

import fr.mrmicky.fastboard.FastBoard;
import me.pebbleprojects.falconskywars.FalconSkyWars;
import me.pebbleprojects.falconskywars.engine.LootItem;
import me.pebbleprojects.falconskywars.engine.SWLocation;
import me.pebbleprojects.falconskywars.engine.SavedInventory;
import me.pebbleprojects.falconskywars.engine.game.Game;
import me.pebbleprojects.falconskywars.engine.game.Status;
import net.minecraft.server.v1_8_R3.*;
import org.apache.commons.io.FileUtils;
import org.bukkit.Material;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameHandler {

    public Team team;
    public Scoreboard board;
    private final Handler handler;
    public final ArrayList<Player> players;
    private final ArrayList<String> underMaking;
    private final ArrayList<UUID> lookingForGame;
    private final HashMap<String, Game> originals;
    public final HashMap<UUID, Game> inGamePlayers;
    public final HashMap<String, List<Game>> games;
    private final HashMap<UUID, FastBoard> scoreboards;
    private final HashMap<UUID, SavedInventory> savedInventories;
    private final ArrayList<LootItem> normalLootItems, middleLootItems;

    public GameHandler(final Handler handler) {
        this.handler = handler;

        players = new ArrayList<>();
        underMaking = new ArrayList<>();
        lookingForGame = new ArrayList<>();
        normalLootItems = new ArrayList<>();
        middleLootItems = new ArrayList<>();

        games = new HashMap<>();
        originals = new HashMap<>();
        scoreboards = new HashMap<>();
        inGamePlayers = new HashMap<>();
        savedInventories = new HashMap<>();

        ConfigurationSection section = handler.getConfigSection("chestItems.normal");

        if (section != null) {
            for (final String key : section.getKeys(false)) {
                normalLootItems.add(new LootItem(section.getConfigurationSection(key)));
            }
        }

        section = handler.getConfigSection("chestItems.middle");

        if (section != null) {
            for (final String key : section.getKeys(false)) {
                middleLootItems.add(new LootItem(section.getConfigurationSection(key)));
            }
        }

        handler.runTask(() -> {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            team = board.registerNewTeam("team");
            team.setAllowFriendlyFire(true);
            team.setCanSeeFriendlyInvisibles(true);
            team.setDisplayName("Ghost");
        });

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                init();
            }
        }, 1000);
    }

    public void init() {
        final File file = new File(FalconSkyWars.INSTANCE.getDataFolder(), "games");

        if (file.exists()) {
            final File[] files = file.listFiles();

            if (files != null) {

                Game game;
                for (final File gameFile : files) {
                    game = new Game(gameFile.getName().replace(".yml", ""), handler, null, 2, YamlConfiguration.loadConfiguration(gameFile));

                    if (game.status == Status.WAITING) {
                        originals.put(gameFile.getName().toLowerCase().replace(".yml", ""), game);
                        System.out.println("Loaded " + gameFile.getName());
                        continue;
                    }
                    System.out.println("Failed to load " + gameFile.getName());
                }
            }
        }
    }

    public void getPlayerReady(final Player player, final boolean game, final boolean save) {
        handler.runTask(() -> {
            for (final Player p : players) {
                p.showPlayer(player);
            }
            player.setGameMode(GameMode.SURVIVAL);

            for (final PotionEffect potionEffect : player.getActivePotionEffects()) {
                player.removePotionEffect(potionEffect.getType());
            }
        });

        setGhost(player, false);

        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setHealth(player.getMaxHealth());

        if (!players.contains(player)) players.add(player);

        if (save) saveInventory(player);

        clearInventory(player);

        if (game) {
            final PlayerInventory inventory = player.getInventory();

            inventory.setItem(8, createItemStack(Material.BED, "§cLeave", null, 1, false));
            return;
        }

        ConfigurationSection section = handler.getDataSection("spawn");

        if (section != null) {
            final SWLocation location = new SWLocation(section);

            if (location.location != null) handler.runTask(() -> player.teleport(location.location));

            if (Boolean.parseBoolean(handler.getConfig("scoreboard.lobby.enabled", false).toString())) {
                section = handler.getConfigSection("scoreboard.lobby");

                final List<String> list = section.getStringList("lines");

                String s;
                for (int i = 0; i < list.size(); i++) {
                    s = replaceStringWithData(player.getUniqueId(), list.get(i)).replace("%player%", player.getName());
                    list.set(i, ChatColor.translateAlternateColorCodes('&', s));
                }

                newScoreboard(player, ChatColor.translateAlternateColorCodes('&', section.getString("title")), list);
            }
        }
    }

    public void join(final Player player, String game) {
        if (lookingForGame.contains(player.getUniqueId())) {
            player.sendMessage("§cYou're already looking for an arena!");
            return;
        }

        final File file = new File(FalconSkyWars.INSTANCE.getDataFolder(), "games");

        if (file.exists()) {
            final File[] files = file.listFiles();

            if (files != null) {
                final Game target = game == null ? getGameByName(files[(files.length == 1 ? 0 : new Random().nextInt(files.length - 1))].toString().toLowerCase().replace(".yml", "")) : game.contains("/") ? null : getGameByName(game.toLowerCase());

                if (target == null) {
                    player.sendMessage("§cThere is no arena with that name!");
                    return;
                }

                game = target.name;

                player.sendMessage("§7Finding a game for you...");

                lookingForGame.add(player.getUniqueId());

                if (underMaking.contains(game)) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            lookingForGame.remove(player.getUniqueId());

                            join(player, target.name);
                        }
                    }, 250);
                    return;
                }

                if (games.containsKey(game)) {
                    for (final Game choice : games.get(game)) {
                        if (choice.status == Status.WAITING || choice.status == Status.STARTING) {
                            choice.join(player);
                            lookingForGame.remove(player.getUniqueId());
                            return;
                        }
                    }

                    underMaking.add(game);

                    final Game[] newGame = new Game[1];
                    final String finalGame = game;
                    handler.runTask(() -> {
                        newGame[0] = createNewArena(finalGame);

                        if (newGame[0] == null) {
                            underMaking.remove(finalGame);
                            lookingForGame.remove(player.getUniqueId());
                            player.sendMessage("§cAn error has occurred (1)");
                            return;
                        }

                        new Thread(() -> new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                final List<Game> arenas = games.get(target.name);

                                arenas.add(newGame[0]);

                                games.put(target.name, arenas);

                                newGame[0].join(player);

                                underMaking.remove(target.name);
                                lookingForGame.remove(player.getUniqueId());
                            }
                        }, 250)).start();
                    });

                    return;
                }

                underMaking.add(game);

                final Game[] newGame = new Game[1];
                final String finalGame = game;
                handler.runTask(() -> {
                    newGame[0] = createNewArena(finalGame);

                    if (newGame[0] == null) {
                        underMaking.remove(finalGame);
                        lookingForGame.remove(player.getUniqueId());
                        player.sendMessage("§cAn error has occurred (1)");
                        return;
                    }

                    new Thread(() -> new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            games.put(target.name, Collections.singletonList(newGame[0]));

                            newGame[0].join(player);

                            underMaking.remove(target.name);
                            lookingForGame.remove(player.getUniqueId());
                        }
                    }, 250)).start();
                });
                return;
            }
            player.sendMessage("§cIt seems like there are no arenas available yet, please wait or contact one of the admins.");
        }
    }

    public void leave(final Player player) {
        if (!players.contains(player)) {
            player.sendMessage("§cYou're not even in SkyWars");
            return;
        }

        if (!inGamePlayers.containsKey(player.getUniqueId())) {
            players.remove(player);

            restoreInventory(player);

            scoreboards.get(player.getUniqueId()).delete();
            scoreboards.remove(player.getUniqueId());
            return;
        }

        final Game game = inGamePlayers.get(player.getUniqueId());

        game.leave(player);
    }

    public void setGhost(final Player player, final boolean option) {
        if (option) {
            handler.runTask(() -> {
                player.setAllowFlight(true);
                player.setFlying(true);
                player.setScoreboard(handler.gameHandler.board);
                handler.gameHandler.team.addEntry(player.getName());
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 4, true, false));
            });
            return;
        }

        handler.runTask(() -> {
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            team.removeEntry(player.getName());
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        });
    }

    public final Game createNewArena(final String arena) {
        if (arena == null) return null;

        Game game = getGameByName(arena.toLowerCase());

        if (game == null) return null;

        final String code = game.spawn.getWorld().getName() + "_" + UUID.randomUUID().toString().split("-")[0];
        try {
            File file = new File("swArenas");

            if (!file.exists()) file.mkdirs();

            file = new File("swArenas/" + code);

            FileUtils.copyDirectory(new File(game.spawn.getWorld().getName()), file);

            new File(file, "uid.dat").delete();

            return new Game(arena, handler, Bukkit.createWorld(new WorldCreator("swArenas/" + code)), 2, YamlConfiguration.loadConfiguration(new File(FalconSkyWars.INSTANCE.getDataFolder(), "games/" + arena + ".yml")));
        } catch (final IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public final Game getGameByName(final String gameName) {
        return originals.getOrDefault(gameName, null);
    }

    public void sendTitle(final Player player, final String title, final String subTitle) {
        PacketPlayOutTitle packet;
        final PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
        if (title != null) {
            packet = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, new ChatComponentText(title), 1, 5, 1);
            playerConnection.sendPacket(packet);
        }
        if (subTitle != null) {
            packet = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, new ChatComponentText(subTitle), 1, 5, 1);
            playerConnection.sendPacket(packet);
        }
    }

    public void sendActionbar(final Player player, final String message) {
        if (player == null || message == null) return;

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(new ChatComponentText(message), (byte) 2));
    }

    public void newScoreboard(final Player player, final String title, final List<String> lines) {
        new Thread(() -> {
            FastBoard scoreboard = scoreboards.getOrDefault(player.getUniqueId(), null);

            if (scoreboard == null) {
                scoreboard = new FastBoard(player);
                scoreboards.put(player.getUniqueId(), scoreboard);
            }

            scoreboard.updateTitle(title);
            scoreboard.updateLines(lines);
        }).start();
    }

    public void broadcast(final String message) {
        for (final Player player : players) {
            player.sendMessage(message);
        }
    }

    public void resetChests(final Game game) {
        LootItem lootItem;
        org.bukkit.inventory.ItemStack itemStack;
        Inventory inventory;

        boolean b;
        final Set<LootItem> used = new HashSet<>();

        for (final Chest chest : game.chests.keySet()) {
            used.clear();
            final ThreadLocalRandom random = ThreadLocalRandom.current();

            b = false;

            inventory = chest.getBlockInventory();

            inventory.clear();

            for (int index = 0; index < inventory.getSize(); index++) {
                lootItem = game.chests.get(chest) ? middleLootItems.get(random.nextInt(middleLootItems.size())) : normalLootItems.get(random.nextInt(normalLootItems.size()));

                if (used.contains(lootItem)) continue;

                used.add(lootItem);

                if (lootItem.shouldFill(random)) {
                    itemStack = lootItem.make(random);

                    inventory.setItem(index, itemStack);

                    if (itemStack.getType().equals(Material.BOW)) b = true;
                }
            }

            if (b) {
                int i = 0;
                for (final org.bukkit.inventory.ItemStack item : inventory.getContents()) {
                    if (item == null || item.getType().equals(Material.AIR)) {
                        itemStack = new org.bukkit.inventory.ItemStack(Material.ARROW);
                        itemStack.setAmount(16);
                        inventory.setItem(i, itemStack);
                        break;
                    }
                    i++;
                }
            }
        }
    }

    public void addWin(final UUID uuid) {
        handler.writeData("players." + uuid + ".wins", getWins(uuid)+1);
    }

    public void addKill(final UUID uuid) {
        handler.writeData("players." + uuid + ".kills", getKills(uuid)+1);
    }

    public void addDeath(final UUID uuid) {
        handler.writeData("players." + uuid + ".deaths", getDeaths(uuid)+1);
    }

    public void addCoins(final UUID uuid, final int amount) {
        handler.writeData("players." + uuid + ".coins", getCoins(uuid)+amount);
    }

    public void addGamePlayed(final UUID uuid) {
        handler.writeData("players." + uuid + ".coins", getGamesPlayed(uuid)+1);
    }

    public final int getWins(final UUID uuid) {
        final Object o = handler.getData("players." + uuid + ".wins");
        return o instanceof Integer ? (Integer) o : 0;
    }

    public final int getKills(final UUID uuid) {
        final Object o = handler.getData("players." + uuid + ".kills");
        return o instanceof Integer ? (Integer) o : 0;
    }

    public final int getDeaths(final UUID uuid) {
        final Object o = handler.getData("players." + uuid + ".deaths");
        return o instanceof Integer ? (Integer) o : 0;
    }

    public final int getCoins(final UUID uuid) {
        final Object o = handler.getData("players." + uuid + ".coins");
        return o instanceof Integer ? (Integer) o : 0;
    }

    public final int getGamesPlayed(final UUID uuid) {
        final Object o = handler.getData("players." + uuid + ".gamesPlayed");
        return o instanceof Integer ? (Integer) o : 0;
    }


    // Internal Functions

    public String replaceStringWithData(final UUID uuid, final String input) {
        return input.replace("%kills%", String.valueOf(getKills(uuid)))
                .replace("%coins%", String.valueOf(getCoins(uuid)))
                .replace("%gamesPlayed%", String.valueOf(getGamesPlayed(uuid)))
                .replace("%deaths%", String.valueOf(getDeaths(uuid)))
                .replace("%wins%", String.valueOf(getWins(uuid)));
    }

    private org.bukkit.inventory.ItemStack createItemStack(final org.bukkit.Material material, final String name, final List<String> lore, int amount, boolean unbreakable) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material, amount);
        final ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            if (name != null)
                meta.setDisplayName(name);

            if (lore != null)
                meta.setLore(lore);

            item.setItemMeta(meta);
        }

        if (unbreakable) {
            final net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
            final NBTTagCompound tag = nmsStack.getTag();
            tag.setBoolean("Unbreakable", true);
            nmsStack.setTag(tag);
            item = CraftItemStack.asBukkitCopy(nmsStack);
        }
        return item;
    }

    private void saveInventory(final Player player) {
        savedInventories.put(player.getUniqueId(), new SavedInventory(player, true));
    }

    private void restoreInventory(final Player player) {
        if (savedInventories.containsKey(player.getUniqueId())) savedInventories.get(player.getUniqueId()).restoreInventory();
    }

    private void clearInventory(final Player player) {
        final PlayerInventory inventory = player.getInventory();

        inventory.clear();

        inventory.setHelmet(null);
        inventory.setChestplate(null);
        inventory.setLeggings(null);
        inventory.setBoots(null);
    }

}
