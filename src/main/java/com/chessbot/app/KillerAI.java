package com.chessbot.app;

import java.util.HashMap;
import java.util.Random;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import chesspresso.position.Position;

public class KillerAI implements ChessAI {
	
	private int ai_color, maxDepth, nodesExplored, transpositionUsed;
	private HashMap<Long, Entry> transposition;
	private boolean foundMate;
	private KillerTuple[] killerMoves;
	private TextArea logView;
	
	public KillerAI(int depth, TextArea logView){
		this.maxDepth = depth;
		this.transposition = new HashMap<Long, Entry>();
		this.logView = logView;
		
		// make a list of KillerTuples with an index for each depth
		killerMoves = new KillerTuple[maxDepth];
		for(int i = 0; i < maxDepth; i++){
			killerMoves[i] = new KillerTuple();
		}
	}
	
	private void log(String text){
		Platform.runLater(() -> logView.appendText(text + "\n"));
	}
	
	public String getMoveString(int move){
		char col = (char) ((move % 8) + 97);
		int row = (move / 8) + 1;
		return col + Integer.toString(row);
	}
	
	public short getMove(Position position) {
		transpositionUsed = 0;
		nodesExplored = 0;
		ai_color = position.getToPlay();
		return MTDFID(position, maxDepth);
		//return TransAlphaBeta(position, maxDepth);
	}
	
	// iterative deepening functionality built into Minimax search
	// with Alpha-Beta pruning and transposition table usage
	private short MTDFID(Position position, int maxDepth){
		int firstGuess = 0, secondGuess = 0;
		short bestMove = 0;
		Tuple result;
		foundMate = false;
		transposition.clear();
		log("ChessBot computing best move...");
		
		for(int i = 1; i <= maxDepth; i++){
			
			// oscillation between even and odd depths is handled
			if(i % 2 == 0){
				result = MTDF(position, secondGuess, i);
				secondGuess = result.getValue();
			}
			else{
				result = MTDF(position, firstGuess, i);
				firstGuess = result.getValue();
			}
			
			bestMove = result.getMove();
			
			// found winning move
			if(foundMate){
				break;
			}
		}
		
		log("ChessBot Move: " + getMoveString(Move.getFromSqi(bestMove)) + getMoveString(Move.getToSqi(bestMove)) + "\n");
		return bestMove;
	}
	
	private Tuple MTDF(Position position, int firstGuess, int MaxDepth){
		int beta, g = firstGuess;
		int upperbound = Integer.MAX_VALUE;
		int lowerbound = -Integer.MAX_VALUE;
		
		Tuple result = null;
		
		while(lowerbound < upperbound){
			// the window should be targeted above the lower bound
			if(g == lowerbound){
				beta = g + 1;
			}
			else{
				beta = g;
			}
			
			// get an upper/lower bound on the minimax value by trying AlphaBeta with zero window
			// with each successive call move the window towards the actual value
			result = TransAlphaBeta(position, beta-1, beta, MaxDepth);
			g = result.getValue();
			
			// adjust the bounds of the window
			if(g < beta){
				upperbound = g;
			}
			else{
				lowerbound = g;
			}
		}
		
		return result;
	}
	
	private int getMaterialValue(Position position){
		
		// this is a waste of time
		/* even terminal states could be in the transposition table, doesn't matter
		// about the quality since any quality is better than the evaluation
		if(transposition.containsKey(position.getHashCode())){
			transpositionUsed++;
			return transposition.get(position.getHashCode()).getValue();
		}
		*/
		
		int sqi, color, stone, material = 0;
		
		// note that we have added more informative weights to pieces
		for(int r = 0; r < 8; r++){
			for(int c = 0; c < 8; c++){
				sqi = Chess.coorToSqi(c, r);
				color = 1;
				stone = position.getStone(sqi);
				
				// utility is negative for each of the opponent's pieces
				if((stone < 0 && ai_color == 1) || (stone > 0 && ai_color == 0)){
					color = -1;
				}
				
				// based on the piece type, sum up the utilities
				if(Math.abs(stone) == Chess.PAWN){
					material += (100 * color);
				}
				if(Math.abs(stone) == Chess.KNIGHT){
					material += (320 * color);
				}
				if(Math.abs(stone) == Chess.BISHOP){
					material += (330 * color);
				}
				if(Math.abs(stone) == Chess.ROOK){
					material += (500 * color);
				}
				if(Math.abs(stone) == Chess.QUEEN){
					material += (900 * color);
				}
			}
		}
		
		// waste of time
		/* add it, although it will only be used for other terminal positions
		transposition.put(position.getHashCode(), new Entry(0, material, Entry.EXACT));
		*/
		
		return material;
	}
	
	private Tuple TransAlphaBeta(Position position, int alpha, int beta, int MaxDepth){
		
		int value, max = -Integer.MAX_VALUE;
		for(int i = 0; i < maxDepth; i++){
			killerMoves[i].clear();
		}
		
		// if it is checkmate, you lose!
		if(position.isMate()){
			return null;
		}
		
		short [] moves = position.getAllMoves();
		short bestMove = moves[new Random().nextInt(moves.length)];
		
		for(short move : moves){
			try{	
				position.doMove(move);
				value = getMinValue(position, alpha, beta, MaxDepth-1);
				position.undoMove();
				
				// update the bestMove if we found a better option
				if(value > max){
					max = value;
					bestMove = move;
				}
				
				// found winning move
				if(value == Integer.MAX_VALUE){
					foundMate = true;
					break;
				}
			}
			catch(IllegalMoveException e){
				System.out.print("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
				log("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
				return null;
			}
		}
		
		// System.out.println("Alpha-Beta Nodes Explored at depth " + MaxDepth + ": " + nodesExplored 
		//		+ "\nAlpha-Beta Value: " + max + "\nUsed Transposition Table: " + transpositionUsed + "\n");
		return new Tuple(bestMove, max);
	}
	
	private int getMinValue(Position position, int alpha, int beta, int depth){
		
		nodesExplored++;
		int min = Integer.MAX_VALUE;
		
		// if checkmate, the AI won
		if(position.isMate()){
			return Integer.MAX_VALUE;
		}
		
		// stalemate is always 0
		if(position.isStaleMate()){
			return 0;
		}
		
		// only bounds matter, exact values aren't accurate with MTD(f)
		// try to get existing value from transposition table
		if(transposition.containsKey(position.getHashCode())){
			// we only want high quality values, depends on how much depth is left
			Entry entry = transposition.get(position.getHashCode());
			if(entry.getQuality() >= depth){
				// if we found an upper bound that is less than alpha, we can prune
				if(entry.getBound() == Entry.UPPER && entry.getValue() <= alpha){
					transpositionUsed++;
					return entry.getValue();
				}
			}
		}
		
		// don't make a move if you're in to deep
		if(depth == 0){
			return getMaterialValue(position);
		}
		
		try{
			
			short[] moves = position.getAllMoves();
			short killer1 = killerMoves[depth].getFirst();
			short killer2 = killerMoves[depth].getSecond();
			
			// make sure they aren't both negative
			if(killer1 >0 && killer2 > 0){
				int index = 0;
				for(int i = 0; i < moves.length; i++){
					// move the killer move to the front
					if(moves[i] == killer1){
						short temp = moves[index];
						moves[index] = moves[i];
						moves[i] = temp;
						index++;
					}
					// move the other killer to the front
					if(moves[i] == killer2){
						short temp = moves[index];
						moves[index] = moves[i];
						moves[i] = temp;
						index++;
					}
				}
			}
			
			// try to get the min of the possible moves by recursing with getMaxValue
			for(short move : moves){
				// the user makes a move
				position.doMove(move);
				min = Math.min(min, getMaxValue(position, alpha, beta, depth-1));
				position.undoMove();
				
				// you lost so stop searching
				if(min == -Integer.MAX_VALUE){
					return min;
				}
				
				// update beta
				if(min < beta){
					beta = min;
				}
				
				// make sure we are still in the window, otherwise we have an upper bound
				if(min <= alpha){
					// add as an upper bound
					transposition.put(position.getHashCode(), new Entry(depth, min, Entry.UPPER));
					
					// add the killer move
					killerMoves[depth].addMove(move);
					return min;
				}
			}
			
			return min;
		}
		
		catch(IllegalMoveException e){
			System.out.print("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
			log("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
			return -Integer.MAX_VALUE;
		}
	}
	
	private int getMaxValue(Position position, int alpha, int beta, int depth){
		
		nodesExplored++;
		int max = -Integer.MAX_VALUE;
		
		// if checkmate, the AI lost
		if(position.isMate()){
			return -Integer.MAX_VALUE;
		}
		
		// stalemate is always 0
		if(position.isStaleMate()){
			return 0;
		}
		
		// only bounds matter, exact values aren't accurate with MTD(f)
		// try to get existing value from transposition table
		if(transposition.containsKey(position.getHashCode())){
			// we only want high quality values, depends on how much depth is left
			Entry entry = transposition.get(position.getHashCode());
			if(entry.getQuality() >= depth){
				// if we found a lower bound that exceeds beta, we can prune
				if(entry.getBound() == Entry.LOWER && entry.getValue() >= beta){
					transpositionUsed++;
					return entry.getValue();
				}
			}
		}
		
		// don't make a move if you're in to deep
		if(depth == 0){
			return getMaterialValue(position);
		}
		
		try{
			
			short[] moves = position.getAllMoves();
			short killer1 = killerMoves[depth].getFirst();
			short killer2 = killerMoves[depth].getSecond();
			
			// make sure they aren't both negative
			if(killer1 >0 && killer2 > 0){
				int index = 0;
				for(int i = 0; i < moves.length; i++){
					// move the killer move to the front
					if(moves[i] == killer1){
						short temp = moves[index];
						moves[index] = moves[i];
						moves[i] = temp;
						index++;
					}
					// move the other killer to the front
					if(moves[i] == killer2){
						short temp = moves[index];
						moves[index] = moves[i];
						moves[i] = temp;
						index++;
					}
				}
			}
			
			// try to get the max of the possible moves by recursing with getMinValue
			for(short move : moves){
				position.doMove(move);
				max = Math.max(max, getMinValue(position, alpha, beta, depth-1));
				position.undoMove();
				
				// you won so stop searching
				if(max == Integer.MAX_VALUE){
					return max;
				}
				
				// update alpha
				if(max > alpha){
					alpha = max;
				}
				
				// make sure we are still in the window otherwise we have a lower bound
				if(max >= beta){
					// add as a lower bound
					transposition.put(position.getHashCode(), new Entry(depth, max, Entry.LOWER));
					
					// add the killer move
					killerMoves[depth].addMove(move);
					return max;
				}
			}
			
			return max;
		}
		
		catch(IllegalMoveException e){
			System.out.println("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
			log("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
			return Integer.MAX_VALUE;
		}
	}
	
	// used for transposition tables and what not
	private class Entry{
		static final int LOWER = -1;
		static final int EXACT = 0;
		static final int UPPER = 1;
		
		private int quality, value, bound;
		private Entry(int quality, int value, int bound){
			this.quality = quality;
			this.value = value;
			this.bound = bound;
		}
		
		private int getValue(){ return value; }
		private int getQuality(){ return quality; }
		private int getBound(){ return bound; }
		
	}
	
	// used for returning best move and associated value
	private class Tuple{
		
		private short move;
		private int value;
		
		private Tuple(short move, int value){
			this.move = move;
			this.value = value;
		}
		
		private short getMove(){ return move; }		
		private int getValue(){ return value; }

	}
	
	// store the killer moves for a particular depth, and note which was
	// the last to be updated
	private class KillerTuple{
		private boolean last;
		private short first, second;
		
		private KillerTuple(){
			first = 1;
			second = -1;
			last = true;
		}
		
		private short getFirst(){return first;}
		private short getSecond(){return second;}
		
		private void addMove(short move){
			// don't add duplicates
			if(move != first && move != second){
				if(last){
					first = move;
				}
				else{
					second = move;
				}
				// flip the value so next time the other is updated
				last = !last;
			}
		}
		
		private void clear(){
			first = -1;
			second = -1;
			last = true;
		}
	}
	
}
