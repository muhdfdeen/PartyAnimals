package com.muhdfdeen.partyanimals.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import de.exlll.configlib.Configuration;

public class LocationSerializer {
    @Configuration
    public static class SerializableLocation {
        public String world = "world";
        public double x = 0;
        public double y = 100;
        public double z = 0;
        public float yaw = 0;
        public float pitch = 0;

        public SerializableLocation() {
        }

        public SerializableLocation(Location location) {
            this.world = location.getWorld().getName();
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.yaw = location.getYaw();
            this.pitch = location.getPitch();
        }

        public Location toBukkit() {
            return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
        }
    }
}
