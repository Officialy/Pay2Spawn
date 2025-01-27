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

package net.doubledoordev.pay2spawn.random;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;

/**
 * Handles random tags placed inside the NBT data from the JSON.
 * Gets redone for every reward.
 *
 * @author Dries007
 */
public class RandomRegistry {
    private static final HashMap<Class<? extends IRandomResolver>, IRandomResolver> RANDOM_RESOLVERS = new HashMap<>();

    static {
        addRandomResolver(new RndVariable());
        addRandomResolver(new RndBoolean());
        addRandomResolver(new RndColors());
//        addRandomResolver(new RndEntity());
        addRandomResolver(new RndListValue());
        addRandomResolver(new RndNumberRange());
        addRandomResolver(new ItemId<Item>() {
            @Override
            public @NotNull Collection<Item> getRegistry() {
                return ForgeRegistries.ITEMS.getValues();
            }
        });
        addRandomResolver(new ItemId<Block>() {
            @Override
            public @NotNull Collection<Block> getRegistry() {
                return ForgeRegistries.BLOCKS.getValues();
            }
        });
        addRandomResolver(new ASCIIFilter());
    }

    /**
     * Register your IRandomResolver here
     *
     * @param resolver the instance of the resolver
     */
    public static void addRandomResolver(IRandomResolver resolver) {
        RANDOM_RESOLVERS.put(resolver.getClass(), resolver);
    }

    /**
     * Internal method. Please leave alone.
     *
     * @param type  NBT type
     * @param value The to be randomised string
     * @return the original or a randomised version
     */
    public static String solveRandom(int type, String value) {
        String oldValue;
        do {
            oldValue = value;
            for (IRandomResolver resolver : RANDOM_RESOLVERS.values()) {
                if (resolver.matches(type, value)) value = resolver.solverRandom(type, value);
            }
        }
        while (!oldValue.equals(value));

        return value;
    }

    public static IRandomResolver getInstanceFromClass(Class<? extends IRandomResolver> aClass) {
        return RANDOM_RESOLVERS.get(aClass);
    }
}
