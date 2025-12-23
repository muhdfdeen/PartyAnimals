package com.muhdfdeen.partyanimals.handler;

import com.muhdfdeen.partyanimals.config.settings.PinataConfig.EffectGroup;
import com.muhdfdeen.partyanimals.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class EffectHandler {

    private final Logger log;

    public EffectHandler(Logger log) {
        this.log = log;
    }

    public void playEffects(EffectGroup effect, Location location, boolean globalSound) {
        if (effect == null) return;

        if (effect.sound() != null) {
            String soundType = effect.sound().type();
            float volume = effect.sound().volume();
            float pitch = effect.sound().pitch();

            if (soundType != null && !soundType.isEmpty()) {
                if (globalSound) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), soundType, volume, pitch);
                    }
                } else {
                    if (location != null && location.getWorld() != null) {
                        location.getWorld().playSound(location, soundType, volume, pitch);
                    }
                }
            }
        }

        if (location == null || location.getWorld() == null) return;

        if (effect.particle() != null) {
            String particleType = effect.particle().type();
            int count = effect.particle().count();

            if (particleType != null && !particleType.isEmpty()) {
                try {
                    Particle particle = Particle.valueOf(particleType.toUpperCase());
                    location.getWorld().spawnParticle(particle, location.clone().add(0, 1, 0), count);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid particle type in config: " + particleType);
                }
            }
        }
    }
}
