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

import com.google.common.base.Throwables;
import com.google.gson.JsonObject;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.permissions.Node;
import net.doubledoordev.pay2spawn.types.guis.FireworksTypeGui;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static net.doubledoordev.pay2spawn.util.Constants.*;

/**
 * @author Dries007
 */
public class FireworksType extends TypeBase
{
    public static final String NODENAME = "fireworks";

    public static final String FLIGHT_KEY  = "Flight";
    public static final String TYPE_KEY    = "Type";
    public static final String FLICKER_KEY = "Flicker";
    public static final String TRAIL_KEY   = "Trail";
    public static final String COLORS_KEY  = "Colors";
    public static final String FADECOLORS_KEY  = "FadeColors";

    public static final String EXPLOSIONS_KEY = "Explosions";
    public static final String FIREWORKS_KEY  = "Fireworks";

    public static final String RIDETHISMOB_KEY = "RideThisMob";

    public static final String RADIUS_KEY = "RADIUS";
    public static final String AMOUNT_KEY = "AMOUNT";

    public static final HashMap<String, String> typeMap = new HashMap<>();

    static
    {
        typeMap.put(FLIGHT_KEY, NBTTypes[BYTE]);
        typeMap.put(TYPE_KEY, NBTTypes[BYTE]);
        typeMap.put(FLICKER_KEY, NBTTypes[BYTE]);
        typeMap.put(TRAIL_KEY, NBTTypes[BYTE]);
        typeMap.put(COLORS_KEY, NBTTypes[INT_ARRAY]);
        typeMap.put(FADECOLORS_KEY, NBTTypes[INT_ARRAY]);

        typeMap.put(RIDETHISMOB_KEY, NBTTypes[BYTE]);
        typeMap.put(RADIUS_KEY, NBTTypes[INT]);
        typeMap.put(AMOUNT_KEY, NBTTypes[INT]);
    }

    private static final Field fireworkAgeField = getHackField(3);
    private static final Field lifetimeField    = getHackField(4);

    private static Field getHackField(int id)
    {
        try
        {
            Field f = FireworkRocketEntity.class.getDeclaredFields()[id];
            f.setAccessible(true);
            return f;
        }
        catch (Throwable t)
        {
            Throwables.propagate(t);
        }
        return null;
    }

    @Override
    public String getName()
    {
        return NODENAME;
    }

    @Override
    public CompoundTag getExample()
    {
        /**
         * YOU CAN'T TOUCH THIS.
         * No srsly. Touch it and you rebuild it from scratch!
         */
        ItemStack out = Items.FIREWORK_ROCKET.getDefaultInstance();//new ItemStack((Item) Item.itemRegistry.getObject("fireworks"));
        CompoundTag tag = new CompoundTag();
        CompoundTag fireworks = new CompoundTag();
        fireworks.putByte(FLIGHT_KEY, (byte) 0);

        ListTag explosions = new ListTag();
        CompoundTag explosion = new CompoundTag();
        explosion.putByte(TYPE_KEY, (byte) 0);
        explosion.putByte(FLICKER_KEY, (byte) 0);
        explosion.putByte(TRAIL_KEY, (byte) 0);
        explosion.putIntArray(COLORS_KEY, new int[]{0x12DE5D, 0x7B2FBE});
        explosion.putIntArray(FADECOLORS_KEY, new int[]{0xA9CF42, 0xF50CBF});
        explosions.add(explosion);
        explosion = new CompoundTag();
        explosion.putByte(TYPE_KEY, (byte) 1);
        explosion.putByte(FLICKER_KEY, (byte) 1);
        explosion.putByte(TRAIL_KEY, (byte) 0);
        explosion.putIntArray(COLORS_KEY, new int[]{0x12DE5D, 0x7B2FBE});
        explosion.putIntArray(FADECOLORS_KEY, new int[]{0xA9CF42, 0xF50CBF});
        explosions.add(explosion);
        fireworks.put(EXPLOSIONS_KEY, explosions);
        tag.put(FIREWORKS_KEY, fireworks);
        out.setTag(tag);

        tag = out.save(new CompoundTag());

        tag.putInt(RADIUS_KEY, 10);
        tag.putInt(AMOUNT_KEY, 10);

        return tag;
    }

    @Override
    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData)
    {
        ItemStack itemStack = ItemStack.of(dataFromClient);

        if (itemStack == null)
        {
            Pay2Spawn.getLogger().error("ItemStack from reward was null? NBT: {}", dataFromClient.toString());
            return;
        }

        int flight = 0;
        CompoundTag nbttagcompound1 = itemStack.getTag().getCompound(FIREWORKS_KEY);
        if (nbttagcompound1 != null) flight += nbttagcompound1.getByte(FLIGHT_KEY);

        try
        {
            int rndFirework = RANDOM.nextInt(dataFromClient.getInt(AMOUNT_KEY));
            int rad = dataFromClient.getInt(RADIUS_KEY);
            double angle = 2 * Math.PI / dataFromClient.getInt(AMOUNT_KEY);
            for (int i = 0; i < dataFromClient.getInt(AMOUNT_KEY); i++)
            {
                FireworkRocketEntity entityfireworkrocket = new FireworkRocketEntity(player.level, player.getX() + rad * Math.cos(angle * i), player.getY(), player.getZ() + rad * Math.sin(angle * i), itemStack.copy());
                fireworkAgeField.set(entityfireworkrocket, 1);
                lifetimeField.set(entityfireworkrocket, 10 + 10 * flight);
                player.level.addFreshEntity(entityfireworkrocket);
                if (i == rndFirework && dataFromClient.contains(RIDETHISMOB_KEY) && dataFromClient.getBoolean(RIDETHISMOB_KEY)) player.startRiding(entityfireworkrocket);
            }
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void openNewGui(int rewardID, JsonObject data)
    {
        new FireworksTypeGui(rewardID, getName(), data, typeMap);
    }

    @Override
    public Collection<Node> getPermissionNodes()
    {
        HashSet<Node> nodes = new HashSet<>();
        nodes.add(new Node(NODENAME));
        return nodes;
    }

    @Override
    public Node getPermissionNode(Player player, CompoundTag dataFromClient)
    {
        return new Node(NODENAME);
    }

    @Override
    public String replaceInTemplate(String id, JsonObject jsonObject)
    {
        return id;
    }
}
