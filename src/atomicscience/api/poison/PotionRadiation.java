package atomicscience.api.poison;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import universalelectricity.prefab.potion.CustomPotion;
import atomicscience.api.AtomicScience;

public class PotionRadiation extends CustomPotion
{
	public static final PotionRadiation INSTANCE;

	static
	{
		AtomicScience.CONFIGURATION.load();
		INSTANCE = new PotionRadiation(21, true, 5149489, "radiation");
		AtomicScience.CONFIGURATION.save();
	}

	public PotionRadiation(int id, boolean isBadEffect, int color, String name)
	{
		super(AtomicScience.CONFIGURATION.get("Potion", name + " potion ID", id).getInt(id), isBadEffect, color, name);
		this.setIconIndex(6, 0);
	}

	@Override
	public void performEffect(EntityLivingBase par1EntityLivingBase, int amplifier)
	{
		if (par1EntityLivingBase.worldObj.rand.nextFloat() > 0.9 - (amplifier * 0.08))
		{
			par1EntityLivingBase.attackEntityFrom(PoisonRadiation.damageSource, 1);

			if (par1EntityLivingBase instanceof EntityPlayer)
			{
				((EntityPlayer) par1EntityLivingBase).addExhaustion(0.010F * (amplifier + 1));
			}
		}

	}

	@Override
	public boolean isReady(int duration, int amplifier)
	{
		if (duration % 10 == 0)
		{
			return true;
		}

		return false;
	}
}
