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

import com.google.gson.JsonParseException;
import net.doubledoordev.d3core.D3Core;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Dries007
 */
public class FMLEventHandler
{
    public static final FMLEventHandler FML_EVENT_HANDLER = new FMLEventHandler();
    private FMLEventHandler()
    {
    }

    public boolean norain;
    public boolean insomnia;
    public boolean lilypad;

    @SubscribeEvent
    public void worldTickHandler(TickEvent.WorldTickEvent event)
    {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.START) return;

        if (norain)
        {
            ServerLevelData worldInfo = event.world.getServer().getWorldData().overworldData();
            worldInfo.setThundering(false);
            worldInfo.setRaining(false);
            worldInfo.setRainTime(Integer.MAX_VALUE);
            worldInfo.setThunderTime(Integer.MAX_VALUE);
        }
    }

    int aprilFoolsDelay = 0;
    @SubscribeEvent
    public void playerTickHandler(TickEvent.PlayerTickEvent event)
    {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.START) return;

        if (insomnia)
        {
            if (event.player.getSleepTimer() > 90)
            {
                event.player.sleepCounter = 90;
            }
        }


        if (CoreConstants.isAprilFools())
        {
            if (aprilFoolsDelay-- == 0)
            {
                aprilFoolsDelay = 100 * (5 + CoreConstants.RANDOM.nextInt(event.player.getServer().getPlayerCount()));
                CoreConstants.spawnRandomFireworks(event.player, 1 + CoreConstants.RANDOM.nextInt(5), 1 + CoreConstants.RANDOM.nextInt(5));
            }
        }
    }

    @SubscribeEvent
    public void playerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event)
    {
        File file = new File(D3Core.getFolder(), "loginmessage.txt");
       /*todo if (file.exists())
        {
            try
            {
                String txt = FileUtils.readFileToString(file);
                try
                {
                    event.getPlayer().displayClientMessage(IChatComponent.Serializer.func_150699_a(txt));
                }
                catch (JsonParseException jsonparseexception)
                {
                    event.getPlayer().displayClientMessage(new TextComponent(txt), false); //todo boolean
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }*/

        if (lilypad) lilypad(event.getPlayer());
        if (CoreConstants.isAprilFools()) CoreConstants.spawnRandomFireworks(event.getPlayer(), 1 + CoreConstants.RANDOM.nextInt(5), 1 + CoreConstants.RANDOM.nextInt(5));
    }

    @SubscribeEvent
    public void playerRespawnEvent(PlayerEvent.PlayerRespawnEvent event)
    {
        if (lilypad) lilypad(event.getPlayer());
        if (CoreConstants.isAprilFools()) CoreConstants.spawnRandomFireworks(event.getPlayer(), 1 + CoreConstants.RANDOM.nextInt(5), 1 + CoreConstants.RANDOM.nextInt(5));
    }

    private void lilypad(Player player)
    {
        Level world = player.level;
        int x = (int)(player.getX());
        int y = (int)(player.getY());
        int z = (int)(player.getZ());

        if (x < 0) x --;
        if (z < 0) z --;

        int limiter = world.getHeight() * 2;

        while (world.getBlockState(new BlockPos(x, y, z)).getMaterial() == Material.WATER && --limiter != 0) y++;
        while (world.getBlockState(new BlockPos(x, y, z)).getMaterial() == Material.AIR && --limiter != 0) y--;
        if (limiter == 0) return;
        if (world.getBlockState(new BlockPos(x, y, z)).getMaterial() == Material.WATER)
        {
            world.setBlock(new BlockPos(x, y + 1, z), Blocks.LILY_PAD.defaultBlockState(), 3);
            player.setPos(x + 0.5, y + 2, z + 0.5);
        }
    }
}
