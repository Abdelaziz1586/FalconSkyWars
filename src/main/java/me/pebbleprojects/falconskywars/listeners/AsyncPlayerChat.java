package me.pebbleprojects.falconskywars.listeners;

import me.pebbleprojects.falconskywars.handlers.GameHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class AsyncPlayerChat implements Listener {

    private final GameHandler gameHandler;

    public AsyncPlayerChat(final GameHandler gameHandler) {
        this.gameHandler = gameHandler;
    }

    @EventHandler
    public void onAsyncPlayerChat(final AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();

        if (gameHandler.inGamePlayers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            new Thread(() -> gameHandler.inGamePlayers.get(player.getUniqueId()).broadcast(event.getMessage())).start();
            return;
        }

        if (gameHandler.players.contains(player)) {
            new Thread(() -> {
                event.getRecipients().clear();

                for (final Player p : gameHandler.players) {
                    if (!gameHandler.inGamePlayers.containsKey(p.getUniqueId())) {
                        event.getRecipients().add(p);
                    }
                }
            }).start();
            return;
        }

        new Thread(() -> {
            for (final Player p : gameHandler.players) {
                event.getRecipients().remove(p);
            }
        }).start();
    }

}
