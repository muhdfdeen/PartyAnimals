package org.maboroshi.partyanimals.behavior;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

public class PinataFloatGoal implements Goal<Creature> {
    private final Creature mob;
    private final GoalKey<Creature> key;
    private Vector cachedLandDirection = null;
    private int cooldown = 0;

    public PinataFloatGoal(Plugin plugin, Creature mob) {
        this.mob = mob;
        this.key = GoalKey.of(Creature.class, new NamespacedKey(plugin, "pinata_float"));
    }

    @Override
    public boolean shouldActivate() {
        return mob.isInWater() || mob.getEyeLocation().getBlock().isLiquid();
    }

    @Override
    public void tick() {
        if (ThreadLocalRandom.current().nextFloat() < 0.8F) {
            Vector velocity = mob.getVelocity();
            if (velocity.getY() < 0.1) {
                velocity.setY(0.15); 
            }
            Vector dir = getDirectionToLand();
            dir = avoidWalls(dir);
            velocity.add(dir);
            mob.setVelocity(velocity);
            if (dir.getX() != 0 || dir.getZ() != 0) {
                Location loc = mob.getLocation();
                loc.setDirection(dir);
                mob.setRotation(loc.getYaw(), loc.getPitch());
            }
        }
    }

    private Vector avoidWalls(Vector intendedDir) {
        Location eyeLoc = mob.getEyeLocation();
        Vector checkDir = intendedDir.clone().normalize();
        if (isBlocked(eyeLoc, checkDir)) {
            double[] angles = {45, -45, 90, -90, 135, -135, 180};
            for (double angle : angles) {
                Vector rotated = checkDir.clone().rotateAroundY(Math.toRadians(angle));
                if (!isBlocked(eyeLoc, rotated)) {
                    return rotated.multiply(0.15);
                }
            }
        }
        return intendedDir;
    }

    private boolean isBlocked(Location origin, Vector direction) {
        return origin.clone().add(direction).getBlock().getType().isSolid();
    }

    private Vector getDirectionToLand() {
        if (cooldown-- > 0 && cachedLandDirection != null) {
            return cachedLandDirection;
        }
        cooldown = 20;
        Location start = mob.getLocation();
        Block nearestLand = null;
        double nearestDistSq = Double.MAX_VALUE;
        int radius = 8;
        for (int x = -radius; x <= radius; x += 2) { 
            for (int z = -radius; z <= radius; z += 2) {
                Block b = start.getWorld().getBlockAt(start.getBlockX() + x, start.getBlockY(), start.getBlockZ() + z);
                if (b.getType().isSolid() && !b.isLiquid()) {
                    double distSq = b.getLocation().distanceSquared(start);
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearestLand = b;
                    }
                }
            }
        }
        if (nearestLand != null) {
            cachedLandDirection = nearestLand.getLocation().subtract(start).toVector();
            cachedLandDirection.setY(0); 
            cachedLandDirection.normalize().multiply(0.15); 
        } else {
            cachedLandDirection = start.getDirection().setY(0).normalize().multiply(0.1);
        }
        return cachedLandDirection;
    }

    @Override
    public GoalKey<Creature> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.JUMP, GoalType.MOVE);
    }
}
