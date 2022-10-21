package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.goal.head.HeadGoalWatch;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.Placeholder;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.util.Vector;

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
            this.targetIncomplete = true;
        }
        this.movementSpeed = MovementSpeed.STAND;
        this.clearHeadGoals();

        if (this.target == null || !this.target.isOnline()) {
            return;
        }

        this.headGoalWatch = new HeadGoalWatch(this.target, this.getEntity());

        this.addHeadGoal(this.headGoalWatch);
    }

    @Override
    public void update() {

        super.update();
        if (this.target != null) {
            this.uniqueId = this.target.getUniqueId();
        }

        if (this.uniqueId == null) {
            return;
        }

        if (this.targetIncomplete) {
            final McmeEntity search = EntitiesPlugin.getEntityServer().getEntity(this.uniqueId);
            if (search != null) {
                this.target = search;
                this.targetIncomplete = false;

                if (this.headGoalWatch == null || this.getHeadGoals().isEmpty()) {
                    this.headGoalWatch = new HeadGoalWatch(this.target, this.getEntity());
                    this.addHeadGoal(this.headGoalWatch);
                }

                this.headGoalWatch.setTarget(this.target);
            }
        }

        if (this.target == null || !this.target.isOnline() || this.target.isDead()) {
            this.targetIncomplete = true;
            return;
        }

        if (!this.targetIncomplete) {
            if (this.tickCounter % 3 == 0) {
                final Location location = this.getEntity().getLocation().clone();
                final Location targetLocation = this.target.getLocation().clone();

                final Location orientation = location.setDirection(
                    targetLocation.toVector().subtract(
                        location.toVector()
                    )
                );
                this.setYaw(orientation.getYaw());
                this.setPitch(orientation.getPitch());
                this.tickCounter = 0;

                if (this.headGoalWatch == null || this.getHeadGoals().isEmpty()) {
                    this.headGoalWatch = new HeadGoalWatch(this.target, this.getEntity());
                    this.addHeadGoal(this.headGoalWatch);
                }

                this.headGoalWatch.setTarget(this.target);
            }
            this.tickCounter++;
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
        return super.getFactory().withTargetEntity(this.target);
    }
}
