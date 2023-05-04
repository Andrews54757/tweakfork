package fi.dy.masa.tweakeroo.items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.tweaks.RenderTweaks;
import fi.dy.masa.tweakeroo.tweaks.RenderTweaks.ContainerEntry;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

public class ItemList
{
    public static final ItemList INSTANCE = new ItemList();


    protected final List<ItemListEntry> itemListPreFiltered = new ArrayList<>();
    protected final List<ItemListEntry> itemListFiltered = new ArrayList<>();
    protected final HashSet<Item> selectedItems = new HashSet<Item>();
    protected HashMap<Item, ItemListEntry> itemListAll = new HashMap<Item, ItemListEntry>();
    protected SortCriteria sortCriteria = SortCriteria.COUNT_TOTAL;
    protected boolean reverse;
    protected boolean hideAvailable;
    protected int multiplier = 1;
    protected long countTotal;
    protected long countBoxes;
    protected long countItemsInBoxes;
    protected long scanCount;
    protected long containerCount;

    public String getName() {
        return "";
    }

    public String getTitle() {
        return "Container Scanned Items";
    }

    public void clearSelected() {
        selectedItems.clear();
        rebuildOverlayData();
    }

    public boolean isEntrySelected(ItemListEntry entry) {
        return this.selectedItems.contains(entry.getItem());
    }
    public boolean isItemSelected(Item item) {
        return this.selectedItems.contains(item);
    }
    public Collection<ItemListEntry> getItemsAll()
    {
        return this.itemListAll.values();
    }

    public List<ItemListEntry> getItemsFiltered(boolean refresh)
    {
        if (this.hideAvailable)
        {
            return this.getItemsMissingOnly(refresh);
        }

        return this.itemListPreFiltered;
    }

    public List<ItemListEntry> getItemsMissingOnly(boolean refresh)
    {
        if (refresh)
        {
            this.recreateFilteredList();
        }

        return this.itemListFiltered;
    }

    

    public void recreateFilteredList()
    {
        this.itemListFiltered.clear();
        this.itemListFiltered.addAll(this.itemListPreFiltered);
      
    }

    public void reCreateItemList() {
        this.itemListAll.clear();
        createItemList(RenderTweaks.CONTAINERCACHE);
        
        this.refreshPreFilteredList();
        this.updateCounts();
    }

    private void createItemList(ConcurrentHashMap<Long, ContainerEntry> CONTAINERCACHE) {
        HashMap<Item, ItemListEntry> map = this.itemListAll;
        this.containerCount = CONTAINERCACHE.size();
        this.scanCount = 0;
        Iterator<ContainerEntry> iterator = CONTAINERCACHE.values().iterator();
        while (iterator.hasNext()) {
            ContainerEntry entry = iterator.next();
            if (entry.skipCount) continue;
            if (entry.status != 2) continue;
            this.scanCount++;
            for (ItemStack stack : entry.contentList) {
                if (stack.isEmpty()) continue;
                Item item = stack.getItem();
                if (item instanceof BlockItem) {
                    if (((BlockItem)item).getBlock() instanceof ShulkerBoxBlock && InventoryUtils.shulkerBoxHasItems(stack)) {
                        HashSet<Item> hasSeenInBox = new HashSet<Item>();
       
                        for (ItemStack stack2 : InventoryUtils.getStoredItems(stack)) {
                            if (stack2.isEmpty()) continue;
                            Item item2 = stack2.getItem();

                            if (!map.containsKey(item2)) {
                                map.put(item2,new ItemListEntry(stack2, 0, 0, 0, 0));
                            }
                            ItemListEntry listEntry = map.get(item2);
                            listEntry.countTotal += stack2.getCount();
            
                            if(!listEntry.containers.contains(entry)) {
                                listEntry.containers.add(entry);
                            }
                            if(!hasSeenInBox.contains(item2)) {
                                hasSeenInBox.add(item2);
                                listEntry.countBoxes++;
                            }
                            listEntry.countItemsInBoxes += stack2.getCount();
                        }
                    }
                }

                if (!map.containsKey(item)) {
                    map.put(item,new ItemListEntry(stack, 0, 0, 0, 0));
                }

                ItemListEntry listEntry = map.get(item);
                listEntry.countTotal += stack.getCount();

                if(!listEntry.containers.contains(entry)) {
                    listEntry.containers.add(entry);
                }
               
            }
            
        }

    }

    /**
     * Resets the pre-filtered items list to the all items list
     */
    public void refreshPreFilteredList()
    {
        this.itemListPreFiltered.clear();
        this.itemListPreFiltered.addAll(this.getItemsAll());
    }

    public SortCriteria getSortCriteria()
    {
        return this.sortCriteria;
    }

    public boolean getSortInReverse()
    {
        return this.reverse;
    }

    public boolean getHideAvailable()
    {
        return this.hideAvailable;
    }

    public int getMultiplier()
    {
        return this.multiplier;
    }

    public void setSortCriteria(SortCriteria criteria)
    {
        if (this.sortCriteria == criteria)
        {
            this.reverse = ! this.reverse;
        }
        else
        {
            this.sortCriteria = criteria;
            this.reverse = criteria == SortCriteria.NAME;
        }
    }

    public void setHideAvailable(boolean hideAvailable)
    {
        this.hideAvailable = hideAvailable;
    }

    public void setMultiplier(int multiplier)
    {
        this.multiplier = MathHelper.clamp(multiplier, 1, Integer.MAX_VALUE);
    }

    public void updateCounts()
    {
        this.countTotal = 0;
        this.countBoxes = 0;
        this.countItemsInBoxes = 0;

        for (ItemListEntry entry : this.getItemsAll())
        {
            this.countTotal += entry.getCountTotal();
            this.countBoxes += entry.getCountBoxes();
            this.countItemsInBoxes += entry.getCountItemsInBoxes();
        }
    }

    public long getCountTotal()
    {
        return this.countTotal;
    }

    public long getCountBoxes()
    {
        return this.countBoxes;
    }

    public long getCountItemsInBoxes()
    {
        return this.countItemsInBoxes;
    }


    public boolean getScanToggle() {
        return FeatureToggle.TWEAK_CONTAINER_SCAN.getBooleanValue();
    }

    public boolean getCountsRenderToggle() {
        return FeatureToggle.TWEAK_CONTAINER_SCAN_COUNTS.getBooleanValue();
    }

   
    public long getItemTypesCountTotal() {
        return this.itemListAll.size();
    }

    public long getScanCount() {
        return this.scanCount;
    }

    public long getContainerCount() {
        return this.containerCount;
    }


    public void clearCache() {
        RenderTweaks.clearContainerScanCache();
        reCreateItemList();
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("sort_criteria", new JsonPrimitive(this.sortCriteria.name()));
        obj.add("sort_reverse", new JsonPrimitive(this.reverse));

        return obj;
    }

    public void fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "sort_criteria"))
        {
            this.sortCriteria = SortCriteria.fromStringStatic(JsonUtils.getString(obj, "sort_criteria"));
        }

        this.reverse = JsonUtils.getBooleanOrDefault(obj, "sort_reverse", false);
    }

    public enum SortCriteria
    {
        NAME,
        COUNT_TOTAL,
        COUNT_BOXES,
        COUNT_CONTAINERS;

        public static SortCriteria fromStringStatic(String name)
        {
            for (SortCriteria mode : SortCriteria.values())
            {
                if (mode.name().equalsIgnoreCase(name))
                {
                    return mode;
                }
            }

            return SortCriteria.COUNT_TOTAL;
        }
    }

    public void toggleCountsRender() {
        FeatureToggle.TWEAK_CONTAINER_SCAN_COUNTS.toggleBooleanValue();
    }

    public void toggleScan() {
        FeatureToggle.TWEAK_CONTAINER_SCAN.toggleBooleanValue();
    }

    public void rebuildOverlayData() {
        
        HashMap<Long, ArrayList<Item>> data = new HashMap<Long, ArrayList<Item>>();
        Iterator<Item> iterator = this.selectedItems.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (!itemListAll.containsKey(item)) continue;
            ItemListEntry entry = itemListAll.get(item);
            
            Iterator<ContainerEntry> iterator2 = entry.containers.iterator();
            while (iterator2.hasNext()) {
                ContainerEntry ce = iterator2.next();

                if (!data.containsKey(ce.pos.asLong())) {
                    data.put(ce.pos.asLong(), new ArrayList<Item>());
                }
                data.get(ce.pos.asLong()).add(item);
            }
            
        }

        RenderTweaks.setContainerOverlayData(data);
    }

    public void toggleEntrySelection(ItemListEntry entry) {
        if (this.isEntrySelected(entry)) {
            this.selectedItems.remove(entry.getItem());
        } else {
            this.selectedItems.add(entry.getItem());
        }

        this.rebuildOverlayData();
    }

   

}