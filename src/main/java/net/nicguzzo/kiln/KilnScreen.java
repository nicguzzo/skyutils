package net.nicguzzo.kiln;

import net.minecraft.client.gui.screen.ingame.AbstractContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;

public class KilnScreen  extends AbstractContainerScreen<KilnContainer>{
     
    private static final Identifier TEXTURE = new Identifier("skyutils","textures/gui/container/kiln.png");
 
    public KilnScreen(KilnContainer container, PlayerInventory playerInventory, Text title) {
        super(container, playerInventory, title);
        this.containerHeight = 114 + 6 * 18;
    }
 
    @Override
    protected void drawForeground(int mouseX, int mouseY) {
        float p = (float)((KilnContainer)this.container).getProgress()/10.0f;
        String string = this.title.asFormattedString()+ " "+String.format("%.01f", p) +"%";
        this.font.draw(string, (float)(this.containerWidth / 2 - this.font.getStringWidth(string) / 2), 6.0F, 4210752);       
    }
 
    @Override
    protected void drawBackground(float delta, int mouseX, int mouseY) {
        
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(TEXTURE);
        int i = this.x;
        int j = this.y;
        this.blit(i, j, 0, 0, this.containerWidth, this.containerHeight);
        int p = ((KilnContainer)this.container).getBurnTime();
        float prog = (p / (float) KilnBlockEntity.CHARCOAL_BURN_TIME);        
        if (prog != 0) {
            int l = (int) (12.0 * prog);
            this.blit(i + 56, j + 36 + 12 - l, 176, 12 - l, 14, l + 1);
        }
        float prog2 = (float) ((KilnContainer) this.container).getProgress() / 1000.0f;
        if (prog2 !=0) {
            this.blit(i + 79, j + 34, 176, 14, (int) (24 * prog2) + 1, 16);
        }
    }


}