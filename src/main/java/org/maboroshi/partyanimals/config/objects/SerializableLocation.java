package org.maboroshi.partyanimals.config.objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import de.exlll.configlib.Configuration;

@Configuration
public class SerializableLocation {
    public String world = "world";
    public double x = 0;
    public double y = 100;
    public double z = 0;
    public float yaw = 0;
    public float pitch = 0;

    public SerializableLocation() {
    }

    public SerializableLocation(Location location) {
        if (location != null && location.getWorld() != null) {
            this.world = location.getWorld().getName();
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.yaw = location.getYaw();
            this.pitch = location.getPitch();
        }
    }

    public Location toBukkit() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            bukkitWorld = Bukkit.getWorlds().get(0); 
        }
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }
}
