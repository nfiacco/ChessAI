package com.chessbot.app;

import java.util.HashMap;
import java.util.Random;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.position.Position;

public class TransAI implements ChessAI {
	
	private int ai_color, maxDepth, nodesExplored, transpositionUsed;
	private HashMap<Long, Entry> transposition;
	private boolean foundMate;
	
	public TransAI(int depth){
		this.maxDepth = depth;
		this.transposition = new HashMap<Long, Entry>();
	}
	
	public short getMove(Position position) {
		transpositionUsed = 0;
		nodesExplored = 0;
		ai_color = position.getToPlay();
		return TransAlphaBetaID(position, maxDepth);
		//return TransAlphaBeta(position, maxDepth);
	}
	
	// iterative deepening functionality built into Minimax search
	// with Alpha-Beta pruning and transposition table usage
	private short TransAlphaBetaID(Position position, int maxDepth){
		short bestMove = 0;
		foundMate = false;
		transposition.clear();
		
		for(int i = 1; i <= maxDepth; i++){
			bestMove = TransAlphaBeta(position, i);
			
			// found winning move
			if(foundMate){
				break;
			}
		}
		
		return bestMove;
	}
	
	private int getMaterialValue(Position position){
		
		// this is kind of a waste
		/* even terminal states could be in the transposition table, note that
		// the quality doesn't matter since any quality is better than evaluation
		if(transposition.containsKey(position.getHashCode())){
			transpositionUsed++;
			return transposition.get(position.getHashCode()).getValue();
		}
		*/
		
		int color, material = 0;

		String fen = position.getFEN();
		for(int i = 0; i < fen.length(); i++){
			char c = fen.charAt(i);
			// space character means you have reached the end of the line
			if(c == ' '){
				break;
			}
				
			// black is lowercase in FEN and the constant in chesspresso for black is 1
			// the opposite of both statements is true for white
			if((Character.isLowerCase(c) && ai_color == 0) || (Character.isUpperCase(c) && ai_color == 1)){
				// utility is negative for each of the opponent's pieces
				color = -1;
			}
			else{
				color = 1;
			}
			
			// based on piece type, sum up the utilities
			if(c == 'p' || c == 'P'){
				material += (100 * color);
			}
			if(c == 'n' || c == 'N'){
				material += (320 * color);
			}
			if(c == 'b' || c == 'B'){
				material += (330 * color);
			}
			if(c == 'r' || c == 'R'){
				material += (500 * color);
			}
			if(c == 'q' || c == 'Q'){
				material += (900 * color);
			}
		}

		// determined this was kind of a waste
		/* add it, although it will only be used for other terminal positions
		transposition.put(position.getHashCode(), new Entry(0, material, Entry.EXACT));
		*/
		
		return material;
	}
	
	private short TransAlphaBeta(Position position, int MaxDepth){
		
		int value, max = -Integer.MAX_VALUE;
		
		// if it is checkmate, you lose!
		if(position.isMate()){
			return 0;
		}
		
		short [] moves = position.getAllMoves();
		short bestMove = moves[new Random().nextInt(moves.length)];
		
		for(short move : moves){
			try{	
				position.doMove(move);
				value = getMinValue(position, -Integer.MAX_VALUE, Integer.MAX_VALUE, MaxDepth-1);
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
				return 0;
			}
		}
		
		System.out.println("Alpha-Beta Nodes Explored at depth " + MaxDepth + ": " + nodesExplored 
				+ "\nAlpha-Beta Value: " + max + "\nUsed Transposition Table: " + transpositionUsed + "\n");
		return bestMove;
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
		
		// try to get existing value from transposition table
		if(transposition.containsKey(position.getHashCode())){
			// we only want high quality values, depends on how much depth is left
			Entry entry = transposition.get(position.getHashCode());
			if(entry.getQuality() >= depth){
				
				// we can just use the exact value
				if(entry.getBound() == Entry.EXACT){
					transpositionUsed++;
					return entry.getValue();
				}
				
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

			// try to get the min of the possible moves by recursing with getMaxValue
			for(short move : position.getAllMoves()){
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
					return min;
				}
			}
			
			// add this position to the transposition table
			transposition.put(position.getHashCode(), new Entry(depth, min, Entry.EXACT));
			return min;
		}
		
		catch(IllegalMoveException e){
			System.out.print("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
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
		
		// try to get existing value from transposition table
		if(transposition.containsKey(position.getHashCode())){
			// we only want high quality values, depends on how much depth is left
			Entry entry = transposition.get(position.getHashCode());
			if(entry.getQuality() >= depth){
				
				// we can just use the exact value
				if(entry.getBound() == Entry.EXACT){
					transpositionUsed++;
					return entry.getValue();
				}
				
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
			
			// try to get the max of the possible moves by recursing with getMinValue
			for(short move : position.getAllMoves()){
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
					return max;
				}
			}
			
			// add this position to the transposition table
			transposition.put(position.getHashCode(), new Entry(depth, max, Entry.EXACT));
			return max;
		}
		
		catch(IllegalMoveException e){
			System.out.println("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
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
	
}