package me.pebbleprojects.falconskywars.listeners;

import me.pebbleprojects.falconskywars.handlers.GameHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class FoodLevelChange implements Listener {

    private final GameHandler gameHandler;

    public FoodLevelChange(final GameHandler gameHandler) {
        this.gameHandler = gameHandler;
    }

    @EventHandler
    public void onFoodLevelChange(final FoodLevelChangeEvent event) {
        final Player player = (Player) event.getEntity();

        if (gameHandler.players.contains(player) && !gameHandler.inGamePlayers.containsKey(player.getUniqueId())) event.setCancelled(true);
    }

}
