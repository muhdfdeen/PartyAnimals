package org.maboroshi.partyanimals.config.objects.effects;

import de.exlll.configlib.Configuration;

@Configuration
public class SoundEffect {
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
