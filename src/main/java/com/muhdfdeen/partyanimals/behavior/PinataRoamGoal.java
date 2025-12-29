package com.muhdfdeen.partyanimals.behavior;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;

public class PinataRoamGoal implements Goal<Creature> {
    private final PartyAnimals plugin;
    private final Creature mob;
    private final GoalKey<Creature> key;
    private final double speed;

    public PinataRoamGoal(PartyAnimals plugin, Creature mob) {
        this.plugin = plugin;
        this.mob = mob;
        PinataConfiguration config = plugin.getPinataManager().getPinataConfig(mob);
        this.key = GoalKey.of(Creature.class, new NamespacedKey(plugin, "pinata_roam"));
        this.speed = config.behavior.movement().speed();
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
        
        double rangeX = config.behavior.movement().wanderRadius().x();
        double rangeZ = config.behavior.movement().wanderRadius().z();

        double x = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * rangeX;
        double z = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * rangeZ;

        Location currentLoc = mob.getLocation();
        int targetX = currentLoc.getBlockX() + (int) x;
        int targetZ = currentLoc.getBlockZ() + (int) z;

        Block targetBlock = mob.getWorld().getHighestBlockAt(targetX, targetZ);
        
        if (Math.abs(targetBlock.getY() - currentLoc.getY()) > 5) {
            return; 
        }

        if (targetBlock.isLiquid() || targetBlock.getRelative(0, 1, 0).isLiquid()) {
            return;
        }

        Location target = targetBlock.getLocation().add(0.5, 1.1, 0.5);
        if (target.getBlock().isPassable()) {
            mob.getPathfinder().moveTo(target, speed);
        }
    }

    @Override
    public GoalKey<Creature> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
