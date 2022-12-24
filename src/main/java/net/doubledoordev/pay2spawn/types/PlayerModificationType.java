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
//import net.doubledoordev.pay2spawn.permissions.Node;
//import net.doubledoordev.pay2spawn.types.guis.PlayerModificationTypeGui;
//import net.minecraft.ChatFormatting;
//import net.minecraft.network.chat.TextComponent;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.util.*;
//import net.minecraft.world.entity.player.Player;
//
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.HashSet;
//
//import static net.doubledoordev.pay2spawn.util.Constants.*;
//
///**
// * @author Dries007
// */
//public class PlayerModificationType extends TypeBase
//{
//    public static final String TYPE_KEY      = "type";
//    public static final String OPERATION_KEY = "operation";
//    public static final String AMOUNT_KEY    = "amount";
//
//    public static final int SET      = 0;
//    public static final int ADD      = 1;
//    public static final int SUBTRACT = 2;
//    public static final int ENABLE   = 3;
//    public static final int DISABLE  = 4;
//
//    public static final HashMap<String, String> typeMap = new HashMap<>();
//
//    static
//    {
//        typeMap.put(TYPE_KEY, NBTTypes[INT]);
//        typeMap.put(OPERATION_KEY, NBTTypes[INT]);
//        typeMap.put(AMOUNT_KEY, NBTTypes[FLOAT]);
//    }
//
//    @Override
//    public String getName()
//    {
//        return "playermodification";
//    }
//
//    @Override
//    public CompoundTag getExample()
//    {
//        CompoundTag data = new CompoundTag();
//        data.putInt(TYPE_KEY, Type.HUNGER.ordinal());
//        data.putInt(OPERATION_KEY, ADD);
//        data.putFloat(AMOUNT_KEY, 20F);
//        return data;
//    }
//
//    @Override
//    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData)
//    {
//        Type.values()[dataFromClient.getInt(TYPE_KEY)].doOnServer(player, dataFromClient);
//    }
//
//    @Override
//    public void openNewGui(int rewardID, JsonObject data)
//    {
//        new PlayerModificationTypeGui(rewardID, getName(), data, typeMap);
//    }
//
//    @Override
//    public Collection<Node> getPermissionNodes()
//    {
//        HashSet<Node> nodes = new HashSet<>();
//        for (Type type : Type.values()) nodes.add(new Node(getName(), type.name().toLowerCase()));
//        return nodes;
//    }
//
//    @Override
//    public Node getPermissionNode(Player player, CompoundTag dataFromClient)
//    {
//        return new Node(getName(), Type.values()[dataFromClient.getInt(TYPE_KEY)].name().toLowerCase());
//    }
//
//    @Override
//    public String replaceInTemplate(String id, JsonObject jsonObject)
//    {
//        switch (id)
//        {
//            case "type":
//                return Type.values()[Integer.parseInt(jsonObject.get(TYPE_KEY).getAsString().split(":", 2)[1])].name().toLowerCase();
//            case "operation":
//                switch (Integer.parseInt(jsonObject.get(OPERATION_KEY).getAsString().split(":", 2)[1]))
//                {
//                    case ADD:
//                        return "adding";
//                    case SUBTRACT:
//                        return "subtracting";
//                    case SET:
//                        return "setting";
//                    case ENABLE:
//                        return "enabling it" + (jsonObject.has(AMOUNT_KEY) ? " for" : "");
//                    case DISABLE:
//                        return "disabling it" + (jsonObject.has(AMOUNT_KEY) ? " for" : "");
//                }
//            case "amount":
//                if (jsonObject.has(AMOUNT_KEY)) return NUMBER_FORMATTER.format(Float.parseFloat(jsonObject.get(OPERATION_KEY).getAsString().split(":", 2)[1]));
//                else return "";
//        }
//        return id;
//    }
//
//    public enum Type
//    {
//        HEALTH(false)
//                {
//                    @Override
//                    public void doOnServer(Player player, CompoundTag dataFromClient)
//                    {
//                        switch (dataFromClient.getInt(OPERATION_KEY))
//                        {
//                            case ADD:
//                                player.setHealth(player.getHealth() + dataFromClient.getFloat(AMOUNT_KEY));
//                                break;
//                            case SUBTRACT:
//                                player.setHealth(player.getHealth() - dataFromClient.getFloat(AMOUNT_KEY));
//                                break;
//                            case SET:
//                                player.setHealth(dataFromClient.getFloat(AMOUNT_KEY));
//                                break;
//                        }
//                    }
//                },
//        HUNGER(false)
//                {
//                    @Override
//                    public void doOnServer(Player player, CompoundTag dataFromClient)
//                    {
//                        FoodStats food = player.getFoodStats();
//                        switch (dataFromClient.getInt(OPERATION_KEY))
//                        {
//                            case ADD:
//                                food.addStats((int) dataFromClient.getFloat(AMOUNT_KEY), 0);
//                                break;
//                            case SUBTRACT:
//                                food.addStats((int) -dataFromClient.getFloat(AMOUNT_KEY), 0);
//                                break;
//                            case SET:
//                                food.addStats(-food.getFoodLevel(), 0);
//                                food.addStats((int) dataFromClient.getFloat(AMOUNT_KEY), 0);
//                                break;
//                        }
//                    }
//                },
//        SATURATION(false)
//                {
//                    @Override
//                    public void doOnServer(Player player, CompoundTag dataFromClient)
//                    {
//                        FoodStats food = player.getFoodStats();
//                        switch (dataFromClient.getInt(OPERATION_KEY))
//                        {
//                            case ADD:
//                                food.addStats(0, dataFromClient.getFloat(AMOUNT_KEY));
//                                break;
//                            case SUBTRACT:
//                                food.addStats(0, -dataFromClient.getFloat(AMOUNT_KEY));
//                                break;
//                            case SET:
//                                food.addStats(0, -food.getSaturationLevel());
//                                food.addStats(0, dataFromClient.getFloat(AMOUNT_KEY));
//                                break;
//                        }
//                    }
//                },
//        XP(false)
//                {
//                    @Override
//                    public void doOnServer(Player player, CompoundTag dataFromClient)
//                    {
//                        switch (dataFromClient.getInt(OPERATION_KEY))
//                        {
//                            case ADD:
//                                player.giveExperiencePoints((int) dataFromClient.getFloat(AMOUNT_KEY)); //todo could be levels instead of points
//                                break;
//                            case SUBTRACT:
//                                player.giveExperiencePoints((int) Mth.clamp(-dataFromClient.getFloat(AMOUNT_KEY), -player.totalExperience, 0));
//                                break;
//                            case SET:
//                                player.displayClientMessage(new TextComponent("You can't set the XP amount with pay2spawn. Only add and subtract.").withStyle(ChatFormatting.AQUA), false);
//                        }
//                    }
//                },
//        XP_LEVEL(false)
//                {
//                    @Override
//                    public void doOnServer(Player player, CompoundTag dataFromClient)
//                    {
//                        switch (dataFromClient.getInt(OPERATION_KEY))
//                        {
//                            case ADD:
//                                player.giveExperiencePoints((int) dataFromClient.getFloat(AMOUNT_KEY));
//                                break;
//                            case SUBTRACT:
//                                player.giveExperiencePoints((int) -dataFromClient.getFloat(AMOUNT_KEY));
//                                break;
//                            case SET:// Trickery
//                                player.giveExperiencePoints(Integer.MIN_VALUE);
//                                player.giveExperiencePoints((int) dataFromClient.getFloat(AMOUNT_KEY));
//                        }
//                    }
//                },
//        FLIGHT(true)
//                {
//                    @Override
//                    public void doOnServer(Player player, CompoundTag dataFromClient)
//                    {
//                        switch (dataFromClient.getInt(OPERATION_KEY))
//                        {
//                            case ENABLE:
//                                player.capabilities.allowFlying = true;
//                                player.capabilities.isFlying = true;
//                                player.sendPlayerAbilities();
//                                if (dataFromClient.contains(AMOUNT_KEY))
//                                {
//                                    CompoundTag tagCompound = player.getEntityData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("P2S");
//                                    tagCompound.putInt(name(), (int) (dataFromClient.getFloat(AMOUNT_KEY) * 20));
//                                    if (!player.getEntityData().contains(Player.PERSISTED_NBT_TAG)) player.getEntityData().put(Player.PERSISTED_NBT_TAG, new CompoundTag());
//                                    player.getEntityData().getCompound(Player.PERSISTED_NBT_TAG).put("P2S", tagCompound);
//                                }
//                                break;
//                            case DISABLE:
//                                player.capabilities.allowFlying = false;
//                                player.capabilities.isFlying = false;
//                                player.sendPlayerAbilities();
//                                break;
//                        }
//                    }
//
//                    @Override
//                    public void undo(Player player)
//                    {
//                        player.capabilities.allowFlying = player.isCreative();
//                        player.capabilities.isFlying = player.isCreative();
//                        player.sendPlayerAbilities();
//                    }
//                },
//        INVULNERABILITY(true)
//                {
//                    @Override
//                    public void doOnServer(Player player, CompoundTag dataFromClient)
//                    {
//                        switch (dataFromClient.getInt(OPERATION_KEY))
//                        {
//                            case ENABLE:
//                                player.disableDamage = true;
//                                player.sendPlayerAbilities();
//                                if (dataFromClient.contains(AMOUNT_KEY))
//                                {
//                                    CompoundTag tagCompound = player.getEntityData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("P2S");
//                                    tagCompound.putInt(name(), (int) (dataFromClient.getFloat(AMOUNT_KEY) * 20));
//                                    if (!player.getEntityData().contains(Player.PERSISTED_NBT_TAG)) player.getEntityData().put(Player.PERSISTED_NBT_TAG, new CompoundTag());
//                                    player.getEntityData().getCompound(Player.PERSISTED_NBT_TAG).put("P2S", tagCompound);
//                                }
//                                break;
//                            case DISABLE:
//                                player.capabilities.disableDamage = false;
//                                player.sendPlayerAbilities();
//                                break;
//                        }
//                    }
//
//                    @Override
//                    public void undo(Player player)
//                    {
//                        player.capabilities.disableDamage = player.capabilities.isCreativeMode;
//                        player.sendPlayerAbilities();
//                    }
//                };
//        private boolean timable;
//
//        Type(boolean timable)
//        {
//            this.timable = timable;
//        }
//
//        public abstract void doOnServer(Player player, CompoundTag dataFromClient);
//
//        public boolean isTimable()
//        {
//            return timable;
//        }
//
//        public void undo(Player player)
//        {
//
//        }
//    }
//}
