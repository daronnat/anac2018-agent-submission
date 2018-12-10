package strathclyde.agent.emma;

import java.util.ArrayList;

import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.MessagePolicy;
import edu.usc.ict.iago.utils.Preference;

public abstract class IAGOCoreMessage implements MessagePolicy
{
	public abstract void updateOrderings (ArrayList<ArrayList<Integer>> orderings);
	
	protected abstract String getEndOfTimeResponse();
	
	protected abstract String getSemiFairResponse();
	
	protected abstract String getContradictionResponse(String drop);
	
	protected abstract void setUtils(AgentUtilsExtension utils);
	
	protected static String prefToEnglish(Preference preference, GameSpec game)
	{
		// very evasive answer to prevent the player from asking more question and encourage them to send offers
		String ans = "";
		ans += "Yeah, I like ";
		
		if (preference.getIssue1() >= 0)
			ans += game.getIssuePluralNames()[preference.getIssue1()] + " too. ";
		else
			ans += " this thing too. ";
		ans += "But let's not waste time, send me an offer!";
		
		return ans;
	}
}
