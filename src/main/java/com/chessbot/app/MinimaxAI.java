package com.chessbot.app;

import java.util.Random;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.position.Position;

public class MinimaxAI implements ChessAI {
	
	private int ai_color, maxDepth, nodesExplored;
	private boolean foundMate;
	
	public MinimaxAI(int depth){
		this.maxDepth = depth;
	}
	
	public short getMove(Position position) {
		nodesExplored = 0;
		ai_color = position.getToPlay();
		return MinimaxID(position, maxDepth);
		//return Minimax(position, maxDepth);
	}
	
	private short MinimaxID(Position position, int maxDepth){
		short bestMove = 0;
		foundMate = false;
		
		for(int i = 1; i <= maxDepth; i++){
			bestMove = Minimax(position, i);
			
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
	
	private short Minimax(Position position, int MaxDepth){
		
		int value, max = -Integer.MAX_VALUE;
		
		// if its checkmate, you lose!
		if(position.isMate()){
			return 0;
		}
		
		short [] moves = position.getAllMoves();
		short bestMove = moves[new Random().nextInt(moves.length)];
		
		for(short move : moves){
			try {
				// the AI makes a move
				position.doMove(move);
				value = getMinValue(position, MaxDepth-1);
				position.undoMove();
				
				// maximize value of the AI's initial move
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		System.out.println("Minimax Nodes Explored: " + nodesExplored + "\nMinimax Value: " + max);
		return bestMove;
	}
	
	private int getMinValue(Position position, int depth){
		
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
			short[] moves = position.getAllMoves();
			for(short move : moves){
				
				// minimize the value of the user's moves
				position.doMove(move);
				min = Math.min(min, getMaxValue(position, depth-1));
				position.undoMove();
			}
			
			return min;
		}
		catch(IllegalMoveException e){
			System.out.print("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
			return -Integer.MAX_VALUE;
		}
		

	}
	
	private int getMaxValue(Position position, int depth){
		
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
			short[] moves = position.getAllMoves();
			for(short move : moves){
				
				// maximize the value of the AI's moves
				position.doMove(move);
				max = Math.max(max, getMinValue(position, depth-1));
				position.undoMove();
			}
			
			return max;
		}
		catch(IllegalMoveException e){
			System.out.println("You get washed. https://www.youtube.com/watch?v=4UDnTJcjPhY");
			return Integer.MAX_VALUE;
		}

	}
	
}