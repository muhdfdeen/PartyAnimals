package org.maboroshi.partyanimals.behavior;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import java.util.EnumSet;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;

public class PinataFleeGoal implements Goal<Mob> {
    private final PinataConfiguration config;
    private final Mob mob;
    private final double speed;
    private final double checkRadius;
    private Player targetPlayer;
    private final GoalKey<Mob> key;

    public PinataFleeGoal(PartyAnimals plugin, Mob mob) {
        this.mob = mob;
        this.config = plugin.getPinataManager().getPinataConfig(mob);
        this.speed = config.behavior.movement.speed;
        this.checkRadius = Math.max(config.behavior.movement.radius.x, 5.0);
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "pinata_flee"));
    }

    @Override
    public boolean shouldActivate() {
        targetPlayer = null;
        double closestDist = Double.MAX_VALUE;

        for (Player p : mob.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) continue;
            if (p.getLocation().distanceSquared(mob.getLocation()) > (checkRadius * checkRadius)) continue;

            double distSq = p.getLocation().distanceSquared(mob.getLocation());
            if (distSq < (checkRadius * checkRadius) && distSq < closestDist) {
                closestDist = distSq;
                targetPlayer = p;
            }
        }

        return targetPlayer != null;
    }

    @Override
    public boolean shouldStayActive() {
        if (targetPlayer == null || !targetPlayer.isValid()) return false;
        Pathfinder.PathResult path = mob.getPathfinder().getCurrentPath();
        if (path == null || path.getNextPoint() == null) return false;
        return mob.getLocation().distanceSquared(targetPlayer.getLocation()) < (checkRadius * checkRadius * 1.5);
    }

    @Override
    public void start() {
        runAway();
    }

    @Override
    public void tick() {
        if (mob.getTicksLived() % 10 == 0) {
            runAway();
        }
    }

    private void runAway() {
        if (targetPlayer == null) return;

        Vector direction =
                mob.getLocation().toVector().subtract(targetPlayer.getLocation().toVector());

        if (direction.lengthSquared() < 0.01) {
            direction = new Vector(1, 0, 0);
        } else {
            direction.normalize();
        }

        Location targetLoc = mob.getLocation().add(direction.multiply(5));
        targetLoc.setY(mob.getLocation().getY());

        if (isSafeLocation(targetLoc)) {
            mob.getPathfinder().moveTo(targetLoc, speed);
        }
    }

    private boolean isSafeLocation(Location location) {
        if (!location.getBlock().isPassable()) return false;

        double currentHeight = mob.getHeight();
        int blocksToCheck = (int) Math.ceil(currentHeight);

        for (int i = 1; i < blocksToCheck; i++) {
            if (!location.clone().add(0, i, 0).getBlock().isPassable()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}
