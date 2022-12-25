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

package net.doubledoordev.pay2spawn.util;

import com.google.gson.JsonObject;
import net.doubledoordev.pay2spawn.hud.Hud;
import net.doubledoordev.pay2spawn.network.NbtRequestMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

import static net.doubledoordev.pay2spawn.util.Constants.MODID;

/**
 * Handler for all forge events.
 *
 * @author Dries007
 */
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EventHandler
{
    static boolean entityTracking = false, blockTracking = false;
    private JsonObject perks;

    public EventHandler()
    {
        try
        {
            MinecraftForge.EVENT_BUS.register(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void addEntityTracking()
    {
        entityTracking = true;
    }

    public static void addBlockTracker()
    {
        blockTracking = true;
    }

    @SubscribeEvent
    public void event(PlayerInteractEvent e)
    {
        if (blockTracking)
        {
            blockTracking = false;

            NbtRequestMessage.requestBlock(e.getPos().getX(), e.getPos().getY(), e.getPos().getZ(), e.getWorld().dimension().getRegistryName());

            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void event(PlayerInteractEvent.EntityInteract event)
    {
        if (entityTracking)
        {
            entityTracking = false;
            NbtRequestMessage.requestByEntityID(event.getTarget().getId());
        }
    }

    @SubscribeEvent
    public void hudEvent(RenderGameOverlayEvent.Text event)
    {
        ArrayList<String> bottomLeft = new ArrayList<>();
        ArrayList<String> bottomRight = new ArrayList<>();

        Font fontRenderer = Minecraft.getInstance().font;

        Hud.INSTANCE.render(event.getLeft(), event.getRight(), bottomLeft, bottomRight);

        int baseHeight = event.getWindow().getGuiScaledHeight() - 25 - bottomLeft.size() * 10;
        if (!(Minecraft.getInstance().screen instanceof ChatScreen))
        {
            for (int x = 0; x < bottomLeft.size(); x++)
            {
                String msg = bottomLeft.get(x);
                fontRenderer.draw(event.getMatrixStack(), msg, 2, baseHeight + 2 + x * 10, 0xFFFFFF);
            }
        }

        baseHeight = event.getWindow().getGuiScaledHeight() - 25 - bottomRight.size() * 10;
        if (!(Minecraft.getInstance().screen instanceof ChatScreen))
        {
            for (int x = 0; x < bottomRight.size(); x++)
            {
                String msg = bottomRight.get(x);
                int w = fontRenderer.width(msg);
                fontRenderer.draw(event.getMatrixStack(), msg, event.getWindow().getGuiScaledWidth() - w - 10, baseHeight + 2 + x * 10, 0xFFFFFF);
            }
        }
    }
}
