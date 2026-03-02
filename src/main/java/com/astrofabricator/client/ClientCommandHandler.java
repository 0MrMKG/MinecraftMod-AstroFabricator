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
                                // --- 核心改进：代码补全逻辑 ---
                                .suggests((ctx, builder) -> {
                                    var connection = Minecraft.getInstance().getConnection();
                                    if (connection != null) {
                                        // 从客户端注册表获取所有已定义的维度 Key
                                        // 这包括了主世界、地狱、末地以及你 Mod 中注册的所有星域
                                        connection.registryAccess().registry(Registries.DIMENSION).ifPresent(registry -> {
                                            registry.keySet().forEach(loc -> {
                                                // 将所有的维度 ResourceLocation 转换为字符串加入补全列表
                                                builder.suggest(loc.toString());
                                            });
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
}