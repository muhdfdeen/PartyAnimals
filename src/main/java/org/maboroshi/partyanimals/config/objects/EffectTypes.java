package org.maboroshi.partyanimals.config.objects;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import java.util.HashMap;
import java.util.Map;

public final class EffectTypes {

    @Configuration
    public static class ParticleOffset {
        public double x = 0.0;
        public double y = 0.0;
        public double z = 0.0;

        public ParticleOffset() {}

        public ParticleOffset(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    @Configuration
    public static class SoundEffect {
        public String type = "entity.experience_orb.pickup";
        public float volume = 1.0f;
        public float pitch = 1.0f;

        public SoundEffect() {}

        public SoundEffect(String type, float volume, float pitch) {
            this.type = type;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    @Configuration
    public static class ParticleEffect {
        public String type = "VILLAGER_HAPPY";
        public int count = 10;
        public ParticleOffset offset = new ParticleOffset();

        @Comment("Speed of particles. 0 = still, 1 = fast.")
        public double speed = 0.1;

        public ParticleEffect() {}

        public ParticleEffect(String type, int count, ParticleOffset offset, double speed) {
            this.type = type;
            this.count = count;
            this.offset = offset;
            this.speed = speed;
        }
    }

    @Configuration
    public static class EffectGroup {
        public Map<String, SoundEffect> sounds = new HashMap<>();
        public Map<String, ParticleEffect> particles = new HashMap<>();

        public EffectGroup() {}

        public EffectGroup(Map<String, SoundEffect> sounds, Map<String, ParticleEffect> particles) {
            if (sounds != null) this.sounds = sounds;
            if (particles != null) this.particles = particles;
        }
    }
}
