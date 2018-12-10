package strathclyde.agent.emma;

import java.util.ArrayList;
import edu.usc.ict.iago.utils.Event;
import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.MessagePolicy;
import edu.usc.ict.iago.utils.Preference;
import edu.usc.ict.iago.utils.Preference.Relation;
import edu.usc.ict.iago.utils.ServletUtils;

public class STRATH_IAGO_Message extends IAGOCoreMessage implements MessagePolicy {
	protected final String[] proposal = {"I think this deal is good for the both of us.", 
						 			   "I think this offer is the fairest one.", 
						 			   "You need these points more than I do, please accept this one.", 
						 			   "I think this deal will interest you, it's the fairest I can do.",
									   "Please consider this deal, it's the fairest."};	
	
	protected final String[] acceptResponse = {
											 "Great!",
											 "Wonderful!",
											 "I'm glad we could come to an agreement!",
											 "Sounds good!"};
	protected final String[] rejectResponse = {
											 "Oh that's too bad. But let's not waste time now",
											 "Ok, but hurry up.",
											 "Ok, let's try something different.",
											 "Alright."};
	
	protected final String[] vhReject = {
			 "I hope you're kidding with this offer.",
			 "Sorry, I won't be able to accept that.",
			 "I can't accept that, let's try with another offer.",
			 "This deal is really unfair to me."
			 };
	
	protected final String[] vhAccept = {
			 "Your offer is good!",
			 "This one seems fair to me.",
			 "That will work for me.",
			 "Okey, this deal is acceptable."
			 };

	protected void setUtils(AgentUtilsExtension utils)
	{
	}
		
	public void updateOrderings (ArrayList<ArrayList<Integer>> orderings)
	{
	}
		
	public String getProposalLang(History history, GameSpec game){
		return proposal[(int)(Math.random()*proposal.length)];
	}
	
	public String getAcceptLang(History history, GameSpec game){
		return acceptResponse[(int)(Math.random()*acceptResponse.length)];
	}
	
	public String getRejectLang(History history, GameSpec game){
		return rejectResponse[(int)(Math.random()*rejectResponse.length)];
	}
	
	public String getVHAcceptLang(History history, GameSpec game){
		return vhAccept[(int)(Math.random()*vhAccept.length)];
	}
	
	public String getVHRejectLang(History history, GameSpec game){
		return vhReject[(int)(Math.random()*vhReject.length)];
	}
		
	private String getEmotionResponse(History history, GameSpec game) {
		Event e = history.getPlayerHistory().getLast();
		
		if (e.getType() != Event.EventClass.SEND_EXPRESSION)
			throw new UnsupportedOperationException("The last event wasn't an expresion--this method is inappropriate.");
		
		if(e.getMessage().equals("sad") || e.getMessage().equals("angry"))
			return "Calm down and send me an offer instead.";
		else if(e.getMessage().equals("happy"))
			return "I'm glad you're happy! But let's get down to business now.";
		else if(e.getMessage().equals("surprised"))
			return "Keep your head in the game now.";
		return "What?";
	}
	
	protected String getEndOfTimeResponse() {
		return "We're almost out of time, accept this offer or we both lose!";
	}

	protected String getSemiFairResponse() {
		return "Unfortunately, I cannot accept.  But that's getting close to being fair.";
	}
	
	protected String getContradictionResponse(String drop) {
		return "Your preferences are confusing to me.";
	}

	public String getMessageResponse(History history, GameSpec game) {
		Event ePrime = history.getPlayerHistory().getLast();
		
		if (ePrime.getType() == Event.EventClass.TIME)
		{
			if (Integer.parseInt(ePrime.getMessage()) >= game.getTotalTime()/1.5)
			{
				
				return "This round is nearly over, hurry up and accept or send a FULL offer!";
			}
			else if(Integer.parseInt(ePrime.getMessage()) >= game.getTotalTime()/2)
			{
				return "We are getting close to the end of the round, we should settle for something quickly!";
			}
			else
			{
				return "Time is running out fast, you should accept or send me a FULL offer as soon as possible";
			}
		}
		
		if (ePrime.getType() == Event.EventClass.SEND_EXPRESSION)
			return getEmotionResponse(history, game);

		Preference p = ePrime.getPreference();
		if (p != null)
		{
			Relation myRelation = Relation.EQUAL;
			
			return prefToEnglish(new Preference(p.getIssue1(), p.getIssue2(), myRelation, false), game);
		}
		ServletUtils.log("No preference detected in user message.", ServletUtils.DebugLevels.DEBUG);

		//details for each response
		int code = history.getPlayerHistory().getLast().getMessageCode();
		if (code == -1)
		{
			ServletUtils.log("MessageCode missing!", ServletUtils.DebugLevels.WARN);
		}

		String resp = "Anyway, we shouldn't spend too much time chatting, send me a complete offer instead!";

		return resp;
		
	}

}