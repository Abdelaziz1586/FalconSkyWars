package me.pebbleprojects.falconskywars.listeners;

import me.pebbleprojects.falconskywars.engine.game.Status;
import me.pebbleprojects.falconskywars.handlers.GameHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreak implements Listener {

    private final GameHandler gameHandler;

    public BlockBreak(final GameHandler gameHandler) {
        this.gameHandler = gameHandler;
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {

        final Player player = event.getPlayer();

        if (gameHandler.players.contains(player) && !gameHandler.inGamePlayers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (gameHandler.inGamePlayers.containsKey(player.getUniqueId()) && gameHandler.inGamePlayers.get(player.getUniqueId()).status != Status.PLAYING) {
            event.setCancelled(true);
        }
    }

}
