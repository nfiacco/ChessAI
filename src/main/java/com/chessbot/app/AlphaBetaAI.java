package com.chessbot.app;

import java.util.Random;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.position.Position;

public class AlphaBetaAI implements ChessAI {
	
	private int ai_color, maxDepth, nodesExplored, nodesPruned;
	private boolean foundMate;
	
	public AlphaBetaAI(int depth){
		this.maxDepth = depth;
	}
	
	public short getMove(Position position) {
		nodesExplored = 0;
		nodesPruned = 0;
		ai_color = position.getToPlay();
		return AlphaBetaID(position, maxDepth);
		//return AlphaBeta(position, maxDepth);
	}
	
	private short AlphaBetaID(Position position, int maxDepth){
		short bestMove = 0;
		foundMate = false;
		
		for(int i = 1; i <= maxDepth; i++){
			bestMove = AlphaBeta(position, i);
			
			// found winning move
			if(foundMate){
				break;
			}
		}
		
		return bestMove;
	}
	
	private int getMaterialValue(Position position){
		
		int sqi, color, stone, material = 0;
		
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
					material += (1 * color);
				}
				if(Math.abs(stone) == Chess.KNIGHT){
					material += (3 * color);
				}
				if(Math.abs(stone) == Chess.BISHOP){
					material += (3 * color);
				}
				if(Math.abs(stone) == Chess.ROOK){
					material += (5 * color);
				}
				if(Math.abs(stone) == Chess.QUEEN){
					material += (9 * color);
				}
			}
		}	
		return material;
	}
	
	private short AlphaBeta(Position position, int MaxDepth){
		
		int value, max = -Integer.MAX_VALUE;
		
		// if it is checkmate, you lose!
		if(position.isMate()){
			return 0;
		}
		
		short [] moves = position.getAllMoves();
		short bestMove = moves[new Random().nextInt(moves.length)];
		
		for(short move : moves){
			try {
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
			catch (IllegalMoveException e) {
				System.out.print("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
				return 0;
			}

		}
		
		System.out.println("Alpha-Beta Nodes Explored: " + nodesExplored + "\nAlpha-Beta Value: " + max + "\nNodes Pruned: " + nodesPruned);
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
		
		// don't make a move if you're in to deep
		if(depth == 0){
			return getMaterialValue(position);
		}
		
		try{
			
			// try to get the min of the possible moves by recursing with getMaxValue
			for(short move : position.getAllMoves()){
				position.doMove(move);
				min = Math.min(min, getMaxValue(position, alpha, beta, depth-1));
				position.undoMove();
				
				// you lost so stop searching
				if(min == -Integer.MAX_VALUE){
					return min;
				}
				
				// update values
				if(min < beta){
					beta = min;
				}
				if(min <= alpha){
					nodesPruned++;
					break;
				}
			}
			
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
				
				// update values
				if(max > alpha){
					alpha = max;
				}
				if(max >= beta){
					nodesPruned++;
					break;
				}
			}
			
			return max;
		}
		catch(IllegalMoveException e){
			System.out.println("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
			return Integer.MAX_VALUE;
		}
		
	}
	
}