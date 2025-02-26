package fi.dy.masa.tweakeroo.gui;

import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.tweakeroo.Reference;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public enum Icons implements IGuiIcon
{
    DUMMY                   (  0,   0,  0,  0),
    BUTTON_PLUS_MINUS_8     (  0,   0,  8,  8),
    BUTTON_PLUS_MINUS_12    ( 24,   0, 12, 12),
    BUTTON_PLUS_MINUS_16    (  0, 128, 16, 16),
    ENCLOSING_BOX_ENABLED   (  0, 144, 16, 16),
    ENCLOSING_BOX_DISABLED  (  0, 160, 16, 16),
    FILE_ICON_LITEMATIC     (144,   0, 12, 12),
    FILE_ICON_SCHEMATIC     (144,  12, 12, 12),
    FILE_ICON_VANILLA       (144,  24, 12, 12),
    FILE_ICON_JSON          (144,  36, 12, 12),
    FILE_ICON_SPONGE_SCH    (144,  48, 12, 12),
    FILE_ICON_DIR           (156,   0, 12, 12),
    FILE_ICON_DIR_UP        (156,  12, 12, 12),
    FILE_ICON_DIR_ROOT      (156,  24, 12, 12),
    FILE_ICON_SEARCH        (156,  36, 12, 12),
    FILE_ICON_CREATE_DIR    (156,  48, 12, 12),
    SCHEMATIC_TYPE_FILE     (144,   0, 12, 12),
    SCHEMATIC_TYPE_MEMORY   (186,   0, 12, 12),
    INFO_11                 (168,  18, 11, 11),
    NOTICE_EXCLAMATION_11   (168,  29, 11, 11),
    LOCK_LOCKED             (168,  51, 11, 11),
    CHECKBOX_UNSELECTED     (198,   0, 11, 11),
    CHECKBOX_SELECTED       (198,  11, 11, 11),
    ARROW_UP                (209,   0, 15, 15),
    ARROW_DOWN              (209,  15, 15, 15);

    public static final Identifier TEXTURE = Identifier.of(Reference.MOD_ID, "textures/gui/gui_widgets.png");

    private final int u;
    private final int v;
    private final int w;
    private final int h;

    private Icons(int u, int v, int w, int h)
    {
        this.u = u;
        this.v = v;
        this.w = w;
        this.h = h;
    }

    @Override
    public int getWidth()
    {
        return this.w;
    }

    @Override
    public int getHeight()
    {
        return this.h;
    }

    @Override
    public int getU()
    {
        return this.u;
    }

    @Override
    public int getV()
    {
        return this.v;
    }

    @Override
    public void renderAt(int x, int y, float zLevel, boolean enabled, boolean selected, DrawContext context)
    {
        RenderUtils.drawTexturedRect(x, y, this.u, this.v, this.w, this.h, zLevel);
    }

    @Override
    public Identifier getTexture()
    {
        return TEXTURE;
    }

    public IGuiIcon getIconSearch()
    {
        return FILE_ICON_SEARCH;
    }


}