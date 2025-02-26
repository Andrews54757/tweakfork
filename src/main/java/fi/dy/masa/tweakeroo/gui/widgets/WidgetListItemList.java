package fi.dy.masa.tweakeroo.gui.widgets;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.tweakeroo.gui.GuiItemList;
import fi.dy.masa.tweakeroo.gui.Icons;
import fi.dy.masa.tweakeroo.items.ItemListEntry;
import fi.dy.masa.tweakeroo.items.ItemListSorter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class WidgetListItemList extends WidgetListBase<ItemListEntry, WidgetItemListEntry>
{
    private static int lastScrollbarPosition;

    private final GuiItemList gui;
    private final ItemListSorter sorter;
    private boolean scrollbarRestored;

    public WidgetListItemList(int x, int y, int width, int height, GuiItemList parent)
    {
        super(x, y, width, height, null);

        this.browserEntryHeight = 22;
     //   this.allowMultiSelection = true;
        this.gui = parent;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 8, width - 16, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.RIGHT);
        this.widgetSearchBar.setZLevel(1);
        this.sorter = new ItemListSorter(parent.getItemList());
        this.shouldSortList = true;

        this.setParent(parent);
    }

    @Override
    public void drawContents(DrawContext drawContext, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(drawContext, mouseX, mouseY, partialTicks);
        lastScrollbarPosition = this.scrollBar.getValue();
    }

    @Override
    protected void offsetSelectionOrScrollbar(int amount, boolean changeSelection)
    {
        super.offsetSelectionOrScrollbar(amount, changeSelection);
        lastScrollbarPosition = this.scrollBar.getValue();
    }

    @Override
    protected WidgetItemListEntry createHeaderWidget(int x, int y, int listIndexStart, int usableHeight, int usedHeight)
    {
        int height = this.browserEntryHeight;

        if ((usedHeight + height) > usableHeight)
        {
            return null;
        }

        return this.createListEntryWidget(x, y, listIndexStart, true, null);
    }

    @Override
    protected Collection<ItemListEntry> getAllEntries()
    {
        return this.gui.getItemList().getItemsFiltered(true);
    }

    @Override
    protected Comparator<ItemListEntry> getComparator()
    {
        return this.sorter;
    }

    @Override
    protected List<String> getEntryStringsForFilter(ItemListEntry entry)
    {
    
        Identifier rl = Registries.ITEM.getId(entry.getItem());

        if (rl != null)
        {
            return ImmutableList.of(entry.getItemName().toLowerCase(), rl.toString().toLowerCase());
        }
        else
        {
            return ImmutableList.of(entry.getItemName().toLowerCase());
        }
    }

    @Override
    protected void refreshBrowserEntries()
    {
        super.refreshBrowserEntries();

        if (this.scrollbarRestored == false && lastScrollbarPosition <= this.scrollBar.getMaxValue())
        {
            // This needs to happen after the setMaxValue() has been called in reCreateListEntryWidgets()
            this.scrollBar.setValue(lastScrollbarPosition);
            this.scrollbarRestored = true;
            this.reCreateListEntryWidgets();
        }
    }

    @Override
    protected WidgetItemListEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, @Nullable ItemListEntry entry)
    {
        return new WidgetItemListEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, this.gui.getItemList(), entry, listIndex, this);
    }
}