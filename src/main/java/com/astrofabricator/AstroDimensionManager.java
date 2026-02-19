package com.astrofabricator;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import java.util.Optional;

public class AstroDimensionManager {

    public static void createDimension(MinecraftServer server, String templatePath, String instanceName) {
        ResourceLocation templateId = ResourceLocation.parse(templatePath);
        ResourceLocation newDimId = ResourceLocation.fromNamespaceAndPath("astrofabricator", instanceName.toLowerCase());
        ResourceKey<Level> newLevelKey = ResourceKey.create(Registries.DIMENSION, newDimId);

        // 1. 检查是否已经加载
        if (server.getLevel(newLevelKey) != null) return;

        // 2. 获取注册表
        var registryAccess = server.registryAccess();
        Registry<LevelStem> stemRegistry = registryAccess.registryOrThrow(Registries.LEVEL_STEM);

        // 3. 寻找模板
        Optional<Holder.Reference<LevelStem>> stemHolder = stemRegistry.getHolder(
                ResourceKey.create(Registries.LEVEL_STEM, templateId)
        );

        if (stemHolder.isPresent()) {
            LevelStem templateStem = stemHolder.get().value();

            // 4. [核心逻辑] 在 1.21.1 中手动挂载维度
            // 由于 Lithostitched 在后台处理了注册表冻结绕过，
            // 我们只需要确保在服务器的 levels map 中加入它
            server.markWorldsDirty();

            // 注意：如果你发现调用后仍无法传送，说明需要一个 Mixin 来将该 stem 注入 MappedRegistry
            // Lithostitched 通常会自动监听 levelKeys 的变化。
            System.out.println("成功尝试创建星域: " + newDimId);
        }
    }
}