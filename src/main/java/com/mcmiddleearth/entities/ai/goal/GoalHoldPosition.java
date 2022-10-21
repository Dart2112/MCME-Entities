package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.ai.goal.head.HeadGoalStare;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import org.bukkit.Location;
import org.bukkit.util.Vector;


public class GoalHoldPosition extends GoalVirtualEntity {

    private final Location targetLocation;

    public GoalHoldPosition(VirtualEntity entity, VirtualEntityGoalFactory factory) {
        super(entity, factory);

        movementSpeed = MovementSpeed.STAND;

        this.targetLocation = factory.getTargetLocation();

        Location entityLocation = getEntity().getLocation().clone();
        Location orientation = entityLocation.setDirection(targetLocation.toVector().subtract(entityLocation.toVector()));
        setYaw(orientation.getYaw());
        setPitch(orientation.getPitch());

        setDefaultHeadGoal();
    }

    @Override
    public Vector getDirection() {
        return null;
    }

    public void setDefaultHeadGoal() {
        clearHeadGoals();
        addHeadGoal(new HeadGoalStare(getYaw(), getPitch()));
    }

    @Override
    public VirtualEntityGoalFactory getFactory() {
        return super.getFactory().withTargetLocation(targetLocation);
    }
}
