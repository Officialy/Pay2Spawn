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

package net.doubledoordev.pay2spawn.network;

import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.types.MusicType;
import net.doubledoordev.pay2spawn.util.javazoom.jl.decoder.JavaLayerException;
import net.doubledoordev.pay2spawn.util.javazoom.jl.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.function.Supplier;

/**
 * Does music reward on the client
 *
 * @author Dries007
 */
public class MusicMessage {
    private final String name;

    public MusicMessage(String name) {
        this.name = name;
    }

    public MusicMessage(FriendlyByteBuf buf) {
        name = buf.readUtf();
    }

    private static void play(final File file) {
        new Thread(() -> {
            try {
                var player = new Player(new FileInputStream(file));
                player.play();
            } catch (JavaLayerException | FileNotFoundException e) {
                e.printStackTrace();
            }
        }, "Pay2Spawn music thread").start();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(name);
    }

    public static void handle(final MusicMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            File file = new File(MusicType.musicFolder, message.name);

            if (file.exists() && file.isFile()) {
                play(file);
                ctx.get().setPacketHandled(true);
            } else {
                if (!file.isDirectory()) file = file.getParentFile();

                File[] files = file.listFiles((dir, name) -> name.startsWith(message.name));

                if (files.length == 1) {
                    play(files[0]);
                } else if (files.length == 0) {
                    Pay2Spawn.getLogger().warn("MUSIC FILE NOT FOUND: " + message.name);
                } else {
                    Pay2Spawn.getLogger().warn("Multiple matches with music:");
                    for (File file1 : files) Pay2Spawn.getLogger().warn(file1.getName());
                }
            }
            ctx.get().setPacketHandled(true);
        });
    }

}
