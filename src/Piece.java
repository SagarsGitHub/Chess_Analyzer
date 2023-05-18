/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chessgame;

import java.awt.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Khouiled
 *
 * editted by Sagar Rafai
 */

// Piece class that has its coordinates, name, color, and panel position
public class Piece {
    Point coordinates;
    int x;
    int y;
    boolean isWhite;
    String name;

    public Piece(Point coordinates, boolean isWhite,String n) {
        this.coordinates = coordinates;
        x = this.coordinates.x*64;
        y = this.coordinates.y*64;
        this.isWhite = isWhite;
        name=n;
    }

    // removes the piece at the coordinate and replaces it with selected piece
    public void move(Point newCoordinates, ConcurrentHashMap<Point,Piece> boardPieces, Piece selectedPiece){


        if (newCoordinates.x > 7 || newCoordinates.y > 7){
            return;
        }

        // could potentially just change the piece in the hashmap boardPieces
        boardPieces.values().remove(selectedPiece);


        this.coordinates.x= newCoordinates.x;
        this.coordinates.y = newCoordinates.y;
        this.x = newCoordinates.x*64;
        this.y = newCoordinates.y*64;
        boardPieces.put(newCoordinates,this);


    }
}
