package org.maboroshi.partyanimals.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PartyAnimalsReloadEvent extends Event implements PartyAnimalsEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
