package com.mcmiddleearth.entities.ai.movement;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.goal.GoalJockey;
import com.mcmiddleearth.entities.api.MovementType;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.Projectile;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.provider.BlockProvider;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Collection;

public class MovementEngine {

    private final Vector gravity = new Vector(0, -0.2, 0);

    private final VirtualEntity entity;

    private final BlockProvider blockProvider;

    private double fallStart = 0;

    private static final Vector zero = new Vector(0, 0, 0);

    public MovementEngine(VirtualEntity entity) {
        this.entity = entity;
        this.blockProvider = EntitiesPlugin.getEntityServer().getBlockProvider(entity.getLocation().getWorld().getUID());
    }

    public void calculateMovement(Vector direction) {
        if (direction == null) direction = zero.clone();
        if (entity.isDead()) {
            switch (entity.getMovementType()) {
                case FLYING:
                case GLIDING:
                    entity.setMovementType(MovementType.FALL);
                    break;
                case UPRIGHT:
                case SNEAKING:
                    entity.setVelocity(new Vector(0, 0, 0));
                    return;
            }
        }
        switch (entity.getMovementType()) {
            case FLYING:
                Vector velocity = zero.clone();
                if (!direction.equals(zero)) {
                    velocity = direction.normalize().multiply(entity.getFlyingSpeed());
                }

                entity.setVelocity(velocity);
                break;
            case FALL:
                velocity = entity.getVelocity().clone().add(gravity);
                if (cannotMove(velocity)) {
                    double groundDistance = distanceToGround();
                    if (groundDistance < -velocity.getY()) {
                        velocity.setY(-groundDistance);
                        double fallHeight = fallStart - (entity.getEntityBoundingBox().getMin().getY() - groundDistance);
                        if (fallHeight > entity.getFallDepth() + 0.5) {
                            entity.damage((int) (fallHeight - entity.getFallDepth()));
                        }
                        if (entity.isSneaking()) {
                            entity.setMovementType(MovementType.SNEAKING);
                        } else {
                            entity.setMovementType(MovementType.UPRIGHT);
                        }
                    } else {
                        velocity.setX(0);
                        velocity.setZ(0);
                    }
                }
                entity.setVelocity(velocity);
                break;
            case UPRIGHT:
            case SNEAKING:
            default:
                velocity = zero.clone();
                if (!direction.equals(zero)) {
                    velocity = direction.normalize().multiply(entity.getGenericSpeed());
                }
                velocity.setY(0);
                Vector collisionVelocity = handleCollisions(velocity.clone());
                if (!cannotMove(collisionVelocity)) {
                    velocity = collisionVelocity;
                }
                if (cannotMove(velocity)) {
                    double jumpHeight = jumpHeight();
                    if (jumpHeight > 0 && jumpHeight <= entity.getJumpHeight() + 0.01) {
                        entity.setMovementType(MovementType.FALL);
                        velocity.setY(Math.sqrt(-2 * jumpHeight * gravity.getY()));

                    } else {
                        velocity = new Vector(0, 0, 0);
                    }
                } else if (distanceToGround() > 0.01) {
                    if (!cannotMove(velocity.clone().add(gravity))) {
                        entity.setMovementType(MovementType.FALL);
                    }
                }
                entity.setVelocity(velocity);
                break;
        }
    }

    public boolean cannotMove(Vector velocity) {
        BoundingBox entityBB = entity.getEntityBoundingBox().getBoundingBox().clone();
        entityBB.shift(velocity);
        for (int i = getBlock(entityBB.getMinX()); i <= getBlock(entityBB.getMaxX()); i++) {
            for (int j = getBlock(entityBB.getMinY()); j <= getBlock(entityBB.getMaxY()); j++) {
                for (int k = getBlock(entityBB.getMinZ()); k <= getBlock(entityBB.getMaxZ()); k++) {
                    if (!blockProvider.isPassable(i, j, k)
                            && entityBB.overlaps(blockProvider.getBoundingBox(i, j, k))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Vector handleCollisions(Vector velocity) {
        if (entity.getEntityBoundingBox().isZero()) return velocity;
        BoundingBox entityBB = entity.getEntityBoundingBox().getBoundingBox().clone();
        Collection<McmeEntity> closeEntities = EntitiesPlugin.getEntityServer()
                .getEntitiesAt(entity.getLocation(), (int) (entityBB.getWidthX() * 2 + 1),
                        (int) (entityBB.getHeight() * 2 + 1),
                        (int) (entityBB.getWidthZ() * 2 + 1));
        for (McmeEntity search : closeEntities) {
            if (!((search.getGoal() instanceof GoalJockey) && ((GoalJockey) search.getGoal()).getSteed().equals(entity))
                    && !(search instanceof Projectile)
                    && search != entity && search.getEntityBoundingBox() != null && !search.getEntityBoundingBox().isZero()
                    && entityBB.overlaps(search.getEntityBoundingBox().getBoundingBox())) {
                double speed = velocity.length();
                if (Double.isFinite(speed)) {
                    speed = Math.max(speed, 0.01);
                    Vector distance = search.getLocation().toVector().subtract(entity.getLocation().toVector());
                    distance.setY(0);
                    if (distance.getX() == 0 && distance.getZ() == 0) {
                        distance = Vector.getRandom().setY(0);
                    }

                    distance.normalize().multiply(speed);
                    velocity.subtract(distance).normalize().multiply(speed);
                }
            }
        }
        if (Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ())) {
            return velocity;
        } else {
            return new Vector(0, 0, 0);
        }
    }

    public double distanceToGround() {
        BoundingBox entityBB = entity.getEntityBoundingBox().getBoundingBox().clone();
        return distanceToGround(entityBB, (int) entity.getJumpHeight() + 1);
    }

    private double distanceToGround(BoundingBox boundingBox, int range) {
        double distance = Double.MAX_VALUE;
        for (int i = getBlock(boundingBox.getMinX()); i <= getBlock(boundingBox.getMaxX()); i++) {
            for (int j = getBlock(boundingBox.getMinZ()); j <= getBlock(boundingBox.getMaxZ()); j++) {
                int y = getBlock(boundingBox.getMinY());
                double thisDistance = boundingBox.getMinY() - blockProvider.blockTopY(i, y, j, range);
                if (thisDistance < distance) {
                    distance = thisDistance;
                }
            }
        }
        return distance;
    }

    private int getBlock(double cord) {
        return (int) Math.floor(cord);
    }


    public double jumpHeight() {
        BoundingBox entityBB = entity.getEntityBoundingBox().getBoundingBox().clone();
        entityBB.shift(new Vector(entity.getVelocity().getX(), 0, entity.getVelocity().getZ()));
        return -distanceToGround(entityBB, (int) entity.getJumpHeight() + 1);
    }

    public void setFallStart(double fallStart) {
        this.fallStart = fallStart;
    }
}
