package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.goal.head.HeadGoalLook;
import com.mcmiddleearth.entities.ai.goal.head.HeadGoalStare;
import com.mcmiddleearth.entities.ai.goal.head.HeadGoalWatch;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.events.events.goal.GoalVirtualEntityIsClose;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.logging.Logger;


public class GoalDefinedLocation extends GoalVirtualEntity {

    private final Location targetLocation;

    public GoalDefinedLocation(VirtualEntity entity, VirtualEntityGoalFactory factory) {
        super(entity, factory);
        this.targetLocation = factory.getTargetLocation();

        setDefaultHeadGoal();
    }

    @Override
    public void doTick() {
        super.doTick();
        getEntity().setLocation(targetLocation);
        setPitch(targetLocation.getPitch());
        setYaw(targetLocation.getYaw());
    }

    @Override
    public MovementSpeed getMovementSpeed() {
        return MovementSpeed.STAND;
    }

    @Override
    public Vector getDirection() {
        return null;
    }

    public void setDefaultHeadGoal() {
        clearHeadGoals();
        addHeadGoal(new HeadGoalStare(targetLocation.getYaw(), targetLocation.getPitch()));
    }

    @Override
    public VirtualEntityGoalFactory getFactory() {
        return super.getFactory().withTargetLocation(targetLocation);
    }
}
