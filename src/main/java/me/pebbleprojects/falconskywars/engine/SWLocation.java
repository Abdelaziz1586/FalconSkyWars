package me.pebbleprojects.falconskywars.engine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class SWLocation {

    public Location location;

    public SWLocation(final ConfigurationSection section) {

        try {
            final World world = Bukkit.getWorld(section.getString("world"));

            final double x = section.getDouble("x"),
                    y = section.getDouble("y"),
                    z = section.getDouble("z");

            final float yaw = (float) section.getDouble("yaw"),
                    pitch = (float) section.getDouble("pitch");

            location = new Location(world, x, y, z, yaw, pitch);
        } catch (final Exception e) {
            location = null;
            e.printStackTrace();
        }

    }

}
