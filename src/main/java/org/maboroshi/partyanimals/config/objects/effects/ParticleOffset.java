package org.maboroshi.partyanimals.config.objects.effects;

import de.exlll.configlib.Configuration;

@Configuration
public class ParticleOffset {
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
