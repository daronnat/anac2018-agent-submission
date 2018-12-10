package strathclyde.agent.emma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import edu.usc.ict.iago.utils.BehaviorPolicy;
import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;

public class STRATH_IAGO_Behavior extends IAGOCoreBehavior implements BehaviorPolicy {
		
	private AgentUtilsExtension utils;
	private GameSpec game;	
	private Offer allocated;

	@Override
	protected void setUtils(AgentUtilsExtension utils)
	{
		this.utils = utils;
		this.game = this.utils.getSpec();
		allocated = new Offer(game.getNumIssues());
		for(int i = 0; i < game.getNumIssues(); i++)
		{
			int[] init = {0, game.getIssueQuants()[i], 0};
			allocated.setItem(i, init);
		}
	}
	
	@Override
	protected void updateAllocated (Offer update)
	{
		allocated = update;
	}
	
	@Override
	protected Offer getAllocated ()
	{
		return allocated;
	}
	
	@Override
	protected Offer getConceded ()
	{
		return allocated;
	}

	// get the offer when out of time
	@Override
	protected Offer getFinalOffer(History history)
	{
		
		int ct = 0;
		int total_val = 0;
		while(ct < game.getNumIssues()) 
		{
			total_val += game.getIssueQuants()[ct]*game.getSimpleVHPoints().get(game.getIssuePluralNames()[ct]);
			ct+=1;
		}
		
		
		int val_alloc = 0;
		Offer new_offer = new Offer(game.getNumIssues());
		int cpt = 0;

		while(cpt < game.getNumIssues()) 
		{
			int issue_alloc_vh = 0;
			
			while (val_alloc < (total_val/2) && issue_alloc_vh < game.getIssueQuants()[cpt]) 
			{
				val_alloc += game.getSimpleVHPoints().get(game.getIssuePluralNames()[cpt]);	
				issue_alloc_vh += 1;
			}

			int issue_alloc_player = game.getIssueQuants()[cpt]-issue_alloc_vh;
			int[] temp_array = {issue_alloc_vh,0,issue_alloc_player};
			new_offer.setItem(cpt,temp_array);
			cpt+=1;
		}
		
		return new_offer;	
	}

	// get the counter offers that the agent will send to the participant
	@Override
	public Offer getNextOffer(History history) 
	{	
		// create empty new offer to be returned at the end
		Offer new_offer = new Offer(game.getNumIssues());
		// get the current scenario number (depend on the number of current rejected offer/new offer started)
		int scenario = IAGOCoreVH.scenario_nb;
		// set the additional margin that the agent is going to want in an offer
		double margin = IAGOCoreVH.margin;
		// create a new hash to contain issues and their quantity
		Hashtable<Integer, Integer> table_quant = new Hashtable<Integer, Integer>();
		// create a list containing all of the values per issue
		List<Integer> list_val = new ArrayList<Integer>();

		int i = 0;
		for (int value : game.getSimpleVHPoints().values())
		{
		    list_val.add(value);
		    table_quant.put(i, game.getIssueQuants()[i]);  		    
		    i+=1;
		}
		
		// compute total value possible that one could gain in one round:
		int ct = 0;
		int total_val = 0;
		while(ct < game.getNumIssues()) 
		{
			total_val += game.getIssueQuants()[ct]*game.getSimpleVHPoints().get(game.getIssuePluralNames()[ct]);
			ct+=1;
		}

		// scenario 1: offers centred around the quantity of items
		if (scenario == 1)
		{
			int cpt = 0;
			while (cpt < game.getNumIssues()) 
			{
				int div_issue = table_quant.get(cpt)/2;
				int mod_div_issue = table_quant.get(cpt)%2;
				
				if(mod_div_issue > 0) 
				{
					if (((cpt+1)%2)>0) 
					{
						int temp_score_vh = div_issue+mod_div_issue;
						int[] temp_array = {temp_score_vh,0,div_issue};
						new_offer.setItem(cpt,temp_array);
					}
					else 
					{
						int temp_score_player = div_issue+mod_div_issue;
						int[] temp_array = {div_issue,0,temp_score_player};
						new_offer.setItem(cpt,temp_array);
					}
				}
				else 
				{
					int[] temp_array = {div_issue,0,div_issue};
					new_offer.setItem(cpt,temp_array);
				}
				cpt+=1;
			}
			// get value of current offer
			int val_offer = utils.myActualOfferValue(new_offer);
			// see if target is reached
			if(val_offer < (total_val/2)*margin) 
			{		
				// get the least valuable issue
				Collections.min(list_val);
				int min_val = list_val.get((list_val.size()/2)-1);
				// get the location of the least valuable issue in the offer
				int ci = 0;
				
				while (ci < game.getNumIssues())
				{

					if (game.getSimpleVHPoints().get(game.getIssuePluralNames()[ci]) == min_val)
					{
						if (new_offer.getItem(ci)[2] > 0)
						{
							int[] temp_array = {new_offer.getItem(ci)[0]+1,0,new_offer.getItem(ci)[2]-1};
							new_offer.setItem(ci,temp_array);
						}	
					}
					ci+=1;
				}
			}	
		}

		// scenario 2, offer centred around the value of the items
		if (scenario >= 2) 
		{
			int val_alloc = 0;
			new_offer = new Offer(game.getNumIssues());
			int cpt = 0;

			while(cpt < game.getNumIssues()) 
			{
				int issue_alloc_vh = 0;
				
				while (val_alloc < (total_val/2) && issue_alloc_vh < game.getIssueQuants()[cpt]) 
				{
					val_alloc += game.getSimpleVHPoints().get(game.getIssuePluralNames()[cpt]);	
					issue_alloc_vh += 1;
				}

				int issue_alloc_player = game.getIssueQuants()[cpt]-issue_alloc_vh;
				int[] temp_array = {issue_alloc_vh,0,issue_alloc_player};
				new_offer.setItem(cpt,temp_array);
				cpt+=1;
			}
	
		}
		
		return new_offer;	
	}	


	@Override
	protected Offer getTimingOffer(History history) 
	{
		// create empty new offer to be returned at the end
		Offer new_offer = new Offer(game.getNumIssues());
		// get the number of the current active round
		int curr_round = IAGOCoreVH.current_round;
		// set the additional margin that the agent is going to want in an offer
		double margin = 1.1;
		
		// set the additional margin desired by the agent according to the current round of negotiation
		if (curr_round == 1)
		{
			margin = 1.1;
		}
		
		else if (curr_round == 2)
		{
			margin = 1.2;
		}
		
		else if (curr_round >= 3)
		{
			margin = 1.3;
		}
		
		// compute total value possible that one could gain in one round:
		int ct = 0;
		int total_val = 0;
		while(ct < game.getNumIssues()) 
		{
			total_val += game.getIssueQuants()[ct]*game.getSimpleVHPoints().get(game.getIssuePluralNames()[ct]);
			ct+=1;
		}
	
		int val_alloc = 0;
		new_offer = new Offer(game.getNumIssues());
		int cpt = 0;

		while(cpt < game.getNumIssues()) 
		{
			int issue_alloc_vh = 0;
			
			while (val_alloc < ((total_val/2)*margin) && issue_alloc_vh < game.getIssueQuants()[cpt]) 
			{
				val_alloc += game.getSimpleVHPoints().get(game.getIssuePluralNames()[cpt]);	
				issue_alloc_vh += 1;
			}

			int issue_alloc_player = game.getIssueQuants()[cpt]-issue_alloc_vh;
			int[] temp_array = {issue_alloc_vh,0,issue_alloc_player};
			new_offer.setItem(cpt,temp_array);
			
			cpt+=1;
		}
		
		return new_offer;	
	}

	@Override
	protected Offer getAcceptOfferFollowup(History history) {
		return null;
	}
	
	@Override
	protected Offer getFirstOffer(History history) {
		return null;
	}

	@Override
	protected int getAcceptMargin() {
		return game.getNumIssues();
	}

	@Override
	protected Offer getRejectOfferFollowup(History history) {
		return null;
	}

}
