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
import net.doubledoordev.pay2spawn.types.guis.ItemTypeGui;
import net.doubledoordev.pay2spawn.util.Donation;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.pay2spawn.util.JsonNBTHelper;
import net.doubledoordev.pay2spawn.util.Reward;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static net.doubledoordev.pay2spawn.util.Constants.INT;
import static net.doubledoordev.pay2spawn.util.Constants.NBTTypes;

/**
 * Spawn an itemstack
 * Can handle all custom NBT data
 *
 * @author Dries007
 */
public class ItemType extends TypeBase
{
    public static final String NAME = "item";

    public static final String                  SLOT_KEY = "SLOT";
    public static final HashMap<String, String> typeMap  = new HashMap<>();

    static
    {
        typeMap.put(SLOT_KEY, NBTTypes[INT]);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public CompoundTag getExample()
    {
        ItemStack is = new ItemStack(Items.GOLDEN_APPLE);
//        todo is.setStackDisplayName("$name");
        return is.save(new CompoundTag());
    }

    @Override
    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData)
    {
        ItemsType.spawnItemStackOnPlayer(player, dataFromClient);
    }

    @Override
    public void printHelpList(File configFolder)
    {
        File file = new File(configFolder, "Enchantment.txt");
        try
        {
            if (file.exists()) file.delete();
            file.createNewFile();
            PrintWriter pw = new PrintWriter(file);

            pw.println("Enchantment list file");

            ArrayList<String> ids = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> minlvl = new ArrayList<>();
            ArrayList<String> maxlvl = new ArrayList<>();
           /*todo for (Enchantment enchantment : Enchantments.enchantmentsList)
            {
                if (enchantment != null)
                {
                    ids.add(enchantment.getRegistryName() + "");
                    names.add(enchantment.getTranslatedName(enchantment.getMinLevel()));
                    minlvl.add(enchantment.getMinLevel() + "");
                    maxlvl.add(enchantment.getMaxLevel() + "");
                }
            }*/
            pw.print(Helper.makeTable(new Helper.TableData("ID", ids), new Helper.TableData("name", names), new Helper.TableData("minLvl", minlvl), new Helper.TableData("maxLvl", maxlvl)));

            pw.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void openNewGui(int rewardID, JsonObject data)
    {
        new ItemTypeGui(rewardID, getName(), data, typeMap);
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
        ItemStack itemStack = ItemStack.of(dataFromClient);
        if (itemStack == null)
        {
            Pay2Spawn.getLogger().error("ItemStack from reward was null? NBT: {}", dataFromClient.toString());
            return new Node(NAME, "null");
        }
        return new Node(NAME, itemStack.getDescriptionId().replace(".", "_"));
    }

    @Override
    public String replaceInTemplate(String id, JsonObject jsonObject)
    {
        switch (id)
        {
            case "stacksize":
                return jsonObject.get("Count").getAsString().replace("BYTE:", "");
            case "itemname":
                CompoundTag tagCompound = JsonNBTHelper.parseJSON(jsonObject);
                ItemStack is = ItemStack.of(tagCompound);
                if (is == null)
                {
                    Pay2Spawn.getLogger().error("ItemStack from reward was null? NBT: {}", tagCompound.toString());
                    return "null";
                }
                return is.getItem().getName(is).toString();
        }
        return id;
    }

    @Override
    public void addConfigTags(CompoundTag rewardNtb, Donation donation, Reward reward)
    {
        ItemsType.setConfigTags(rewardNtb, donation, reward);
    }
}
