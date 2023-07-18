package me.pebbleprojects.falconskywars.listeners;

import me.pebbleprojects.falconskywars.handlers.GameHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropItem implements Listener {

    private final GameHandler gameHandler;

    public PlayerDropItem(final GameHandler gameHandler) {
        this.gameHandler = gameHandler;
    }

    @EventHandler
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        if (gameHandler.players.contains(player) && !gameHandler.inGamePlayers.containsKey(player.getUniqueId())) event.setCancelled(true);
    }

}
