package org.maboroshi.partyanimals.handler;

import org.bukkit.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;

public class ReflexHandler {
    private final PartyAnimals plugin;
    private final EffectHandler effectHandler;

    public ReflexHandler(PartyAnimals plugin) {
        this.plugin = plugin;
        this.effectHandler = plugin.getEffectHandler();
    }

    public void onDamage(LivingEntity pinata, Player attacker, PinataConfiguration config) {
        if (!config.behavior.enabled) return;

        var shockwave = config.behavior.reflexes.shockwave;
        if (shockwave.enabled && shouldTrigger(shockwave.chance)) {
            effectHandler.playEffects(shockwave.effects, pinata.getLocation(), false);
            pinata.getNearbyEntities(shockwave.radius, shockwave.radius, shockwave.radius).forEach(entity -> {
                if (entity instanceof Player player && !player.equals(pinata)) {
                    Vector direction = player.getLocation().toVector()
                            .subtract(pinata.getLocation().toVector());
                    if (direction.lengthSquared() < 0.01) {
                        direction = new Vector(0, 0.5, 0);
                    } else {
                        direction.normalize().multiply(shockwave.strength).setY(0.5);
                    }
                    player.setVelocity(direction);
                }
            });
        }

        var morph = config.behavior.reflexes.morph;
        if (morph.enabled && shouldTrigger(morph.chance)) {
            effectHandler.playEffects(morph.effects, pinata.getLocation(), false);
            
            if (morph.baby) {
                if (pinata instanceof Ageable ageable) {
                    ageable.setBaby();
                    pinata.getScheduler().runDelayed(plugin, (task) -> {
                        if (pinata.isValid()) {
                            ageable.setAdult();
                        }
                    }, null, morph.duration);
                }
            } else {
                var scaleAttribute = pinata.getAttribute(Attribute.SCALE);
                if (scaleAttribute != null) {
                    var originalScale = scaleAttribute.getBaseValue();
                    double min = Math.min(morph.scale.min, morph.scale.max);
                    double max = Math.max(morph.scale.min, morph.scale.max);
                    double newScale = ThreadLocalRandom.current().nextDouble(min, max);
                    
                    scaleAttribute.setBaseValue(newScale);

                    pinata.getScheduler().runDelayed(plugin, (task) -> {
                        if (pinata.isValid()) {
                            scaleAttribute.setBaseValue(originalScale);
                        }
                    }, null, morph.duration);
                }
            }   
        }

        var blink = config.behavior.reflexes.blink;
        if (blink.enabled && shouldTrigger(blink.chance)) {
            effectHandler.playEffects(blink.effects, pinata.getLocation(), false);
            
            Location loc = pinata.getLocation();
            double x = loc.getX() + (ThreadLocalRandom.current().nextDouble() - 0.5) * blink.distance * 2;
            double z = loc.getZ() + (ThreadLocalRandom.current().nextDouble() - 0.5) * blink.distance * 2;
            double y;

            if (blink.ignoreYLevel) {
                 y = pinata.getWorld().getHighestBlockYAt((int)x, (int)z) + 1;
            } else {
                 y = loc.getY(); 
            }
            
            Location target = new Location(pinata.getWorld(), x, y, z);
            if (!target.getBlock().isPassable()) return;
            pinata.teleport(target);
        }

        var leap = config.behavior.reflexes.leap;
        if (leap.enabled && shouldTrigger(leap.chance)) {
            effectHandler.playEffects(leap.effects, pinata.getLocation(), false);
            pinata.setVelocity(new Vector(0, leap.strength, 0));
            pinata.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false));
        }

        var sugarRush = config.behavior.reflexes.sugarRush;
        if (sugarRush.enabled && shouldTrigger(sugarRush.chance)) {
            effectHandler.playEffects(sugarRush.effects, pinata.getLocation(), false);
            pinata.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, sugarRush.duration, sugarRush.amplifier));
        }

        var dazzle = config.behavior.reflexes.dazzle;
        if (dazzle.enabled && shouldTrigger(dazzle.chance)) {
            effectHandler.playEffects(dazzle.effects, attacker.getEyeLocation(), false);
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, dazzle.duration, 0));
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, dazzle.duration, 0));
            pinata.getWorld().spawnParticle(Particle.GLOW, attacker.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.1);
        }
    }

    public boolean shouldTrigger(double chance) {
        return ThreadLocalRandom.current().nextDouble(100.0) < chance;
    }
}
