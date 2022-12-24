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
import net.doubledoordev.pay2spawn.types.guis.CommandTypeGui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.doubledoordev.oldforge.Configuration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static net.doubledoordev.pay2spawn.util.Constants.*;

/**
 * @author Dries007
 */
public class CommandType extends TypeBase {
    public static final String COMMAND_KEY = "command";
    public static final HashMap<String, String> typeMap = new HashMap<>();
    public static final HashSet<String> commands = new HashSet<>();
    private static final String NAME = "command";

    static {
        typeMap.put(COMMAND_KEY, NBTTypes[STRING]);
    }

    public boolean feedback = true;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CompoundTag getExample() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(COMMAND_KEY, "weather clear");
        return nbt;
    }

    @Override
    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData) {
//    todo    MinecraftServer.getServer().getCommandManager().executeCommand(new cmdSender((ServerPlayer) player), dataFromClient.getString(COMMAND_KEY));
    }

    @Override
    public void doConfig(Configuration configuration) {
        configuration.addCustomCategoryComment(TYPES_CAT, "Reward config options");
        configuration.addCustomCategoryComment(TYPES_CAT + '.' + NAME, "Used for commands");
        feedback = configuration.get(TYPES_CAT + '.' + NAME, "feedback", feedback, "Disable command feedback. (server overrides client)").getBoolean(feedback);
    }

    @Override
    public void openNewGui(int rewardID, JsonObject data) {
        new CommandTypeGui(rewardID, NAME, data, typeMap);
    }

    @Override
    public Collection<Node> getPermissionNodes() {
        HashSet<Node> nodes = new HashSet<>();
      /*todo  if (server != null) {
            for (Object o : server.getCommandManager().getCommands().values()) {
                ICommand command = (ICommand) o;
                commands.add(command.getCommandName());
                nodes.add(new Node(NAME, command.getCommandName()));
            }
        } else {
            nodes.add(new Node(NAME));
        }*/

        return nodes;
    }

    @Override
    public Node getPermissionNode(Player player, CompoundTag dataFromClient) {
        return new Node(NAME, dataFromClient.getString(COMMAND_KEY).split(" ")[0]);
    }

    @Override
    public String replaceInTemplate(String id, JsonObject jsonObject) {
        switch (id) {
            case "cmd":
                return jsonObject.get(COMMAND_KEY).getAsString().replace(typeMap.get(COMMAND_KEY) + ":", "");
        }
        return id;
    }

    public class cmdSender extends ServerPlayer {
        public cmdSender(ServerPlayer player) {
            super(player.server, player.server.overworld(), player.getGameProfile());
//            this.theItemInWorldManager.thisPlayerMP = player;
//            this.playerNetServerHandler = player.playerNetServerHandler;
        }

        @Override
        public void displayClientMessage(Component p_9154_, boolean p_9155_) {
            if (feedback) super.displayClientMessage(p_9154_, p_9155_);
        }
    }
}
