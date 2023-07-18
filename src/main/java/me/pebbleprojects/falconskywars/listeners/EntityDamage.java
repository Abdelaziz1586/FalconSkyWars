package me.pebbleprojects.falconskywars.listeners;

import me.pebbleprojects.falconskywars.engine.game.Game;
import me.pebbleprojects.falconskywars.engine.game.Status;
import me.pebbleprojects.falconskywars.handlers.Handler;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamage implements Listener {

    private final Handler handler;

    public EntityDamage(final Handler handler) {
        this.handler = handler;
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        handler.runTask(() -> {

            Entity entity = event.getEntity();

            if (entity instanceof Player) {
                final Player player = (Player) entity;

                if (handler.gameHandler.players.contains(player) && !handler.gameHandler.inGamePlayers.containsKey(player.getUniqueId())) {
                    event.setCancelled(true);

                    final EntityDamageEvent.DamageCause cause = event.getCause();

                    if (cause.equals(EntityDamageEvent.DamageCause.VOID))
                        handler.gameHandler.getPlayerReady(player, false, false);
                    return;
                }

                if (handler.gameHandler.inGamePlayers.containsKey(player.getUniqueId())) {

                    final Game game = handler.gameHandler.inGamePlayers.get(player.getUniqueId());

                    if (game.status != Status.PLAYING) {
                        event.setCancelled(true);
                        return;
                    }

                    Player attacker = null;

                    if (event instanceof EntityDamageByEntityEvent) {
                        entity = ((EntityDamageByEntityEvent) event).getDamager();
                        if (entity instanceof Player) {
                            attacker = (Player) entity;
                            if (!handler.gameHandler.inGamePlayers.containsKey(attacker.getUniqueId())) {
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }

                    if (player.getHealth() <= event.getDamage())
                        handler.gameHandler.inGamePlayers.get(player.getUniqueId()).death(player, attacker);
                }
            }
        });
    }

}
