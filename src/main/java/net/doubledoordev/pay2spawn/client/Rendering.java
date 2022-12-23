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

package net.doubledoordev.pay2spawn.client;

import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * @author Dries007
 */
public class Rendering {
    private CustomRender customRender; //todo THIS IS NULL

    private Rendering() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new Rendering());
//        Render render = RenderManager.instance.getEntityClassRenderObject(EntityZombie.class);
//        if (render instanceof RenderBiped)
//        {
//            RenderManager.instance.entityRenderMap.put(EntityZombie.class, new CustomRender((RenderBiped) render));
//        }
//        else
//        {
//            Pay2Spawn.getLogger().warn("Zombie reskining won't work because the zombie renderer has been overridden by another mod. Class: " + render.getClass());
//        }
    }

    @SubscribeEvent
    public void renderLivingEvent(RenderLivingEvent.Pre<Zombie, ZombieModel<Zombie>> event) {
        if (event.getEntity() instanceof Zombie zombie && event.getEntity().hasCustomName() && !(event.getRenderer() instanceof CustomRender)) {
            event.setCanceled(true);
            customRender.render(zombie, 0, 0, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        }
    }

    private static class CustomRender extends AbstractZombieRenderer<Zombie, ZombieModel<Zombie>> {

        public CustomRender(EntityRendererProvider.Context context) {
            this(context, ModelLayers.ZOMBIE, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR);
        }

        public CustomRender(EntityRendererProvider.Context context, ModelLayerLocation model, ModelLayerLocation inner, ModelLayerLocation outer) {
            super(context, new ZombieModel<>(context.bakeLayer(model)), new ZombieModel<>(context.bakeLayer(inner)), new ZombieModel<>(context.bakeLayer(outer)));
        }

        @Override
        public ResourceLocation getTextureLocation(Zombie zombie) {
            String name = zombie.getCustomName().getString();
            ResourceLocation location = AbstractClientPlayer.getSkinLocation(name);
            AbstractClientPlayer.registerSkinTexture(location, name);
            return location;
        }
    }
}
