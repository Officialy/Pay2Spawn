/*
 * Copyright (c) 2014, DoubleDoorDevelopment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the project nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.doubledoordev.pay2spawn.types;

import com.google.gson.JsonObject;
import net.doubledoordev.oldforge.Configuration;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.permissions.BanHelper;
import net.doubledoordev.pay2spawn.permissions.Node;
import net.doubledoordev.pay2spawn.permissions.PermissionsHandler;
import net.doubledoordev.pay2spawn.types.guis.EntityTypeGui;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.pay2spawn.util.PointD;
import net.doubledoordev.pay2spawn.util.Vector3;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static net.doubledoordev.pay2spawn.util.Constants.*;

/**
 * A simple entity spawner, can handle:
 * - agroing mobs
 * - custom name tags
 * - entities riding entities to infinity
 *
 * @author Dries007
 */
public class EntityType extends TypeBase {
    public static final String ENTITYNAME_KEY = "name";
    public static final String SPAWNRADIUS_KEY = "SPAWNRADIUS";
    public static final String AMOUNT_KEY = "AMOUNT";
    public static final String AGRO_KEY = "agro";
    public static final String CUSTOMNAME_KEY = "CustomName";
    public static final String RIDING_KEY = "Riding";
    public static final String RIDETHISMOB_KEY = "RideThisMob";
    public static final String RANDOM_KEY = "random";
    public static final String THROWTOWARDSPLAYER_KEY = "throwTowardsPlayer";
    public static final HashSet<String> NAMES = new HashSet<>();
    public static final HashMap<String, String> typeMap = new HashMap<>();
    private static final String NAME = "entity";
    public static final String NODENAME = NAME;

    private static int spawnLimit = 100;

    static {
        typeMap.put(ENTITYNAME_KEY, NBTTypes[STRING]);
        typeMap.put(SPAWNRADIUS_KEY, NBTTypes[INT]);
        typeMap.put(AMOUNT_KEY, NBTTypes[INT]);
        typeMap.put(AGRO_KEY, NBTTypes[BYTE]);
        typeMap.put(CUSTOMNAME_KEY, NBTTypes[STRING]);
        typeMap.put(RIDETHISMOB_KEY, NBTTypes[BYTE]);
        typeMap.put(RANDOM_KEY, NBTTypes[BYTE]);
        typeMap.put(THROWTOWARDSPLAYER_KEY, NBTTypes[BYTE]);
    }

    public static int getSpawnLimit() {
        return spawnLimit;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CompoundTag getExample() {
        CompoundTag tag = new CompoundTag();
        tag.putString(ENTITYNAME_KEY, "$randomEntity");
        tag.putBoolean(AGRO_KEY, true);
        tag.putBoolean(RANDOM_KEY, true);
        tag.putString(CUSTOMNAME_KEY, "$name");

        CompoundTag tag2 = new CompoundTag();
        tag2.putString(ENTITYNAME_KEY, "$randomEntity");
        tag2.putBoolean(AGRO_KEY, true);
        tag2.putString(CUSTOMNAME_KEY, "$name");
        tag2.putBoolean(RIDETHISMOB_KEY, true);

        tag.put(RIDING_KEY, tag2);
        tag.putInt(SPAWNRADIUS_KEY, 10);
        tag.putInt(AMOUNT_KEY, 2);

        return tag;
    }

    @Override
    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData) {

        if (!dataFromClient.contains(SPAWNRADIUS_KEY)) dataFromClient.putInt(SPAWNRADIUS_KEY, 10);
        ArrayList<PointD> pointDs = new PointD(player).makeNiceForBlock().getCylinder(dataFromClient.getInt(SPAWNRADIUS_KEY), 6);
        CompoundTag p2sTag = new CompoundTag();
        p2sTag.putString("Type", getName());
        if (rewardData.contains("name")) p2sTag.putString("Reward", rewardData.getString("name"));

        int count = 0;
        if (!dataFromClient.contains(AMOUNT_KEY)) dataFromClient.putInt(AMOUNT_KEY, 1);
        for (int i = 0; i < dataFromClient.getInt(AMOUNT_KEY); i++) {
            Entity entity = Helper.getEntityByName(dataFromClient.getString(ENTITYNAME_KEY), player.getLevel());

            if (entity != null) {
                count++;
                if (getSpawnLimit() != -1 && count > getSpawnLimit())
                    break;
                entity.setPos(player.getX(), player.getY() + 1.1, player.getZ());
                Helper.rndSpawnPoint(pointDs, entity);

                if (dataFromClient.getBoolean(AGRO_KEY) && entity instanceof Mob) {
                    ((Mob) entity).setTarget(player);
                    ((Mob) entity).setPersistenceRequired();
                }
                if (dataFromClient.contains(CUSTOMNAME_KEY) && entity instanceof LivingEntity)
                    entity.setCustomName(Component.literal(dataFromClient.getString(CUSTOMNAME_KEY)));

                entity.setCustomName(Component.literal(p2sTag.getString("name"))); //todo Constants.NAME
                player.getLevel().addFreshEntity(entity);

                Entity entity1 = entity;
                for (CompoundTag tag = dataFromClient; tag.contains(RIDING_KEY); tag = tag.getCompound(RIDING_KEY)) {
                    Entity entity2 = Helper.getEntityByName(tag.getCompound(RIDING_KEY).getString(ENTITYNAME_KEY), player.getLevel());

                    Node node = this.getPermissionNode(player, tag.getCompound(EntityType.RIDING_KEY));
                    if (BanHelper.isBanned(node)) {
                        Helper.sendChatToPlayer(player, "This node (" + node + ") is banned.", ChatFormatting.RED);
                        Pay2Spawn.getLogger().warn(player.getName() + " tried using globally banned node " + node + ".");
                        continue;
                    }
                    if (PermissionsHandler.needPermCheck(player) && !PermissionsHandler.hasPermissionNode(player, node)) {
                        Pay2Spawn.getLogger().warn(player.getDisplayName() + " doesn't have perm node " + node.toString());
                        continue;
                    }

                    if (entity2 != null) {
                        count++;
                        if (getSpawnLimit() != -1 && count > getSpawnLimit()) break;

                        if (tag.getCompound(RIDING_KEY).getBoolean(AGRO_KEY) && entity2 instanceof LivingEntity)
                            ((LivingEntity) entity2).doHurtTarget(player);
                        if (tag.getCompound(RIDING_KEY).contains(CUSTOMNAME_KEY) && entity2 instanceof LivingEntity)
                            (entity2).setCustomName(Component.literal(tag.getCompound(RIDING_KEY).getString(CUSTOMNAME_KEY)));

                        entity2.setPos(entity.getX(), entity.getY(), entity.getZ());
                        entity2.setCustomName(Component.literal(p2sTag.getString("name"))); //todo Constants.NAME
                        player.level.addFreshEntity(entity2);
                        entity1.startRiding(entity2);

                        if (tag.getCompound(RIDING_KEY).contains(RIDETHISMOB_KEY) && tag.getCompound(RIDING_KEY).getBoolean(RIDETHISMOB_KEY))
                            player.startRiding(entity2);
                    }

                    entity1 = entity2;
                }
                if (dataFromClient.contains(RIDETHISMOB_KEY) && dataFromClient.getBoolean(RIDETHISMOB_KEY))
                    player.startRiding(entity);
                if (dataFromClient.contains(THROWTOWARDSPLAYER_KEY) && dataFromClient.getBoolean(THROWTOWARDSPLAYER_KEY)) {
                    Vector3 v = new Vector3(entity, player).normalize();
                    entity.setDeltaMovement(2 * v.x, 2 * v.y, 2 * v.z);
                }
            }
        }
    }

    @Override
    public void doConfig(Configuration configuration) {
        configuration.addCustomCategoryComment(TYPES_CAT, "Reward config options");
        configuration.addCustomCategoryComment(TYPES_CAT + '.' + NAME, "Used for Entity and CustomEntity");
        spawnLimit = configuration.get(TYPES_CAT + '.' + NAME, "spawnLimit", spawnLimit, "A hard entity spawn limit. Only counts 1 reward's mobs. -1 for no limit.").getInt(spawnLimit);
    }

    @Override
    public void printHelpList(File configFolder) {
        File file = new File(configFolder, "EntityList.txt");
        try {
            if (file.exists()) file.delete();
            file.createNewFile();
            PrintWriter pw = new PrintWriter(file);

            pw.println("This is a list of all the entities you can use in the json file.");


            ArrayList<String> modId = new ArrayList<>();
            ArrayList<String> name = new ArrayList<>();

            for (ResourceLocation entry : ForgeRegistries.ENTITY_TYPES.getKeys().stream().filter(s -> !s.getPath().contains("player") && !s.getPath().contains("potion")).toList()) {
                modId.add(entry.getNamespace());
                name.add(entry.getPath());

                NAMES.add(entry.getPath());
            }

            pw.print(Helper.makeTable(new Helper.TableData("modIDs", modId), new Helper.TableData("name", name)));

            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openNewGui(int rewardID, JsonObject data) {
        new EntityTypeGui(rewardID, getName(), data, typeMap);
    }

    @Override
    public Collection<Node> getPermissionNodes() {
        HashSet<Node> nodes = new HashSet<>();
        for (String s : EntityType.NAMES) nodes.add(new Node(NODENAME, s));
        return nodes;
    }

    @Override
    public Node getPermissionNode(Player player, CompoundTag dataFromClient) {
        return new Node(NODENAME, dataFromClient.getString(ENTITYNAME_KEY));
    }

    @Override
    public String replaceInTemplate(String id, JsonObject jsonObject) {
        switch (id) {
            case "entity":
                StringBuilder sb = new StringBuilder();
                sb.append(jsonObject.get(ENTITYNAME_KEY).getAsString().replace("STRING:", ""));
                while (jsonObject.has(RIDING_KEY)) {
                    jsonObject = jsonObject.getAsJsonObject(RIDING_KEY);
                    sb.append(" riding a ").append(jsonObject.get(ENTITYNAME_KEY).getAsString().replace("STRING:", ""));
                }
                return sb.toString();
        }
        return id;
    }
}
