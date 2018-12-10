package strathclyde.agent.emma;

import edu.usc.ict.iago.utils.Event;
import edu.usc.ict.iago.utils.Event.EventClass;
import edu.usc.ict.iago.utils.ExpressionPolicy;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;

@SuppressWarnings("unused")
public class STRATH_IAGO_Expression extends IAGOCoreExpression implements ExpressionPolicy 
{

	protected String getSemiFairEmotion()
	{
		return "neutral";
	}
	
	protected String getFairEmotion()
	{
		return "happy";
	}
	
	protected String getUnfairEmotion()
	{
		return "disgusted";
	}

	@Override
	public String getExpression(History history) 
	{
		Event last = history.getPlayerHistory().getLast();
	
		if(last.getType().equals(Event.EventClass.SEND_EXPRESSION)){
			if(last.getMessage().equals("sad"))
				return "disgusted";
			if(last.getMessage().equals("happy"))
				return "happy";
			if(last.getMessage().equals("surprised"))
				return "neutral";
			if(last.getMessage().equals("angry"))
				return "angry";
		} 
		else if (last.getType().equals(Event.EventClass.SEND_MESSAGE))
		{
			if(last.getMessageCode() > -1)
			{
				switch(last.getMessageCode())
				{
				case 0: //important both happy
				case 2: //split evenly
				case 3: //get most valuable item
				case 9: //benefits both
				case 11://make an offer
					return "happy";
				case 1: //I gave, you give
				case 6: //best offer possible
				case 8: //can't go lower
				case 10://no time!
					return "angry";
				case 4: //accept or consequences
				case 5: //last offer
				case 7: //offer sucks
					return "disgusted";
				default:
					return "neutral";
				}
			}
		}

		return "neutral";
		
	}

}
