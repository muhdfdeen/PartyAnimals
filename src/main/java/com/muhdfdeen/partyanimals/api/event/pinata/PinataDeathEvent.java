package com.muhdfdeen.partyanimals.api.event.pinata;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PinataDeathEvent extends PinataEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Player killer;

    public PinataDeathEvent(LivingEntity pinata, Player killer) {
        super(pinata);
        this.killer = killer;
    }

    public Player getKiller() {
        return killer;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

}
