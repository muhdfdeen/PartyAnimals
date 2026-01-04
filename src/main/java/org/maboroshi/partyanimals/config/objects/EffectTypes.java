package org.maboroshi.partyanimals.config.objects;

import java.util.List;
import de.exlll.configlib.Comment;

public final class EffectTypes {

    public record ParticleOffset(double x, double y, double z) {}

    public record SoundEffect(String type, float volume, float pitch) {}

    public record ParticleEffect(
        String type, 
        int count, 
        ParticleOffset offset, 
        @Comment("Speed of particles. 0 = still, 1 = fast.") double speed
    ) {}

    public record EffectGroup(
        List<SoundEffect> sounds, 
        List<ParticleEffect> particles
    ) {}
}
