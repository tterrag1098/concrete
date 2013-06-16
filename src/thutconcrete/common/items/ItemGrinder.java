package thutconcrete.common.items;

import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.world.World;

import thutconcrete.common.ConcreteCore;
import thutconcrete.common.blocks.Block16Fluid;
import thutconcrete.common.blocks.BlockConcrete;
import thutconcrete.common.blocks.BlockREConcrete;
import thutconcrete.common.items.ItemConcreteDust;
import thutconcrete.common.corehandlers.ItemHandler;
import thutconcrete.common.utils.*;

public class ItemGrinder extends Item {

	public static final int MAX_USES = 128;
	
	public ItemGrinder(int par1) {
		super(par1);
		this.maxStackSize = 1;
		this.setMaxDamage(MAX_USES);
		this.setCreativeTab(ConcreteCore.tabThut);
		this.setUnlocalizedName("smoother");
	}

    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int x, int y, int z, int side, float hitX, float hitY, float hitZ)
    {
    	
    	
        if (!par2EntityPlayer.canPlayerEdit(x, y, z, side, par1ItemStack))
        {
            return false;
        }
        else
        {
        	int blockid = par3World.getBlockId(x, y, z);
        	Block block = Block.blocksList[blockid];
        	int minmeta = 30;
        	int maxmeta = 0;
        	
        	if(block instanceof IStampableBlock && par2EntityPlayer.isSneaking())
        	{
        		int meta = par3World.getBlockMetadata(x, y, z);
        		((IStampableBlock)block).setBlockIcon(blockid, meta, side, par3World, x, y, z, block.getIcon(side, meta), side);
        	}
        	
        	for(int locx = -1; locx < 2; locx++)
        	{
        		for(int locz = -1; locz < 2; locz++)
        		{
        			blockid = par3World.getBlockId(x + locx, y, z + locz);
        			if(block instanceof Block16Fluid)
        			{
        				if(15 - par3World.getBlockMetadata(x + locx, y, z + locz) < minmeta)
        				{
        					minmeta = 15 - par3World.getBlockMetadata(x + locx, y, z + locz);
        				}
        			}
        		}
        	}
        	
        	int modifyx = 0;
        	int modifyz = 0;
        	boolean found = false;
        	int totalPieces = 0;
        	
        	for(int i = 0; i < 9; i++)
        	{
        		found = false;
        		maxmeta = 0;
        		
            	for(int locx = -1; locx < 2; locx++)
            	{
            		for(int locz = -1; locz < 2; locz++)
            		{
            			blockid = par3World.getBlockId(x + locx, y, z + locz);
            			if(Block.blocksList[blockid] instanceof Block16Fluid)
            			{
            				if((15 - par3World.getBlockMetadata(x + locx, y, z + locz)) > maxmeta && (15 - par3World.getBlockMetadata(x + locx, y, z + locz)) != 15)
            				{
            					found = true;
            					modifyx = x + locx;
            					modifyz = z + locz;
            					maxmeta = 15 - par3World.getBlockMetadata(x + locx, y, z + locz);
            				}
            			}
            		}
            	}
            	
            	if(found)
            	{
	            	if(MAX_USES - (15 - par1ItemStack.getItemDamage()) >= maxmeta - minmeta)
	            	{
	            		par3World.setBlockMetadataWithNotify(modifyx, y, modifyz, 15 - minmeta, 3);
	            		par1ItemStack.damageItem(maxmeta - minmeta, par2EntityPlayer);
	            		
	            		blockid = par3World.getBlockId(modifyx, y, modifyz);
	            		
	            		if(Block.blocksList[blockid] instanceof BlockConcrete || Block.blocksList[blockid] instanceof BlockREConcrete)
	            		{
	            			totalPieces += maxmeta - minmeta;
	            		}
	            	}
	            	else
	            	{
	            		par3World.setBlockMetadataWithNotify(modifyx, y, modifyz, MAX_USES - (15 - par1ItemStack.getItemDamage()), 3);
	            		par1ItemStack.damageItem(MAX_USES - (15 - par1ItemStack.getItemDamage()) + 1, par2EntityPlayer);
	            		
	            		blockid = par3World.getBlockId(modifyx, y, modifyz);
	            		
	            		if(Block.blocksList[blockid] instanceof BlockConcrete || Block.blocksList[blockid] instanceof BlockREConcrete)
	            		{
	            			totalPieces += MAX_USES - (15 - par1ItemStack.getItemDamage());
	            		}
	            		
	            		break;
	            	}
            	}
        	}
        	
        	int dustid = -1;
        	
        	for(Item item : ItemHandler.items)
        	{
        		if(item instanceof ItemConcreteDust)
        		{
        			dustid = item.itemID;
        		}
        	}
        	
        	if(dustid == -1)
        	{
        		return true;
        	}
        	
        	if(!par3World.isRemote)
        	{

            	Random rdusts = new Random();
            	int randnum = rdusts.nextInt(totalPieces+1);
            	EntityItem dusts = new EntityItem(par3World, x + hitX, y + hitY, z + hitZ, new ItemStack(dustid, randnum, 0));
            	if(randnum>0)
            	{
            		par3World.spawnEntityInWorld(dusts);
            	}
        	}
        	
        	return true;
        }
        
        
    }
    
	@SideOnly(Side.CLIENT)
    public void registerIcons(IconRegister par1IconRegister)
    {
        this.itemIcon = par1IconRegister.registerIcon("thutconcrete:ItemSmoother");
    }
}
