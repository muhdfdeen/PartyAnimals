package org.maboroshi.partyanimals.config.objects.effects;

import de.exlll.configlib.Configuration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class EffectGroup {
    public Map<String, SoundEffect> sounds = new HashMap<>();
    public Map<String, ParticleEffect> particles = new HashMap<>();

    public EffectGroup() {}

    public EffectGroup(Map<String, SoundEffect> sounds, Map<String, ParticleEffect> particles) {
        if (sounds != null) this.sounds = sounds;
        if (particles != null) this.particles = particles;
    }
}
