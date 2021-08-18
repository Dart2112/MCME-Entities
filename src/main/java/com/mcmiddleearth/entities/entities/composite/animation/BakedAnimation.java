package com.mcmiddleearth.entities.entities.composite.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mcmiddleearth.entities.entities.composite.BakedAnimationEntity;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BakedAnimation {

    private final List<Frame> frames = new ArrayList<>();

    private int currentFrame, ticks;

    private final BakedAnimationType type;

    private final String next;

    private final int interval;

    private boolean finished;

    private final BakedAnimationEntity entity;

    private final String name;

    public BakedAnimation(BakedAnimationEntity entity, BakedAnimationType type, String name, String next, int interval) {
        this.entity = entity;
        this.name = name;
        this.type = type;
        this.next = next;
        this.interval = interval;
        /*for (int i = 0; i < states.length; i++) {
            this.states.put(states[i], i);
        }*/
        reset();
    }

    public void reset() {
        currentFrame = -1;
        ticks = -1;
        finished = false;
    }

    public void doTick() {
        if(finished) {
            return;
        }
        ticks++;
        if(ticks%interval==0) {
            currentFrame++;
            if(currentFrame == frames.size()) {
                if(type.equals(BakedAnimationType.LOOP)) {
                    currentFrame = 0;
                } else {
                    finished = true;
                    return;
                }
            }
            frames.get(currentFrame).apply(entity.getState());
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isAtLastFrame() {
        return currentFrame == frames.size()-1;
    }

    public void addFrame(Frame frame) {
        frames.add(frame);
    }

    public String getName() {
        return name;
    }

    public String getNext() {
        return next;
    }

    public BakedAnimationType getType() {
        return type;
    }

    public void applyFrame(int frameIndex) {
//Logger.getGlobal().info("1");
        Frame frame = frames.get(frameIndex);
//Logger.getGlobal().info("apply frame: "+frameIndex +" -> "+frame);
        if(frame!=null) {
            frame.apply(entity.getState());
        }
    }

    public static BakedAnimation loadAnimation(JsonObject data, Material itemMaterial, BakedAnimationEntity entity, String name) {
        Map<String, Integer> states = new HashMap<>();
        BakedAnimationType type;
        try {
            type = BakedAnimationType.valueOf(data.get("loop").getAsString().toUpperCase());
        }catch (IllegalArgumentException ex) {
            type = BakedAnimationType.ONCE;
        }
        int interval = (data.get("interval") == null? 1 : data.get("interval").getAsInt());
        String next = (data.has("next")?data.get("next").getAsString():null);
        BakedAnimation animation = new BakedAnimation(entity, type, name, next, interval);
        JsonArray frameData = data.get("frames").getAsJsonArray();
//long start = System.currentTimeMillis();
        for(int i = 0; i< frameData.size(); i++) {
            animation.addFrame(Frame.loadFrame(entity,animation,frameData.get(i).getAsJsonObject(),itemMaterial, entity.getHeadPoseDelay()));
        }
//Logger.getGlobal().info("Frame loading: "+(System.currentTimeMillis()-start));
        return animation;
    }

}
