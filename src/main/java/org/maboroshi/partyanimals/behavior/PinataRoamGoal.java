package org.maboroshi.partyanimals.behavior;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;

public class PinataRoamGoal implements Goal<Mob> {
    private final PartyAnimals plugin;
    private final Mob mob;
    private final GoalKey<Mob> key;
    private final double speed;

    public PinataRoamGoal(PartyAnimals plugin, Mob mob) {
        this.plugin = plugin;
        this.mob = mob;
        PinataConfiguration config = plugin.getPinataManager().getPinataConfig(mob);
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "pinata_roam"));
        this.speed = config.behavior.movement.speed;
    }

    @Override
    public boolean shouldActivate() {
        return !mob.getPathfinder().hasPath();
    }

    @Override
    public boolean shouldStayActive() {
        return mob.getPathfinder().hasPath();
    }

    @Override
    public void start() {
        PinataConfiguration config = plugin.getPinataManager().getPinataConfig(mob);

        double rangeX = config.behavior.movement.radius.x;
        int rangeY = (int) config.behavior.movement.radius.y;
        double rangeZ = config.behavior.movement.radius.z;

        double x = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * rangeX;
        double z = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * rangeZ;

        Location currentLoc = mob.getLocation();
        int targetX = currentLoc.getBlockX() + (int) x;
        int targetZ = currentLoc.getBlockZ() + (int) z;
        int currentY = currentLoc.getBlockY();

        Block validTargetBlock = null;

        for (int dy = rangeY; dy >= -rangeY; dy--) {
            Block candidate = mob.getWorld().getBlockAt(targetX, currentY + dy, targetZ);
            Block above = candidate.getRelative(0, 1, 0);
            Block twoAbove = candidate.getRelative(0, 2, 0);

            if (candidate.getType().isSolid()
                    && !candidate.isLiquid()
                    && above.isPassable()
                    && !above.isLiquid()
                    && twoAbove.isPassable()) {

                validTargetBlock = candidate;
                break;
            }
        }

        if (validTargetBlock == null) {
            return;
        }

        Location target = validTargetBlock.getLocation().add(0.5, 1.1, 0.5);
        if (target.getBlock().isPassable()) {
            mob.getPathfinder().moveTo(target, speed);
        }
    }

    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
