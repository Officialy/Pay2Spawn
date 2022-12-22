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

package net.doubledoordev.d3core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.doubledoordev.d3core.D3Core;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.GameData;
import org.apache.commons.io.IOUtils;

import java.net.URL;
import java.nio.charset.Charset;

/**
 * Something other than capes for once
 *
 * @author Dries007
 */
public class DevPerks
{
    private JsonObject  perks = new JsonObject();

    public DevPerks()
    {
        try
        {
            perks = new JsonParser().parse(IOUtils.toString(new URL(CoreConstants.PERKSURL), Charset.forName("UTF-8"))).getAsJsonObject();
        }
        catch (Exception e)
        {
            if (D3Core.debug()) e.printStackTrace();
        }
    }

    public static ItemStack getItemStackFromJson(JsonObject data, int defaultMeta, int defaultStacksize)
    {
        int meta = data.has("meta") ? data.get("meta").getAsInt() : defaultMeta;
        int size = data.has("size") ? data.get("size").getAsInt() : defaultStacksize;
        ItemStack stack = new ItemStack(GameData.getItemRegistry().getObject(data.get("name").getAsString()), size, meta);
        if (data.has("display")) stack.setStackDisplayName(data.get("display").getAsString());
        if (data.has("color"))
        {
            CompoundTag root = stack.get();
            if (root == null) root = new CompoundTag();
            CompoundTag display = root.getCompound("display");
            display.putInt("color", data.get("color").getAsInt());
            root.put("display", display);
            stack.setTag(root);
        }
        if (data.has("lore"))
        {
            CompoundTag root = stack.get();
            if (root == null) root = new CompoundTag();
            CompoundTag display = root.getCompound("display");
            ListTag lore = new ListTag();
            for (JsonElement element : data.getAsJsonArray("lore")) lore.add(new StringTag(element.getAsString()));
            display.put("Lore", lore);
            root.put("display", display);
            stack.setTag(root);
        }
        return stack;
    }

    /**
     * Something other than capes for once
     */
    @SubscribeEvent
    public void nameFormatEvent(PlayerEvent.NameFormat event)
    {
        try
        {
            if (D3Core.debug()) perks = new JsonParser().parse(IOUtils.toString(new URL(CoreConstants.PERKSURL), Charset.forName("UTF-8"))).getAsJsonObject();
            if (perks.has(event.getUsername()))
            {
                JsonObject perk = perks.getAsJsonObject(event.getUsername());
                if (perk.has("displayname")) event.displayname = perk.get("displayname").getAsString();
                if (perk.has("hat") && (event.getPlayer().getInventory().armorInventory[3] == null || event.getPlayer().getInventory().armorInventory[3].stackSize == 0))
                {
                    ItemStack hat = getItemStackFromJson(perk.getAsJsonObject("hat"), 0, 0);
                    hat.setCount(0);
                    event.getPlayer().getInventory().armorInventory[3] = hat;
                }
            }
        }
        catch (Exception e)
        {
            if (D3Core.debug()) e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void cloneEvent(PlayerEvent.Clone event)
    {
        try
        {
            if (D3Core.debug()) perks = new JsonParser().parse(IOUtils.toString(new URL(CoreConstants.PERKSURL), Charset.forName("UTF-8"))).getAsJsonObject();
            if (perks.has(event.original.getCommandSenderName()))
            {
                JsonObject perk = perks.getAsJsonObject(event.original.getCommandSenderName());
                if (perk.has("hat") && (event.getPlayer().getInventory().getArmor(3) == null || event.getPlayer().getInventory().getArmor(3).getCount() == 0))
                {
                    ItemStack hat = getItemStackFromJson(perk.getAsJsonObject("hat"), 0, 0);
                    hat.setCount(0);
                    event.getPlayer().getInventory().armor.set(3, hat); //todo make sure this works
                }
            }
        }
        catch (Exception e)
        {
            if (D3Core.debug()) e.printStackTrace();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void deathEvent(LivingDropsEvent event)
    {
        try
        {
            if (D3Core.debug()) perks = new JsonParser().parse(IOUtils.toString(new URL(CoreConstants.PERKSURL), Charset.forName("UTF-8"))).getAsJsonObject();
            if (perks.has(event.getEntityLiving().getName()))
            {
                JsonObject perk = perks.getAsJsonObject(event.getEntityLiving().getCommandSenderName());
                if (perk.has("drop"))
                {
                    event.getDrops().add(new ItemEntity(event.getEntityLiving().getLevel(), event.getEntityLiving().getX(), event.getEntityLiving().getY(), event.getEntityLiving().getZ(), getItemStackFromJson(perk.getAsJsonObject("drop"), 0, 1)));
                }
            }
        }
        catch (Exception e)
        {
            if (D3Core.debug()) e.printStackTrace();
        }
    }

    public void update(boolean sillyness)
    {
        try
        {
            if (sillyness) MinecraftForge.EVENT_BUS.register(this);
            else MinecraftForge.EVENT_BUS.unregister(this);
        }
        catch (Exception e)
        {
            if (D3Core.debug()) e.printStackTrace();
        }
        try
        {
            if (sillyness) MinecraftForge.EVENT_BUS.register(this);
            else MinecraftForge.EVENT_BUS.unregister(this);
        }
        catch (Exception e)
        {
            if (D3Core.debug()) e.printStackTrace();
        }
    }
}
