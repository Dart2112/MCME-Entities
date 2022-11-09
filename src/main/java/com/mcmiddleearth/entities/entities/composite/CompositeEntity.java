package com.mcmiddleearth.entities.entities.composite;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.api.McmeEntityType;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.entities.composite.bones.Bone;
import com.mcmiddleearth.entities.entities.composite.bones.BoneThreeAxis;
import com.mcmiddleearth.entities.entities.composite.bones.BoneTwoAxis;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import com.mcmiddleearth.entities.protocol.packets.*;
import com.mcmiddleearth.entities.protocol.packets.composite.CompositeEntityMovePacket;
import com.mcmiddleearth.entities.protocol.packets.composite.CompositeEntitySpawnPacket;
import com.mcmiddleearth.entities.protocol.packets.composite.CompositeEntityTeleportPacket;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Rotation;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public abstract class CompositeEntity extends VirtualEntity {

    private final Set<Bone> bones = new HashSet<>();
    /****
     * Main Head Bone is a reference to the root head bone in the set of bones. It must be called "head".
     * Note that there can be more than one head bone, but this refers only to one of them
     * It is used for head pos + real head yaw / pitch.
     */
    private Bone mainHeadBone;

    /****
     * Forward bone is used to determine the forward direction of the head to allow for spewing effects. You can definitely
     * get that directly from the head pose of the armor stand as well but I'm too stupid to figure it out
     * (you have to take roll into account due to extrinsic rotation (I think))
     */
    private Bone forwardBone;
    private final Set<Bone> headBones = new HashSet<>();
    private boolean goalHasHeadControl;

    private final int firstEntityId;

    private Bone displayNameBone;
    private Vector displayNamePosition;

    private int headPoseDelay;

    //private ActionType animation = null;

    //private boolean rotationUpdate;
    protected float currentYaw, currentHeadPitch, currentHeadYaw;

    protected float maxRotationStep = 40f;

    protected RotationMode rotationMode;

    public CompositeEntity(int entityId, VirtualEntityFactory factory) throws InvalidLocationException, InvalidDataException {
        this(entityId,factory, RotationMode.YAW);
    }

    protected CompositeEntity(int entityId, VirtualEntityFactory factory,
                              RotationMode rotationMode) throws InvalidLocationException, InvalidDataException {
        super(factory);
        firstEntityId = entityId;
        headPoseDelay = factory.getHeadPoseDelay();
        this.rotationMode = rotationMode;
        maxRotationStep = factory.getMaxRotationStep();
        currentYaw = getYaw();
        currentHeadYaw = getHeadYaw();
        currentHeadPitch = getHeadPitch();
        displayNamePosition = factory.getDisplayNamePosition().clone();
        if(getDisplayName()!=null) {
            createDisplayBone();
        }
    }

    protected CompositeEntity(int entityId, McmeEntityType type, Location location) {
        super(type, location);
        firstEntityId = entityId;
    }

    protected void createPackets() {
        spawnPacket = new CompositeEntitySpawnPacket(this);
        int[] ids = new int[bones.size()];
        for(int i = 0; i < bones.size(); i++) {
            ids[i] = firstEntityId+i;
        }
        removePacket = new VirtualEntityDestroyPacket(ids);
        teleportPacket = new CompositeEntityTeleportPacket(this);
        movePacket = new CompositeEntityMovePacket(this);
        //namePacket = new DisplayNamePacket(firstEntityId);
    }

    private void createDisplayBone() {
        if (rotationMode.equals(RotationMode.YAW_PITCH_ROLL)) {
            displayNameBone = new BoneThreeAxis("displayName", this, new EulerAngle(0, 0, 0),
                    displayNamePosition, null, false, 0);
        } else {
            displayNameBone = new Bone("displayName", this, new EulerAngle(0, 0, 0),
                    displayNamePosition, null, false, 0);
        }
        displayNameBone.setDisplayName(getDisplayName());
        bones.add(displayNameBone);
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public void move() {
        checkHeadYaw();
        if(hasRotationUpdate()) {
            updateBodyBones();
        }
        if(hasLookUpdate()) {
            updateHeadBones();
        }
        bones.forEach(Bone::move);
        super.move();
        bones.forEach(Bone::resetUpdateFlags);
    }

    @Override
    public void teleport() {
//Logger.getGlobal().info("Composite teleport method");
        checkHeadYaw();
        updateBodyBones();
        updateHeadBones();
        bones.forEach(Bone::teleport);
        super.teleport();
        bones.forEach(Bone::resetUpdateFlags);
    }

    protected void updateBodyBones() {
        currentYaw = turn(currentYaw,getLocation().getYaw(),maxRotationStep);
        bones.forEach(bone-> {
            if(!bone.isHeadBone() || !goalHasHeadControl) // if the goal has head control, don't rotate head bones.
                bone.setRotation(currentYaw);
        });
    }

    private void updateHeadBones() {
        currentHeadYaw = turn(currentHeadYaw, getHeadYaw(),maxRotationStep);
        currentHeadPitch = turn(currentHeadPitch,getHeadPitch(),maxRotationStep);

        if(goalHasHeadControl){
            headBones.forEach(bone-> {
                if((bone instanceof BoneTwoAxis)){
                    bone.setRotation(currentHeadYaw);
                    ((BoneTwoAxis)bone).setPitch(currentHeadPitch);
                }
            });
        }
    }

    protected float turn(float currentAngle, float aimAngle, float maxStep) {
        float diff = aimAngle - currentAngle;
        while(diff < -180) {
            diff += 360;
        }
        while(diff > 180) {
            diff -= 360;
        }
        if(Math.abs(diff)<maxStep) {
            return aimAngle;
        } else if(diff<0) {
            return currentAngle - maxStep;
        } else {
            return currentAngle + maxStep;
        }
    }

    private void checkHeadYaw() {
        float diff = getHeadYaw() - getLocation().getYaw();
        while(diff < -180) {
            diff += 360;
        }
        while(diff > 180) {
            diff -= 360;
        }
        float maxYaw = Math.min(90, 180 - 2 * Math.abs(getLocation().getPitch()));
        if(diff > maxYaw) {//90) {
            setHeadRotation(getLocation().getYaw()+maxYaw/*90f*/,getLocation().getPitch());
        }
        else if(diff < -90) {
            setHeadRotation(getLocation().getYaw() - maxYaw/*90f*/, getLocation().getPitch());
        }
    }

    public void setMainHeadBone(Bone headBone) {
        this.mainHeadBone = headBone;
        Logger.getGlobal().warning("SET THE HEAD BONE!");
    }

    public Set<Bone> getHeadBones() {
        return headBones;
    }

    public Bone getForwardBone() {
        return forwardBone;
    }

    public void setForwardBone(Bone forwardBone) {
        this.forwardBone = forwardBone;
        Logger.getGlobal().warning("SET THE FORWARD BONE!");
    }

    @Override
    public Vector getHeadPosition() {
        if(mainHeadBone != null){
            return mainHeadBone.getLocation().toVector().add(new Vector(0,1.5,0));
        }
        Logger.getGlobal().warning("head bone is null!");
        return getLocation().toVector().add(new Vector(0,1.5,0));
    }

    public Vector getRelativeHeadPosition() {
        if(mainHeadBone != null){
            return mainHeadBone.getRelativePosition();
        }
        return new Vector(0,0,0);
    }

    public double getRealHeadPitch(){

        // TODO: Find a better way of doing this without relying on forward bone

        if(mainHeadBone != null && forwardBone != null){
            Location headLocation = mainHeadBone.getLocation().clone();
            headLocation.setDirection(forwardBone.getLocation().toVector().subtract(mainHeadBone.getLocation().toVector()));
            return headLocation.getPitch();
        }
        return getHeadPitch();
    }

    public double getRealHeadYaw(){

        // TODO: Find a better way of doing this without relying on forward bone

        if(mainHeadBone != null && forwardBone != null){
            Location headLocation = mainHeadBone.getLocation().clone();
            headLocation.setDirection(forwardBone.getLocation().toVector().subtract(mainHeadBone.getLocation().toVector()));
            return headLocation.getYaw();
        }
        return getHeadYaw();
    }

    private Rotation rotation = null;

    public boolean goalHasHeadControl() {
        return goalHasHeadControl;
    }

    public void setGoalHasHeadControl(boolean goalHasHeadControl) {
        this.goalHasHeadControl = goalHasHeadControl;
    }

    @Override
    public boolean hasLookUpdate() {
        return currentHeadPitch != getLocation().getPitch() || currentHeadYaw != getHeadYaw();
    }

    @Override
    public boolean hasRotationUpdate() {
        return currentYaw != getLocation().getYaw();
    }


    public Set<Bone> getBones() {
        return bones;
    }

    @Override
    public int getEntityId() {
        return firstEntityId;
    }

    @Override
    public int getEntityQuantity() {
        return bones.size();
    }

    @Override
    public void setDisplayName(String displayName) {
        if (displayNameBone != null) {
            super.setDisplayName(displayName);
            displayNameBone.setDisplayName(displayName);
        }
    }

    public int getHeadPoseDelay() {
        return headPoseDelay;
    }

    @Override
    public boolean hasId(int entityId) {
        return this.firstEntityId <= entityId && this.firstEntityId+getEntityQuantity() > entityId;
    }

    public RotationMode getRotationMode() {
        return rotationMode;
    }

    public float getMaxRotationStep() {
        return maxRotationStep;
    }

    public void setMaxRotationStep(float maxRotationStep) {
        this.maxRotationStep = maxRotationStep;
    }

    public float getCurrentYaw() {
        return currentYaw;
    }

    public enum RotationMode {
        YAW, YAW_PITCH, YAW_PITCH_ROLL;
    }

    public VirtualEntityFactory getFactory() {
        VirtualEntityFactory factory = super.getFactory()
            .withHeadPoseDelay(headPoseDelay)
            .withMaxRotationStep(maxRotationStep)
            .withDisplayNamePosition(displayNamePosition);
        /*if(getDisplayName() != null && displayNameBone != null) {
            factory.withDisplayNamePosition(displayNameBone.getRelativePosition());
        }*/
        return factory;
    }
}
