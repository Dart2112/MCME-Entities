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

    //TODO MAKE THE MOVEMENT OF THE HEAD ALSO AFFECT THE BODY
    public GoalWatchNearestEntity(VirtualEntity entity, VirtualEntityGoalFactory factory) {
        super(entity, factory);

        this.clearHeadGoals();
        this.selectTarget();
    }

    @Override
    public void update() {
        this.selectTarget();
        super.update();
    }

    @SuppressWarnings("all")
    private void selectTarget() {
        this.targetIncomplete = false;

        final Location entityLocation = this.getEntity().getLocation();
        final Collection<Player> nearbyPlayers = this.getEntity().getLocation().getWorld()
                .getNearbyEntities(entityLocation, 10, 10, 10, Player.class::isInstance)
                .stream()
                .map(Player.class::cast)
                .collect(Collectors.toList());

        if (nearbyPlayers.size() == 1 && this.target != null && this.target.isOnline() && this.target.getLocation() != null) {
            return;
        }

        final Optional<Player> playerOptional = nearbyPlayers.stream()
                .min(Comparator.comparingDouble(o -> o.getLocation().distanceSquared(entityLocation)));
        if(!playerOptional.isPresent()) {
            this.target = null;
            this.targetIncomplete = true;
            return;
        }

        final Player player = playerOptional.get();
        final McmeEntity search = EntitiesPlugin.getEntityServer().getEntity(player.getUniqueId());
        if (search != null) {
            if (this.target != null && this.target.getUniqueId().equals(search.getUniqueId())) return;

            this.target = search;
            this.targetIncomplete = false;
        }
    }
}
