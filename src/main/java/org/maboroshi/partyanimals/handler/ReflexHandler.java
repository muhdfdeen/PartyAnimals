package org.maboroshi.partyanimals.handler;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;

public class ReflexHandler {
    private final PartyAnimals plugin;
    private final EffectHandler effectHandler;
    private final ActionHandler actionHandler;

    public ReflexHandler(PartyAnimals plugin) {
        this.plugin = plugin;
        this.effectHandler = plugin.getEffectHandler();
        this.actionHandler = plugin.getActionHandler();
    }

    public void onDamage(LivingEntity pinata, Player attacker, PinataConfiguration config) {
        if (!config.behavior.enabled) return;

        var shockwave = config.behavior.reflexes.shockwave;
        if (shockwave.enabled && shouldTrigger(shockwave.chance)) {
            effectHandler.playEffects(shockwave.effects, pinata.getLocation(), false);
            pinata.getNearbyEntities(shockwave.radius, shockwave.radius, shockwave.radius)
                    .forEach(entity -> {
                        if (entity instanceof Player player && !player.equals(pinata)) {
                            Vector direction = player.getLocation()
                                    .toVector()
                                    .subtract(pinata.getLocation().toVector());
                            if (direction.lengthSquared() < 0.01) {
                                direction = new Vector(0, shockwave.verticalBoost, 0);
                            } else {
                                direction
                                        .normalize()
                                        .multiply(shockwave.strength)
                                        .setY(shockwave.verticalBoost);
                            }
                            player.setVelocity(direction);
                        }
                    });
            if (!shockwave.commands.isEmpty()) {
                actionHandler.process(attacker, shockwave.commands.values(), cmd -> plugin.getMessageUtils().parsePinataPlaceholders(pinata, cmd));
            }
        }

        var morph = config.behavior.reflexes.morph;
        if (morph.enabled && shouldTrigger(morph.chance)) {
            effectHandler.playEffects(morph.effects, pinata.getLocation(), false);
            if (morph.type.equalsIgnoreCase("AGE")) {
                if (pinata instanceof Ageable ageable) {
                    ageable.setBaby();
                    pinata.getScheduler()
                            .runDelayed(
                                    plugin,
                                    (task) -> {
                                        if (pinata.isValid()) {
                                            ageable.setAdult();
                                        }
                                    },
                                    null,
                                    morph.duration);
                }
            } else if (morph.type.equalsIgnoreCase("SCALE")) {
                var scaleAttribute = pinata.getAttribute(Attribute.SCALE);
                if (scaleAttribute != null) {
                    var originalScale = scaleAttribute.getBaseValue();
                    double min = Math.min(morph.scale.min, morph.scale.max);
                    double max = Math.max(morph.scale.min, morph.scale.max);
                    double newScale = ThreadLocalRandom.current().nextDouble(min, max);

                    scaleAttribute.setBaseValue(newScale);

                    pinata.getScheduler()
                            .runDelayed(
                                    plugin,
                                    (task) -> {
                                        if (pinata.isValid()) {
                                            scaleAttribute.setBaseValue(originalScale);
                                        }
                                    },
                                    null,
                                    morph.duration);
                }
            } else {
                plugin.getPluginLogger().warn("Unknown morph type: " + morph.type);
            }
            if (!morph.commands.isEmpty()) {
                actionHandler.process(attacker, morph.commands.values(), cmd -> plugin.getMessageUtils().parsePinataPlaceholders(pinata, cmd));
            }
        }

        var blink = config.behavior.reflexes.blink;
        if (blink.enabled && shouldTrigger(blink.chance)) {
            effectHandler.playEffects(blink.effects, pinata.getLocation(), false);

            Location location = pinata.getLocation();
            double x = location.getX() + (ThreadLocalRandom.current().nextDouble() - 0.5) * blink.distance * 2;
            double z = location.getZ() + (ThreadLocalRandom.current().nextDouble() - 0.5) * blink.distance * 2;
            Location target = findSafeY(pinata.getWorld(), x, z, location.getY(), 8);

            if (target != null) {
                target.setYaw(location.getYaw());
                target.setPitch(location.getPitch());
                pinata.teleport(target);
                if (!blink.commands.isEmpty()) {
                    actionHandler.process(attacker, blink.commands.values(), cmd -> plugin.getMessageUtils().parsePinataPlaceholders(pinata, cmd));
                }
            }
        }

        var leap = config.behavior.reflexes.leap;
        if (leap.enabled && shouldTrigger(leap.chance)) {
            double blinkDistance = config.behavior.reflexes.blink.distance;
            double thresholdSq = blinkDistance * blinkDistance;
            thresholdSq = Math.max(thresholdSq, 25.0);
            if (pinata.getLocation().distanceSquared(attacker.getLocation()) > thresholdSq) {
                return;
            }
            effectHandler.playEffects(leap.effects, pinata.getLocation(), false);
            pinata.setVelocity(new Vector(0, leap.strength, 0));
            if (!leap.commands.isEmpty()) {
                actionHandler.process(attacker, leap.commands.values(), cmd -> plugin.getMessageUtils().parsePinataPlaceholders(pinata, cmd));
            }
        }

        var sugarRush = config.behavior.reflexes.sugarRush;
        if (sugarRush.enabled && shouldTrigger(sugarRush.chance)) {
            effectHandler.playEffects(sugarRush.effects, pinata.getLocation(), false);
            pinata.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, sugarRush.duration, sugarRush.amplifier));
            if (!sugarRush.commands.isEmpty()) {
                actionHandler.process(attacker, sugarRush.commands.values(), cmd -> plugin.getMessageUtils().parsePinataPlaceholders(pinata, cmd));
            }
        }

        var dazzle = config.behavior.reflexes.dazzle;
        if (dazzle.enabled && shouldTrigger(dazzle.chance)) {
            effectHandler.playEffects(dazzle.effects, attacker.getEyeLocation(), false);
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, dazzle.duration, 0));
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, dazzle.duration, 0));
            if (!dazzle.commands.isEmpty()) {
                actionHandler.process(attacker, dazzle.commands.values(), cmd -> plugin.getMessageUtils().parsePinataPlaceholders(pinata, cmd));
            }
        }
    }

    public boolean shouldTrigger(double chance) {
        return ThreadLocalRandom.current().nextDouble(100.0) < chance;
    }

    private Location findSafeY(org.bukkit.World world, double x, double z, double startY, int verticalRange) {
        Location target = new Location(world, x, startY, z);
        if (isSafeLocation(target)) return target;

        for (int i = 1; i <= verticalRange; i++) {
            target.setY(startY + i);
            if (isSafeLocation(target)) return target;

            target.setY(startY - i);
            if (isSafeLocation(target)) return target;
        }

        return null;
    }

    private boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        if (feet.getRelative(BlockFace.DOWN).isPassable()) return false;
        if (!feet.isPassable()) return false;
        if (!feet.getRelative(BlockFace.UP).isPassable()) return false;
        return true;
    }
}
