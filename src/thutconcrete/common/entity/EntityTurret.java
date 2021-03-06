package thutconcrete.common.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import thutconcrete.api.utils.IMultiBox;
import thutconcrete.api.utils.Vector3;
import thutconcrete.api.utils.Vector3.Matrix3;
import thutconcrete.common.ConcreteCore;
import thutconcrete.common.blocks.*;
import thutconcrete.common.network.PacketBeam;
import thutconcrete.common.network.PacketMountedCommand;
import thutconcrete.common.utils.LinearAlgebra;
import universalelectricity.core.block.IElectricityStorage;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.WatchableObject;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBook;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.event.terraingen.BiomeEvent.GetWaterColor;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraft.inventory.IInventory;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class EntityTurret  extends EntityLiving implements IEntityAdditionalSpawnData, IMultiBox
{
	int rate = 100;
	int fireCooldown = 100;
	int mountTime = 0;
	
	public int type = 1;
	
	public double v = 100;
	
	public Entity owner;
	
	private AITurret ai;
	
	public float yaw = 0; 
	public float pitch = 0;
	
	public Vector3 origin = new Vector3();
	public Vector3 thisLaser = new Vector3();

	public Vector3 turretDir = new Vector3(1,0,0);
	
	public static boolean energyEnabled = false;

	public boolean powered = false;
	public float mass = 3;
	public int size = 1;
	public boolean toDismount;
	public boolean fire;
	
	public ConcurrentHashMap<String, Matrix3> boxes = new ConcurrentHashMap<String, Matrix3>();
	public ConcurrentHashMap<String, Vector3> offsets = new ConcurrentHashMap<String, Vector3>();
	
	public Matrix3 turretBox = new Matrix3();
	
	public EntityTurret(World par1World) {
		super(par1World);
		this.ignoreFrustumCheck = true;
	}
	
	public EntityTurret(World world, double x, double y, double z)
	{
		this(world);
		this.setPosition(x, y, z);
		setSize(size);
	}
	
	public EntityTurret(World world, double x, double y, double z, int size)
	{
		this(world);
		this.setPosition(x, y, z);
		this.size = size;
		setSize(size);
	}
	
	public Vector3 source()
	{
		thisLaser = new Vector3(this);
		return thisLaser.add(new Vector3(0.0,(size*0.8),0.0));
	}
	
    /**
     * Checks if the entity's current position is a valid location to spawn this entity.
     */
    public boolean getCanSpawnHere()
    {
    	return false;
    }
	
    public boolean isPotionApplicable(PotionEffect par1PotionEffect)
    {
    	return false;
    }
	
	@Override
	public void onUpdate()
	{
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        clamp();
        if(motionY<0.03)
        {
        	this.motionY -= 0.01D;
        }
        else
        {
        	this.motionY *= 0.1;
        }
        this.motionX *= 0.1;
        this.motionZ *= 0.1;
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
		init();
		checkCollision();
		if(worldObj.getBlockPowerInput((int)Math.floor(posX),(int)Math.floor(posY)-1,(int)Math.floor(posZ))>0)
		{
			powered = true;;
		}
		else
		{
			powered = false;
		}

		if(this.riddenByEntity==null)
		{
			ai.autoFire();
		}
		else if(this.riddenByEntity!=null)
		{
			turretDir = new Vector3(this.riddenByEntity.getLookVec());
			if(turretDir.y<-0.3)
			{
				turretDir.y = -0.3;
			}
			if(this.riddenByEntity instanceof EntityPlayer)
			{
				EntityPlayer player = (EntityPlayer)this.riddenByEntity;
				fakeRotationAngles();
				
				if(worldObj.isRemote)
				{
					if(Mouse.isButtonDown(1)&&mountTime>10)
					{
						PacketDispatcher.sendPacketToServer(PacketMountedCommand.getPacket(this, 0));
					}
					if(!fire&&Keyboard.isKeyDown(Keyboard.KEY_SPACE)&&fireCooldown>=rate)
					{
						PacketDispatcher.sendPacketToServer(PacketMountedCommand.getPacket(this, 1));
						System.out.println(fireCooldown+" "+rate);
						fire = true;
						fireCooldown = 0;
					}
				}
				if(toDismount&&!worldObj.isRemote)
				{
					interact(player);
					toDismount = false;
				}
				if(fire&&fireCooldown>=rate)
				{
					fire();
					fireCooldown = 0;
					fire = false;
				}
				else
				{
					fireCooldown++;
				}
			}
		}
		mountTime++;
	}
	
	public void init()
	{
		if(!origin.equals(source()))
		{
			origin.set(source());
		}
		if(this.health<=0)
		{
			System.out.println(this.health);
			this.setDead();
		}
		
		rate = checkAmmo(type)? 50:0;
		
		if(ai==null)
		{
			ai = new AITurret(this);
		}
		this.prevPosY = this.posY;
	}
	
	public void clamp()
	{
		this.setPosition(Math.floor(posX) + 0.5, posY, Math.floor(posZ) + 0.5);
	}
	
	public void fire()
	{	
		boolean ammo = checkAmmo(type);
		
		type = ammo?0:1;
		
		double requiredEnergy = type==0?100000:1000;
		
		boolean energy = checkEnergy(requiredEnergy);
		if(energyEnabled&&!energy)
		{
			if(riddenByEntity!=null&&riddenByEntity instanceof EntityPlayer)
			{
				((EntityPlayer)riddenByEntity).addChatMessage("Insufficient Energy");
			}
			return;
		}
		if(ammo)
		{
			consumeAmmo();
		}
		
		consumeEnergy(requiredEnergy);
		
		if(type==0)
		{
			if(!worldObj.isRemote)
			{
				EntityBeam beam = new EntityBeam(worldObj, origin.add(turretDir.scalarMult(size)), turretDir, true,mass,v);
				worldObj.spawnEntityInWorld(beam);
			}
			origin.add(turretDir.scalarMult(size)).playSoundEffect(worldObj, 300, 1, "railgun");
		}
		if(type==1)
		{
			if(!worldObj.isRemote)
			{
				EntityBeam beam = new EntityBeam(worldObj, origin.add(turretDir.scalarMult(size)), turretDir, false,size,10);
				worldObj.spawnEntityInWorld(beam);
				
				Vector3 next = Vector3.getNextSurfacePoint(worldObj, origin.add(turretDir.scalarMult(size)), turretDir, 256);	
			}
			//origin.add(turretDir.scalarMult(size)).playSoundEffect(worldObj, 300, 1, "railgun");
		}
	}
	
	public boolean checkAmmo(int type)
	{
		int r = 1+(int)size/2;
		
		int[][] sides = {{0,r},{0,-r},{-r,0},{r,0}};
		
		for(int i = 0; i<sides.length; i++)
		{
			Vector3 vec = (new Vector3(this)).add(new Vector3(sides[i][0],-1,sides[i][1]));
			TileEntity te = vec.getTileEntity(worldObj);
			if(te!=null&&te instanceof IInventory)
			{
				IInventory inv = (IInventory)te;
				for(int j = 0; j<inv.getSizeInventory();j++)
				{
					ItemStack stack = inv.getStackInSlot(j);
					if(stack!=null&&stack.itemID==Item.ingotIron.itemID)
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public void consumeAmmo()
	{
		int r = 1+(int)size/2;
		
		int[][] sides = {{0,r},{0,-r},{-r,0},{r,0}};
		
		for(int i = 0; i<sides.length; i++)
		{
			Vector3 vec = (new Vector3(this)).add(new Vector3(sides[i][0],-1,sides[i][1]));
			TileEntity te = vec.getTileEntity(worldObj);
			if(te!=null&&te instanceof IInventory)
			{
				IInventory inv = (IInventory)te;
				for(int j = 0; j<inv.getSizeInventory();j++)
				{
					ItemStack stack = inv.getStackInSlot(j);
					if(stack!=null&&stack.itemID==Item.ingotIron.itemID)
					{
						inv.decrStackSize(j, 1);
					}
				}
			}
		}
	}
	
	
	public boolean checkEnergy(double energy)
	{
		boolean ret = false;
		int r = 1+(int)size/2;
		
		int[][] sides = {{0,r},{0,-r},{-r,0},{r,0}};
		
		for(int i = 0; i<sides.length; i++)
		{
			Vector3 vec = (new Vector3(this)).add(new Vector3(sides[i][0],-1,sides[i][1]));
			TileEntity te = vec.getTileEntity(worldObj);
			if(te!=null&&te instanceof IElectricityStorage)
			{
				IElectricityStorage inv = (IElectricityStorage)te;
				if(inv.getJoules()>=energy)
				{
					return true;
				}
			}
		}
		return ret;
	}
	
	public void consumeEnergy(double energy)
	{
		boolean ret = false;
		int r = 1+(int)size/2;
		
		int[][] sides = {{0,r},{0,-r},{-r,0},{r,0}};
		
		for(int i = 0; i<sides.length; i++)
		{
			Vector3 vec = (new Vector3(this)).add(new Vector3(sides[i][0],-1,sides[i][1]));
			TileEntity te = vec.getTileEntity(worldObj);
			if(te!=null&&te instanceof IElectricityStorage)
			{
				IElectricityStorage inv = (IElectricityStorage)te;
				if(inv.getJoules()>=energy)
				{
					inv.setJoules(inv.getJoules() - energy);
				}
			}
		}
	}
		
	public void rotationAngles()
	{
		this.rotationPitch = pitch = (float) Math.toDegrees((turretDir.toSpherical()).y);
		this.rotationYaw = yaw = (float) (Math.toDegrees((turretDir.toSpherical()).z));
	}
	public void fakeRotationAngles()
	{
		pitch = (float) Math.toDegrees((turretDir.toSpherical()).y);
		yaw = (float) (Math.toDegrees((turretDir.toSpherical()).z));
	}
	public void setAngles(float pitch, float yaw)
	{
		this.rotationPitch = pitch;
		this.rotationYaw = yaw;
		this.pitch = pitch;
		this.yaw = yaw;
	}
	
	@Override
	public void writeSpawnData(ByteArrayDataOutput data) 
	{
		data.writeFloat(pitch);
		data.writeFloat(yaw);
		data.writeInt(size);
	}

	@Override
	public void readSpawnData(ByteArrayDataInput data) {
		pitch = data.readFloat();
		yaw = data.readFloat();
		size = data.readInt();
	}

	@Override
	protected void entityInit() 
	{
		super.entityInit();
		this.dataWatcher.addObject(31, Integer.valueOf(0));
	}


	@Override
	public int getMaxHealth() 
	{
		return 20;
	}


	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		mass = nbt.getFloat("mass");
		size = nbt.getInteger("size")==0?5:nbt.getInteger("size");
		turretDir = turretDir.readFromNBT(nbt, "turretDir");
	//	System.out.println("read: "+turretDir.toString());
		rotationAngles();
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setInteger("size", size);
		nbt.setFloat("mass", mass);
		turretDir.writeToNBT(nbt, "turretDir");
	//	System.out.println("wrote: "+turretDir.toString());
	}

    /**
     * Returns true if other Entities should be prevented from moving through this Entity.
     */
    public boolean canBeCollidedWith()
    {
        return true;
    }

    /**
     * Returns true if this entity should push and be pushed by other entities when colliding.
     */
    public boolean canBePushed()
    {
        return true;
    }
    
    /**
     * Returns the Y offset from the entity's position for any entity riding this one.
     */
    public double getMountedYOffset()
    {
        return (double)this.height+1.5;
    }
    
    public void printThis(EntityPlayer player)
    {
    	player.addChatMessage(Double.toString(posX)+" "+Double.toString(posY)+" "+Double.toString(posZ));
    }
    
    /**
     * Called when a player interacts with a mob. e.g. gets milk from a cow, gets into the saddle on a pig.
     */
    public boolean interact(EntityPlayer player)
    {
    	if(worldObj.isRemote)
    	{
    		player.addChatMessage(Integer.toString(size));
    		printThis(player);
    	}
    	ItemStack item = player.getHeldItem();
    	if(item!=null&&item.getItem() instanceof ItemAxe)
    	{
    		setDead();
    		return true;
    	}
    	if(item!=null&&item.getItem() instanceof ItemBook)
    	{
    		size = size==5?3:size==3?1:5;
    		setSize(size);
    		return true;
    	}
    	
    	if(!this.worldObj.isRemote&&size==5)
    	{
	        if (this.riddenByEntity != null && this.riddenByEntity instanceof EntityPlayer && this.riddenByEntity != player)
	        {
	            return true;
	        }
	        else
	        {
	            if (!this.worldObj.isRemote&&mountTime>10)
	            {
	                player.mountEntity(this);
	                mountTime = 0;
	            }
	
	            return true;
	        }
    	}
        
        return false;
    }

    public boolean shouldRenderInPass(int pass)
    {
        return pass == 1;
    }
    
    /**
     * Applies a velocity to each of the entities pushing them away from each other. Args: entity
     */
    public void applyEntityCollision(Entity entity)
    {
    	for(String key: boxes.keySet())
    	{
    		Matrix3 box = boxes.get(key);
    		Vector3 offset = new Vector3();
    		if(offsets.containsKey(key))
    		{
    			offset = offsets.get(key);
    		}
    		if(box!=null)
    		{
				boolean t = box.pushOutOfBox(this, entity, offset);
				if(t)
				System.out.println(this+" "+box.toString());
    		}
    	}
    }
    
    
    private void checkCollision()
    {
    	setSize(size);
        List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, AxisAlignedBB.getBoundingBox(posX - (size+5), posY, posZ - (size+5), posX+(size+5), posY + 2*size, posZ + (size+5)));

    	setOffsets();
        if (list != null && !list.isEmpty())
        {
        	if(list.size() == 1 && this.riddenByEntity!=null)
        	{
        		return;
        	}
        	
            for (int i = 0; i < list.size(); ++i)
            {
                Entity entity = (Entity)list.get(i);
                if(entity!=this.riddenByEntity)
                	applyEntityCollision(entity);
            }
        }
    }
    
    
	public boolean checkBlocks(double dir)
	{
		boolean ret = true;
		Vector3 thisloc = new Vector3(this);
		thisloc = thisloc.add(new Vector3(0,dir,0));
		for(int i = -2; i<=2;i++)
			for(int j = -2;j<=-2;j++)
			{
				ret = ret && thisloc.getBlockId(worldObj)==0;
			}
		return ret;
	}
    
    public void setDead()
    {

		System.out.println("dead");
    	super.setDead();
    }
    
    
    protected void setSize(double size)
    {
    	super.setSize((float)size/2, (float)size/2);
    	setBoxes();
    }
    
	@Override
	public String getTranslatedEntityName()
	{
		return "railgun";
	}
    /**
     * Gets the username of the entity.
     */
    public String getEntityName()
    {
    	return "railgun";
    }

	@Override
	public void setBoxes() {
    	boxes.clear();
    	//Base Cuboid 
    	boxes.put("base",new Matrix3(new Vector3(-size/2,0,-size/2), new Vector3(size/2,size*0.2,size/2)));
    	//*
    	//Turret Cylinder
    	boxes.put("mid0",new Matrix3(new Vector3(-size/2,0,-size/4), new Vector3(size/2,size*0.6,size/4), new Vector3(0,yaw)));
    	boxes.put("mid1",new Matrix3(new Vector3(-size/2,0,-size/4), new Vector3(size/2,size*0.6,size/4), new Vector3(0,45+yaw)));
    	boxes.put("mid2",new Matrix3(new Vector3(-size/2,0,-size/4), new Vector3(size/2,size*0.6,size/4), new Vector3(0,-45+yaw)));
    	boxes.put("mid3",new Matrix3(new Vector3(-size/2,0,-size/4), new Vector3(size/2,size*0.6,size/4), new Vector3(0,-90+yaw)));
    	
    	boxes.put("mid4",new Matrix3(new Vector3(-size/2.1,0,-size/7), new Vector3(size/2.1,size*0.8,size/7), new Vector3(0,-90+yaw)));

    	//*
    	//Turret top
    	boxes.put("top0",new Matrix3(new Vector3(-size/4,0,-size/4), new Vector3(size/4,size*1.2,size/4)));
    	boxes.put("top1",new Matrix3(new Vector3(-size/3,0,-size/3), new Vector3(size/3,size*1.1,size/3)));
    	//*/
    	
    	boxes.put("turret", new Matrix3(new Vector3(0,0,-size/8), new Vector3(size*0.8,size*0.1,size/8), new Vector3(pitch,yaw)));
    	
	}

	@Override
	public void setOffsets() 
	{
    	offsets.clear();
		offsets.put("turret", new Vector3(0,3.6,0));
	}

	@Override
	public ConcurrentHashMap<String, Matrix3> getBoxes() 
	{
		return boxes;
	}

	@Override
	public void addBox(String name, Matrix3 box) 
	{
		boxes.put(name, box);
	}

	@Override
	public ConcurrentHashMap<String, Vector3> getOffsets()
	{
		return offsets;
	}

	@Override
	public void addOffset(String name, Vector3 offset) 
	{
		offsets.put(name, offset);
	}

	@Override
	public Matrix3 bounds(Vector3 target) {
		return new Matrix3(new Vector3(-size/2,0, -size/2), new Vector3(size/2, size, size/2));
	}

    /**
     * Returns true if the entity's rider (EntityPlayer) should face forward when mounted.
     * currently only used in vanilla code by pigs.
     *
     * @param player The player who is riding the entity.
     * @return If the player should orient the same direction as this entity.
     */
    public boolean shouldRiderFaceForward(EntityPlayer player)
    {
        return true;
    }
}
