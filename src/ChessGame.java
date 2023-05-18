/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chessgame;


import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Khouiled
 *
 * editted by Sagar Rafai
 *
 * TO-DO:
 * - clean up main and move unecessary methods to new board class
 * - implement method to recursively analyze moves that doesn't get stuck
 * - break out things into smaller functions
 * - separate pieces into json file/ external format to populate board cleanly (look into json, xml, or csv)
 *
 */

// simple chess engine that determines what color controls each square displayed on a simple gui.
public class ChessGame {

    // map for the piece palette
    public static HashMap<Point,Piece> piecePalette = new HashMap<>();

    // map for the pieces on the board (concurrent for easy editing)
    public static ConcurrentHashMap<Point,Piece> boardPieces = new ConcurrentHashMap<>();

    // square array to store the board state for analysis
    public static Square[][] boardState = new Square[8][8];

    // variable to store pieces bing dragged
    public static Piece selectedPiece = null;



    public static void main(String[] args) throws IOException {

        // import images
        BufferedImage all=ImageIO.read(new File("D:\\Coding_Projects\\ChessGame-Works but not recursive\\src\\chess.png"));

        Image[] pieceImages =new Image[12];
        Image trashIcon = ImageIO.read(new File("D:\\Coding_Projects\\ChessGame-Works but not recursive\\src\\trash.png")).getScaledInstance(64, 64, BufferedImage.SCALE_SMOOTH);
        Image clearIcon = ImageIO.read(new File("D:\\Coding_Projects\\ChessGame-Works but not recursive\\src\\clear.png")).getScaledInstance(64, 64, BufferedImage.SCALE_SMOOTH);
        Image startIcon = ImageIO.read(new File("D:\\Coding_Projects\\ChessGame-Works but not recursive\\src\\start.png")).getScaledInstance(64, 64, BufferedImage.SCALE_SMOOTH);
        Image closeIcon = ImageIO.read(new File("D:\\Coding_Projects\\ChessGame-Works but not recursive\\src\\close.png")).getScaledInstance(64, 64, BufferedImage.SCALE_SMOOTH);

        // cut chess png into separate icons and store in an array
        int ind=0;

        for(int y=0;y<400;y+=200){
            for(int x=0;x<1200;x+=200){
                pieceImages[ind]=all.getSubimage(x, y, 200, 200).getScaledInstance(64, 64, BufferedImage.SCALE_SMOOTH);
                ind++;
            }
        }

        // add pieces to the board and piece palette
        initializeBoard(boardPieces,piecePalette);


        // initialize board state
        for (int y = 0; y < 8; y++){
            for (int x = 0; x < 8; x++){
                boardState[x][y] = new Square(new Point(x,y),null);
            }
        }

        // GUI code
        JFrame frame = new JFrame("Chess Color Analyzer");
        frame.setBounds(10, 10, 640, 512);
        frame.setUndecorated(true);
        // code for painting the jpanel
        JPanel panel=new JPanel(){

            @Override
            public void paint(Graphics g) {
                boolean white=true;

                // draw board and palette
                for(int y= 0;y<8;y++){
                    for(int x= 0;x<10;x++){
                        if (x >= 8){ // If outside board, color grey
                            g.setColor(new Color(128,128, 128));
                        } else if (boardState[x][y].isContested){ // If contested, color based on threat level
                            int threatInt = boardState[x][y].threatValue;
                            if ( threatInt > 0){
                                g.setColor(new Color(0,50,Math.min(100 + threatInt,255)));
                            } else if (threatInt < 0){
                                g.setColor(new Color(Math.min(100 + Math.abs(threatInt),255),50,0));
                            } else{
                                g.setColor(new Color(70,50,70));
                            }
                        } else if(white){ //If not contested, color board regularly
                            g.setColor(new Color(235,235, 208));
                        } else {
                            g.setColor(new Color(119, 148, 85));
                        }
                        g.fillRect(x*64, y*64, 64, 64);
                        white=!white;
                    }
                    white=!white;
                }


                // draw icons
                g.drawImage(trashIcon, 9*64, 6*64, this);
                g.drawImage(clearIcon, 8*64, 6*64, this);
                g.drawImage(startIcon, 8*64, 7*64, this);
                g.drawImage(closeIcon, 9*64, 7*64, this);

                // draw piece palette pieces
                for (Map.Entry<Point,Piece> p : piecePalette.entrySet()) {
                    int ind=getIndex(p.getValue());
                    g.drawImage(pieceImages[ind], p.getValue().x, p.getValue().y, this);
                }


                // draw board pieces
                for (Map.Entry<Point,Piece> p : boardPieces.entrySet()) {
                    int ind = getIndex(p.getValue());
                    g.drawImage(pieceImages[ind], p.getValue().coordinates.x * 64, p.getValue().coordinates.y * 64, this);
                }

                // draw selected piece being dragged
                if (selectedPiece != null) {
                    Image pieceImage = pieceImages[getIndex(selectedPiece)];
                    if (!(selectedPiece.coordinates.x-32 < 0 || selectedPiece.coordinates.y-32 < 0)) {
                        g.drawImage(pieceImage, selectedPiece.coordinates.x - 32, selectedPiece.coordinates.y - 32, this);
                    }

                }
            }
            
        };

        frame.add(panel);
        // mouse functionality
        frame.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedPiece != null){
                    selectedPiece.coordinates.x=e.getX();
                    selectedPiece.coordinates.y=e.getY();
                }
                frame.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        });
        frame.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int xCoord = e.getX()/64;
                int yCoord = e.getY()/64;

                //if on clear icon, delete all and repaint
                if (xCoord == 8 && yCoord == 6){
                    boardPieces.clear();
                    clearBoardState(boardState);
                    clearBoardStateThreat(boardState);
                    frame.repaint();
                } else if (xCoord == 8 && yCoord == 7){// if on power button, start
                    // Add current board pieces to board state
                    clearBoardState(boardState);
                    clearBoardStateThreat(boardState);
                    updateBoardState(boardState,boardPieces);
                    analyze(boardState,boardPieces,1);
                } else if (xCoord == 9 && yCoord == 7){// if on close button, end program
                    System.exit(0);
                } else if (e.getButton() == MouseEvent.BUTTON3){
                    if (e.getX()/64 <= 7 && e.getY()/64 <= 7){
                        boardPieces.remove(new Point(e.getX()/64,e.getY()/64));
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                //if on clear icon, skip
                if (e.getX()/64 == 8 && e.getY()/64 == 6){
                    frame.repaint();
                    return;
                }
                // if on start button, skip
                if (e.getX()/64 == 8 && e.getY()/64 == 7){
                    frame.repaint();
                    return;
                }
                // if on trash button, skip
                if (e.getX()/64 == 9 && e.getY()/64 == 6){
                    frame.repaint();
                    return;
                }

                //if on close button, skip
                if (e.getX()/64 == 9 && e.getY()/64 == 7){
                    frame.repaint();
                    return;
                }
                selectedPiece=null;
                selectedPiece = getPiece(e.getX(),e.getY());
                frame.repaint();

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selectedPiece!=null) selectedPiece.move(new Point((e.getX()/64),(e.getY()/64)),boardPieces,selectedPiece);
                selectedPiece=null;
                frame.repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        frame.setDefaultCloseOperation(3);
        frame.setVisible(true);
    }

    // method to get the piece at the specified panel coordinates
    public static Piece getPiece(int x,int y){
            int xCoord = (x)/64;
            int yCoord = (y)/64;

            for (Map.Entry<Point,Piece> p : piecePalette.entrySet()) {
                if (p.getKey().x == xCoord && p.getKey().y == yCoord){

                    return new Piece(new Point(xCoord,yCoord),p.getValue().isWhite,p.getValue().name);

                }
            }

            for (Map.Entry<Point,Piece> p : boardPieces.entrySet()){
                if (p.getKey().x == xCoord && p.getKey().y == yCoord){
                    Piece result = p.getValue();
                    boardPieces.values().remove(p);
                    return result;
                }
            }
            return null;
    }

    // method to analyze the board state and update threat values
    public static void analyze(Square[][] boardState,ConcurrentHashMap<Point,Piece> boardPieces,int times){

        Square[][] originalBoardState = boardState.clone();
        //for each piece
        for (Map.Entry<Point,Piece> p:boardPieces.entrySet()){
            // check if out of bounds
            if (p.getValue().coordinates.x > 7 || p.getValue().coordinates.y > 7){

                boardPieces.remove(p.getKey());
                continue;
            }
            Piece pieceToMove = p.getValue();
            //find every available move
            LinkedList<Point> availableMoves = findValidMoves(pieceToMove,boardState);

            //update threat level
            for (Point m : availableMoves) {
                boardState[m.x][m.y].isContested = true;
                if (pieceToMove.isWhite){
                    boardState[m.x][m.y].threatValue += calculateThreat(p.getValue())/times;
                } else {
                    boardState[m.x][m.y].threatValue -= calculateThreat(p.getValue())/times;
                }
            }
        }
    }


    // converts board pieces to board state for analysis
    public static void updateBoardState(Square[][] boardState,ConcurrentHashMap<Point,Piece> BoardPieces){
        for (Map.Entry<Point,Piece> p: boardPieces.entrySet()){
            Piece pieceToAdd = p.getValue();
            if (!(pieceToAdd.coordinates.x > 7 || pieceToAdd.coordinates.y > 7)) {
                boardState[pieceToAdd.coordinates.x][pieceToAdd.coordinates.y] = new Square(new Point(pieceToAdd.coordinates.x,pieceToAdd.coordinates.y),pieceToAdd);
            } else {
                boardPieces.remove(pieceToAdd);
            }
        }
    }


    // calculates specific threat for each piece (weights can be adjusted)
    public static int calculateThreat(Piece p) {
        if (p.name.equalsIgnoreCase("queen")) {
            return 25;
        }
        if (p.name.equalsIgnoreCase("bishop")) {
            return 40;
        }
        if (p.name.equalsIgnoreCase("knight")) {
            return 40;
        }
        if (p.name.equalsIgnoreCase("rook")) {
            return 35;
        }
        if (p.name.equalsIgnoreCase("pawn")) {
            return 60;
        }

        return 10;
    }

    // finds valid moves for each piece, calling the needed method for the needed piece
    public static LinkedList<Point> findValidMoves(Piece pieceToMove,Square[][] boardState){
        LinkedList<Point> addedList = new LinkedList<>();
        LinkedList<Point> result = new LinkedList<>();
        // if pawn
        if (pieceToMove.name.equals("pawn")){
            addPawnCaptures(pieceToMove,pieceToMove.isWhite,addedList);
            result.addAll(addedList);
        }
        // if rook
        if (pieceToMove.name.equals("rook")){
            addRookCaptures(pieceToMove,pieceToMove.isWhite,boardState,addedList);
            result.addAll(addedList);
        }
        // if bishop
        if (pieceToMove.name.equals("bishop")){
            addBishopCaptures(pieceToMove,pieceToMove.isWhite,boardState,addedList);
            result.addAll(addedList);
        }
        // if queen
        if (pieceToMove.name.equals("queen")){
            addQueenCaptures(pieceToMove,pieceToMove.isWhite,boardState,addedList);
            result.addAll(addedList);
        }
        // if knight
        if (pieceToMove.name.equals("knight")){
            addKnightCaptures(pieceToMove,pieceToMove.isWhite,boardState,addedList);
            result.addAll(addedList);
        }
        // if king
        if (pieceToMove.name.equals("king")){
            addKingCaptures(pieceToMove,pieceToMove.isWhite,boardState);
        }
        return result;
    }

    // find moves for the king
    public static void addKingCaptures(Piece pieceToMove,boolean isWhite,Square[][] boardState){
        if (pieceToMove.coordinates.x - 1 >= 0 && pieceToMove.coordinates.y - 1 >= 0){
            boardState[pieceToMove.coordinates.x - 1][ pieceToMove.coordinates.y - 1].isContested = true;
            if (isWhite){
                boardState[pieceToMove.coordinates.x - 1][ pieceToMove.coordinates.y - 1].threatValue += 15;
            } else {
                boardState[pieceToMove.coordinates.x - 1][ pieceToMove.coordinates.y - 1].threatValue -= 15;
            }
        }
        if (pieceToMove.coordinates.x + 1 <= 7 && pieceToMove.coordinates.y - 1 >= 0){
            boardState[pieceToMove.coordinates.x + 1][ pieceToMove.coordinates.y - 1].isContested = true;
            if (isWhite){
                boardState[pieceToMove.coordinates.x + 1][ pieceToMove.coordinates.y - 1].threatValue += 15;
            } else {
                boardState[pieceToMove.coordinates.x + 1][ pieceToMove.coordinates.y - 1].threatValue -= 15;
            }
        }
        if (pieceToMove.coordinates.x + 1 <= 7 && pieceToMove.coordinates.y + 1 <= 7){
            boardState[pieceToMove.coordinates.x + 1][ pieceToMove.coordinates.y + 1].isContested = true;
            if (isWhite){
                boardState[pieceToMove.coordinates.x + 1][ pieceToMove.coordinates.y + 1].threatValue += 15;
            } else {
                boardState[pieceToMove.coordinates.x + 1][ pieceToMove.coordinates.y + 1].threatValue -= 15;
            }
        }
        if (pieceToMove.coordinates.x  - 1 >= 0 && pieceToMove.coordinates.y + 1 <= 7){
            boardState[pieceToMove.coordinates.x - 1][ pieceToMove.coordinates.y + 1].isContested = true;
            if (isWhite){
                boardState[pieceToMove.coordinates.x - 1][ pieceToMove.coordinates.y + 1].threatValue += 15;
            } else {
                boardState[pieceToMove.coordinates.x - 1][ pieceToMove.coordinates.y + 1].threatValue -= 15;
            }
        }
        if (pieceToMove.coordinates.y - 1 >= 0){
            boardState[pieceToMove.coordinates.x][ pieceToMove.coordinates.y - 1].isContested = true;
            if (isWhite){
                boardState[pieceToMove.coordinates.x][ pieceToMove.coordinates.y - 1].threatValue += 15;
            } else {
                boardState[pieceToMove.coordinates.x][ pieceToMove.coordinates.y - 1].threatValue -= 15;
            }
        }
        if (pieceToMove.coordinates.y + 1 <= 7){
            boardState[pieceToMove.coordinates.x][ pieceToMove.coordinates.y + 1].isContested = true;
            if (isWhite){
                boardState[pieceToMove.coordinates.x][ pieceToMove.coordinates.y + 1].threatValue += 15;
            } else {
                boardState[pieceToMove.coordinates.x][ pieceToMove.coordinates.y + 1].threatValue -= 15;
            }
        }
        if (pieceToMove.coordinates.x - 1 >= 0){
            boardState[pieceToMove.coordinates.x - 1][ pieceToMove.coordinates.y].isContested = true;
            if (isWhite){
                boardState[pieceToMove.coordinates.x - 1][ pieceToMove.coordinates.y].threatValue += 15;
            } else {
                boardState[pieceToMove.coordinates.x - 1][ pieceToMove.coordinates.y].threatValue -= 15;
            }
        }
        if (pieceToMove.coordinates.x + 1 <= 7){
            boardState[pieceToMove.coordinates.x + 1][ pieceToMove.coordinates.y].isContested = true;
            if (isWhite){
                boardState[pieceToMove.coordinates.x + 1][ pieceToMove.coordinates.y].threatValue += 15;
            } else {
                boardState[pieceToMove.coordinates.x + 1][ pieceToMove.coordinates.y].threatValue -= 15;
            }
        }
    }

    // find moves for pawns
    public static void addPawnCaptures(Piece pieceToMove,boolean isWhite, LinkedList<Point> moves){

        if (isWhite){
            if (pieceToMove.coordinates.x - 1 >= 0 && pieceToMove.coordinates.y - 1 >= 0){
                moves.add(new Point(pieceToMove.coordinates.x - 1, pieceToMove.coordinates.y - 1));
            }
            if (pieceToMove.coordinates.x + 1 < 8 && pieceToMove.coordinates.y - 1 >= 0){
                moves.add(new Point(pieceToMove.coordinates.x + 1, pieceToMove.coordinates.y - 1));
            }
        } else {
            if (pieceToMove.coordinates.x - 1 >= 0 && pieceToMove.coordinates.y + 1 < 8){
                moves.add(new Point(pieceToMove.coordinates.x - 1, pieceToMove.coordinates.y + 1));
            }
            if (pieceToMove.coordinates.x + 1 < 8 && pieceToMove.coordinates.y + 1 < 8){
                moves.add(new Point(pieceToMove.coordinates.x + 1, pieceToMove.coordinates.y + 1));
            }
        }



    }

    // find moves for knights
    public static void addKnightCaptures(Piece pieceToMove, boolean isWhite,Square[][] boardstate, LinkedList<Point> moves){
        //check up
        // left
        int coordinateToCheckY = pieceToMove.coordinates.y - 2;
        int coordinateToCheckX = pieceToMove.coordinates.x - 1;
        if (coordinateToCheckX >= 0 && coordinateToCheckY >= 0 ){
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
        }
        // right
        coordinateToCheckY = pieceToMove.coordinates.y - 2;
        coordinateToCheckX = pieceToMove.coordinates.x + 1;
        if (coordinateToCheckX <= 7 && coordinateToCheckY >= 0 ){
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
        }
        //check down
        // left
        coordinateToCheckY = pieceToMove.coordinates.y + 2;
        coordinateToCheckX = pieceToMove.coordinates.x - 1;
        if (coordinateToCheckX >= 0 && coordinateToCheckY <= 7 ){
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
        }
        // right
        coordinateToCheckY = pieceToMove.coordinates.y + 2;
        coordinateToCheckX = pieceToMove.coordinates.x + 1;
        if (coordinateToCheckX <= 7 && coordinateToCheckY <= 7){
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
        }
        //check left
        // up
        coordinateToCheckY = pieceToMove.coordinates.y - 1;
        coordinateToCheckX = pieceToMove.coordinates.x - 2;
        if (coordinateToCheckX >= 0 && coordinateToCheckY >= 0 ){
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
        }
        // down
        coordinateToCheckY = pieceToMove.coordinates.y + 1;
        coordinateToCheckX = pieceToMove.coordinates.x - 2;
        if (coordinateToCheckX >= 0 && coordinateToCheckY <= 7 ){
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
        }
        //check right
        // up
        coordinateToCheckY = pieceToMove.coordinates.y - 1;
        coordinateToCheckX = pieceToMove.coordinates.x + 2;
        if (coordinateToCheckX <= 7 && coordinateToCheckY >= 0 ){
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
        }
        // down
        coordinateToCheckY = pieceToMove.coordinates.y + 1;
        coordinateToCheckX = pieceToMove.coordinates.x + 2;
        if (coordinateToCheckX <= 7 && coordinateToCheckY <= 7 ){
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
        }
    }

    // find moves for rooks
    public static void addRookCaptures(Piece pieceToMove, boolean isWhite,Square[][] boardState, LinkedList<Point> moves){
        moves = addAdjacent(pieceToMove,boardState,moves);
    }

    // find moves for bishops
    public static void addBishopCaptures(Piece pieceToMove, boolean isWhite,Square[][] boardState, LinkedList<Point> moves){
        moves = addDiagonal(pieceToMove,boardState,moves);
    }

    // find moves for queens
    public static void addQueenCaptures(Piece pieceToMove, boolean isWhite,Square[][] boardState, LinkedList<Point> moves){
        LinkedList<Point> result = new LinkedList<Point>();
        moves.addAll(addDiagonal(pieceToMove,boardState,result));
        result.clear();
        moves.addAll(addAdjacent(pieceToMove,boardState,result));
    }

    // helper method for adjacent piece moves
    public static LinkedList<Point> addAdjacent(Piece pieceToMove,Square[][] boardState,LinkedList<Point> moves){
        int coordinateToCheck = pieceToMove.coordinates.y -1;
        boolean encounteredPiece = false;
        // check up
        while(coordinateToCheck >= 0 && !encounteredPiece){
            // add move if not encountered a piece
            moves.add(new Point(pieceToMove.coordinates.x, coordinateToCheck));
            if (boardState[pieceToMove.coordinates.x][coordinateToCheck].currentPiece!=null) encounteredPiece = true;
            coordinateToCheck--;
        }

        while (coordinateToCheck >= 0){
            boardState[pieceToMove.coordinates.x][coordinateToCheck].isContested = true;
            if (pieceToMove.isWhite){
                boardState[pieceToMove.coordinates.x][coordinateToCheck].threatValue += calculateThreat(pieceToMove)/3;
            } else {
                boardState[pieceToMove.coordinates.x][coordinateToCheck].threatValue -= calculateThreat(pieceToMove)/3;
            }
            coordinateToCheck--;
        }

        coordinateToCheck = pieceToMove.coordinates.y + 1;
        encounteredPiece = false;
        // check down
        while(coordinateToCheck <= 7 && !encounteredPiece){
            // add move if not encountered a piece
            moves.add(new Point(pieceToMove.coordinates.x, coordinateToCheck));

            if (boardState[pieceToMove.coordinates.x][coordinateToCheck].currentPiece!=null) encounteredPiece = true;
            coordinateToCheck++;
        }

        while (coordinateToCheck <= 7){
            boardState[pieceToMove.coordinates.x][coordinateToCheck].isContested = true;
            if (pieceToMove.isWhite){
                boardState[pieceToMove.coordinates.x][coordinateToCheck].threatValue += calculateThreat(pieceToMove)/3;
            } else {
                boardState[pieceToMove.coordinates.x][coordinateToCheck].threatValue -= calculateThreat(pieceToMove)/3;
            }
            coordinateToCheck++;
        }
        coordinateToCheck = pieceToMove.coordinates.x - 1;
        encounteredPiece = false;
        // check left
        while(coordinateToCheck >= 0 && !encounteredPiece){
            // add move if not encountered a piece
            moves.add(new Point(coordinateToCheck,pieceToMove.coordinates.y));

            if (boardState[coordinateToCheck][pieceToMove.coordinates.y].currentPiece!=null) encounteredPiece = true;
            coordinateToCheck--;
        }

        while (coordinateToCheck >= 0){
            boardState[coordinateToCheck][pieceToMove.coordinates.y].isContested = true;
            if (pieceToMove.isWhite){
                boardState[coordinateToCheck][pieceToMove.coordinates.y].threatValue += calculateThreat(pieceToMove)/3;
            } else {
                boardState[coordinateToCheck][pieceToMove.coordinates.y].threatValue -= calculateThreat(pieceToMove)/3;
            }
            coordinateToCheck--;
        }
        coordinateToCheck = pieceToMove.coordinates.x + 1;
        encounteredPiece = false;
        // check right
        while(coordinateToCheck <= 7 && !encounteredPiece){
            // add move if not encountered a piece
            moves.add(new Point(coordinateToCheck,pieceToMove.coordinates.y));

            if (boardState[coordinateToCheck][pieceToMove.coordinates.y].currentPiece!=null) encounteredPiece = true;
            coordinateToCheck++;
        }
        while (coordinateToCheck <= 7){
            boardState[coordinateToCheck][pieceToMove.coordinates.y].isContested = true;
            if (pieceToMove.isWhite){
                boardState[coordinateToCheck][pieceToMove.coordinates.y].threatValue += calculateThreat(pieceToMove)/3;
            } else {
                boardState[coordinateToCheck][pieceToMove.coordinates.y].threatValue -= calculateThreat(pieceToMove)/3;
            }
            coordinateToCheck++;
        }
        return moves;
    }

    // helper method for diagonal moves
    public static LinkedList<Point> addDiagonal(Piece pieceToMove,Square[][] boardState,LinkedList<Point> moves){
        int coordinateToCheckY = pieceToMove.coordinates.y + 1;
        int coordinateToCheckX = pieceToMove.coordinates.x - 1;
        boolean encounteredPiece = false;
        // check down left
        while(coordinateToCheckX >= 0 && coordinateToCheckY <= 7 && !encounteredPiece){
            // add move if not encountered a piece
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
            if (boardState[coordinateToCheckX][coordinateToCheckY].currentPiece!=null) encounteredPiece = true;
            coordinateToCheckX--;
            coordinateToCheckY++;
        }

        while (coordinateToCheckX >= 0 && coordinateToCheckY <= 7){
            boardState[coordinateToCheckX][coordinateToCheckY].isContested = true;
            if (pieceToMove.isWhite){
                boardState[coordinateToCheckX][coordinateToCheckY].threatValue += calculateThreat(pieceToMove)/3;
            } else {
                boardState[coordinateToCheckX][coordinateToCheckY].threatValue -= calculateThreat(pieceToMove)/3;
            }
            coordinateToCheckX--;
            coordinateToCheckY++;
        }

        coordinateToCheckY = pieceToMove.coordinates.y + 1;
        coordinateToCheckX = pieceToMove.coordinates.x + 1;
        encounteredPiece = false;
        // check down right
        while(coordinateToCheckX <= 7 && coordinateToCheckY <= 7 && !encounteredPiece){
            // add move if not encountered a piece
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
            if (boardState[coordinateToCheckX][coordinateToCheckY].currentPiece!=null) encounteredPiece = true;
            coordinateToCheckX++;
            coordinateToCheckY++;
        }

        while (coordinateToCheckX <= 7 && coordinateToCheckY <= 7){
            boardState[coordinateToCheckX][coordinateToCheckY].isContested = true;
            if (pieceToMove.isWhite){
                boardState[coordinateToCheckX][coordinateToCheckY].threatValue += calculateThreat(pieceToMove)/3;
            } else {
                boardState[coordinateToCheckX][coordinateToCheckY].threatValue -= calculateThreat(pieceToMove)/3;
            }
            coordinateToCheckX++;
            coordinateToCheckY++;
        }

        coordinateToCheckY = pieceToMove.coordinates.y - 1;
        coordinateToCheckX = pieceToMove.coordinates.x - 1;
        encounteredPiece = false;
        // check up left
        while(coordinateToCheckX >= 0 && coordinateToCheckY >= 0 && !encounteredPiece){
            // add move if not encountered a piece
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
            if (boardState[coordinateToCheckX][coordinateToCheckY].currentPiece!=null) encounteredPiece = true;
            coordinateToCheckX--;
            coordinateToCheckY--;
        }

        while (coordinateToCheckX >= 0 && coordinateToCheckY >= 0){
            boardState[coordinateToCheckX][coordinateToCheckY].isContested = true;
            if (pieceToMove.isWhite){
                boardState[coordinateToCheckX][coordinateToCheckY].threatValue += calculateThreat(pieceToMove)/3;
            } else {
                boardState[coordinateToCheckX][coordinateToCheckY].threatValue -= calculateThreat(pieceToMove)/3;
            }
            coordinateToCheckX--;
            coordinateToCheckY--;
        }
        coordinateToCheckY = pieceToMove.coordinates.y - 1;
        coordinateToCheckX = pieceToMove.coordinates.x + 1;
        encounteredPiece = false;
        // check up right
        while(coordinateToCheckX <= 7 && coordinateToCheckY >= 0 && !encounteredPiece){
            // add move if not encountered a piece
            moves.add(new Point(coordinateToCheckX, coordinateToCheckY));
            if (boardState[coordinateToCheckX][coordinateToCheckY].currentPiece!=null) encounteredPiece = true;
            coordinateToCheckX++;
            coordinateToCheckY--;
        }

        while (coordinateToCheckX <= 7 && coordinateToCheckY >= 0){
            boardState[coordinateToCheckX][coordinateToCheckY].isContested = true;
            if (pieceToMove.isWhite){
                boardState[coordinateToCheckX][coordinateToCheckY].threatValue += calculateThreat(pieceToMove)/3;
            } else {
                boardState[coordinateToCheckX][coordinateToCheckY].threatValue -= calculateThreat(pieceToMove)/3;
            }
            coordinateToCheckX++;
            coordinateToCheckY--;
        }
        return moves;
    }

    // helper method to return the index value of each piece in the image array
    public static int getIndex(Piece p){
        int ind = 0;
        if(p.name.equalsIgnoreCase("queen")){
            ind=1;
        }
        if(p.name.equalsIgnoreCase("bishop")){
            ind=2;
        }
        if(p.name.equalsIgnoreCase("knight")){
            ind=3;
        }
        if(p.name.equalsIgnoreCase("rook")){
            ind=4;
        }
        if(p.name.equalsIgnoreCase("pawn")){
            ind=5;
        }
        if(!p.isWhite){
            ind+=6;
        }
        return ind;
    }

    // method to reset the state of the board pieces
    public static void clearBoardState(Square[][] boardState){
        for (int y = 0; y < 8; y++){
            for (int x = 0; x < 8; x++){
                boardState[x][y].currentPiece = null;
            }
        }
    }

    // method to reset the threat values of the board state
    public static void clearBoardStateThreat(Square[][] boardState){
        for (int y = 0; y < 8; y++){
            for (int x = 0; x < 8; x++){
                boardState[x][y].isContested = false;
                boardState[x][y].threatValue = 0;
            }
        }
    }

    // method to add pieces to the board and piece palette
    public static void initializeBoard(ConcurrentHashMap<Point,Piece> boardPieces,HashMap<Point,Piece> piecePalette){
        // Add black pieces to board
        Piece startBrook = new Piece(new Point(0,0),false,"rook");
        boardPieces.put(startBrook.coordinates,startBrook);
        Piece startBrook2 = new Piece(new Point(7,0),false,"rook");
        boardPieces.put(startBrook2.coordinates,startBrook2);
        Piece startBknight = new Piece(new Point(1,0),false,"knight");
        boardPieces.put(startBknight.coordinates,startBknight);
        Piece startBknight2 = new Piece(new Point(6,0),false,"knight");
        boardPieces.put(startBknight2.coordinates,startBknight2);
        Piece startBbishop = new Piece(new Point(2,0),false,"bishop");
        boardPieces.put(startBbishop.coordinates,startBbishop);
        Piece startBbishop2 = new Piece(new Point(5,0),false,"bishop");
        boardPieces.put(startBbishop2.coordinates,startBbishop2);
        Piece startBqueen = new Piece(new Point(3,0),false,"queen");
        boardPieces.put(startBqueen.coordinates,startBqueen);
        Piece startBking = new Piece(new Point(4,0),false,"king");
        boardPieces.put(startBking.coordinates,startBking);
        Piece startBpawn = new Piece(new Point(0,1),false,"pawn");
        boardPieces.put(startBpawn.coordinates,startBpawn);
        Piece startBpawn2 = new Piece(new Point(1,1),false,"pawn");
        boardPieces.put(startBpawn2.coordinates,startBpawn2);
        Piece startBpawn3 = new Piece(new Point(2,1),false,"pawn");
        boardPieces.put(startBpawn3.coordinates,startBpawn3);
        Piece startBpawn4 = new Piece(new Point(3,1),false,"pawn");
        boardPieces.put(startBpawn4.coordinates,startBpawn4);
        Piece startBpawn5 = new Piece(new Point(4,1),false,"pawn");
        boardPieces.put(startBpawn5.coordinates,startBpawn5);
        Piece startBpawn6 = new Piece(new Point(5,1),false,"pawn");
        boardPieces.put(startBpawn6.coordinates,startBpawn6);
        Piece startBpawn7 = new Piece(new Point(6,1),false,"pawn");
        boardPieces.put(startBpawn7.coordinates,startBpawn7);
        Piece startBpawn8 = new Piece(new Point(7,1),false,"pawn");
        boardPieces.put(startBpawn8.coordinates,startBpawn8);

        // add white pieces to board
        Piece startWrook = new Piece(new Point(0,7),true,"rook");
        boardPieces.put(startWrook.coordinates,startWrook);
        Piece startWrook2 = new Piece(new Point(7,7),true,"rook");
        boardPieces.put(startWrook2.coordinates,startWrook2);
        Piece startWknight = new Piece(new Point(1,7),true,"knight");
        boardPieces.put(startWknight.coordinates,startWknight);
        Piece startWknight2 = new Piece(new Point(6,7),true,"knight");
        boardPieces.put(startWknight2.coordinates,startWknight2);
        Piece startWbishop = new Piece(new Point(2,7),true,"bishop");
        boardPieces.put(startWbishop.coordinates,startWbishop);
        Piece startWbishop2 = new Piece(new Point(5,7),true,"bishop");
        boardPieces.put(startWbishop2.coordinates,startWbishop2);
        Piece startWqueen = new Piece(new Point(3,7),true,"queen");
        boardPieces.put(startWqueen.coordinates,startWqueen);
        Piece startWking = new Piece(new Point(4,7),true,"king");
        boardPieces.put(startWking.coordinates,startWking);
        Piece startWpawn = new Piece(new Point(0,6),true,"pawn");
        boardPieces.put(startWpawn.coordinates,startWpawn);
        Piece startWpawn2 = new Piece(new Point(1,6),true,"pawn");
        boardPieces.put(startWpawn2.coordinates,startWpawn2);
        Piece startWpawn3 = new Piece(new Point(2,6),true,"pawn");
        boardPieces.put(startWpawn3.coordinates,startWpawn3);
        Piece startWpawn4 = new Piece(new Point(3,6),true,"pawn");
        boardPieces.put(startWpawn4.coordinates,startWpawn4);
        Piece startWpawn5 = new Piece(new Point(4,6),true,"pawn");
        boardPieces.put(startWpawn5.coordinates,startWpawn5);
        Piece startWpawn6 = new Piece(new Point(5,6),true,"pawn");
        boardPieces.put(startWpawn6.coordinates,startWpawn6);
        Piece startWpawn7 = new Piece(new Point(6,6),true,"pawn");
        boardPieces.put(startWpawn7.coordinates,startWpawn7);
        Piece startWpawn8 = new Piece(new Point(7,6),true,"pawn");
        boardPieces.put(startWpawn8.coordinates,startWpawn8);

        // add black pieces to palette
        Piece brook=new Piece(new Point(9,0), false, "rook");
        piecePalette.put(brook.coordinates,brook);
        Piece bknight=new Piece(new Point(9,1), false, "knight");
        piecePalette.put(bknight.coordinates,bknight);
        Piece bbishop=new Piece(new Point(9,2), false, "bishop");
        piecePalette.put(bbishop.coordinates,bbishop);
        Piece bqueen=new Piece(new Point(9,3), false, "queen");
        piecePalette.put(bqueen.coordinates,bqueen);
        Piece bking=new Piece(new Point(9,4), false, "king");
        piecePalette.put(bking.coordinates,bking);
        Piece bpawn=new Piece(new Point(9,5), false, "pawn");
        piecePalette.put(bpawn.coordinates,bpawn);

        // add white pieces to palette
        Piece wrook=new Piece(new Point(8,0), true, "rook");
        piecePalette.put(wrook.coordinates,wrook);
        Piece wknight=new Piece(new Point(8,1), true, "knight");
        piecePalette.put(wknight.coordinates,wknight);
        Piece wbishop=new Piece(new Point(8,2), true, "bishop");
        piecePalette.put(wbishop.coordinates,wbishop);
        Piece wqueen=new Piece(new Point(8,3), true, "queen");
        piecePalette.put(wqueen.coordinates,wqueen);
        Piece wking=new Piece(new Point(8,4), true, "king");
        piecePalette.put(wking.coordinates,wking);
        Piece wpawn=new Piece(new Point(8,5), true, "pawn");
        piecePalette.put(wpawn.coordinates,wpawn);
    }

}


