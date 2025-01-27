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

import net.doubledoordev.pay2spawn.util.Helper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.regex.Pattern;

import static net.doubledoordev.pay2spawn.util.Constants.STRING;

/**
 * Picks a random entity name (see EntityList.txt)
 * Expected syntax: $randomEntity
 * Outcome: The name (id) of 1 random entity
 * Works with: STRING
 *
 * @author Dries007
 */
public class RndEntity implements IRandomResolver {
    public static final String TAG = "$randomEntity";
    private static final Pattern PATTERN = Pattern.compile("\\$randomEntity");

    @Override
    public String solverRandom(int type, String value) {
        return PATTERN.matcher(value).replaceFirst(Helper.getRandomFromSet(ForgeRegistries.ENTITIES.getKeys().stream().filter(s -> !s.getPath().contains("player") && !s.getPath().contains("potion")).toList()).toString());
    }

    @Override
    public boolean matches(int type, String value) {
        return type == STRING && PATTERN.matcher(value).find();
    }
}
