package me.pebbleprojects.falconskywars.listeners;

import me.pebbleprojects.falconskywars.handlers.GameHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeath implements Listener {

    private final GameHandler gameHandler;

    public PlayerDeath(final GameHandler gameHandler) {
        this.gameHandler = gameHandler;
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();

        if (gameHandler.players.contains(player)) event.setDeathMessage("");
    }

}
