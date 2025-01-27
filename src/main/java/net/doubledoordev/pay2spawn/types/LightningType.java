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
import net.doubledoordev.pay2spawn.permissions.Node;
import net.doubledoordev.pay2spawn.types.guis.LightningTypeGui;
import net.doubledoordev.pay2spawn.util.Helper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static net.doubledoordev.pay2spawn.util.Constants.*;

/**
 * Strikes the world within 1 block of the player
 * (randomness to avoid always striking a full 6 heart hit)
 * <p/>
 * No extra data
 *
 * @author Dries007
 */
public class LightningType extends TypeBase {
    public static final String NODENAME = "lightning";
    public static final String SPREAD_KEY = "spread";
    public static final String TYPE_KEY = "type";

    public static final int PLAYER_ENTITY = 0;
    public static final int NEAREST_ENTITY = 1;
    public static final int RND_ENTITY = 2;
    public static final int RND_SPOT = 3;

    public static final HashMap<String, String> typeMap = new HashMap<>();

    static {
        typeMap.put(SPREAD_KEY, NBTTypes[INT]);
        typeMap.put(TYPE_KEY, NBTTypes[INT]);
    }

    @Override
    public String getName() {
        return NODENAME;
    }

    @Override
    public CompoundTag getExample() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(SPREAD_KEY, 10);
        nbt.putInt(TYPE_KEY, RND_ENTITY);
        return nbt;
    }

    @Override
    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData) {
        if (!dataFromClient.contains(SPREAD_KEY)) dataFromClient.putInt(SPREAD_KEY, 10);
        double spread = dataFromClient.getInt(SPREAD_KEY);
        double X = player.getX(), Y = player.getY() - 1, Z = player.getZ();
        if (!dataFromClient.contains(TYPE_KEY)) dataFromClient.putInt(TYPE_KEY, RND_SPOT);

        switch (dataFromClient.getInt(TYPE_KEY)) {
            case PLAYER_ENTITY: {
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(player.getLevel());
                lightning.setPos(X, Y, Z);
                player.getLevel().addFreshEntity(lightning);
                break;
            }
            case NEAREST_ENTITY: {
                AABB AABB = new AABB(X - spread, Y - spread, Z - spread, X + spread, Y + spread, Z + spread);
                Entity entity = player.getLevel().getNearestEntity(LivingEntity.class, TargetingConditions.forNonCombat(), player, 0, 0, 0, AABB); //todo ints?
                if (entity != null) {

                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(player.getLevel());
                    lightning.setPos(entity.getX(), entity.getY(), entity.getZ());
                    player.getLevel().addFreshEntity(lightning);
                } else {
                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(player.getLevel());
                    lightning.setPos(X, Y, Z);
                    player.getLevel().addFreshEntity(lightning);
                }
                break;
            }
            case RND_SPOT: {
                X += (spread - (RANDOM.nextDouble() * spread * 2));
                Z += (spread - (RANDOM.nextDouble() * spread * 2));
                Y += (3 - RANDOM.nextDouble() * 6);
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(player.getLevel());
                lightning.setPos(X, Y, Z);
                player.getLevel().addFreshEntity(lightning);
                break;
            }
          /* todo case RND_ENTITY: {
                *//*IEntitySelector iEntitySelector = new IEntitySelector() {
                    @Override
                    public boolean isEntityApplicable(Entity entity) {
                        return entity instanceof LivingEntity;
                    }
                };*//*
                AABB aabb = new AABB(X - spread, Y - spread, Z - spread, X + spread, Y + spread, Z + spread);
                //noinspection unchecked
                List<LivingEntity> entity = player.level.getEntitiesWithinAABBExcludingEntity(player, aabb, iEntitySelector);
                LivingEntity entityLiving = Helper.getRandomFromSet(entity);
                if (entityLiving != null) {
                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(player.getLevel());
                    lightning.setPos(entityLiving.getX(), entityLiving.getY(), entityLiving.getZ());
                    player.getLevel().addFreshEntity(lightning);
                } else {
                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(player.getLevel());
                    lightning.setPos(X, Y, Z);
                    player.getLevel().addFreshEntity(lightning);
                }
            }*/
        }
    }

    @Override
    public void openNewGui(int rewardID, JsonObject data) {
        new LightningTypeGui(rewardID, getName(), data, typeMap);
    }

    @Override
    public Collection<Node> getPermissionNodes() {
        HashSet<Node> nodes = new HashSet<>();
        nodes.add(new Node(NODENAME, "player"));
        nodes.add(new Node(NODENAME, "nearest"));
        nodes.add(new Node(NODENAME, "rnd_entity"));
        nodes.add(new Node(NODENAME, "rnd_spot"));
        return nodes;
    }

    @Override
    public Node getPermissionNode(Player player, CompoundTag dataFromClient) {
        if (!dataFromClient.contains(TYPE_KEY)) dataFromClient.putInt(TYPE_KEY, RND_SPOT);
        return switch (dataFromClient.getInt(TYPE_KEY)) {
            case PLAYER_ENTITY -> new Node(NODENAME, "player");
            case NEAREST_ENTITY -> new Node(NODENAME, "nearest");
            case RND_SPOT -> new Node(NODENAME, "rnd_entity");
            case RND_ENTITY -> new Node(NODENAME, "rnd_spot");
            default -> new Node(NODENAME, "player");
        };
    }

    @Override
    public String replaceInTemplate(String id, JsonObject jsonObject) {
        switch (id) {
            case "target":
                switch (Integer.parseInt(jsonObject.get(TYPE_KEY).getAsString().split(":", 2)[1])) {
                    case PLAYER_ENTITY -> {
                        return "the streamer";
                    }
                    case NEAREST_ENTITY -> {
                        return "the nearest entity";
                    }
                    case RND_SPOT -> {
                        return "a random near spot";
                    }
                    case RND_ENTITY -> {
                        return "a random near entity";
                    }
                }
        }
        return id;
    }
}
