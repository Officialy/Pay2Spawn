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
import net.doubledoordev.pay2spawn.types.guis.XPOrbsGui;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static net.doubledoordev.pay2spawn.util.Constants.*;

/**
 * @author Dries007
 */
public class XPOrbsType extends TypeBase {
    public static final String NODENAME = "xporbs";
    public static final String AMOUNTOFORBS_KEY = "amoutOfOrbs";

    public static final HashMap<String, String> typeMap = new HashMap<>();

    static {
        typeMap.put(AMOUNTOFORBS_KEY, NBTTypes[INT]);
    }

    @Override
    public String getName() {
        return NODENAME;
    }

    @Override
    public CompoundTag getExample() {
        CompoundTag out = new CompoundTag();
        out.putInt(AMOUNTOFORBS_KEY, 100);
        return out;
    }

    @Override
    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData) {
        for (int i = 0; i < dataFromClient.getInt(AMOUNTOFORBS_KEY); i++) {
            double X = player.getX(), Y = player.getY(), Z = player.getZ();

            X += (0.5 - RANDOM.nextDouble());
            Z += (0.5 - RANDOM.nextDouble());

            player.level.addFreshEntity(new ExperienceOrb(player.level, X, Y, Z, RANDOM.nextInt(5) + 1));
        }
    }

    @Override
    public void openNewGui(int rewardID, JsonObject data) {
        new XPOrbsGui(rewardID, getName(), data, typeMap);
    }

    @Override
    public Collection<Node> getPermissionNodes() {
        HashSet<Node> nodes = new HashSet<>();
        nodes.add(new Node(NODENAME));
        return nodes;
    }

    @Override
    public Node getPermissionNode(Player player, CompoundTag dataFromClient) {
        return new Node(NODENAME);
    }

    @Override
    public String replaceInTemplate(String id, JsonObject jsonObject) {
        return id;
    }
}
