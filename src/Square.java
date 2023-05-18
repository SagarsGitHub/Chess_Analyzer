package chessgame;

import java.awt.*;


// Class that gives coordinates of a square, as well as if it is occupied and contested
public class Square {

    Point coordinate;
    int threatValue = 0;
    boolean isContested = false;

    Piece currentPiece;

    public Square(Point coordinate,Piece currentPiece){
        this.coordinate = coordinate;
        this.currentPiece = currentPiece;
    }
}
