package com.astrofabricator.client;

import com.astrofabricator.client.gui.AstroScreen;
import com.astrofabricator.network.AstroPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
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
                // 1. 打开 GUI
                .then(Commands.literal("gui").executes(c -> {
                    Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new AstroScreen()));
                    return 1;
                }))

                // 2. 列出维度
                .then(Commands.literal("dimlist").executes(c -> {
                    PacketDistributor.sendToServer(new AstroPayload("refresh_list", "", ""));
                    return 1;
                }))

                // 3. 传送：核心修复 - 将参数类型改为 ResourceLocationArgument 以支持冒号 (:)
                .then(Commands.literal("tpd")
                        .then(Commands.argument("target", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> {
                                    // 自动补全当前已知的维度 ID
                                    var connection = Minecraft.getInstance().getConnection();
                                    if (connection != null) {
                                        connection.levels().forEach(key -> builder.suggest(key.location().toString()));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(c -> {
                                    // 使用 ResourceLocationArgument.getId 获取完整路径
                                    ResourceLocation target = ResourceLocationArgument.getId(c, "target");
                                    PacketDistributor.sendToServer(new AstroPayload("teleport", target.toString(), ""));
                                    return 1;
                                })))
        );
    }
}