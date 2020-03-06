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
        String string = this.title.asFormattedString();
        this.font.draw(string, (float)(this.containerWidth / 2 - this.font.getStringWidth(string) / 2), 6.0F, 4210752);       
    }
 
    @Override
    protected void drawBackground(float delta, int mouseX, int mouseY) {
        
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(TEXTURE);
        int i = this.x;
        int j = this.y;
        this.blit(i, j, 0, 0, this.containerWidth, this.containerHeight);
        int p = ((KilnContainer)this.container).getProgress();
        float prog=(p/(float)KilnBlockEntity.BURN_TIME);
        if(p!=0){
            this.blit(i + 79, j + 34, 176, 14, (int)(24*(1.0f-prog)) + 1, 16);
            int l=(int)(12.0*prog);
            this.blit(i + 56, j + 36 + 12 - l, 176, 12 - l, 14, l + 1);
        }
    }


}