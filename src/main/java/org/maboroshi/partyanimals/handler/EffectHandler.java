package org.maboroshi.partyanimals.handler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.maboroshi.partyanimals.config.objects.EffectTypes.*;
import org.maboroshi.partyanimals.util.Logger;

public class EffectHandler {

    private final Logger log;

    public EffectHandler(Logger log) {
        this.log = log;
    }

    public void playEffects(EffectGroup group, Location location, boolean globalSound) {
        if (group == null) return;

        if (group.sounds != null && !group.sounds.isEmpty()) {
            for (SoundEffect sound : group.sounds.values()) {
                playSound(sound, location, globalSound);
            }
        }

        if (location == null || location.getWorld() == null) return;

        if (group.particles != null && !group.particles.isEmpty()) {
            for (ParticleEffect particle : group.particles.values()) {
                playParticle(particle, location);
            }
        }
    }

    private void playSound(SoundEffect sound, Location location, boolean globalSound) {
        if (sound.type == null || sound.type.isEmpty()) return;

        try {
            if (globalSound) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound.type, sound.volume, sound.pitch);
                }
            } else {
                if (location != null && location.getWorld() != null) {
                    location.getWorld().playSound(location, sound.type, sound.volume, sound.pitch);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to play sound: " + sound.type);
        }
    }

    private void playParticle(ParticleEffect particleData, Location location) {
        String particleType = particleData.type;
        if (particleType == null || particleType.isEmpty()) return;

        try {
            Particle particle = Particle.valueOf(particleType.toUpperCase());

            double speed = particleData.speed;
            double offX = 0.5;
            double offY = 0.5;
            double offZ = 0.5;

            if (particleData.offset != null) {
                offX = particleData.offset.x;
                offY = particleData.offset.y;
                offZ = particleData.offset.z;
            }

            location.getWorld()
                    .spawnParticle(
                            particle, location.clone().add(0, 1.0, 0), particleData.count, offX, offY, offZ, speed);
        } catch (IllegalArgumentException e) {
            log.error("Invalid particle type in config: " + particleType);
        }
    }
}
