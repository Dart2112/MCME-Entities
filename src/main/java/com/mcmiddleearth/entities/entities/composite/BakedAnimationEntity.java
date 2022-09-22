package com.mcmiddleearth.entities.entities.composite;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.api.ActionType;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.entities.composite.animation.AnimationJob;
import com.mcmiddleearth.entities.entities.composite.animation.BakedAnimation;
import com.mcmiddleearth.entities.entities.composite.animation.BakedAnimationTree;
import com.mcmiddleearth.entities.entities.composite.animation.BakedAnimationType;
import com.mcmiddleearth.entities.events.events.virtual.composite.BakedAnimationEntityAnimationChangeEvent;
import com.mcmiddleearth.entities.events.events.virtual.composite.BakedAnimationEntityAnimationSetEvent;
import com.mcmiddleearth.entities.events.events.virtual.composite.BakedAnimationEntityStateChangedEvent;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import org.bukkit.Material;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BakedAnimationEntity extends CompositeEntity {

    //private final Map<String, BakedAnimation> animations = new HashMap<>();

    private final BakedAnimationTree animationTree = new BakedAnimationTree(null);

    private final Map<String, Integer> states = new HashMap<>();

    private AnimationJob currentAnimation, nextAnimation;

    private int currentState;

    private boolean manualAnimationControl, manualOverride;

    private MovementSpeed movementSpeedAnimation;
    private int startMovementCounter, stopMovementCounter;

    private final String animationFileName;

    protected boolean instantAnimationSwitching = true;

    public BakedAnimationEntity(int entityId, VirtualEntityFactory factory) throws InvalidLocationException, InvalidDataException {
        this(entityId, factory, RotationMode.YAW);
    }

    public BakedAnimationEntity(int entityId, VirtualEntityFactory factory,
                                RotationMode rotationMode) throws InvalidLocationException, InvalidDataException {
        super(entityId, factory, rotationMode);
        manualAnimationControl = factory.getManualAnimationControl();
        movementSpeedAnimation = getMovementSpeed();
        animationFileName = factory.getDataFile();

        final File animationFile = new File(EntitiesPlugin.getAnimationFolder(), animationFileName + ".json");
        try (FileReader reader = new FileReader(animationFile)) {
            JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject modelData = data.get("model").getAsJsonObject();
            Material itemMaterial = Material.valueOf(modelData.get("head_item").getAsString().toUpperCase());
            JsonObject animationData = data.get("animations").getAsJsonObject();

            animationData.entrySet().forEach(entry -> {
                String[] split;
                if (entry.getKey().contains(factory.getDataFile() + ".")) {
                    split = entry.getKey().split(factory.getDataFile() + "\\.");
                } else {
                    split = entry.getKey().split("animations\\.");
                }

                String animationKey = split.length > 1 ? split[1] : entry.getKey();
                String animationName = animationKey;
                // Ignore integers if they're the last part of the path - those are used to distinguish different animations with the same key
                int lastDot = animationName.lastIndexOf('.');
                if (lastDot > 0) {
                    String lastKeyPart = animationName.substring(lastDot + 1);
                    if (lastKeyPart.matches("^\\d+$")) {
                        animationName = animationName.substring(0, lastDot);
                    }
                }
                animationTree.addAnimation(animationName, BakedAnimation.loadAnimation(entry.getValue().getAsJsonObject(),
                        itemMaterial, this, animationKey, animationName));
            });
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new InvalidDataException("Data file '" + factory.getDataFile() + "' doesn't exist or does not contain valid animation data.");
        }
        createPackets();
    }

    public static List<String> getDataFiles() {
        return Arrays.stream(Objects.requireNonNull(EntitiesPlugin.getAnimationFolder().listFiles((dir, name) -> name.endsWith(".json")))).map(file -> file.getName().substring(0, file.getName().lastIndexOf('.'))).collect(Collectors.toList());
    }

    @Override
    public void doTick() {
        if (movementSpeedAnimation.equals(MovementSpeed.STAND) && !getMovementSpeed().equals(MovementSpeed.STAND)) {
            startMovementCounter++;
            if (startMovementCounter > 0) {
                movementSpeedAnimation = getMovementSpeed();
                startMovementCounter = 0;
            }
        } else if (getMovementSpeed().equals(MovementSpeed.STAND) && !movementSpeedAnimation.equals(MovementSpeed.STAND)) {
            stopMovementCounter++;
            if (stopMovementCounter > 3) {
                movementSpeedAnimation = getMovementSpeed();
                stopMovementCounter = 0;
            }
        } else {
            startMovementCounter = 0;
            stopMovementCounter = 0;
            if (!getMovementSpeed().equals(movementSpeedAnimation)) {
                movementSpeedAnimation = getMovementSpeed();
            }
        }
        if(!manualAnimationControl) {
            AnimationJob expected = new AnimationJob(animationTree.getAnimation(this),null,0);
            if (expected.getAnimation() != null
                    && (
                    currentAnimation == null
                            || currentAnimation.getAnimation() == null
                            || currentAnimation.getAnimation().isAtLastFrame()
                            || !expected.getAnimation().getAnimationName().equals(currentAnimation.getAnimation().getAnimationName())
            )) {
                if (!manualOverride && instantAnimationSwitching
                        && callAnimationChangeEvent(currentAnimation, expected)) {
                    currentAnimation = expected;
                    if (currentAnimation.getAnimation() != null)
                        currentAnimation.getAnimation().reset();
                } else {
                    nextAnimation = expected;
                }
            }
        }
        if (currentAnimation != null) {
            if (currentAnimation.getAnimation().isFinished() || currentAnimation.getAnimation().isAtLastFrame()) {
                if (currentAnimation.getAnimation().getType().equals(BakedAnimationType.CHAIN)) {
                    AnimationJob nextAnim = new AnimationJob(animationTree.getAnimation(currentAnimation.getAnimation().getNext()),
                            null, 0);
                    if (callAnimationChangeEvent(currentAnimation, nextAnim)) {
                        currentAnimation = nextAnim;
                        currentAnimation.getAnimation().reset();
                    }
                } else {
                    manualOverride = false;
                }
            }
            if (!manualOverride
                    && (currentAnimation.getAnimation().isAtLastFrame()
                    || currentAnimation.getAnimation().isFinished())
                    && nextAnimation != null && callAnimationChangeEvent(currentAnimation, nextAnimation)) {
                currentAnimation = nextAnimation;
                currentAnimation.getAnimation().reset();
                nextAnimation = null;
            }
        } else {
            manualOverride = false;
            if (nextAnimation != null
                    && callAnimationChangeEvent(null, nextAnimation)) {
                currentAnimation = nextAnimation;
                currentAnimation.getAnimation().reset();
                nextAnimation = null;
            }
        }
        if (currentAnimation != null) {
            currentAnimation.doTick();
        }
        super.doTick();
    }

    public void setAnimation(String name, boolean manualOverride, Payload payload, int delay) {
        BakedAnimationEntityAnimationSetEvent event = new BakedAnimationEntityAnimationSetEvent(this, name);
        EntitiesPlugin.getEntityServer().handleEvent(event);
        if (!event.isCancelled()) {
            this.manualOverride = manualOverride;
            AnimationJob newAnim = new AnimationJob(animationTree.getAnimation(event.getNextAnimationKey()), payload, delay);
            if (instantAnimationSwitching || manualOverride) {
                if (callAnimationChangeEvent(currentAnimation, newAnim)) {
                    if (newAnim.getAnimation() != null) {
                        currentAnimation = newAnim;
                        currentAnimation.getAnimation().reset();
                    } else {
                        currentAnimation = null;
                    }
                }
            } else {
                nextAnimation = newAnim;
            }
        }
    }

    @Override
    public void playAnimation(ActionType type, Payload payload, int delay) {
        setAnimation(this.getMovementType().name().toLowerCase()
                        + "." + this.getMovementSpeed().name().toLowerCase()
                        + "." + type.name().toLowerCase(),
                true, payload, delay);
    }

    private boolean callAnimationChangeEvent(AnimationJob current, AnimationJob next) {
        BakedAnimationEntityAnimationChangeEvent event
                = new BakedAnimationEntityAnimationChangeEvent(this, (current == null ? null : current.getAnimation()),
                (next == null ? null : next.getAnimation()), manualAnimationControl,
                instantAnimationSwitching);
        EntitiesPlugin.getEntityServer().handleEvent(event);
        return !event.isCancelled();
    }

    public void setState(String state) {
        BakedAnimationEntityStateChangedEvent event = new BakedAnimationEntityStateChangedEvent(this, state);
        EntitiesPlugin.getEntityServer().handleEvent(event);
        if (!event.isCancelled()) {
            Integer stateId = states.get(event.getNextState());
            if (stateId != null) {
                currentState = stateId;
            }
        }
    }

    public int getState() {
        return currentState;
    }

    public Map<String, Integer> getStates() {
        return states;
    }

    public void setAnimationFrame(String animation, int frameIndex) {
//Logger.getGlobal().info("set Animation Frame "+animation + " "+ frameIndex);
        manualOverride = true;
        currentAnimation = null;
        BakedAnimation anim = animationTree.getAnimation(animation);
        if (anim != null) {
//Logger.getGlobal().info("Apply Frame: "+ anim);
            anim.applyFrame(frameIndex);
        }
    }

    public boolean isManualAnimationControl() {
        return manualAnimationControl;
    }

    public void setManualAnimationControl(boolean manualAnimationControl) {
        this.manualAnimationControl = manualAnimationControl;
    }

    public boolean isManualOverride() {
        return manualOverride;
    }

    public boolean isInstantAnimationSwitching() {
        return instantAnimationSwitching;
    }

    public BakedAnimation getCurrentAnimation() {
        return (currentAnimation == null ? null : currentAnimation.getAnimation());
    }

    public List<String> getAnimations() {
        return animationTree.getAnimationKeys();
    }

    public MovementSpeed getMovementSpeedAnimation() {
        return movementSpeedAnimation;
    }

    @Override
    public VirtualEntityFactory getFactory() {
        return super.getFactory()
                .withDataFile(animationFileName)
                .withManualAnimationControl(manualAnimationControl);
    }

}
