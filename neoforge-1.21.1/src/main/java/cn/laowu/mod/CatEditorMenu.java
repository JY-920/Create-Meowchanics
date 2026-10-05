package cn.laowu.mod;

import cn.laowu.mod.genetics.*;
import cn.laowu.mod.create.CatAutoLaserTargets;
import cn.laowu.mod.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.util.*;

/** Locked seat occupant, ordered trait transfers, and a server-owned appearance draft. */
public final class CatEditorMenu extends AbstractContainerMenu {
    public static final int TEMP_SLOTS = 16, RESET = 30, COMMIT = 31, SELECT = 2000;
    private final BlockPos pos;
    private final UUID targetId;
    private final int entityId;
    private final List<CatTraitType> catalog;
    private final List<ResourceLocation> materials;
    private final long revision;
    private final Cat viewedCat;
    private final boolean client;
    private boolean released;
    private boolean draftInitialized, awaitingDraftAck;
    private int pendingEpoch;
    private int status, page, draftEpoch;
    private CatGenome baseline;
    private final int[] choices = new int[12];
    public final SimpleContainer input = new SimpleContainer(TEMP_SLOTS);
    private final ContainerData data;

    public CatEditorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), buf.readUUID(), buf.readVarInt(), readCatalog(buf),
                CatGenome.load(buf.readNbt()).orElseThrow(), null);
    }
    public CatEditorMenu(int id, Inventory inv, Cat cat, BlockPos pos) {
        this(id, inv, pos, cat.getUUID(), cat.getId(), catalogFor(cat), CatGenomeData.ensure(cat), cat);
    }
    private CatEditorMenu(int id, Inventory inv, BlockPos pos, UUID target, int entityId,
                          List<CatTraitType> catalog, CatGenome genome, Cat cat) {
        super(LaoWuMod.CAT_EDITOR_MENU.get(), id);
        this.pos=pos.immutable(); targetId=target; this.entityId=entityId; this.catalog=catalog;
        baseline=genome; client=inv.player.level().isClientSide;
        materials=CatMaterialRegistry.catVariants().stream().filter(v->v.getNamespace().equals("minecraft")).toList();
        Arrays.fill(choices,-1);
        revision=CatTraitRegistry.revision(client); viewedCat=cat;
        int count=catalog.size();
        data=cat==null?new SimpleContainerData(count+6):new ContainerData() {
            public int get(int i) {
                if(i==count)return status;
                if(i==count+5)return draftEpoch;
                var traits=CatTraitData.ensure(cat).traits();
                if(i>count){int row=i-count-1;return row<traits.size()?catalog.indexOf(traits.get(row).trait())+1:0;}
                return CatTraitData.ensure(cat).rawLevel(catalog.get(i));
            }
            public void set(int i,int value){} public int getCount(){return count+6;}
        };
        addDataSlots(data);
        for(int i=0;i<4;i++) {
            final int row=i;
            addSlot(new Slot(input,i,233,(i==0?34:35+i*29)) {
                @Override public boolean mayPlace(ItemStack s){return isActive()&&s.is(LaoWuMod.CAT_TRAIT_TOKEN.get());}
                @Override public boolean mayPickup(Player p){return isActive();}
                @Override public boolean isActive(){return page==0&&traitsReady()&&(row<=installedCount()||hasItem());}
            });
        }
        for(int i=0;i<12;i++) {
            addSlot(new Slot(input,4+i,151+(i/6)*144,18+(i%6)*25) {
                @Override public boolean mayPlace(ItemStack s){return isActive()&&appearanceReady()&&CatMaterialRegistry.blockMaterial(s).isPresent();}
                @Override public boolean mayPickup(Player p){return isActive()&&appearanceReady();}
                @Override public boolean isActive(){return page==1;}
            });
        }
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,124+x*18,225+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,124+x*18,283));
        if(cat!=null)CatProfileData.beginViewing(cat);
    }
    public static boolean eligible(Cat cat,Player player,BlockPos machine) {
        if(cat==null||!cat.isAlive()||!cat.isTame()||!player.getUUID().equals(cat.getOwnerUUID()))return false;
        BlockPos seat=CatAutoLaserTargets.seat(cat);
        return seat!=null&&seat.getY()==machine.getY()&&Math.abs(seat.getX()-machine.getX())+Math.abs(seat.getZ()-machine.getZ())==1;
    }
    public static Cat select(Player player,BlockPos machine) {
        return player.level().getEntitiesOfClass(Cat.class,new net.minecraft.world.phys.AABB(machine).inflate(3),
                c->eligible(c,player,machine)).stream().min(Comparator.comparing(Cat::getUUID)).orElse(null);
    }
    public Cat cat(Player player) {
        var entity=player.level() instanceof ServerLevel server?server.getEntity(targetId):player.level().getEntity(entityId);
        return entity instanceof Cat c&&c.getUUID().equals(targetId)?c:null;
    }
    public int entityId(){return entityId;}
    @Override public void setData(int index,int value) {
        super.setData(index,value);
        if(client&&index==catalog.size()+5) {
            draftInitialized=true;
            if(awaitingDraftAck&&value!=pendingEpoch) {
                Arrays.fill(choices,-1);awaitingDraftAck=false;
            }
        }
        if(client&&index==catalog.size()&&value==5)awaitingDraftAck=false;
    }
    public boolean appearanceReady(){return !client||draftInitialized&&!awaitingDraftAck;}
    public boolean traitsReady(){return !client||draftInitialized;}
    public int page(){return page;}
    public List<CatTraitType> catalog(){return catalog;}
    public int traitLevel(int index){return index>=0&&index<catalog.size()?data.get(index):0;}
    public int installedIndex(int row){return row>=0&&row<4?data.get(catalog.size()+1+row)-1:-1;}
    public int installedCount(){int n=0;for(int row=0;row<4;row++)if(installedIndex(row)>=0)n++;return n;}
    public CatTraitProfile displayedTraits() {
        CompoundTag tag=new CompoundTag();tag.putInt("Version",1);ListTag entries=new ListTag();
        for(int row=0;row<4;row++){int index=installedIndex(row);if(index<0||index>=catalog.size())continue;
            CompoundTag entry=new CompoundTag();entry.putString("Id",catalog.get(index).id().toString());
            entry.putInt("Level",traitLevel(index));entries.add(entry);}
        tag.put("Traits",entries);return CatTraitProfile.load(tag,client).orElse(CatTraitProfile.EMPTY);
    }
    public Component statusMessage(){return Component.translatable("gui.laowu.cat_editor.status."+data.get(catalog.size()));}
    public static Component regionName(int i){return CatMaterialEditorMenu.regionName(i);}
    public int materialCount(){return materials.size();}
    public int selection(int region){return choices[region];}
    public void observeAppearance(Cat source){if(client)baseline=CatGenomeData.getOrFallback(source);}
    public Component materialName(int region) {
        var sample=CatMaterialRegistry.blockMaterial(input.getItem(4+region));
        if(sample.isPresent())return CatMaterialRegistry.displayName(sample.get());
        if(choices[region]<0)return Component.translatable("gui.laowu.cat_editor.unchanged");
        return CatMaterialRegistry.displayName(materials.get(choices[region]));
    }
    public int selectionAction(int region,int material){return SELECT+region*32+material+1;}
    public CatGenome pendingGenome() {
        CatGenome next=baseline;
        for(int i=0;i<12;i++) {
            var sample=CatMaterialRegistry.blockMaterial(input.getItem(4+i));
            ResourceLocation material=sample.orElse(choices[i]<0?null:materials.get(choices[i]));
            if(material!=null)next=i==0?next.withUniformMaterial(material):next.withMaterial(CatRegion.values()[i-1],material);
        }
        return next;
    }
    private static List<CatTraitType> catalogFor(Cat cat) {
        var all=new ArrayList<>(CatTraitRegistry.values(false));
        for(var entry:CatTraitData.ensure(cat).traits())if(all.stream().noneMatch(t->t.id().equals(entry.trait().id())))all.add(entry.trait());
        return List.copyOf(all);
    }
    public static void writeOpeningData(FriendlyByteBuf buf,Cat cat,BlockPos pos) {
        buf.writeBlockPos(pos);buf.writeUUID(cat.getUUID());buf.writeVarInt(cat.getId());
        var entries=catalogFor(cat);buf.writeVarInt(entries.size());for(var t:entries)buf.writeUtf(t.id().toString(),128);
        buf.writeNbt(CatGenomeData.ensure(cat).save());
    }
    private static List<CatTraitType> readCatalog(FriendlyByteBuf buf) {
        int count=buf.readVarInt();if(count<0||count>CatTrait.values().length+CatTraitRegistry.MAX_CUSTOM_TRAITS+4)throw new IllegalArgumentException("Trait catalog bounds");
        var all=new ArrayList<CatTraitType>();var seen=new HashSet<ResourceLocation>();
        for(int i=0;i<count;i++){var id=ResourceLocation.tryParse(buf.readUtf(128));if(id==null||!seen.add(id))throw new IllegalArgumentException("Trait ID");
            all.add(CatTraitRegistry.resolve(id,true));}return List.copyOf(all);
    }
    @Override public boolean stillValid(Player player) {
        if(released)return false;
        if(client)return true;
        return CatTraitRegistry.revision(false)==revision&&player.level().hasChunkAt(pos)
                &&player.level().getBlockState(pos).is(LaoWuMod.CAT_EDITOR.get())
                &&player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=64&&eligible(cat(player),player,pos);
    }
    @Override public boolean clickMenuButton(Player player,int action) {
        if(!stillValid(player))return false;
        if(action==0||action==1){page=action;return true;}
        if(action>=SELECT&&action<SELECT+12*32) {
            int region=(action-SELECT)/32, choice=(action-SELECT)%32-1;
            if(page!=1||!appearanceReady()||choice>=materials.size())return false;
            choices[region]=choice;return true;
        }
        if(client) {
            if(page!=1||!appearanceReady()||(action!=RESET&&action!=COMMIT))return false;
            awaitingDraftAck=true;pendingEpoch=data.get(catalog.size()+5);return true;
        }
        Cat cat=cat(player);boolean success=false;status=0;
        if(page==1&&action==RESET) {
            clearDraft(player);
            success=true;
        } else if(page==1&&action==COMMIT) {
            var current=CatGenomeData.ensure(cat);
            if(!current.equals(baseline)){status=5;broadcastChanges();return false;}
            var next=pendingGenome();
            if(!next.equals(current)) {
                CatGenomeData.set(cat,next);
                for(int i=0;i<12;i++) {
                    var material=CatMaterialRegistry.blockMaterial(input.getItem(4+i));
                    if(material.isEmpty())continue;
                    boolean used=false;
                    for(CatRegion region:CatRegion.values()) {
                        int r=region.ordinal()+1;
                        if(i!=0&&i!=r)continue;
                        if(i==0&&(choices[r]>=0||CatMaterialRegistry.blockMaterial(input.getItem(4+r)).isPresent()))continue;
                        if(!next.material(region).equals(current.material(region))&&next.material(region).equals(material.get()))used=true;
                    }
                    if(used)input.getItem(4+i).shrink(1);
                }
                baseline=next;ModNetwork.syncCatGenomeToTracking(cat);
            }
            // A consumed override must never expose a leftover whole-body sample
            // or older list choice. Repeated confirmation remains a true no-op.
            clearDraft(player);
            success=true;
        } else if(page==0&&action>=20&&action<24) {
            int row=action-20;
            if(row!=installedCount())return false;
            success=CatEditorTraits.install(cat,input.getItem(row));status=success?1:3;
        } else if(page==0&&action>=1000&&action<1000+catalog.size()) {
            int index=action-1000,row=-1;
            for(int i=0;i<4;i++)if(installedIndex(i)==index)row=i;
            if(row<0)return false;
            if(!input.getItem(row).isEmpty()){status=4;broadcastChanges();return false;}
            ItemStack token=CatEditorTraits.extract(cat,catalog.get(index).id());
            if(!token.isEmpty()){input.setItem(row,token);success=true;}else status=3;
        } else return false;
        input.setChanged();if(success)status=1;broadcastChanges();return success;
    }
    private void clearDraft(Player player) {
        Arrays.fill(choices,-1);draftEpoch++;
        for(int i=4;i<TEMP_SLOTS;i++) {
            ItemStack stack=input.removeItemNoUpdate(i);
            if(!stack.isEmpty())player.getInventory().placeItemBackInInventory(stack);
        }
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size()||!stillValid(player))return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.isActive()||!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),before=stack.copy();
        if(index<TEMP_SLOTS){if(!moveItemStackTo(stack,TEMP_SLOTS,TEMP_SLOTS+36,true))return ItemStack.EMPTY;}
        else if(page==0&&stack.is(LaoWuMod.CAT_TRAIT_TOKEN.get())&&installedCount()<4){
            int row=installedCount();if(!moveItemStackTo(stack,row,row+1,false))return ItemStack.EMPTY;
        }else if(page==1&&CatMaterialRegistry.blockMaterial(stack).isPresent()){
            if(!moveItemStackTo(stack,4,16,false))return ItemStack.EMPTY;
        }else return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return before;
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if(!released){released=true;if(!client)clearContainer(player,input);
            if(viewedCat!=null)CatProfileData.endViewing(viewedCat);}
    }
}
