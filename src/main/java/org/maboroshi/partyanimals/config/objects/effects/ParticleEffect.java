package org.maboroshi.partyanimals.config.objects.effects;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;

@Configuration
public class ParticleEffect {
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
