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

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.permissions.Node;
import net.doubledoordev.pay2spawn.types.guis.ItemsTypeGui;
import net.doubledoordev.pay2spawn.util.Donation;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.pay2spawn.util.JsonNBTHelper;
import net.doubledoordev.pay2spawn.util.Reward;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static net.doubledoordev.pay2spawn.util.Constants.*;

/**
 * Upgraded version of the Item reward
 *
 * @author Dries007
 */
public class ItemsType extends TypeBase
{
    public static final String NAME = "items";

    public static final String SLOT_KEY   = "SLOT";
    public static final String WEIGHT_KEY = "WEIGHT";
    public static final String ITEMS_KEY  = "ITEMS";
    public static final String MODE_KEY   = "MODE";

    public static final byte MODE_ALL      = 0; // default mode
    public static final byte MODE_PICK_ONE = 1;

    public static final HashMap<String, String> typeMap = new HashMap<>();

    static
    {
        typeMap.put(SLOT_KEY, NBTTypes[INT]);
        typeMap.put(WEIGHT_KEY, NBTTypes[INT]);
        typeMap.put(MODE_KEY, NBTTypes[BYTE]);
    }

    public static void setConfigTags(CompoundTag tagCompound, Donation donation, Reward reward)
    {
        ItemStack itemStack = ItemStack.of(tagCompound);
        if (itemStack == null)
        {
            Pay2Spawn.getLogger().error("ItemStack from reward was null? NBT: {}", tagCompound.toString());
            return;
        }
    /* todo    if (!itemStack.hasDisplayName() && !Strings.isNullOrEmpty(Pay2Spawn.getConfig().allItemName))
        {
            itemStack.setStackDisplayName(Helper.formatText(Pay2Spawn.getConfig().allItemName, donation, reward));
        }*/
        if (Pay2Spawn.getConfig().allItemLore.length != 0)
        {
            CompoundTag root = itemStack.hasTag() ? itemStack.getTag() : new CompoundTag();
            itemStack.setTag(root);
            CompoundTag display = root.getCompound("display");
            root.put("display", display);
            if (!display.contains("Lore"))
            {
                ListTag lore = new ListTag();
                for (String line : Pay2Spawn.getConfig().allItemLore) lore.add(StringTag.valueOf(Helper.formatText(line, donation, reward)));
                display.put("Lore", lore);
            }
        }
        itemStack.save(tagCompound);
    }

    public static void spawnItemStackOnPlayer(ServerPlayer player, CompoundTag dataFromClient)
    {
        try
        {
            ItemStack itemStack = ItemStack.of(dataFromClient);

            if (itemStack == null)
            {
                Pay2Spawn.getLogger().error("ItemStack from reward was null? NBT: {}", dataFromClient.toString());
                return;
            }

            itemStack.setCount(((IntTag) dataFromClient.get("Count")).getAsInt()); //todo check cast
            while (itemStack.getCount() != 0)
            {
                ItemStack itemStack1 = itemStack.split(Math.min(itemStack.getMaxStackSize(), itemStack.getCount()));
                int id = dataFromClient.contains(SLOT_KEY) ? dataFromClient.getInt(SLOT_KEY) : -1;
                if (id != -1 && player.getInventory().getItem(id) == null)
                {
                    player.getInventory().setItem(id, itemStack1);
                }
                else
                {
                    ItemEntity entityitem = player.drop(itemStack1, false);
                    entityitem.setNoPickUpDelay();
//             todo       entityitem.func_145797_a(player.getCommandSenderName());
                }
            }
        }
        catch (Exception e)
        {
            Pay2Spawn.getLogger().warn("ItemStack could not be spawned. Does the item exists? JSON: " + JsonNBTHelper.parseNBT(dataFromClient));
        }

    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public CompoundTag getExample()
    {
        CompoundTag root = new CompoundTag();
        ListTag items = new ListTag();
        {
            ItemStack itemStack = new ItemStack(Items.GOLDEN_APPLE);
//    todo        itemStack.setStackDisplayName("$name");
            CompoundTag itemNbt = itemStack.save(new CompoundTag());
            itemNbt.putInt(WEIGHT_KEY, 3);
            items.add(itemNbt);
        }
        {
            ItemStack itemStack = new ItemStack(Items.MUSIC_DISC_13);
//     todo       itemStack.setStackDisplayName("$name");
            CompoundTag itemNbt = itemStack.save(new CompoundTag());
            items.add(itemNbt);
        }
        {
            ItemStack itemStack = new ItemStack(Items.GOLDEN_CARROT);
//     todo       itemStack.setStackDisplayName("$name");
            CompoundTag itemNbt = itemStack.save(new CompoundTag());
            items.add(itemNbt);
        }
        root.put(ITEMS_KEY, items);
        root.putByte(MODE_KEY, MODE_PICK_ONE);
        return root;
    }

    @Override
    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData)
    {
        if (dataFromClient.getByte(MODE_KEY) == MODE_ALL)
        {
            ListTag tagList = dataFromClient.getList(ITEMS_KEY, COMPOUND);
            for (int i = 0; i < tagList.size(); i++)
            {
                spawnItemStackOnPlayer(player, tagList.getCompound(i));
            }
        }
        else if (dataFromClient.getByte(MODE_KEY) == MODE_PICK_ONE)
        {
            ArrayList<CompoundTag> stacks = new ArrayList<>();
            ListTag tagList = dataFromClient.getList(ITEMS_KEY, COMPOUND);
            for (int i = 0; i < tagList.size(); i++)
            {
                CompoundTag tag = tagList.getCompound(i);
                if (!tag.contains(WEIGHT_KEY)) stacks.add(tag);
                else for (int j = 0; j < tag.getInt(WEIGHT_KEY); j++) stacks.add(tag);
            }
            spawnItemStackOnPlayer(player, Helper.getRandomFromSet(stacks));
        }
    }

    @Override
    public void openNewGui(int rewardID, JsonObject data)
    {
        new ItemsTypeGui(rewardID, NAME, data, typeMap);
    }

    @Override
    public Collection<Node> getPermissionNodes()
    {
        HashSet<Node> nodes = new HashSet<>();
        for (Object itemName : ForgeRegistries.ITEMS.getValues())
        {
            nodes.add(new Node(ItemType.NAME, itemName.toString().replace(".", "_")));
        }
        for (Object itemName : ForgeRegistries.BLOCKS.getValues())
        {
            nodes.add(new Node(ItemType.NAME, itemName.toString().replace(".", "_")));
        }
        return nodes;
    }

    @Override
    public Node getPermissionNode(Player player, CompoundTag dataFromClient)
    {
        return new Node(NAME);
    }

    @Override
    public String replaceInTemplate(String id, JsonObject jsonObject)
    {
        switch (id)
        {
            case "mode":
                return jsonObject.has(MODE_KEY) && jsonObject.get(MODE_KEY).getAsString().replace("BYTE:", "").equals(String.valueOf(MODE_ALL)) ? "all" : "one";
            case "items":
                JsonArray array = jsonObject.getAsJsonArray(ITEMS_KEY);
                StringBuilder sb = new StringBuilder(array.size() * 20);
                for (int i = 0; i < array.size(); i++)
                {
                    CompoundTag tagCompound = JsonNBTHelper.parseJSON(array.get(i).getAsJsonObject());
                    ItemStack itemStack = ItemStack.of(tagCompound);
                    if (itemStack == null)
                    {
                        Pay2Spawn.getLogger().error("ItemStack from reward was null? NBT: {}", tagCompound.toString());
                        continue;
                    }
                    sb.append(itemStack);
                }
                return sb.toString();
        }
        return id;
    }

    @Override
    public void addConfigTags(CompoundTag rewardNtb, Donation donation, Reward reward)
    {
        ListTag tagList = rewardNtb.getList(ITEMS_KEY, COMPOUND);
        for (int i = 0; i < tagList.size(); i++) setConfigTags(tagList.getCompound(i), donation, reward);
    }
}
