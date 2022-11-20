package com.mcmiddleearth.entities.entities.composite.collision;

import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.provider.EntityProvider;
import com.mcmiddleearth.entities.provider.PlayerProvider;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class CollisionServer {
    private final EntityProvider entityProvider;
    private final PlayerProvider playerProvider;

    public CollisionServer(EntityProvider entityProvider, PlayerProvider playerProvider) {
        this.entityProvider = entityProvider;
        this.playerProvider = playerProvider;
    }

    public void doTick() {
        // Get current list of collision entities (ideally we'd already have this, but there's no way to observe entity spawns and removals)
        List<CollisionEntity> entities = new ArrayList<>();
        for (McmeEntity mcmeEntity : entityProvider.getEntities()) {
            if (mcmeEntity instanceof CollisionEntity) {
                entities.add((CollisionEntity) mcmeEntity);
            }
        }

        // Update all collision boxes to match the current entity state
        for (CollisionEntity entity : entities) {
            entity.updateColliders();
        }

        HashMap<World, List<Entity>> realWorldEntities = new HashMap<>();

        for (CollisionEntity entity : entities) {
            World world = entity.getLocation().getWorld();
            Vector entityOffset = entity.getLocation().toVector().multiply(-1f);
            List<Entity> realEntities = realWorldEntities.computeIfAbsent(world, CollisionServer::getRealEntitiesForWorld);
            BoundingBox boundingBox = entity.getCollisionBoundingBox();

            for (Entity otherEntity : realEntities) {
                BoundingBox otherBoundingBox = otherEntity.getBoundingBox();

                // Translate other bounding box to our entity's local space
                otherBoundingBox.shift(entityOffset);

                // Do a coarse check first
                if (!boundingBox.overlaps(otherBoundingBox)) continue;

                for (Iterator<BoundingBox> it = entity.getColliders().iterator(); it.hasNext(); ) {
                    BoundingBox entityCollider = it.next();

                    if (entityCollider.overlaps(otherBoundingBox)) {
                        // Collision detected
                        onCollision(entity, otherEntity);
                        break;
                    }
                }
            }
        }
    }

    private void onCollision(CollisionEntity target, Entity attacker) {
        if (target instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) target;

            double damage = 1.0;
            double knockbackStrength = 1.0;
            Entity realAttacker = attacker;

            if (attacker instanceof Arrow) {
                Arrow arrow = (Arrow) attacker;

                damage = arrow.getDamage();
                knockbackStrength = arrow.getKnockbackStrength();
                if (arrow.getShooter() instanceof Player) {
                    realAttacker = ((Player) arrow.getShooter());
                }
            } else if (attacker instanceof LivingEntity) {
                AttributeInstance attackDamageAttribute = ((LivingEntity) attacker).getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                if (attackDamageAttribute != null) {
                    damage = attackDamageAttribute.getValue();
                }

                AttributeInstance attackKnockbackAttribute = ((LivingEntity) attacker).getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK);
                if (attackKnockbackAttribute != null) {
                    knockbackStrength = attackKnockbackAttribute.getValue();
                }
            }

            if (target instanceof McmeEntity) {
                McmeEntity mcmeEntity = (McmeEntity) target;

                if (realAttacker instanceof Player) {
                    // Special case to allow setting enemies for AI
                    mcmeEntity.receiveAttack(playerProvider.getOrCreateMcmePlayer((Player) realAttacker), damage, knockbackStrength);
                } else {
                    mcmeEntity.receiveAttack(null, damage, knockbackStrength);
                }
            } else {
                // TODO: apply knockback?
                livingEntity.damage(damage);
            }
        }

        // Projectiles get destroyed on hit
        // TODO: handle thrown potions (bukkit API has no method to just apply all their effects immediately...)
        if (attacker instanceof Projectile) {
            attacker.remove();

            // Mimic behavior of the other Projectile implementation that I couldn't be bothered to modify
            Location location = attacker.getLocation();
            location.getWorld().spawnParticle(Particle.SPELL_INSTANT, location, 1, 0, 0, 0, 2, null, true);
        }
    }

    private static List<Entity> getRealEntitiesForWorld(World world) {
        return world.getEntities().stream()
                .filter(entity -> isEntityCollidable(entity))
                .collect(Collectors.toList());
    }

    private static boolean isEntityCollidable(Entity entity) {
        EntityType entityType = entity.getType();

        switch (entityType) {
            case ARROW:
            case SPECTRAL_ARROW:
                // Do not collide with arrows which already hit a block.
                return !((Arrow) entity).isInBlock();
            case SNOWBALL:
                return true;
            default:
                return false;
        }
    }
}
