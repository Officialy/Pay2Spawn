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
import net.doubledoordev.pay2spawn.permissions.Node;
import net.doubledoordev.pay2spawn.types.guis.DeleteworldTypeGui;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import static net.doubledoordev.pay2spawn.util.Constants.*;

/**
 * This should be !FUN!
 *
 * @author Dries007
 */

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeleteworldType extends TypeBase {
    public static final String MESSAGE_KEY = "message";
    public static final HashMap<String, String> typeMap = new HashMap<>();
    private static final String NAME = "deleteworld";
    private static boolean deleteWorld = false;

    static {
        typeMap.put(MESSAGE_KEY, NBTTypes[STRING]);
    }

    public static String DEFAULTMESSAGE = "A Pay2Spawn donation deleted the world.\\nGoodbye!";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CompoundTag getExample() {
        CompoundTag nbtTagCompound = new CompoundTag();
        nbtTagCompound.putString(MESSAGE_KEY, DEFAULTMESSAGE);
        return nbtTagCompound;
    }

    @Override
    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData) {
        for (int i = 0; i < player.getServer().getPlayerList().getPlayerCount(); ++i) {
            player.getServer().getPlayerList().getPlayers().get(i).connection.disconnect(new TextComponent(dataFromClient.getString(MESSAGE_KEY).replace("\\n", "\n")));
        }
        deleteWorld = true;
    }

    @SubscribeEvent
    public static void saveEvent(WorldEvent.Save event) {
        if (deleteWorld) {
            String brokenPath = event.getWorld().getServer().getWorldPath(LevelResource.LEVEL_DATA_FILE).getParent().toAbsolutePath().toString();
            String fixedPath = brokenPath.replace(".", "").replace("\\\\", "\\");

            MinecraftServer server = event.getWorld().getServer();
            server.doRunTask(new TickTask(server.getTickCount(), () -> {
                try {
                    FileUtils.deleteDirectory(new File(fixedPath));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
            deleteWorld = false;
        }
    }

    @Override
    public void openNewGui(int rewardID, JsonObject data) {
        new DeleteworldTypeGui(rewardID, NAME, data, typeMap);
    }

    @Override
    public Collection<Node> getPermissionNodes() {
        return Collections.singletonList(new Node(NAME));
    }

    @Override
    public Node getPermissionNode(Player player, CompoundTag dataFromClient) {
        return new Node(NAME);
    }

    @Override
    public boolean isInDefaultConfig() {
        return false;
    }

    @Override
    public String replaceInTemplate(String id, JsonObject jsonObject) {
        return id;
    }
}
