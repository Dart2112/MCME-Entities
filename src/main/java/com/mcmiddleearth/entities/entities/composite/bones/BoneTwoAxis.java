package com.mcmiddleearth.entities.entities.composite.bones;

import com.mcmiddleearth.entities.entities.composite.CompositeEntity;
import com.mcmiddleearth.entities.util.RotationMatrix;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class BoneTwoAxis extends Bone {

    protected float pitch;

    public BoneTwoAxis(String name, CompositeEntity parent, EulerAngle headPose,
                       Vector relativePosition, ItemStack headItem, boolean isHeadBone, int headPoseDelay) {
        super(name,parent,headPose, relativePosition,headItem, isHeadBone, headPoseDelay);
        pitch = 0;
    }

    @Override
    public void doTick() {
    }

    public void move() {
        if(hasHeadPoseUpdate) {
            rotatedHeadPose = RotationMatrix.rotateXEulerAngleDegree(headPose,pitch);
        }
        Vector shift;
        if(hasRotationUpdate()) {
            Vector newRelativePositionRotated;
            if(isHeadBone() || !parent.goalHasHeadControl()) {

                newRelativePositionRotated =
                        RotationMatrix.fastRotateY(RotationMatrix
                        .fastRotateX(relativePosition.clone().subtract(parent.getRelativeHeadPosition()), pitch), -yaw).add(RotationMatrix.fastRotateY(parent.getRelativeHeadPosition(),-parent.getYaw()));

            } else {
                newRelativePositionRotated = RotationMatrix.fastRotateY(RotationMatrix
                    .fastRotateX(relativePosition, pitch), -yaw);
            }
            shift = newRelativePositionRotated.clone().subtract(this.relativePositionRotated);
            relativePositionRotated = newRelativePositionRotated;
        } else {
            shift = new Vector(0,0,0);
        }


        velocity = parent.getVelocity().clone().add(shift);

    }

    public void teleport() {
        if(hasHeadPoseUpdate) {
            rotatedHeadPose = RotationMatrix.rotateXEulerAngleDegree(headPose, pitch);
        }
        if(isHeadBone() || !parent.goalHasHeadControl()) {
            relativePositionRotated = RotationMatrix.fastRotateY(RotationMatrix
                    .fastRotateX(relativePosition.clone().subtract(parent.getRelativeHeadPosition()), pitch), -yaw).add(parent.getRelativeHeadPosition());
        } else {
            relativePositionRotated = RotationMatrix.fastRotateY(RotationMatrix
                    .fastRotateX(relativePosition, pitch), -yaw);
        }
    }

    public void setPitch(float pitch) {
        setPitch(pitch,true);
    }

    public void setPitch(float pitch, boolean headPoseUpdate) {
        this.pitch = pitch;
        rotationUpdate = true;
        if(headPoseUpdate) hasHeadPoseUpdate = true;
    }

    @Override
    public float getPitch(){
        return pitch;
    }
}
