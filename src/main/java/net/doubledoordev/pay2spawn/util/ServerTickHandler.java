///*
// * Copyright (c) 2014, DoubleDoorDevelopment
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *  Redistributions of source code must retain the above copyright notice, this
// *   list of conditions and the following disclaimer.
// *
// *  Redistributions in binary form must reproduce the above copyright notice,
// *   this list of conditions and the following disclaimer in the documentation
// *   and/or other materials provided with the distribution.
// *
// *  Neither the name of the project nor the names of its
// *   contributors may be used to endorse or promote products derived from
// *   this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package net.doubledoordev.pay2spawn.util;
//
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.world.entity.player.Player;
//import net.minecraftforge.common.MinecraftForge;
//import net.minecraftforge.event.TickEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//
//import static net.doubledoordev.pay2spawn.types.PlayerModificationType.Type;
//
///**
// * Server side tick things, does timeable player effects
// *
// * @author Dries007
// */
//public class ServerTickHandler
//{
//    public static final ServerTickHandler INSTANCE = new ServerTickHandler();
//
//    private ServerTickHandler()
//    {
//        MinecraftForge.EVENT_BUS.register(this);
//    }
//
//    @SubscribeEvent
//    public void tickEvent(TickEvent.PlayerTickEvent event)
//    {
//        if (event.phase != TickEvent.Phase.START)
//            return;
//
//        CompoundTag data = event.player.getEntityData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("P2S");
//        for (PlayerModificationType.Type t : PlayerModificationType.Type.values())
//        {
//            if (t.isTimable())
//            {
//                if (data.contains(t.name()))
//                {
//                    int i = data.getInt(t.name());
//                    if (i == 0)
//                    {
//                        t.undo(event.player);
//                        data.remove(t.name());
//                    }
//                    else
//                    {
//                        data.putInt(t.name(), --i);
//                    }
//                }
//            }
//        }
//    }
//
//    public void init()
//    {
//
//    }
//}
