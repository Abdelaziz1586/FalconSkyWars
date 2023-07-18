package me.pebbleprojects.falconskywars.listeners;

import me.pebbleprojects.falconskywars.handlers.GameHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuit implements Listener {

    private final GameHandler gameHandler;

    public PlayerQuit(final GameHandler gameHandler) {
        this.gameHandler = gameHandler;
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        new Thread(() -> {
            if (gameHandler.players.contains(event.getPlayer())) gameHandler.leave(event.getPlayer());
        }).start();
    }

}
