package org.maboroshi.partyanimals.api.event.pinata;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class PinataSpawnEvent extends PinataEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Location spawnLocation;
    private boolean cancelled = false;

    public PinataSpawnEvent(LivingEntity pinata, Location spawnLocation) {
        super(pinata);
        this.spawnLocation = spawnLocation;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
