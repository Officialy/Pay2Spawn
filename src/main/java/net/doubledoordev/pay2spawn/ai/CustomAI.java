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

package net.doubledoordev.pay2spawn.ai;

import com.google.common.collect.ImmutableList;
import net.doubledoordev.pay2spawn.util.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static net.doubledoordev.pay2spawn.util.Constants.MODID;

/**
 * @author Dries007
 */
public class CustomAI
{
    public static final String CUSTOM_AI_TAG = MODID + "_CustomAI";
    public static final String OWNER_TAG = MODID + "_Owner";
    public static final CustomAI INSTANCE = new CustomAI();

    public CustomAI()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void respawnHandler(EntityJoinWorldEvent event)
    {
        if (event.getEntity() instanceof AmbientCreature && event.getEntity().getEntityData().contains(CUSTOM_AI_TAG, Constants.COMPOUND))
        {
            AmbientCreature entity = (AmbientCreature) event.getEntity();
            for (Object o : ImmutableList.copyOf(entity.tasks.taskEntries)) entity.tasks.removeTask(((EntityAITasks.EntityAITaskEntry) o).action);
            for (Object o : ImmutableList.copyOf(entity.targetTasks.taskEntries)) entity.targetTasks.removeTask(((EntityAITasks.EntityAITaskEntry) o).action);

            entity.tasks.addTask(0, new EntityAISwimming(entity));
            entity.tasks.addTask(1, new CustomAIAttackOnCollide(entity, 1.0d, false));
            entity.tasks.addTask(2, new CustomAIFollowOwner(entity, 1.5d, 6.0f, 10.0f));
            entity.tasks.addTask(3, new EntityAIWander(entity, 1.0D));
            entity.tasks.addTask(4, new EntityAIWatchClosest(entity, Player.class, 8.0F));
            entity.tasks.addTask(5, new EntityAILookIdle(entity));

            entity.targetTasks.addTask(1, new CustomAIOwnerHurtByTarget(entity));
            entity.targetTasks.addTask(2, new CustomAIOwnerHurtTarget(entity));
            entity.targetTasks.addTask(3, new CustomAIHurtByTarget(entity, true));
        }
    }

    public void test(final Player player)
    {
        final EntityZombie zombie = new EntityZombie(player.getEntityWorld());
        zombie.setCanPickUpLoot(true);
        zombie.setPosition(player.getX(), player.getY(), player.getZ());

        setOwner(zombie, player.getCommandSenderName());
        zombie.setCustomNameTag("dries007");
        player.getEntityWorld().addFreshEntity(zombie);
    }

    public void init()
    {

    }

    public static Player getOwner(LivingEntity mob)
    {
        if (mob == null || mob.level == null) return null;
        return mob.level.getPlayerEntityByName(mob.getEntityData().getCompound(CUSTOM_AI_TAG).getString(OWNER_TAG));
    }

    public static void setOwner(LivingEntity mob, String owner)
    {
        CompoundTag compound = mob.getEntityData().getCompound(CUSTOM_AI_TAG);
        compound.putString(OWNER_TAG, owner);
        mob.getEntityData().put(CUSTOM_AI_TAG, compound);
    }

    public static boolean isOnSameTeam(LivingEntity m1, LivingEntity m2)
    {
        if (m1 == null || m2 == null) return false;
        Player o1 = getOwner(m1);
        Player o2 = getOwner(m2);

        if (m1 == o2 || m2 == o1) return false;

        boolean b = o1 != null && o2 != null && o1 != o2 && o1.isOnSameTeam(o2);
        //System.out.println(m1 + "\t" + m2 + "\t" + o1 + "\t" + o2 + "\t" + b);
        return b;
    }
}
