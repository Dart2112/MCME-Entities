package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Comparator;

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
}
