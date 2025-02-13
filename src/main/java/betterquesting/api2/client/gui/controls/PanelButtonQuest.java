package betterquesting.api2.client.gui.controls;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.enums.EnumQuestState;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api.storage.BQ_Settings;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.client.gui.misc.GuiRectangle;
import betterquesting.api2.client.gui.resources.colors.IGuiColor;
import betterquesting.api2.client.gui.resources.textures.GuiTextureColored;
import betterquesting.api2.client.gui.resources.textures.IGuiTexture;
import betterquesting.api2.client.gui.resources.textures.OreDictTexture;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.questing.QuestDatabase;
import betterquesting.storage.QuestSettings;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PanelButtonQuest extends PanelButtonStorage<Map.Entry<UUID, IQuest>>
{
    public final GuiRectangle rect;
    public final EntityPlayer player;
	public final IGuiTexture txFrame;

	public PanelButtonQuest(GuiRectangle rect, int id, String txt, Map.Entry<UUID, IQuest> value)
    {
		super(rect, id, txt, value);
        this.rect = rect;
        
        player = Minecraft.getMinecraft().thePlayer;
        EnumQuestState qState = value == null ? EnumQuestState.LOCKED : value.getValue().getState(player);
        IGuiColor txIconCol = null;
        boolean main = value == null ? false : value.getValue().getProperty(NativeProps.MAIN);
        boolean lock = false;
        
        switch(qState)
        {
            case LOCKED:
				txFrame = main ? PresetTexture.QUEST_MAIN_0.getTexture() : PresetTexture.QUEST_NORM_0.getTexture();
                txIconCol = PresetColor.QUEST_ICON_LOCKED.getColor();
                lock = true;
                break;
            case UNLOCKED:
				txFrame = main ? PresetTexture.QUEST_MAIN_1.getTexture() : PresetTexture.QUEST_NORM_1.getTexture();
                txIconCol = PresetColor.QUEST_ICON_UNLOCKED.getColor();
                break;
            case UNCLAIMED:
				txFrame = main ? PresetTexture.QUEST_MAIN_2.getTexture() : PresetTexture.QUEST_NORM_2.getTexture();
                txIconCol = PresetColor.QUEST_ICON_PENDING.getColor();
                break;
            case COMPLETED:
				txFrame = main ? PresetTexture.QUEST_MAIN_3.getTexture() : PresetTexture.QUEST_NORM_3.getTexture();
                txIconCol = PresetColor.QUEST_ICON_COMPLETE.getColor();
                break;
			case REPEATABLE:
				txFrame = main ? PresetTexture.QUEST_MAIN_4.getTexture() : PresetTexture.QUEST_NORM_4.getTexture();
				txIconCol = PresetColor.QUEST_ICON_REPEATABLE.getColor();
				break;
			default:
				txFrame = null;
        }

		IGuiTexture btnTx = new GuiTextureColored(txFrame, txIconCol);
        setTextures(btnTx, btnTx, btnTx);
        setIcon(new OreDictTexture(1F, value == null ? new BigItemStack(Items.nether_star) : value.getValue().getProperty(NativeProps.ICON), false, true), 4);
        //setTooltip(value == null ? Collections.emptyList() : value.getValue().getTooltip(player));
        setActive(QuestingAPI.getAPI(ApiReference.SETTINGS).canUserEdit(player) || !lock || BQ_Settings.viewMode);
    }

    @Override
    public List<String> getTooltip(int mx, int my)
    {
        if(!this.getTransform().contains(mx, my)) return null;

        Map.Entry<UUID, IQuest> value = this.getStoredValue();
        return value == null ? Collections.emptyList() : getQuestTooltip(value.getValue(), player, value.getKey());
    }

    private List<String> getQuestTooltip(IQuest quest, EntityPlayer player, UUID qID)
    {
		List<String> tooltip = getStandardTooltip(quest, player, qID);

		if(Minecraft.getMinecraft().gameSettings.advancedItemTooltips && QuestSettings.INSTANCE.getProperty(NativeProps.EDIT_MODE))
		{
			tooltip.add("");
			tooltip.addAll(this.getAdvancedTooltip(quest, player, qID));
		}

		return tooltip;
    }

    private List<String> getStandardTooltip(IQuest quest, EntityPlayer player, UUID qID)
    {
		List<String> list = new ArrayList<>();

		list.add(QuestTranslation.translateQuestName(qID, quest));

		UUID playerID = QuestingAPI.getQuestingUUID(player);

		if(quest.isComplete(playerID))
		{
			list.add(ChatFormatting.GREEN + QuestTranslation.translate("betterquesting.tooltip.complete"));

			if (quest.canClaimBasically(player)) {
				list.add(ChatFormatting.GRAY + QuestTranslation.translate("betterquesting.tooltip.rewards_pending"));
			} else if (!quest.hasClaimed(playerID)) {
				list.add(ChatFormatting.GRAY + QuestTranslation.translate("betterquesting.tooltip.repeatable"));
			} else if(quest.getProperty(NativeProps.REPEAT_TIME) > 0)
			{
				long time = getRepeatSeconds(quest, player);
				DecimalFormat df = new DecimalFormat("00");
				String timeTxt = "";
				if (time < 0){
					timeTxt += "-";
					time = time * -1;
				}

				if(time >= 3600)
				{
					timeTxt += (time/3600) + "h " + df.format((time%3600)/60) + "m ";
				} else if(time >= 60)
				{
					timeTxt += (time/60) + "m ";
				}

				timeTxt += df.format(time%60) + "s";

				list.add(ChatFormatting.GRAY + QuestTranslation.translate("betterquesting.tooltip.repeat", timeTxt));
				if(QuestSettings.INSTANCE.getProperty(NativeProps.EDIT_MODE)){
					list.add(ChatFormatting.RED + QuestTranslation.translate("betterquesting.tooltip.repeat_with_edit_mode"));
				}
			}
		} else if(!quest.isUnlocked(playerID))
		{
			list.add(ChatFormatting.RED + "" + ChatFormatting.UNDERLINE + QuestTranslation.translate("betterquesting.tooltip.requires") + " (" + quest.getProperty(NativeProps.LOGIC_QUEST).toString().toUpperCase() + ")");

            // TODO: Make this lookup unnecessary
            QuestDatabase.INSTANCE.filterKeys(quest.getRequirements()).entrySet().stream()
                    .filter(entry -> !entry.getValue().isComplete(playerID))
                    .forEach(entry -> list.add(ChatFormatting.RED + "- " + QuestTranslation.translateQuestName(entry)));
		} else
		{
			int n = 0;

			for(DBEntry<ITask> task : quest.getTasks().getEntries())
			{
				if(task.getValue().isComplete(playerID))
				{
					n++;
				}
			}

			list.add(ChatFormatting.GRAY + QuestTranslation.translate("betterquesting.tooltip.tasks_complete", n, quest.getTasks().size()));
		}

		return list;
    }

    private List<String> getAdvancedTooltip(IQuest quest, EntityPlayer player, UUID qID)
    {
		List<String> list = new ArrayList<>();

		list.add(ChatFormatting.GRAY + QuestTranslation.translate("betterquesting.tooltip.global_quest", quest.getProperty(NativeProps.GLOBAL)));
		if(quest.getProperty(NativeProps.GLOBAL))
		{
			list.add(ChatFormatting.GRAY + QuestTranslation.translate("betterquesting.tooltip.global_share", quest.getProperty(NativeProps.GLOBAL_SHARE)));
		}
		list.add(ChatFormatting.GRAY + QuestTranslation.translate("betterquesting.tooltip.quest_logic", quest.getProperty(NativeProps.LOGIC_QUEST).toString().toUpperCase()));
		list.add(ChatFormatting.GRAY + QuestTranslation.translate("betterquesting.tooltip.simultaneous", quest.getProperty(NativeProps.SIMULTANEOUS)));
		list.add(ChatFormatting.GRAY + QuestTranslation.translate("betterquesting.tooltip.auto_claim", quest.getProperty(NativeProps.AUTO_CLAIM)));
		if(quest.getProperty(NativeProps.REPEAT_TIME).intValue() >= 0)
		{
			long time = quest.getProperty(NativeProps.REPEAT_TIME)/20;
			DecimalFormat df = new DecimalFormat("00");
			String timeTxt = "";

			if(time >= 3600)
			{
				timeTxt += (time/3600) + "h " + df.format((time%3600)/60) + "m ";
			} else if(time >= 60)
			{
				timeTxt += (time/60) + "m ";
			}

			timeTxt += df.format(time%60) + "s";

			list.add(ChatFormatting.GRAY + QuestTranslation.translate("betterquesting.tooltip.repeat", timeTxt));
		} else
		{
			list.add(ChatFormatting.GRAY + QuestTranslation.translate("betterquesting.tooltip.repeat", false));
		}

		return list;
    }

	private long getRepeatSeconds(IQuest quest, EntityPlayer player)
	{
		if(quest.getProperty(NativeProps.REPEAT_TIME) < 0) return -1;

        NBTTagCompound ue = quest.getCompletionInfo(QuestingAPI.getQuestingUUID(player));
        if(ue == null) return 0;

        return ((quest.getProperty(NativeProps.REPEAT_TIME) * 50L) - (System.currentTimeMillis() - ue.getLong("timestamp"))) / 1000L;
	}
}
