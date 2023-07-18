package me.pebbleprojects.falconskywars;

import me.pebbleprojects.falconskywars.handlers.Handler;
import org.bukkit.plugin.java.JavaPlugin;

public final class FalconSkyWars extends JavaPlugin {

    private Handler handler;
    public static FalconSkyWars INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;

        new Thread(() -> handler = new Handler()).start();
    }

    @Override
    public void onDisable() {
        handler.shutdown();
    }
}
