package strathclyde.agent.emma;

import java.util.LinkedList;

import javax.websocket.Session;

import edu.usc.ict.iago.utils.Event;
import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.GeneralVH;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;
import edu.usc.ict.iago.utils.Preference;
import edu.usc.ict.iago.utils.ServletUtils;

public abstract class IAGOCoreVH extends GeneralVH
{
	
	private Offer lastOfferReceived;
	private Offer lastOfferSent;
	private IAGOCoreBehavior behavior;
	private IAGOCoreExpression expression;
	private IAGOCoreMessage messages;
	private AgentUtilsExtension utils;
	private boolean timeFlag = false;
	private boolean firstFlag = false;
	private int noResponse = 0;
	private boolean firstGame = true;
	// keep track of the current scenario numbers
	public static int scenario_nb = 1;
	// margin used in sent offers
	public static double margin = 1;
	// keep the number of the previous scenario
	public static int prev_scenario_nb = 1;
	// keep the duration of the last round
	public static int prev_time = 0;
	// keep track of the current round
	public static int current_round = 0;
	// set the time before the agent send its first offer
	public static int time_vhoffer = 0;
	// check if last round was too long
	public boolean long_round = false;
	// check if an offer is in progress
	public boolean is_offer_progress = false;
	// used to keep track of the time
	public int track_time = 0;
	
	public IAGOCoreVH(String name, GameSpec game, Session session, IAGOCoreBehavior behavior,
			IAGOCoreExpression expression, IAGOCoreMessage messages)
	{
		super(name, game, session);

		AgentUtilsExtension aue = new AgentUtilsExtension();
		aue.configureGame(game);
		this.utils = aue;
		this.expression = expression;
		this.messages = messages;
		this.behavior = behavior;
		
		this.messages.setUtils(utils);
		this.behavior.setUtils(utils);
		
	}
	
	@Override
	public LinkedList<Event> updateHistory(Event e)
	{
		LinkedList<Event> resp = new LinkedList<Event>();
		
		/**what to do when the game has changed -- this is only necessary because our AUE needs to be updated.
			Game, the current GameSpec from our superclass has been automatically changed!
			IMPORTANT: between GAME_END and GAME_START, the gameSpec stored in the superclass is undefined.
			Furthermore, attempting to access data that is decipherable with a previous gameSpec could lead to exceptions!
			For example, attempting to decipher an offer from Game 1 while in Game 2 could be a problem (what if Game 1 had 4 issues, but Game 2 only has 3?)
			You should always treat the current GameSpec as true (following a GAME_START) and store any useful metadata about past games yourself.
		**/
		
		if(e.getType().equals(Event.EventClass.GAME_START))
		{		
			ServletUtils.log("Game has changed... reconfiguring!", ServletUtils.DebugLevels.DEBUG);
			AgentUtilsExtension aue = new AgentUtilsExtension();
			aue.configureGame(game);
			this.utils = aue;
			this.messages.setUtils(utils);
			this.behavior.setUtils(utils);		

			// reset some things
			timeFlag = false;
			firstFlag = false;
			noResponse = 0;
			// reset newly added variables
			scenario_nb = 1;
			is_offer_progress = false;
			track_time = 0;
			// we keep track of the current round
			current_round += 1;
			// used to delay the time when the agent send its first offer, as long as it's not in the last 40 seconds of the game
			if (time_vhoffer < game.getTotalTime()-40)
			{
				time_vhoffer += 20;
			}
			
			if (current_round == 1)
			{
				margin = 1.1;
			}
			
			else if (current_round == 2)
			{
				margin = 1.2;
			}
			
			else if (current_round >= 3)
			{
				margin = 1.3;
			}

			// first welcome message, when no round has been played so far
			if(firstGame)
			{
				Event e0 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, "First time huh? Let's settle this quickly, send me an offer!", 2500);
				resp.add(e0);
				firstGame = false;
				return resp;
			}
			
			// set the welcome message for each new rounds depending on how it went during the last one
			if(!firstGame)
			{
				
				if (prev_time <= game.getTotalTime()/4)
				{
					Event e1 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, "Last round was amazing, let's finish this one as efficiently!", 1000);
					prev_scenario_nb = 1;
					resp.add(e1);
				}
				
				else if (prev_time <= game.getTotalTime()/2)
				{
					Event e2 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, "It's nice negotiating again with you, but let's be even faster this time!", 1000);
					resp.add(e2);
					prev_scenario_nb = 1;
				}
				
				else if (prev_time <= game.getTotalTime()/1.3)
				{
					Event e3 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, "Back at it again, hopefully this time we'll settle on something quicker.", 1000);
					resp.add(e3);
					prev_scenario_nb = 1;
				}
				
				else if (prev_time >= game.getTotalTime()/1.3)
				{
					Event e4 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, "Last round was way too long, we have to settle on something quicker this time!", 0);
					resp.add(e4);
					prev_scenario_nb = 0;
				}					
				return resp;
			}
			firstGame = false;
		}
		
		//detect when an offer is in progress
		if(e.getType().equals(Event.EventClass.OFFER_IN_PROGRESS))
		{
			is_offer_progress = true;
		}
		
		//should we lead with an offer?
		if(!firstFlag)
		{
			firstFlag = true;
			Event e2 = new Event(History.VH_ID, Event.EventClass.SEND_OFFER, behavior.getFirstOffer(getHistory()));
			if(e2.getOffer() != null)
			{
				Event e3 = new Event(History.VH_ID, Event.EventClass.OFFER_IN_PROGRESS, 0);
				resp.add(e3);
				Event e4 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getProposalLang(getHistory(), game), 1000);
				resp.add(e4);
				resp.add(e2);
			}
		}
		
		//what to do when player sends an expression -- react to it with text and our own expression
		if(e.getType().equals(Event.EventClass.SEND_EXPRESSION))
		{
			String expr = expression.getExpression(getHistory());
			Event e1 = new Event(History.VH_ID, Event.EventClass.SEND_EXPRESSION, expr, 2000, 0);
			resp.add(e1);
			
			Event e0 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getMessageResponse(getHistory(), game), 0);
			resp.add(e0);
			return resp;
		}
		
		//when to formally accept when player send an incoming formal acceptance
		if(e.getType().equals(Event.EventClass.FORMAL_ACCEPT))
		{
			Event lastOffer = utils.lastEvent(getHistory().getHistory(), Event.EventClass.SEND_OFFER);
			Event lastTime = utils.lastEvent(getHistory().getHistory(), Event.EventClass.TIME);
			
			int totalIssues = 0;
			for (int i = 0; i < game.getNumIssues(); i++)
				totalIssues += game.getIssueQuants()[i];
			if(lastOffer != null && lastTime != null)
			{
				//approximation based on distributive case
				int fairSplit = ((game.getNumIssues() + 1) * totalIssues / 4);
				//down to the wire, accept anything better than batna
				if(utils.myActualOfferValue(lastOffer.getOffer()) > game.getVHBATNA() && Integer.parseInt(lastTime.getMessage()) + 30 > game.getTotalTime()) 
				{
					Event e0 = new Event(History.VH_ID, Event.EventClass.FORMAL_ACCEPT, 0);
					resp.add(e0);
					return resp;
				}
				//accept anything better than fair minus margin
				if(utils.myActualOfferValue(lastOffer.getOffer()) > fairSplit - behavior.getAcceptMargin())
				{
					Event e0 = new Event(History.VH_ID, Event.EventClass.FORMAL_ACCEPT, 0);
					resp.add(e0);
					return resp;
				}
				else
				{
					Event e1 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getRejectLang(getHistory(), game), 0);
					resp.add(e1);
					return resp;					
				}
			}
		}
		
		

		
		//what to do with delays on the part of the other player
		if(e.getType().equals(Event.EventClass.TIME))
		{
			track_time=Integer.parseInt(e.getMessage());
			prev_time = Integer.parseInt(e.getMessage());
			
			noResponse += 1;
			for(int i = getHistory().getHistory().size() - 1 ; i > 0 && i > getHistory().getHistory().size() - 4; i--)//if something from anyone for two time intervals
			{
				if(getHistory().getHistory().get(i).getType() != Event.EventClass.TIME)
					noResponse = 0;
			}
						
			if(noResponse >= 2)
			{
				Event e1 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getMessageResponse(getHistory(), game), 0);
				resp.add(e1);
			}

			// first offer issued by the agent based on time if no offer has been previously issued by the participant
			if(!is_offer_progress && Integer.parseInt(e.getMessage()) == time_vhoffer)
			{
				timeFlag = true;
				Event e1 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getProposalLang(getHistory(), game), 0);
				resp.add(e1);
				Event e2 = new Event(History.VH_ID, Event.EventClass.SEND_OFFER, behavior.getTimingOffer(getHistory()));
				if(e2.getOffer() != null)
					resp.add(e2);			
	
			}
			
			// offer sent when timeout
			if(!timeFlag && game.getTotalTime() - Integer.parseInt(e.getMessage()) < 30)
			{
				timeFlag = true;
				Event e1 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getEndOfTimeResponse(), 0);
				resp.add(e1);
				Event e2 = new Event(History.VH_ID, Event.EventClass.SEND_OFFER, behavior.getFinalOffer(getHistory()));
				if(e2.getOffer() != null)
					resp.add(e2);
				
			}

			if (Integer.parseInt(e.getMessage()) >= game.getTotalTime()/2)
			{
				timeFlag = true;
				scenario_nb=2;
			}

			return resp;
		}
		
		//what to do when the player sends an offer
		if(e.getType().equals(Event.EventClass.SEND_OFFER))
		{				
			Offer o = e.getOffer();//incoming offer
			this.lastOfferReceived = o;
		
			// get the highest score possible for this round
			int ct = 0;
			int total_val = 0;
			while(ct < game.getNumIssues()) 
			{
				total_val += game.getIssueQuants()[ct]*game.getSimpleVHPoints().get(game.getIssuePluralNames()[ct]);
				ct+=1;
			}
			
			// set the threshold when to accept lower offers
			double time_limit = 1.5;

			if (track_time < game.getTotalTime()/time_limit)
			{
				if (utils.myActualOfferValue(o) > (total_val/2)*margin)
				{
					Event eExpr = new Event(History.VH_ID, Event.EventClass.SEND_EXPRESSION, expression.getFairEmotion(), 2000, 0);
					resp.add(eExpr);
					Event e0 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getVHAcceptLang(getHistory(), game), 0);
					resp.add(e0);
					behavior.updateAllocated(this.lastOfferReceived);
					
					Event eFinalize = new Event(History.VH_ID, Event.EventClass.FORMAL_ACCEPT, 0);
					if(utils.isFullOffer(o))
						resp.add(eFinalize);
				}
				
				else
				{
					Event eExpr = new Event(History.VH_ID, Event.EventClass.SEND_EXPRESSION, expression.getSemiFairEmotion(), 2000, 0);
					resp.add(eExpr);
					Event e0 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getSemiFairResponse(), 0);
					resp.add(e0);
					Event e3 = new Event(History.VH_ID, Event.EventClass.SEND_OFFER, behavior.getNextOffer(getHistory()), 700);
					if(e3.getOffer() != null)
					{
						Event e1 = new Event(History.VH_ID, Event.EventClass.OFFER_IN_PROGRESS, 0);
						resp.add(e1);
						Event e2 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getProposalLang(getHistory(), game), 3000);
						resp.add(e2);
						this.lastOfferSent = e3.getOffer();
						resp.add(e3);
					}
				}
			}
			
			else if (track_time >= game.getTotalTime()/time_limit)
			{
				if (utils.myActualOfferValue(o) >= (total_val/2)-2)
				{
					Event eExpr = new Event(History.VH_ID, Event.EventClass.SEND_EXPRESSION, expression.getFairEmotion(), 2000, 0);
					resp.add(eExpr);
					Event e0 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getVHAcceptLang(getHistory(), game), 0);
					resp.add(e0);
					behavior.updateAllocated(this.lastOfferReceived);
					
					Event eFinalize = new Event(History.VH_ID, Event.EventClass.FORMAL_ACCEPT, 0);
					if(utils.isFullOffer(o))
						resp.add(eFinalize);
				}
				else
				{
					Event eExpr = new Event(History.VH_ID, Event.EventClass.SEND_EXPRESSION, expression.getSemiFairEmotion(), 2000, 0);
					resp.add(eExpr);
					Event e0 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getSemiFairResponse(), 0);
					resp.add(e0);
					Event e3 = new Event(History.VH_ID, Event.EventClass.SEND_OFFER, behavior.getNextOffer(getHistory()), 700);
					if(e3.getOffer() != null)
					{
						Event e1 = new Event(History.VH_ID, Event.EventClass.OFFER_IN_PROGRESS, 0);
						resp.add(e1);
						Event e2 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getProposalLang(getHistory(), game), 3000);
						resp.add(e2);
						this.lastOfferSent = e3.getOffer();
						resp.add(e3);
					}
				}
			}
			
			return resp;
		}
		
		//what to do when the player sends a message (including offer acceptances and rejections)
		if(e.getType().equals(Event.EventClass.SEND_MESSAGE))
		{
			Preference p = e.getPreference() == null ? null : new Preference(e.getPreference().getIssue1(), e.getPreference().getIssue2(), e.getPreference().getRelation(), e.getPreference().isQuery());
			if (p != null && !p.isQuery()) //a preference was expressed
			{
				utils.addPref(p);
				if(utils.reconcileContradictions())
				{
					//we simply drop the oldest expressed preference until we are reconciled.  This is not the best method, as it may not be the the most efficient route.
					LinkedList<String> dropped = new LinkedList<String>();
					dropped.add(IAGOCoreMessage.prefToEnglish(utils.dequeuePref(), game));
					while(utils.reconcileContradictions())
						dropped.add(IAGOCoreMessage.prefToEnglish(utils.dequeuePref(), game));
					
					String drop = "";
					for (String s: dropped)
						drop += "\"" + s + "\", and ";
					
					drop = drop.substring(0, drop.length() - 6);//remove last 'and'
					
					Event e1 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, 
							messages.getContradictionResponse(drop), 2000);
					//history.updateHistory(e1);
					resp.add(e1);
				}
			}
			
			Event e0 = new Event(History.VH_ID, Event.EventClass.SEND_EXPRESSION, expression.getExpression(getHistory()), 2000, 1000);
			resp.add(e0);
			
			Event e1 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getMessageResponse(getHistory(), game), 3000);
			resp.add(e1);
			
			
			if(e.getMessageCode() == 11)//offer requested
			{
				Event e2 = new Event(History.VH_ID, Event.EventClass.SEND_OFFER, behavior.getNextOffer(getHistory()), 3000);
				if(e2 != null)
				{
					Event e3 = new Event(History.VH_ID, Event.EventClass.OFFER_IN_PROGRESS, 0);
					resp.add(e3);
					Event e4 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getProposalLang(getHistory(), game), 1000);
					resp.add(e4);
					this.lastOfferSent = e2.getOffer();
					resp.add(e2);		
				}
			}
			if(e.getMessageCode() == 101)//offer accepted
			{
				if(this.lastOfferSent != null)
					behavior.updateAllocated(this.lastOfferSent);
				
				Event e2 = new Event(History.VH_ID, Event.EventClass.SEND_OFFER, behavior.getAcceptOfferFollowup(getHistory()), 3000);
				if(e2.getOffer() != null)
				{
					Event e3 = new Event(History.VH_ID, Event.EventClass.OFFER_IN_PROGRESS, 0);
					resp.add(e3);
					Event e4 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getProposalLang(getHistory(), game), 1000);
					resp.add(e4);
					this.lastOfferSent = e2.getOffer();
					resp.add(e2);		
				}
			}
			
			if(e.getMessageCode() == 100)//offer rejected
			{			
				Event e2 = new Event(History.VH_ID, Event.EventClass.SEND_OFFER, behavior.getRejectOfferFollowup(getHistory()), 3000);
				if(e2.getOffer() != null)
				{
					Event e3 = new Event(History.VH_ID, Event.EventClass.OFFER_IN_PROGRESS, 0);
					resp.add(e3);
					Event e4 = new Event(History.VH_ID, Event.EventClass.SEND_MESSAGE, messages.getProposalLang(getHistory(), game), 1000);
					resp.add(e4);
					this.lastOfferSent = e2.getOffer();
					resp.add(e2);		
				}
			}
			return resp;
		}
		
		return null;
	}

	@Override
	public abstract String getArtName();
	
	@Override
	public abstract String agentDescription();
}
