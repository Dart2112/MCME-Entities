package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.goal.head.HeadGoalWatch;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.Placeholder;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.UUID;

public class GoalWatchEntity extends GoalVirtualEntity {

    protected McmeEntity target;
    protected UUID uniqueId;
    protected boolean targetIncomplete = false;
    protected HeadGoalWatch headGoalWatch;
    private int tickCounter = 0;

    public GoalWatchEntity(VirtualEntity entity, VirtualEntityGoalFactory factory) {
        super(entity, factory);
        this.target = factory.getTargetEntity();
        if (this.target instanceof Placeholder) {
            targetIncomplete = true;
        }
        movementSpeed = MovementSpeed.STAND;

        if (this.target == null) return;

       this.headGoalWatch = new HeadGoalWatch(target, getEntity());

        clearHeadGoals();
        addHeadGoal(headGoalWatch);
    }

    @Override
    public void update() {
        super.update();
        if(target != null) {
            this.uniqueId = target.getUniqueId();
        }

        if (targetIncomplete) {
            McmeEntity search = EntitiesPlugin.getEntityServer().getEntity(uniqueId);
            if (search != null) {
                target = search;
                targetIncomplete = false;
                headGoalWatch.setTarget(target);
            }
        }

        if (target == null || !target.isOnline() || target.isDead()) {
            this.targetIncomplete = true;
            return;
        }

        if (!targetIncomplete) {
            if (tickCounter % 3 == 0) {
                Location orientation = getEntity().getLocation().clone()
                        .setDirection(target.getLocation().toVector().subtract(getEntity().getLocation().toVector()));
                setYaw(orientation.getYaw());
                setPitch(orientation.getPitch());
                tickCounter = 0;
            }
            tickCounter++;
        }
    }

    @Override
    public Vector getDirection() {
        return null;
    }


    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public float getRoll() {
        return 0;
    }

    @Override
    public VirtualEntityGoalFactory getFactory() {
        return super.getFactory().withTargetEntity(target);
    }
}
