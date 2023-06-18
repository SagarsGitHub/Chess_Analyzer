# Chess_Analyzer
Basic chess app that analyzes the board state of a chess game and colors the squares based on the threat level from both players


# Basic Usage
This project can be compiled and run in any Java environment.

## Starting Board State
The application initializes with a standard chess starting position. The pieces on the board can be moved and placed anywhere on the board. Additionally, pieces can be added to the board from the piece palette on the right side of the application GUI.

![](https://github.com/SagarsGitHub/Chess_Analyzer/blob/master/src/ReadME%20Documentation/Starting%20Board%20State.png)

*This is the starting state of the board after starting the application.*

The application works by totalling each piece's control of each square of the board. The amount of control depends on the value of the piece controlling it. A piece in the way of another one will affect the amount of control on squares behind the piece in the way.

Squares under white's control will be blue, red for black, and purple for an even split. **Squares under more control will be a brighter color.**

![](https://github.com/SagarsGitHub/Chess_Analyzer/blob/master/src/ReadME%20Documentation/Blue%20Control.png)

![](https://github.com/SagarsGitHub/Chess_Analyzer/blob/master/src/ReadME%20Documentation/Red%20Control.png)

![](https://github.com/SagarsGitHub/Chess_Analyzer/blob/master/src/ReadME%20Documentation/Contested%20Control.png)

## Running the Application

Once the desired board state to analyze has been reached, press the power button to run the analyzer.

![](https://github.com/SagarsGitHub/Chess_Analyzer/blob/master/src/ReadME%20Documentation/Average%20Board%20State.png)

This program is meant to give a more qualitative analysis of the board state than t he standard chess engine. The results of the analysis will not give any specific hints as to what the next best move is, but the threat levels of each square might let a user know where to focus their efforts.

Hope this program helps make your next chess game a more interesting one!
