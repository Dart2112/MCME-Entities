package com.mcmiddleearth.entities.entities.composite.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mcmiddleearth.entities.entities.composite.BakedAnimationEntity;
import com.mcmiddleearth.entities.entities.composite.bones.Bone;
import com.mcmiddleearth.entities.entities.composite.bones.BoneThreeAxis;
import com.mcmiddleearth.entities.entities.composite.bones.BoneTwoAxis;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class Frame {

    private final Map<Bone,BoneData> bones = new HashMap<>();

    public Frame() {
    }

    public void apply(int state) {
        bones.forEach((bone, boneData) -> {
            bone.setRelativePosition(boneData.getPosition());
/*if(bone.getName().equals("bone")) {
    Logger.getGlobal().info("Frame position: "+bone.getRelativePosition().toString());
}*/
            bone.setHeadPose(boneData.getHeadPose());
            bone.setHeadItem(boneData.getItems()[state]);
        });
    }

    public static Frame loadFrame(BakedAnimationEntity entity, BakedAnimation animation,
                                  JsonObject data, Material itemMaterial, int headPoseDelay) {
        Set<Map.Entry<String, JsonElement>> entries = data.get("bones").getAsJsonObject().entrySet();
        //long start = System.currentTimeMillis();
        Frame frame = new Frame();
        entries.forEach(entry-> {
            BoneData boneData = BoneData.loadBoneData(entity.getStates(),entry.getValue().getAsJsonObject(),itemMaterial);
            Bone bone = entity.getBones().stream().filter(searchBone->entry.getKey().equals(searchBone.getName())).findFirst().orElse(null);
            if(bone == null) {
                //long boneStart = System.currentTimeMillis();
                boolean headBone = entry.getKey().startsWith("head");
                switch(entity.getRotationMode()) {
                    case YAW:
                        if (headBone) {
                            bone = new BoneTwoAxis(entry.getKey(), entity, boneData.getHeadPose(),
                                    boneData.getPosition(), boneData.getItems()[0], true, headPoseDelay);
                        } else {
                            bone = new Bone(entry.getKey(), entity, boneData.getHeadPose(),
                                    boneData.getPosition(), boneData.getItems()[0], false, headPoseDelay);
                        }
                        break;
                    case YAW_PITCH:
                        bone = new BoneTwoAxis(entry.getKey(), entity, boneData.getHeadPose(),
                                boneData.getPosition(), boneData.getItems()[0], headBone, headPoseDelay);
                        break;
                    case YAW_PITCH_ROLL:
                        bone = new BoneThreeAxis(entry.getKey(), entity, boneData.getHeadPose(),
                                boneData.getPosition(), boneData.getItems()[0], headBone, headPoseDelay);
                        break;
                }
                entity.getBones().add(bone);
                if(headBone){
                    entity.getHeadBones().add(bone);
                    if(entry.getKey().equals("head")) {
                        // Set main head bone. This is the root head bone that other head bones rotate around
                        entity.setMainHeadBone(bone);
                    } else if(entry.getKey().equals("head_forward")) {
                        // Set forward bone used to calculate proper head rotation to allow for spewing effects
                        entity.setForwardBone(bone);
                    }
                }
            }
            frame.bones.put(bone,boneData);
        });
        //Logger.getGlobal().info("Frame loading: "+(System.currentTimeMillis()-start));
        return frame;
    }
}
