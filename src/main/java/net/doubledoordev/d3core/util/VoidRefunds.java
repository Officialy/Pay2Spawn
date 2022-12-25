/*
 * Copyright (c) 2014,
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
 *  Neither the name of the {organization} nor the names of its
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
 *
 *
 */

package net.doubledoordev.d3core.util;

import net.doubledoordev.oldforge.Configuration;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.HashMap;
import java.util.UUID;

import static net.doubledoordev.d3core.util.CoreConstants.MODID;

/**
 * @author Dries007
 */
public class VoidRefunds
{
    public static final VoidRefunds VOID_REFUNDS = new VoidRefunds();
    private ResourceKey<Level>[] voidRefundDimensions;

    private final HashMap<UUID, Inventory> map = new HashMap<>();

    private VoidRefunds()
    {
    }

    public void config(Configuration configuration)
    {
        final String catVoidDeaths = MODID + ".VoidDeaths";
        configuration.addCustomCategoryComment(catVoidDeaths, "In these dimensions, when you die to void damage, you will keep your items.");
//    todo    voidRefundDimensions = configuration.get(catVoidDeaths, "refundDimensions", new int[] {}).getIntList();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void livingDeathEvent(LivingDeathEvent event)
    {
        if (FMLLoader.getDist().isClient()) return;
        if (event.getSource() != DamageSource.OUT_OF_WORLD || !(event.getEntity() instanceof Player)) return;
        if (event.getEntityLiving().lastHurt >= (Float.MAX_VALUE / 2)) return; // try to ignore /kill command
        for (ResourceKey<Level> dim : voidRefundDimensions)
        {
            if (dim != event.getEntity().getLevel().dimension()) continue;
            event.setCanceled(true);

            Inventory tempCopy = new Inventory(null);
//          todo  tempCopy.copyInventory(((Player) event.getEntity()).getInventory());
//    todo        tempCopy.save();
//            tempCopy.load();
            map.put(event.getEntity().getUUID(), tempCopy);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerRespawnEvent(PlayerEvent.PlayerRespawnEvent event)
    {
        if (FMLLoader.getDist().isClient()) return;
        Inventory oldInventory = map.get(event.getPlayer().getUUID());
        if (oldInventory == null) return;
        event.getPlayer().getInventory().replaceWith(oldInventory);
        map.remove(event.getPlayer().getUUID());
    }
}
