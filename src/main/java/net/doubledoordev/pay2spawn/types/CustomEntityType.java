///*
// * Copyright (c) 2014, DoubleDoorDevelopment
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *  Redistributions of source code must retain the above copyright notice, this
// *   list of conditions and the following disclaimer.
// *
// *  Redistributions in binary form must reproduce the above copyright notice,
// *   this list of conditions and the following disclaimer in the documentation
// *   and/or other materials provided with the distribution.
// *
// *  Neither the name of the project nor the names of its
// *   contributors may be used to endorse or promote products derived from
// *   this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package net.doubledoordev.pay2spawn.types;
//
//import com.google.gson.JsonObject;
//import net.doubledoordev.pay2spawn.Pay2Spawn;
//import net.doubledoordev.pay2spawn.permissions.BanHelper;
//import net.doubledoordev.pay2spawn.permissions.Node;
//import net.doubledoordev.pay2spawn.permissions.PermissionsHandler;
//import net.doubledoordev.pay2spawn.types.guis.CustomEntityTypeGui;
//import net.doubledoordev.pay2spawn.util.Constants;
//import net.doubledoordev.pay2spawn.util.Helper;
//import net.doubledoordev.pay2spawn.util.PointD;
//import net.doubledoordev.pay2spawn.util.Vector3;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.EntityList;
//import net.minecraft.entity.LivingEntity;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.util.ChatFormatting;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashSet;
//
//import static net.doubledoordev.pay2spawn.types.EntityType.*;
//
///**
// * A reward for complex custom entities
// * (aka custom nbt based ones)
// *
// * @author Dries007
// */
//public class CustomEntityType extends TypeBase
//{
//    private static final String NAME = "customeentity";
//
//    @Override
//    public String getName()
//    {
//        return NAME;
//    }
//
//    @Override
//    public CompoundTag getExample()
//    {
//        CompoundTag tag = new CompoundTag();
//        Entity entity = EntityList.createEntityByName("Wolf", null);
//        entity.writeMountToNBT(tag);
//        tag.putBoolean(AGRO_KEY, true);
//        return tag;
//    }
//
//    @Override
//    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData)
//    {
//        if (!dataFromClient.contains(SPAWNRADIUS_KEY)) dataFromClient.putInt(SPAWNRADIUS_KEY, 10);
//        ArrayList<PointD> pointDs = new PointD(player).getCylinder(dataFromClient.getInt(SPAWNRADIUS_KEY), 6);
//        CompoundTag p2sTag = new CompoundTag();
//        p2sTag.putString("Type", getName());
//        if (rewardData.contains("name")) p2sTag.putString("Reward", rewardData.getString("name"));
//
//        int count = 0;
//        if (!dataFromClient.contains(AMOUNT_KEY)) dataFromClient.putInt(AMOUNT_KEY, 1);
//        for (int i = 0; i < dataFromClient.getInt(AMOUNT_KEY); i++)
//        {
//            Entity entity = EntityList.createEntityFromNBT(dataFromClient, player.getLevel());
//
//            if (entity != null)
//            {
//                count++;
//                if (getSpawnLimit() != -1 && count > getSpawnLimit()) break;
//
//                entity.setPosition(player.getX(), player.getY(), player.getZ());
//                Helper.rndSpawnPoint(pointDs, entity);
//
//                if (dataFromClient.getBoolean(AGRO_KEY) && entity instanceof LivingEntity) ((LivingEntity) entity).setAttackTarget(player);
//
//                entity.getEntityData().put(Constants.NAME, p2sTag.copy());
//                player.level.addFreshEntity(entity);
//
//                Entity entity1 = entity;
//                for (CompoundTag tag = dataFromClient; tag.contains(RIDING_KEY); tag = tag.getCompound(RIDING_KEY))
//                {
//                    Entity entity2 = EntityList.createEntityFromNBT(tag.getCompound(RIDING_KEY), player.getLevel());
//
//                    Node node = this.getPermissionNode(player, tag.getCompound(RIDING_KEY));
//                    if (BanHelper.isBanned(node))
//                    {
//                        Helper.sendChatToPlayer(player, "This node (" + node + ") is banned.", ChatFormatting.RED);
//                        Pay2Spawn.getLogger().warn(player.getCommandSenderName() + " tried using globally banned node " + node + ".");
//                        continue;
//                    }
//                    if (PermissionsHandler.needPermCheck(player) && !PermissionsHandler.hasPermissionNode(player, node))
//                    {
//                        Pay2Spawn.getLogger().warn(player.getDisplayName() + " doesn't have perm node " + node.toString());
//                        continue;
//                    }
//
//                    if (entity2 != null)
//                    {
//                        count++;
//                        if (getSpawnLimit() != -1 && count > getSpawnLimit()) break;
//
//                        if (tag.getCompound(RIDING_KEY).getBoolean(AGRO_KEY) && entity2 instanceof LivingEntity) ((LivingEntity) entity2).setAttackTarget(player);
//
//                        entity2.setPosition(entity.getX(), entity.getY(), entity.getZ());
//                        entity2.getEntityData().put(Constants.NAME, p2sTag.copy());
//                        player.level.addFreshEntity(entity2);
//                        entity1.startRiding(entity2);
//                        if (tag.getCompound(RIDING_KEY).contains(RIDETHISMOB_KEY) && tag.getCompound(RIDING_KEY).getBoolean(RIDETHISMOB_KEY)) player.startRiding(entity2);
//                    }
//
//                    entity1 = entity2;
//                }
//                if (dataFromClient.contains(RIDETHISMOB_KEY) && dataFromClient.getBoolean(RIDETHISMOB_KEY)) player.startRiding(entity);
//                if (dataFromClient.contains(THROWTOWARDSPLAYER_KEY) && dataFromClient.getBoolean(THROWTOWARDSPLAYER_KEY))
//                {
//                    Vector3 v = new Vector3(entity, player).normalize();
//                    entity.motionX = 2 * v.x;
//                    entity.motionY = 2 * v.y;
//                    entity.motionZ = 2 * v.z;
//                }
//            }
//        }
//    }
//
//    @Override
//    public void openNewGui(int rewardID, JsonObject data)
//    {
//        new CustomEntityTypeGui(rewardID, getName(), data, EntityType.typeMap);
//    }
//
//    @Override
//    public Collection<Node> getPermissionNodes()
//    {
//        HashSet<Node> nodes = new HashSet<>();
//        for (String s : NAMES) nodes.add(new Node(NODENAME, s));
//        return nodes;
//    }
//
//    @Override
//    public Node getPermissionNode(Player player, CompoundTag dataFromClient)
//    {
//        return new Node(NODENAME, EntityList.getEntityString(EntityList.createEntityFromNBT(dataFromClient, player.getLevel())));
//    }
//
//    @Override
//    public String replaceInTemplate(String id, JsonObject jsonObject)
//    {
//        switch (id)
//        {
//            case "entity":
//                StringBuilder sb = new StringBuilder();
//                if (jsonObject.has("id")) sb.append(jsonObject.get("id").getAsString().replace("STRING:", ""));
//                else sb.append("null");
//                while (jsonObject.has(RIDING_KEY))
//                {
//                    jsonObject = jsonObject.getAsJsonObject(RIDING_KEY);
//                    sb.append(" riding a ").append(jsonObject.get("id").getAsString().replace("STRING:", ""));
//                }
//                return sb.toString();
//        }
//        return id;
//    }
//}
