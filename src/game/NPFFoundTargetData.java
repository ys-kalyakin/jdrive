package game;

/* Meant to be stored in AyStar.userpath */
public class NPFFoundTargetData 
{
	int best_bird_dist; /* The best heuristic found. Is 0 if the target was found */
	int best_path_dist; /* The shortest path. Is (int)-1 if no path is found */
	Trackdir best_trackdir; /* The trackdir that leads to the shortest path/closest birds dist */
	AyStarNode node; /* The node within the target the search led us to */
	PathNode path;

}