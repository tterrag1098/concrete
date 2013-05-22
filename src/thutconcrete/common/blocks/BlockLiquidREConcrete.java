package thutconcrete.common.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import thutconcrete.client.BlockRenderHandler;
import thutconcrete.common.ConcreteCore;
import thutconcrete.common.blocks.Block16Fluid.WetConcrete;
import thutconcrete.common.corehandlers.TSaveHandler;
import thutconcrete.common.utils.IRebar;
import thutconcrete.common.utils.ISaveable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

public class BlockLiquidREConcrete extends Block16Fluid implements IRebar
{
	
	public static Block instance;
	public int colourid;
	static Material wetConcrete = (new WetConcrete(MapColor.stoneColor));
	Integer[][] data;
	boolean[] side = new boolean[6];
	
	public BlockLiquidREConcrete(int par1) {
		super(par1, wetConcrete);
		setUnlocalizedName("REconcreteLiquid");
		this.setResistance((float) 10.0);
		this.instance = this;
	}
	
	
	@Override
    public void onBlockPlacedBy(World worldObj,int x,int y,int z,EntityLiving entity, ItemStack item){
		worldObj.setBlockMetadataWithNotify(x, y, z, 15, 3);

		if(data==null){
			setData();
			}
    	super.onBlockPlacedBy(worldObj, x, y, z, entity, item);
    }
	
	@Override
    public void onBlockAdded(World worldObj, int x, int y, int z) {
		if(data==null){
			setData();
			}
    }
	
	@Override
	public void updateTick(World worldObj, int x, int y, int z, Random par5Random){

		if(data==null){
			setData();
			}
		 super.updateTick(worldObj, x, y, z, par5Random);
		 
	}
	
	public void setData(){
		
		List<Integer> combinationList = new ArrayList<Integer>();
		List<Integer> desiccantList = new ArrayList<Integer>();
		
		//Rebar
		combinationList.add(BlockRebar.instance.blockID+4096*BlockLiquidREConcrete.instance.blockID);
		
		//RE Concrete to make this colour
		combinationList.add(BlockLiquidREConcrete.instance.blockID+4096*BlockLiquidREConcrete.instance.blockID);
		combinationList.add(BlockREConcrete.instance.blockID+4096*BlockLiquidREConcrete.instance.blockID);
		
		
		desiccantList.add(0+5*4096);
		
		desiccantList.add(BlockREConcrete.instance.blockID+5*4096*4);
	
		desiccantList.add(BlockConcrete.instance.blockID+5*4096*4);
		data = new Integer[][]{
				{	
					BlockRebar.instance.blockID,//ID that this returns when meta hits -1, 
					0,//the viscosity factor,
					BlockREConcrete.instance.blockID,//a secondary ID that this can turn into used for hardening,
					1,//The hardening differential that prevents things staying liquid forever.,
					0,//a randomness coefficient, this is multiplied by a random 0-10 then added to the hardening differential and viscosity.,
					1,//The will fall of edges factor, this is 0 or 1,
					1,//0 = not colourable, 1 = colourable.
				},
				
				desiccantList.toArray(new Integer[0]),
				combinationList.toArray(new Integer[0]),
			};
			fluid16Blocks.put(BlockLiquidREConcrete.instance.blockID,data);
	}
	

	

	/////////////////////////////////////////Block Bounds Stuff//////////////////////////////////////////////////////////
	
	
	//*
    /**
     * Adds all intersecting collision boxes to a list. (Be sure to only add boxes to the list if they intersect the
     * mask.) Parameters: World, X, Y, Z, mask, list, colliding entity
     */
	@Override
    public void addCollisionBoxesToList(World worldObj, int x, int y, int z, AxisAlignedBB aaBB, List list, Entity par7Entity)
    {
		side = sides(worldObj,x,y,z);
		
		if(!(side[0]||side[1]||side[2]||side[3]||side[4]||side[5]))
			side = new boolean[] {true, true, true, true, false, false};
		
    	AxisAlignedBB aabb;
    	int n = 5;
    	
        for (ForgeDirection fside : ForgeDirection.VALID_DIRECTIONS)
        {
                AxisAlignedBB coll = getBoundingBoxForSide(fside).offset(x, y, z);
                if (aaBB.intersectsWith(coll)&&this.side[n])
                        list.add(coll);
                n--;
        }

		
    }

	

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess par1IBlockAccess, int x, int y, int z)
    {
		side = sides(par1IBlockAccess,x,y,z);
		
		if(!(side[0]||side[1]||side[2]||side[3]||side[4]||side[5]))
			side = new boolean[] {true, true, true, true, false, false};
		setBlockBounds(0.35F, 0.35F, 0.35F, 0.65F, 0.65F, 0.65F);

    	this.setBoundsByMeta(par1IBlockAccess.getBlockMetadata(x, y, z));
    }

    public AxisAlignedBB getBoundingBoxForSide(ForgeDirection fside)
    {
            switch (fside)
            {
                    case UP:
                    {
                            return AxisAlignedBB.getBoundingBox(0.35F, 0.4F, 0.35F, 0.65F, 1F, 0.65F);
                    }
                    case DOWN:
                    {
                            return AxisAlignedBB.getBoundingBox(0.35F, 0.0F, 0.35F, 0.65F, 0.6F, 0.65F);
                    }
                    case NORTH:
                    {
                            return AxisAlignedBB.getBoundingBox(0.35F, 0.35F, 0.0F, 0.65F, 0.65F, 0.6F);
                    }
                    case SOUTH:
                    {
                            return AxisAlignedBB.getBoundingBox(0.35F, 0.35F, 0.4F, 0.65F, 0.65F, 1F);
                    }
                    case EAST:
                    {
                            return AxisAlignedBB.getBoundingBox(0.4F, 0.35F, 0.35F, 1F, 0.65F, 0.65F);
                    }
                    case WEST:
                    {
                            return AxisAlignedBB.getBoundingBox(0.0F, 0.35F, 0.35F, 0.60F, 0.65F, 0.65F);
                    }
                    default:
                    {
                            return AxisAlignedBB.getBoundingBox(0f, 0f, 0f, 1f, 1f, 1f);
                    }
            }
    }
    
    private void setBlockBoundsForSide(int x, int y, int z, ForgeDirection side)
    {
            switch (side)
	        {
		            case UP:
		            {
		                    setBlockBounds(0.35F, 0.4F, 0.35F, 0.65F, 1F, 0.65F);
		                    break;
		            }
		            case DOWN:
		            {
		                    setBlockBounds(0.35F, 0.0F, 0.35F, 0.65F, 0.6F, 0.65F);
		                    break;
		            }
		            case NORTH:
		            {
		                    setBlockBounds(0.35F, 0.35F, 0.0F, 0.65F, 0.65F, 0.6F);
		                    break;
		            }
		            case SOUTH:
		            {
		                    setBlockBounds(0.35F, 0.35F, 0.4F, 0.65F, 0.65F, 1F);
		                    break;
		            }
		            case EAST:
		            {
		                    setBlockBounds(0.4F, 0.35F, 0.35F, 1F, 0.65F, 0.65F);
		                    break;
		            }
		            case WEST:
		            {
		                    setBlockBounds(0.0F, 0.35F, 0.35F, 0.60F, 0.65F, 0.65F);
		                    break;
		            }
		            default:
		            {
		                    setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f);
		                    break;
		            }
            }
    }
    
    
    
	
	
   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SideOnly(Side.CLIENT)
    private Icon[] iconArray;
	@SideOnly(Side.CLIENT)
    public void registerIcons(IconRegister par1IconRegister)
    {
        this.blockIcon = par1IconRegister.registerIcon("thutconcrete:wetConcrete_"+colourid);
    	this.theIcon = par1IconRegister.registerIcon("thutconcrete:" + "rebar");
    	this.iconArray = new Icon[16];
    	for (int i = 0; i < this.iconArray.length; ++i)
        {
            this.iconArray[i] = par1IconRegister.registerIcon("thutconcrete:" + "wetConcrete_"+i);
        }
    	
    }
	
    @SideOnly(Side.CLIENT)

    /**
     * Returns the texture index of the thin side of the pane.
     */
    public Icon getSideTextureIndex()
    {
        return this.theIcon;
    }

    @SideOnly(Side.CLIENT)
    public Icon theIcon;
	

	    @Override
	    public int quantityDropped(int meta, int fortune, Random random)
	    {
	        return 0;
	    }
	    
	    /**
	     * The type of render function that is called for this block
	     */
	    @Override
	    public int getRenderType()
	    {
	        return BlockRenderHandler.ID;
	    }

		public boolean[] sides(IBlockAccess worldObj, int x, int y, int z) {
			boolean[] side = new boolean[6];
	    	int[][]sides = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0,1,0},{0,-1,0}};
			for(int i = 0; i<6; i++){
				int id = worldObj.getBlockId(x+sides[i][0], y+sides[i][1], z+sides[i][2]);
				Block block = Block.blocksList[id];
				side[i] = (block instanceof IRebar);
			}
			return side;
		}

		@Override
		public boolean[] sides(World worldObj, int x, int y, int z) {
			boolean[] side = new boolean[6];
	    	int[][]sides = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0,1,0},{0,-1,0}};
			for(int i = 0; i<6; i++){
				int id = worldObj.getBlockId(x+sides[i][0], y+sides[i][1], z+sides[i][2]);
				Block block = Block.blocksList[id];
				side[i] = (block instanceof IRebar);
			}
			return side;
		}

		@Override
		public Icon getIcon(Block block) {
			return this.blockIcon;
		}

		 @SideOnly(Side.CLIENT)

		    /**
		     * Retrieves the block texture to use based on the display side. Args: iBlockAccess, x, y, z, side
		     */
		    public Icon getBlockTexture(IBlockAccess par1IBlockAccess, int x, int y, int z, int par5)
		    {
			 TileEntityBlock16Fluid te = (TileEntityBlock16Fluid) par1IBlockAccess.getBlockTileEntity(x, y, z);
			 return this.iconArray[te.metaArray[par5]];
			 	
		    }

		    /**
		     * Sets the block's bounds for rendering it as an item
		     */
		    public void setBlockBoundsForItemRender()
		    {
		        this.setBoundsByMeta(15);
		    }
		 
		 
}