package strathclyde.agent.emma;

import javax.websocket.Session;

import edu.usc.ict.iago.utils.GameSpec;

public class STRATH_IAGO_VH extends IAGOCoreVH {
		
	public STRATH_IAGO_VH(String name, GameSpec game, Session session)
	{
		super("Strathclyde", game, session, new STRATH_IAGO_Behavior(), new STRATH_IAGO_Expression(), 
				new STRATH_IAGO_Message());	
	}

	@Override
	public String getArtName()
	{
		return "Laura";
	}
	
	@Override
	public String agentDescription()
	{
		return "<h1>Emma</h1><p>The sooner it's done, the better for both of us!</p>";
	}
	
}