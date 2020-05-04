package me.tepis.integratednbt;

import me.tepis.integratednbt.NBTExtractorUpdateTreeMessage.ErrorCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glScaled;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glTranslated;

@SideOnly(Side.CLIENT)
public class NBTExtractorGui extends ExtendedGuiContainer {
    public static final int SCREEN_EDGE = 4;
    public static final ResourceLocation NBT_EXTRACTOR_GUI_TEXTURES = new ResourceLocation(
        "integratednbt",
        "textures/gui/nbt_extractor.png"
    );
    // Different parts of the texture; See texture file for definitions
    private static final TexturePart PART0 = new TexturePart(0, 0, 8, 24);
    private static final TexturePart PART1 = new TexturePart(12, 0, 4, 24);
    private static final TexturePart PART2 = new TexturePart(20, 0, 8, 24);
    private static final TexturePart PART3 = new TexturePart(0, 28, 8, 4);
    private static final TexturePart PART4 = new TexturePart(12, 28, 4, 4);
    private static final TexturePart PART5 = new TexturePart(20, 28, 8, 4);
    private static final TexturePart PART6 = new TexturePart(0, 36, 8, 8);
    private static final TexturePart PART7 = new TexturePart(12, 36, 4, 8);
    private static final TexturePart PART8 = new TexturePart(20, 36, 178, 110);
    private static final TexturePart PART9 = new TexturePart(202, 36, 8, 8);
    private static final int BASE_PADDING = 200;
    private static final int INVENTORY_WIDTH = 178;
    private static final int INVENTORY_HEIGHT = 110;
    private static final int TOP_BORDER_SIZE = 24;
    private static final int SIDE_BORDER_SIZE = 8;
    private static final double CENTERED_TEXT_MAX_RATIO = 0.8;
    private static ErrorCode errorCode = null;
    private static NBTTagCompound nbt;
    private NBTTreeViewer treeViewer;
    private NBTExtractorContainer nbtExtractorContainer;
    /**
     * Padding outside the GUI; Responsive; Updated by updateCalculations
     */
    private int padding;
    /**
     * The width of NBT screen; Responsive; Updated by updateCalculations
     */
    private int screenWidth;
    /**
     * The height of NBT screen; Responsive; Updated by updateCalculations
     */
    private int screenHeight;
    /**
     * The scale factor of Minecraft; Updated by updateCalculations
     */
    private int scaleFactor;

    public NBTExtractorGui(NBTExtractorContainer nbtExtractorContainer) {
        super(nbtExtractorContainer);
        this.nbtExtractorContainer = nbtExtractorContainer;
        NBTExtractorTileEntity tileEntity = nbtExtractorContainer.getNbtExtractorEntity();
        this.treeViewer = new NBTTreeViewer(
            this,
            tileEntity.getExpandedPaths(),
            tileEntity.getScrollTop()
        ) {
            @Override
            public void onUpdateSelectedPath(NBTPath newPath, NBTBase nbt) {
                IntegratedNBT.getNetworkChannel().sendToServer(
                    new NBTExtractorUpdateExtractionPathMessage(
                        nbtExtractorContainer.getNbtExtractorEntity().getPos(),
                        newPath,
                        nbt.getId()
                    )
                );
            }

            @Override
            public NBTPath getSelectedPath() {
                return nbtExtractorContainer.getNbtExtractorEntity().getExtractionPath();
            }
        };
    }

    public static void updateError(ErrorCode errorCode) {
        NBTExtractorGui.errorCode = errorCode;
    }

    public static void updateNBT(NBTTagCompound nbt) {
        NBTExtractorGui.nbt = nbt;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.treeViewer.mouseClicked(mouseButton);
    }

    @Override
    public void initGui() {
        this.updateCalculations();
        this.xSize = this.width - 2 * this.padding;
        this.ySize = this.height - 2 * this.padding;
        super.initGui();
        this.nbtExtractorContainer.setSlotOffset(
            (this.xSize - INVENTORY_WIDTH) / 2,
            this.ySize - INVENTORY_HEIGHT
        );
        this.treeViewer.updateBounds(
            this.padding + SIDE_BORDER_SIZE,
            this.padding + TOP_BORDER_SIZE,
            this.screenWidth,
            this.screenHeight
        );
    }

    /**
     * Update
     */
    private void updateCalculations() {
        this.scaleFactor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        this.padding = (int) Math.min(
            Math.max(BASE_PADDING / Math.pow(this.scaleFactor, 3), 4),
            Math.min(this.width, this.height) / 10.
        );
        this.screenWidth = this.width - 2 * this.padding - 2 * SIDE_BORDER_SIZE;
        this.screenHeight = this.height - 2 * this.padding - TOP_BORDER_SIZE - INVENTORY_HEIGHT;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (errorCode == ErrorCode.NO_ERROR && nbt != null) {
            this.treeViewer.handleMouseInput();
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.renderGuiParts();
        this.fontRenderer.drawString(
            I18n.format("tile.integratednbt:nbt_extractor.name"),
            this.padding + 8,
            this.padding + 9,
            4210752
        );
        // Scissor test allows restricting rendering to a rectangular portion of the screen.
        // In this case, we only want to render in the screen area of the NBT Extractor.
        glEnable(GL_SCISSOR_TEST);
        glScissor(
            this.scaleFactor * (this.padding + SIDE_BORDER_SIZE),
            this.scaleFactor * (this.padding + INVENTORY_HEIGHT),
            this.scaleFactor * this.screenWidth,
            this.scaleFactor * this.screenHeight
        );
        Slot srcNBTSlot = this.nbtExtractorContainer.getSrcNBTSlot();
        if (!srcNBTSlot.getHasStack()) {
            errorCode = null;
            this.renderWelcome();
        } else if (errorCode == null) {
            this.renderLoading();
        } else if (!errorCode.equals(ErrorCode.NO_ERROR)) {
            this.renderError();
        } else {
            this.treeViewer.render(nbt, mouseX, mouseY);
        }
        glDisable(GL_SCISSOR_TEST);


//        int left = (this.width - WIDTH) / 2;
//        this.drawTexturedModalRect(left, PADDING, 0, 0, WIDTH, TOP_HEIGHT);
//        int treeTop = PADDING + TOP_HEIGHT;
//        int treeHeight = this.height - PADDING * 2 - TOP_HEIGHT - BOTTOM_HEIGHT;
//        this.drawTexturedModalRectScalable(
//            left,
//            treeTop,
//            WIDTH,
//            treeHeight,
//            0,
//            TOP_HEIGHT,
//            WIDTH,
//            TREE_SRC_HEIGHT
//        );
//        int inventoryTop = treeTop + treeHeight;
//        this.drawTexturedModalRect(
//            left,
//            inventoryTop,
//            0,
//            TOP_HEIGHT + TREE_SRC_HEIGHT,
//            WIDTH,
//            BOTTOM_HEIGHT
//        );
//        String name = "NBT Extractor";
//        this.fontRenderer.drawString(name, left + 8, PADDING + 9, 4210752);


//        GL11.glEnable(GL11.GL_SCISSOR_TEST);
////        GL11.glColorMask(false, false, false, false);
////        GL11.glStencilFunc(GL_NEVER, 1, 0xFF);
////        GL11.glStencilOp(GL_REPLACE, GL_KEEP, GL_KEEP);
////        GL11.glStencilMask(0xFF);
////        GL11.glClear(GL_STENCIL_BUFFER_BIT);
////        GL11.glColorMask(true, true, true, true);
////        GL11.glStencilMask(0x00);
////        GL11.glStencilFunc(GL_EQUAL, 0, 0xFF);
////        GL11.glScissor(left + 30, VERTICAL_PADDING + TOP_HEIGHT, 100, 20);
////        this.drawTexturedModalRect(, 0, 0, WIDTH, TOP_HEIGHT);
//
//        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void renderGuiParts() {
        int padding = this.padding;
        int screenWidth = this.screenWidth;
        int screenHeight = this.screenHeight;
        this.mc.getTextureManager().bindTexture(NBT_EXTRACTOR_GUI_TEXTURES);
        PART0.renderTo(this, padding, padding);
        PART1.renderTo(this, padding + SIDE_BORDER_SIZE, padding, screenWidth, -1);
        PART2.renderTo(this, this.width - padding - SIDE_BORDER_SIZE, padding);
        PART3.renderTo(this, padding, padding + TOP_BORDER_SIZE, -1, screenHeight);
        PART4.renderTo(
            this,
            padding + SIDE_BORDER_SIZE,
            padding + TOP_BORDER_SIZE,
            screenWidth,
            screenHeight
        );
        PART5.renderTo(
            this,
            this.width - padding - SIDE_BORDER_SIZE,
            padding + TOP_BORDER_SIZE,
            -1,
            screenHeight
        );
        int topOfPart6789 = this.height - padding - INVENTORY_HEIGHT;
        PART6.renderTo(this, padding, topOfPart6789);
        int part7Width2x = this.width - 2 * padding - 2 * SIDE_BORDER_SIZE - INVENTORY_WIDTH;
        int part7WidthFloor = (int) Math.floor(part7Width2x / 2.0);
        int part7WidthCeil = (int) Math.ceil(part7Width2x / 2.0);
        PART7.renderTo(
            this,
            padding + SIDE_BORDER_SIZE,
            topOfPart6789,
            part7WidthFloor,
            -1
        );
        PART8.renderTo(
            this,
            padding + SIDE_BORDER_SIZE + part7WidthFloor,
            topOfPart6789
        );
        PART7.renderTo(
            this,
            padding + SIDE_BORDER_SIZE + part7WidthFloor + INVENTORY_WIDTH,
            topOfPart6789,
            part7WidthCeil,
            -1
        );
        PART9.renderTo(
            this,
            this.width - padding - SIDE_BORDER_SIZE,
            topOfPart6789
        );
    }

    private void renderWelcome() {
        this.renderCenteredTextGroup(
            I18n.format("integratednbt:nbt_extractor.welcome"),
            0x00FFFF,
            I18n.format("integratednbt:nbt_extractor.welcome.description")
        );
    }

    private void renderLoading() {
        this.renderCenteredTextGroup(
            I18n.format("integratednbt:nbt_extractor.loading"),
            0xFFFF00,
            I18n.format("integratednbt:nbt_extractor.loading.description")
        );
    }

    private void renderError() {
        String errorMessage = "";
        switch (errorCode) {
            case EVAL_ERROR:
                errorMessage = I18n.format("integratednbt:nbt_extractor.error.eval");
                break;
            case TYPE_ERROR:
                errorMessage = I18n.format("integratednbt:nbt_extractor.error.type");
                break;
            case UNEXPECTED_ERROR:
                errorMessage = I18n.format("integratednbt:nbt_extractor.error.unexpected");
                break;
        }
        this.renderCenteredTextGroup(
            I18n.format("integratednbt:nbt_extractor.error"),
            0xFF5555,
            errorMessage
        );
    }

    private void renderCenteredTextGroup(String title, int titleColor, String description) {
        glPushMatrix();
        try {
            int x = this.screenCenterX();
            int y = this.screenCenterY();
            int titleWidth = this.fontRenderer.getStringWidth(title);
            glPushMatrix();
            try {
                this.scaleAt(x, y, 2);
                this.fontRenderer.drawString(
                    title,
                    -titleWidth / 2,
                    -this.fontRenderer.FONT_HEIGHT - 1,
                    titleColor
                );
            } finally {
                glPopMatrix();
            }
            this.scaleAt(x, y, 1);
            int wrappingWidth = (int) (this.screenWidth * CENTERED_TEXT_MAX_RATIO);
            int descriptionWidth = this.fontRenderer.getStringWidth(description);
            if (descriptionWidth > wrappingWidth) {
                this.fontRenderer.drawSplitString(
                    description,
                    -wrappingWidth / 2,
                    4,
                    wrappingWidth,
                    0xFFFFFF
                );
            } else {
                this.fontRenderer.drawString(
                    description,
                    -descriptionWidth / 2,
                    4,
                    0xFFFFFF
                );
            }
        } finally {
            glPopMatrix();
        }
    }

    private int screenCenterX() {
        return this.width / 2;
    }

    private int screenCenterY() {
        return this.padding + TOP_BORDER_SIZE + this.screenHeight / 2;
    }

    private void scaleAt(int x, int y, double scale) {
        glScaled(scale, scale, 1d);
        glTranslated(x / scale, y / scale, 0d);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {

    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}