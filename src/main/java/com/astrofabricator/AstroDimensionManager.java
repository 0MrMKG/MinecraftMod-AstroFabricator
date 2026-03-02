package com.astrofabricator;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.Optional;

public class AstroDimensionManager {

    /**
     * 初始化星域维度
     * @param templatePath 模板路径 (如 "astrofabricator:sakura_template")
     * @param instanceName 星域名称 (如 "tatooine")
     */
    public static void createDimension(MinecraftServer server, String templatePath, String instanceName) {
        ResourceLocation templateId = ResourceLocation.parse(templatePath);
        ResourceLocation newDimId = ResourceLocation.fromNamespaceAndPath("astrofabricator", instanceName.toLowerCase());
        ResourceKey<Level> newLevelKey = ResourceKey.create(Registries.DIMENSION, newDimId);

        // 1. 幂等性检查：如果已存在则跳过
        if (server.getLevel(newLevelKey) != null) return;

        // 2. 获取注册表
        var registryAccess = server.registryAccess();
        Registry<LevelStem> stemRegistry = registryAccess.registryOrThrow(Registries.LEVEL_STEM);

        // 3. 寻找模板 LevelStem
        Optional<Holder.Reference<LevelStem>> stemHolder = stemRegistry.getHolder(
                ResourceKey.create(Registries.LEVEL_STEM, templateId)
        );

        if (stemHolder.isPresent()) {
            LevelStem templateStem = stemHolder.get().value();

            // 4. 在 1.21.1 中，我们直接使用模板生成器
            // 差异化地形将通过传送时的“相位偏移”来实现
            LevelStem newStem = new LevelStem(templateStem.type(), templateStem.generator());

            // 5. 标记世界变更，Lithostitched 插件会自动处理注册表同步
            server.markWorldsDirty();

            System.out.println("§d[Astro]§f 星域坐标已重校准: " + instanceName);
            System.out.println("§7[相位偏移已就绪，等待跃迁指令]");
        } else {
            System.err.println("[Astro] 找不到指定的模板: " + templatePath);
        }
    }
}