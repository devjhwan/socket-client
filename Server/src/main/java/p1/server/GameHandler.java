package p1.server;
import java.io.IOException;

import utils.ComUtils;

public class GameHandler {

    /*
    TO DO
    Protocol dynamics from Server.
    Methods: run(), init(), play().
     */
    private ComUtils comUtils;
    private int idSessio;
    private String userName;
    private char empty;
    private char player;
    private char system;

    public GameHandler(ComUtils comUtils) {
        this.comUtils = comUtils;
        empty = ' ';
        player = 'O';
        system = 'X';
    }

    public void start() {
        System.out.println("GameHandler started");
        try {
            waitHello();
            //print user info
            System.out.println("Session id: " + this.idSessio);
            System.out.println("User name: " + this.userName);
            sendReady();
            System.out.println("Asking for player to ready");
            waitPlay();
            sendAdmit(true);
            System.out.println("Player admited");
            play();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void play() throws IOException, IllegalArgumentException {
        boolean playing = true;
        byte win = 0;
        char board[][] = new char[3][3];
        int actionCount = 0;

        for(int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                board[i][j] = ' ';
        printBoard(board);

        while (playing) {
            boolean validAction = false;
            String action = "";
            byte errCode[] = {-1};

            while (!validAction && playing) {
                byte opcode = comUtils.readByte();
                if (opcode == 5) {
                    action = getAction();
                    validAction = checkAction(action, board, errCode);
                    if (!validAction) {
                        sendError(this.idSessio, errCode[0]);
                        continue ;
                    }
                    setAction(action, board, true);
                    printBoard(board);
                    actionCount++;
                    if (checkWin(action, board)) {
                        win = 1;
                        sendResult(win);
                        playing = false;
                        break ;
                    }
                    if (actionCount == 9) {
                        win = 2;
                        sendResult(win);
                        playing = false;
                        break ;
                    }
                }
                else if (opcode == 8) {
                    readError();
                    return ;
                } else {
                    throw new IllegalArgumentException
                                ("Expected 5 or 8 but found " + opcode);
                }
            }
            if (!playing)
                break ;
            validAction = false;
            while (!validAction) {
                action = selectAction(board);
                validAction = checkAction(action, board, errCode);
            }
            setAction(action, board, false);
            printBoard(board);
            actionCount++;
            if (checkWin(action, board)) {
                win = 0;
                sendResult(win);
                playing = false;
                break ;
            }
            else {
                sendAction(action);
            }
        }
        printBoard(board);
        if (win == 0) {
            System.out.println("Player has loosed the game!");
        } else if (win == 1) {
            System.out.println("Player has winned the game!");
        } else {
            System.out.println("Magnificant draw!");
        }
    }

    //Initial Protocols

    private void waitHello() throws IOException, IllegalArgumentException {
        byte opcode = comUtils.readByte();
        if (opcode == 8){
            int idSessio = comUtils.read_int32();
            byte errCode = comUtils.readByte();
            String errMsg = comUtils.readStringVariable();
            printError(idSessio, errCode, errMsg);
        }
        if (opcode != (byte)1)
            throw new IllegalArgumentException 
                        ("Expected 1 (HELLO) but found " + opcode);
        
        this.idSessio = comUtils.read_int32();
        this.userName = comUtils.readStringVariable();
    }

    private void sendReady() throws IOException {
        //|opcode(2)|idSessio|
        //|byte     |int     |
        //|1 byte   |4 byte  |
        comUtils.writeByte((byte)2);
        comUtils.write_int32(idSessio);
    }

    private void waitPlay() throws IOException, IllegalArgumentException {
        byte opcode = comUtils.readByte();
        if (opcode == 8){
            int idSessio = comUtils.read_int32();
            byte errCode = comUtils.readByte();
            String errMsg = comUtils.readStringVariable();
            printError(idSessio, errCode, errMsg);
        }
        if (opcode != (byte)3) {
            sendAdmit(false);
            throw new IllegalArgumentException
                        ("Expected 3 (PLAY) but found " + opcode);
        }
        int idSessio = comUtils.read_int32();
        if (idSessio != this.idSessio) {
            sendAdmit(false);
            throw new IllegalArgumentException
                        ("Expected idSessio " + this.idSessio + " but found " + idSessio);
        }
    }

    private void sendAdmit(boolean admit) throws IOException {
        //|opcode(4)|idSessio|flag  |
        //|byte     |int     |byte  |
        //|1 byte   |4 byte  |1 byte|
        comUtils.writeByte((byte)4);
        comUtils.write_int32(this.idSessio);
        if (admit == true)
            comUtils.writeByte((byte)1);
        else
            comUtils.writeByte((byte)0);
            
    }
   
    private void sendError(int idSessio, byte errCode) throws IOException {
        //|opcode(8)|idSessio|errCode|errMsg     |00|
        //|byte     |int     |byte   |stringVariable|
        //|1 byte   |4 bytes |1byte  |n + 2 byte    |
        comUtils.writeByte((byte)8);
        comUtils.write_int32(idSessio);
        comUtils.writeByte(errCode);
        if (errCode == 0)
        comUtils.writeStringVariable("Moviment Desconegut");
        else if (errCode == 1)
        comUtils.writeStringVariable("Moviment Invalid");
        else if (errCode == 9)
        comUtils.writeStringVariable("Sessio Incorrecte");
        else
        comUtils.writeStringVariable("Wrong errCode");
    }

    private void printError(int idSessio, byte errCode, String errMsg) 
            throws IOException, IllegalArgumentException {
        StringBuilder sb = new StringBuilder();

        sb.append("ErrCode ").append(errCode).append("\n")
        .append("Error detail:\n")
        .append("\tRegistered session id: ").append(idSessio).append('\n')
        .append("\tError message: ").append(errMsg);

        throw new IllegalArgumentException(sb.toString());
    }

    //GamePlay Logic

    private String getAction() throws IOException, IllegalArgumentException {
        int idSessio = comUtils.read_int32();
        if (this.idSessio != idSessio) {
            sendError(this.idSessio, (byte)9);
            throw new IllegalArgumentException
                        ("Expected " + this.idSessio + " but found " + idSessio);
        }
        String action = comUtils.readStringVariable();
        return action;
    }

    private boolean checkAction(String action, char[][] board, byte[] errCode) {
        int i, j;
        
        if (action.length() != 3) {
            errCode[0] = 0;
            return false;
        }
        i = action.charAt(0) - '0';
        j = action.charAt(2) - '0';
        if (i < 0 || i > 2 || j < 0 || j > 2) {
            errCode[0] = 0;
            return false;
        }
        if (board[i][j] != this.empty) {
            errCode[0] = 1;
            return false;
        }
        return true;
    }

    private void setAction(String action, char[][] board, boolean player) {
        int i, j;

        i = action.charAt(0) - '0';
        j = action.charAt(2) - '0';
        if (player)
            board[i][j] = this.player;
        else
            board[i][j] = this.system;
    }

    private void readError() throws IOException{
        int idSessio = comUtils.read_int32();
        byte errCode = comUtils.readByte();
        String errMsg = comUtils.readStringVariable();

        printError(idSessio, errCode, errMsg);
    }

    private boolean checkWin(String action, char[][] board) {
        int i, j;

        i = action.charAt(0) - '0';
        j = action.charAt(2) - '0';

        //horitzontal check
        if (board[i][0] == board[i][1] && board[i][0] == board[i][2])
            return true;
        //vertical check
        if (board[0][j] == board[1][j] && board[0][j] == board[2][j])
            return true;
        //diagonal check;
        if (i == j && board[0][0] == board[1][1] && board[0][0] == board[2][2])
            return true;
        //reverse diagonal check;
        if (i + j == 2 && board[0][2] == board[1][1] && board[0][2] == board[2][0])
            return true;
        return false;
    }

    private String selectAction(char[][] board) {
        StringBuilder action = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == this.empty) {
                    return action.append(i).append('-').append(j).toString();
                }
            }
        }
        return action.toString();
    }

    private void sendResult(byte result) throws IOException {
        //|opcode(6)|idSessio|accion        |result|
        //|byte     |int     |stringVariable|byte  |
        //|1 byte   |4 bytes |n + 2 bytes   |1 byte|
        comUtils.writeByte((byte)6);
        comUtils.write_int32(this.idSessio);
        comUtils.writeStringVariable("END");
        comUtils.writeByte(result);
    }

    private void sendAction(String action) throws IOException {
        //|opcode(5)|idSessio|posicio       |
        //|byte     |int     |stringVariable|
        //|1 byte   |4 bytes |n + 2 bytes   |
        comUtils.writeByte((byte)5);
        comUtils.write_int32(this.idSessio);
        comUtils.writeStringVariable(action);
    }

    private void printBoard(char[][] board) {
        StringBuilder sb = new StringBuilder();
        sb.append('|').append(board[0][0])
            .append('|').append(board[0][1])
            .append('|').append(board[0][2]).append('|').append('\n')
        .append('|').append(board[1][0])
            .append('|').append(board[1][1])
            .append('|').append(board[1][2]).append('|').append('\n')
        .append('|').append(board[2][0])
            .append('|').append(board[2][1])
            .append('|').append(board[2][2]).append('|').append('\n');
        System.out.println(sb.toString());
    }
}
