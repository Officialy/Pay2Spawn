package net.doubledoordev.pay2spawn.types;

import com.google.gson.JsonObject;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.network.CrashMessage;
import net.doubledoordev.pay2spawn.permissions.Node;
import net.doubledoordev.pay2spawn.types.guis.CrashTypeGui;
import net.doubledoordev.pay2spawn.util.Helper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.network.NetworkDirection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static net.doubledoordev.pay2spawn.util.Constants.*;

/**
 * @author Dries007
 */
public class CrashType extends TypeBase {
    private static final String NAME = "crash";
    public static final String MESSAGE_KEY = "message";
    public static final HashMap<String, String> typeMap = new HashMap<>();
    public static String DEFAULTMESSAGE = "You have not gotten any error messages recently, so here is one, just to let you know that we haven't started caring.";
    public static RuntimeException crash;

    static {
        typeMap.put(MESSAGE_KEY, NBTTypes[STRING]);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CompoundTag getExample() {
        CompoundTag root = new CompoundTag();
        root.putString(MESSAGE_KEY, DEFAULTMESSAGE);
        return root;
    }

    public static StackTraceElement[] getRandomStackTrace() {
        StackTraceElement[] list = new StackTraceElement[10 + RANDOM.nextInt(25)];
        for (int i = 0; i < list.length; i++) {
            list[i] = getRandomStackTraceElement();
        }
        return list;
    }

    public static StackTraceElement getRandomStackTraceElement() {
        var modContainer = Helper.getRandomFromSet(FMLLoader.getLoadingModList().getMods());
        return new StackTraceElement(modContainer.getModId(), modContainer.getDescription(), modContainer.getDisplayName(), RANDOM.nextInt(1000));
    }

    @Override
    public void spawnServerSide(ServerPlayer player, CompoundTag dataFromClient, CompoundTag rewardData) {
        Pay2Spawn.getSnw().sendTo(new CrashMessage(dataFromClient.getString(MESSAGE_KEY)), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    public void openNewGui(int rewardID, JsonObject data) {
        new CrashTypeGui(rewardID, NAME, data, typeMap);
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
    public String replaceInTemplate(String id, JsonObject jsonObject) {
        if (id.equals("message")) {
            return jsonObject.getAsJsonPrimitive(MESSAGE_KEY).getAsString();
        }
        return id;
    }

    @Override
    public boolean isInDefaultConfig() {
        return false;
    }
}
