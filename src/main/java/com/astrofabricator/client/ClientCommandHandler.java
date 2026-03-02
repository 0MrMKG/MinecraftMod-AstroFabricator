package com.astrofabricator.client;

import com.astrofabricator.network.AstroPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;

@EventBusSubscriber(modid = "astrofabricator", value = Dist.CLIENT)
public class ClientCommandHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("af")
                // 1. 维度列表请求
                .then(Commands.literal("dimlist").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal("§7[终端] 正在扫描恒星系特征信号..."), false);
                    PacketDistributor.sendToServer(new AstroPayload("refresh_list", "", ""));
                    return 1;
                }))

                // 2. 维度传送指令 (tpd <维度ID>)
                .then(Commands.literal("tpd")
                        .then(Commands.argument("target", ResourceLocationArgument.id())
                                // --- 核心改进：带优先级的排序代码补全 ---
                                .suggests((ctx, builder) -> {
                                    var connection = Minecraft.getInstance().getConnection();
                                    if (connection != null) {
                                        connection.registryAccess().registry(Registries.DIMENSION).ifPresent(registry -> {
                                            registry.keySet().stream()
                                                    .sorted(getDimensionComparator()) // 调用下方的排序逻辑
                                                    .forEach(loc -> builder.suggest(loc.toString()));
                                        });
                                    }
                                    return builder.buildFuture();
                                })
                                // --- 执行逻辑 ---
                                .executes(c -> {
                                    ResourceLocation target = ResourceLocationArgument.getId(c, "target");

                                    // 向服务端发送传送载荷
                                    PacketDistributor.sendToServer(new AstroPayload("teleport", target.toString(), ""));

                                    // 模拟星际航行的反馈感
                                    c.getSource().sendSuccess(() -> Component.literal("§e[导航]§f 已锁定星域: §b" + target), false);
                                    c.getSource().sendSuccess(() -> Component.literal("§7超空间引擎正在增压..."), false);

                                    return 1;
                                })))
        );
    }

    /**
     * 定义维度排序规则：
     * 1. minecraft 原版命名空间优先级最高
     * 2. 然后按命名空间字母排序 (astrofabricator, etc.)
     * 3. 最后按维度名称字母排序 (sakura_dimension, tatooine, etc.)
     */
    public static Comparator<ResourceLocation> getDimensionComparator() {
        return (loc1, loc2) -> {
            String ns1 = loc1.getNamespace();
            String ns2 = loc2.getNamespace();
            boolean isMc1 = ns1.equals("minecraft");
            boolean isMc2 = ns2.equals("minecraft");

            if (isMc1 && !isMc2) return -1; // loc1 是原版，排前面
            if (!isMc1 && isMc2) return 1;  // loc2 是原版，排前面

            // 如果同为原版或同为 Mod，先按 namespace 字母排
            int nsCompare = ns1.compareTo(ns2);
            if (nsCompare != 0) return nsCompare;

            // 如果 namespace 也一样，按具体名称字母排
            return loc1.getPath().compareTo(loc2.getPath());
        };
    }
}