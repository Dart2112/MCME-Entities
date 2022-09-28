package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.ai.goal.head.HeadGoalLook;
import com.mcmiddleearth.entities.ai.goal.head.HeadGoalStare;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class GoalWatchLocation extends GoalVirtualEntity {

    private final Location targetLocation;

    public GoalWatchLocation(VirtualEntity entity, VirtualEntityGoalFactory factory) {
        super(entity, factory);

        targetLocation = factory.getTargetLocation();


        Location location = entity.getLocation().clone();
        Location orientation = location.setDirection(targetLocation.toVector().subtract(location.toVector()));

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
        addHeadGoal(new HeadGoalLook(targetLocation, getEntity()));
    }

    @Override
    public VirtualEntityGoalFactory getFactory() {
        return super.getFactory().withTargetLocation(targetLocation);
    }

}
