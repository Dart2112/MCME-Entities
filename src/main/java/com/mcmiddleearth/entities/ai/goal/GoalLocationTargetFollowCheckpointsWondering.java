package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.goal.head.HeadGoalWatch;
import com.mcmiddleearth.entities.ai.pathfinding.Pathfinder;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.events.events.goal.GoalCheckpointReachedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Comparator;

public class GoalLocationTargetFollowCheckpointsWondering extends GoalLocationTarget {

    private final Location[] checkpoints;
    private final boolean loop;
    protected McmeEntity target;
    protected boolean targetIncomplete = false;
    private int currentCheckpoint;
    private boolean targetPlayer;
    private int tickCounter = 0;

    public GoalLocationTargetFollowCheckpointsWondering(VirtualEntity entity, VirtualEntityGoalFactory factory, Pathfinder pathfinder) {
        super(entity, factory, pathfinder);

        this.checkpoints = factory.getCheckpoints();
        this.loop = factory.isLoop();
        this.target = factory.getTargetEntity();
        this.targetPlayer = false;
        currentCheckpoint = factory.getStartCheckpoint();

        setTarget(checkpoints[currentCheckpoint]);
        setPathTarget(checkpoints[currentCheckpoint].toVector());

        setDefaultHeadGoal();
    }

    @Override
    public void update() {
        if (isFinished()) {
            if (targetPlayer) {
                selectTarget();
                lookAtTarget();
            }
            return;
        }

        if (isCloseToTarget(GoalDistance.POINT)) {
            EntitiesPlugin.getEntityServer().handleEvent(new GoalCheckpointReachedEvent(getEntity(), this));
            currentCheckpoint++;
            if (currentCheckpoint == checkpoints.length && loop) {
                currentCheckpoint = 0;
            }
            deletePath();
            setFinished();

            if (currentCheckpoint < checkpoints.length) {
                this.targetPlayer = true;

                Bukkit.getScheduler().runTaskLater(EntitiesPlugin.getInstance(), () -> {
                    unsetFinished();
                    setTarget(checkpoints[currentCheckpoint]);
                    setPathTarget(checkpoints[currentCheckpoint].toVector());
                }, 60L);
            } else {
                this.targetPlayer = false;
                movementSpeed = MovementSpeed.STAND;
            }
        }
        super.update();
    }


    @Override
    public VirtualEntityGoalFactory getFactory() {
        return super.getFactory()
                .withStartCheckpoint(Math.min(currentCheckpoint, checkpoints.length - 1))
                .withCheckpoints(checkpoints)
                .withTargetEntity(target)
                .withLoop(loop);
    }

    private void lookAtTarget() {
        if (targetIncomplete) {
            McmeEntity search = EntitiesPlugin.getEntityServer().getEntity(target.getUniqueId());
            if (search != null) {
                target = search;
                targetIncomplete = false;
            }
        }

        if (target == null) return;
        if (!target.isOnline()) {
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
                //hasRotation = true;
            }
            //secondUpdate = !secondUpdate;
            tickCounter++;
        }
    }

    private void selectTarget() {
        targetIncomplete = false;
        Collection<Player> nearbyPlayers = getEntity().getWorld().getNearbyPlayers(getEntity().getLocation(), 10);
        if (nearbyPlayers.size() == 1 && this.target != null && this.target.isOnline()) {
            return;
        }

        final Location entityLocation = getEntity().getLocation();
        nearbyPlayers.stream()
                .min(Comparator.comparingDouble(o -> o.getLocation().distanceSquared(entityLocation)))
                .ifPresentOrElse(player -> {
                    McmeEntity search = EntitiesPlugin.getEntityServer().getEntity(target.getUniqueId());
                    if (search != null) {
                        if (target != null && target.getUniqueId().equals(search.getUniqueId())) return;

                        target = search;
                        setDefaultHeadGoal();
                    }
                }, () -> target = null);
    }


    public void setDefaultHeadGoal() {
        clearHeadGoals();
        addHeadGoal(new HeadGoalWatch(target, getEntity()));
    }
}
