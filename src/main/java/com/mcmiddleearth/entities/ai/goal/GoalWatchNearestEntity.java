package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.goal.head.HeadGoalWatch;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

public class GoalWatchNearestEntity extends GoalWatchEntity {

    public GoalWatchNearestEntity(VirtualEntity entity, VirtualEntityGoalFactory factory) {
        super(entity, factory);
        selectTarget();
    }

    @Override
    public void update() {
        selectTarget();
        super.update();
    }

    private void selectTarget() {
        targetIncomplete = false;

        final Location entityLocation = getEntity().getLocation();
        Collection<Player> nearbyPlayers = getEntity().getLocation().getWorld()
                .getNearbyEntities(entityLocation, 10, 10, 10, entity -> entity instanceof Player)
                .stream()
                .map(entity -> (Player) entity)
                .collect(Collectors.toList());

        if (nearbyPlayers.size() == 1 && this.target != null && this.target.isOnline()) {
            return;
        }

        Optional<Player> playerOptional = nearbyPlayers.stream()
                .min(Comparator.comparingDouble(o -> o.getLocation().distanceSquared(entityLocation)));
        if(!playerOptional.isPresent()) {
            this.target = null;
            targetIncomplete = true;
            return;
        }

        Player player = playerOptional.get();
        McmeEntity search = EntitiesPlugin.getEntityServer().getEntity(player.getUniqueId());
        if (search != null) {
            if (target != null && target.getUniqueId().equals(search.getUniqueId())) return;

            target = search;
            if(headGoalWatch == null) {
                this.headGoalWatch = new HeadGoalWatch(target, getEntity());
            }
            headGoalWatch.setTarget(target);
        }
    }
}
