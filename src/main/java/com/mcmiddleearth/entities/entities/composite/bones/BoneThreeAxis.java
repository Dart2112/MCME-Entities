package com.mcmiddleearth.entities.entities.composite.bones;

import com.mcmiddleearth.entities.entities.composite.CompositeEntity;
import com.mcmiddleearth.entities.util.RotationMatrix;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.logging.Logger;

public class BoneThreeAxis extends BoneTwoAxis {

    private float roll;

    public BoneThreeAxis(String name, CompositeEntity parent, EulerAngle headPose, Vector relativePosition,
                         ItemStack headItem, boolean isHeadBone, int headPoseDelay) {
        super(name, parent, headPose, relativePosition, headItem, isHeadBone, headPoseDelay);
    }

    public void move() {
        if(!isHeadBone() || !parent.goalHasHeadControl()){
            if(hasHeadPoseUpdate) {
                rotatedHeadPose = RotationMatrix.rotateXZEulerAngleDegree(headPose,pitch,roll);
            }
            Vector shift;
            if(hasRotationUpdate()) {
                Vector rotatedZ = RotationMatrix.fastRotateZ(relativePosition,-roll);
                Vector rotatedZX = RotationMatrix.fastRotateX(rotatedZ,pitch);
                Vector newRelativePositionRotated = RotationMatrix.fastRotateY(rotatedZX,-yaw);
                shift = newRelativePositionRotated.clone().subtract(this.relativePositionRotated);
                relativePositionRotated = newRelativePositionRotated;
            } else {
                shift = new Vector(0,0,0);
            }
            velocity = parent.getVelocity().clone().add(shift);
        } else {
            super.move();
        }
    }

    public void teleport() {
        if(!isHeadBone() || !parent.goalHasHeadControl()) {
            if (hasHeadPoseUpdate) {
                rotatedHeadPose = RotationMatrix.rotateXZEulerAngleDegree(headPose, pitch, roll);
            }
            Vector rotatedZ = RotationMatrix.fastRotateZ(relativePosition, -roll);
            Vector rotatedZX = RotationMatrix.fastRotateX(rotatedZ, pitch);
            Vector newRelativePositionRotated = RotationMatrix.fastRotateY(rotatedZX, -yaw);
            relativePositionRotated = newRelativePositionRotated;
        }
        else {
            super.teleport();
        }
    }

    public void setRotation(float yaw, float pitch, float roll) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        rotationUpdate = true;
        hasHeadPoseUpdate = true;
    }

    @Override
    public void setRotation(float yaw) {
        this.yaw = yaw;//-parent.getRotation();
        this.pitch = 0;
        this.roll = 0;
        rotationUpdate = true;
    }

    @Override
    public float getRoll(){
        return roll;
    }
}
