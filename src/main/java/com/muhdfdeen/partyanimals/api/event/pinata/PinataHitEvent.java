package com.muhdfdeen.partyanimals.api.event.pinata;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class PinataHitEvent extends PinataEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Player attacker;
    private boolean cancelled = false;

    public PinataHitEvent(LivingEntity pinata, Player attacker) {
        super(pinata);
        this.attacker = attacker;
    }

    public Player getAttacker() {
        return attacker;
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
