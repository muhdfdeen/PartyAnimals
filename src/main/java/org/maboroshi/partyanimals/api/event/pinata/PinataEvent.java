package org.maboroshi.partyanimals.api.event.pinata;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.maboroshi.partyanimals.api.event.PartyAnimalsEvent;

public abstract class PinataEvent extends Event implements PartyAnimalsEvent {
    protected final LivingEntity pinata;

    protected PinataEvent(LivingEntity pinata) {
        this.pinata = pinata;
    }
    
    public LivingEntity getPinata() {
        return pinata;
    }
}
