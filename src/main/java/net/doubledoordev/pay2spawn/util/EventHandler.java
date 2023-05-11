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
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.hud.Hud;
import net.doubledoordev.pay2spawn.network.NbtRequestMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
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
public class EventHandler {
    static boolean entityTracking = false, blockTracking = false;
    private JsonObject perks;

    public EventHandler() {
        try {
            MinecraftForge.EVENT_BUS.register(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addEntityTracking() {
        entityTracking = true;
    }

    public static void addBlockTracker() {
        blockTracking = true;
    }

    @SubscribeEvent
    public void event(PlayerInteractEvent e) {
        if (blockTracking) {
            blockTracking = false;

            NbtRequestMessage.requestBlock(e.getPos().getX(), e.getPos().getY(), e.getPos().getZ(), e.getLevel().dimension().registry());

            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void event(PlayerInteractEvent.EntityInteract event) {
        if (entityTracking) {
            entityTracking = false;
            NbtRequestMessage.requestByEntityID(event.getTarget().getId());
        }
    }

    @SubscribeEvent
    public void hudEvent(RenderGuiOverlayEvent event) {
        if (event.isCanceled())
            return;

        ArrayList<String> bottomLeft = new ArrayList<>();
        ArrayList<String> bottomRight = new ArrayList<>();
        ArrayList<String> left = new ArrayList<>();
        ArrayList<String> right = new ArrayList<>();

        Font fontRenderer = Minecraft.getInstance().font;

        Hud.INSTANCE.render(left, right, bottomLeft, bottomRight);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        int baseHeight = 2; // Offset from top of screen
        for (int x = 0; x < left.size(); x++) {
            String msg = left.get(x);
//            fontRenderer.draw(event.getPoseStack(), msg, 2, baseHeight + x * 10, 0xFFFFFF);
            var renderbuffer = new RenderBuffers();
            MultiBufferSource.BufferSource bufferSource = renderbuffer.bufferSource();
            var buffer = bufferSource.getBuffer(RenderType.text(new ResourceLocation("textures/font/ascii.png")));

            fontRenderer.drawInBatch(msg, 2, baseHeight + x * 10, 0xFFFFFF, false, event.getPoseStack().last().pose(), bufferSource, false, 0, 15728880, false);
            bufferSource.endBatch();
        }

        for (int x = 0; x < right.size(); x++) {
            String msg = right.get(x);
            int w = fontRenderer.width(msg);
            fontRenderer.draw(event.getPoseStack(), msg, event.getWindow().getGuiScaledWidth() - w - 2, baseHeight + x * 10, 0xFFFFFF);
        }


        baseHeight = event.getWindow().getGuiScaledHeight() - 25 - bottomLeft.size() * 10;
        if (!(Minecraft.getInstance().screen instanceof ChatScreen)) {
            for (int x = 0; x < bottomLeft.size(); x++) {
                String msg = bottomLeft.get(x);
                fontRenderer.draw(event.getPoseStack(), msg, 2, baseHeight + 2 + x * 10, 0xFFFFFF);
            }
        }

        baseHeight = event.getWindow().getGuiScaledHeight() - 25 - bottomRight.size() * 10;
        if (!(Minecraft.getInstance().screen instanceof ChatScreen)) {
            for (int x = 0; x < bottomRight.size(); x++) {
                String msg = bottomRight.get(x);
                int w = fontRenderer.width(msg);
                fontRenderer.draw(event.getPoseStack(), msg, event.getWindow().getGuiScaledWidth() - w - 10, baseHeight + 2 + x * 10, 0xFFFFFF);
            }
        }
        RenderSystem.disableBlend();
    }

}