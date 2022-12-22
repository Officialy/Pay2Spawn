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
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.permissions.Node;
import net.doubledoordev.pay2spawn.types.guis.RandomItemTypeGui;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.pay2spawn.util.JsonNBTHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.Player;
import net.minecraft.entity.player.ServerPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static net.doubledoordev.pay2spawn.util.Constants.NBTTypes;
import static net.doubledoordev.pay2spawn.util.Constants.STRING;

/**
 * @author Dries007
 */
public class RandomItemType extends TypeBase
{
    public static final String                  NAME_KEY    = "Name";
    public static final String                  DISPLAY_KEY = "display";
    public static final String                  TAG_KEY     = "tag";
    public static final HashMap<String, String> typeMap     = new HashMap<>();

    static
    {
        typeMap.put(NAME_KEY, NBTTypes[STRING]);
    }

    @Override
    public String getName()
    {
        return "randomItem";
    }

    @Override
    public CompoundTag getExample()
    {
        CompoundTag root = new CompoundTag();
        CompoundTag tag = new CompoundTag();
        CompoundTag display = new CompoundTag();
        display.putString(NAME_KEY, "$name");
        tag.put(DISPLAY_KEY, display);
        root.put(TAG_KEY, tag);

        return root;
    }

    @Override
    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData)
    {
        try
        {
            ItemStack is = ItemStack.loadItemStackFromNBT(dataFromClient);

            if (is == null)
            {
                Pay2Spawn.getLogger().error("ItemStack from reward was null? NBT: {}", dataFromClient.toString());
                return;
            }

            EntityItem entity = player.dropPlayerItemWithRandomChoice(is, false);
            entity.delayBeforeCanPickup = 0;
            entity.func_145797_a(player.getCommandSenderName());
        }
        catch (Exception e)
        {
            Pay2Spawn.getLogger().warn("ItemStack could not be spawned. Does the item exists? JSON: " + JsonNBTHelper.parseNBT(dataFromClient));
        }
    }

    @Override
    public void openNewGui(int rewardID, JsonObject data)
    {
        new RandomItemTypeGui(rewardID, getName(), data, typeMap);
    }

    @Override
    public Collection<Node> getPermissionNodes()
    {
        return new ArrayList<>();
    }

    @Override
    public Node getPermissionNode(Player player, CompoundTag dataFromClient)
    {
        ItemStack is;
        do
        {
            is = pickRandomItemStack();
        } while (is == null || is.getItem() == null);

        CompoundTag nbtTagCompound = is.writeToNBT(new CompoundTag());
        for (Object o : dataFromClient.getAllKeys())
        {
            nbtTagCompound.put(o.toString(), dataFromClient.get(o.toString()));
        }
        is.readFromNBT(nbtTagCompound);
        is.writeToNBT(dataFromClient);
        String name = is.getUnlocalizedName();
        return new Node(ItemType.NAME, name.replace(".", "_"));
    }

    @Override
    public String replaceInTemplate(String id, JsonObject jsonObject)
    {
        return id;
    }

    public ItemStack pickRandomItemStack()
    {
        ArrayList<ItemStack> itemStacks = new ArrayList<>();
        for (Object itemName : Item.itemRegistry.getKeys())
        {
            itemStacks.add(new ItemStack((Item) Item.itemRegistry.getObject(itemName)));
        }
        for (Object blockName : Block.blockRegistry.getKeys())
        {
            itemStacks.add(new ItemStack(Block.getBlockFromName(blockName.toString())));
        }

        return Helper.getRandomFromSet(itemStacks);
    }
}
