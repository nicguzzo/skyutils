package net.nicguzzo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.text.Text;
///import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import java.util.function.Consumer;

@Environment(value= EnvType.CLIENT)
public class SkyblockLevelScreen   extends Screen {
    private static final Text SKYBLOCK_TEXT = Text.translatable("Islands");
    private final Screen parent;
//    private final Consumer<RegistryEntry<Biome>> onDone;
    //final Registry<Biome> biomeRegistry;
    //private net.minecraft.client.gui.screen.CustomizeBuffetLevelScreen.BuffetBiomesListWidget biomeSelectionList;
    //RegistryEntry<Biome> biome;
    private ButtonWidget confirmButton;

    public SkyblockLevelScreen(Screen parent, GeneratorOptionsHolder generatorOptionsHolder /*, Consumer<RegistryEntry<Biome>> onDone*/) {
        super(Text.translatable("Skyblock"));
        this.parent = parent;
//        this.onDone = onDone;
        //this.biomeRegistry = generatorOptionsHolder.dynamicRegistryManager().get(Registry.BIOME_KEY);
        //RegistryEntry<Biome> registryEntry = this.biomeRegistry.getEntry(BiomeKeys.PLAINS).or(() -> this.biomeRegistry.streamEntries().findAny()).orElseThrow();
        //this.biome = generatorOptionsHolder.generatorOptions().getChunkGenerator().getBiomeSource().getBiomes().stream().findFirst().orElse(registryEntry);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Override
    protected void init() {
        /*this.client.keyboard.setRepeatEvents(true);
        this.biomeSelectionList = new net.minecraft.client.gui.screen.CustomizeBuffetLevelScreen.BuffetBiomesListWidget();
        this.addSelectableChild(this.biomeSelectionList);
        this.confirmButton = this.addDrawableChild(new ButtonWidget(this.width / 2 - 155, this.height - 28, 150, 20, ScreenTexts.DONE, button -> {
            this.onDone.accept(this.biome);
            this.client.setScreen(this.parent);
        }));
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 5, this.height - 28, 150, 20, ScreenTexts.CANCEL, button -> this.client.setScreen(this.parent)));
        this.biomeSelectionList.setSelected((net.minecraft.client.gui.screen.CustomizeBuffetLevelScreen.BuffetBiomesListWidget.BuffetBiomeItem)this.biomeSelectionList.children().stream().filter(entry -> Objects.equals(entry.biome, this.biome)).findFirst().orElse(null));

         */
    }

    void refreshConfirmButton() {
        //this.confirmButton.active = this.biomeSelectionList.getSelectedOrNull() != null;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(0);
        //this.biomeSelectionList.render(matrices, mouseX, mouseY, delta);
        net.minecraft.client.gui.screen.CustomizeBuffetLevelScreen.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        net.minecraft.client.gui.screen.CustomizeBuffetLevelScreen.drawCenteredText(matrices, this.textRenderer, SKYBLOCK_TEXT, this.width / 2, 28, 0xA0A0A0);
        super.render(matrices, mouseX, mouseY, delta);
    }
}


