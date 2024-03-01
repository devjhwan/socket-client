package p1.client;

import utils.ComUtils;

import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class GameClient {
    /*
    TO DO.
    Class that encapsulates the game's logic. Sequence of states following the established protocol .
     */
    private ComUtils comUtils;
    private Scanner sc;
    private int idSessio;
    private String userName;
    private char empty;
    private char player;
    private char system;
    
    public GameClient(ComUtils comUtils){
        this.comUtils = comUtils;
        sc = new Scanner(System.in);
        empty = ' ';
        player = 'O';
        system = 'X';
        createGameSession();
        //Print session information
        System.out.println("Your Session info");
        System.out.println("Session id: " + this.idSessio);
        System.out.println("Name: " + this.userName);
        initGameTicTakToe();
    }

    private void createGameSession() {
        System.out.print("Write your name: ");
        this.userName = sc.nextLine();
        this.idSessio = new Random().nextInt(10000, 99999);
    }

    private void initGameTicTakToe() {
        try {
            sendHello();
            if (waitReady() == false)
                return ;
            System.out.println("Ready to play");
            sendPlay();
            if (waitAdmit() == false)
                return ;
            startPlay();
        } catch (IOException e){
            System.err.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void startPlay() throws IOException, IllegalArgumentException {
        boolean playing = true;
        byte win = 0;
        char board[][] = new char[3][3];

        for(int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                board[i][j] = ' ';
        printBoard(board);
        
        while (playing) {
            boolean validAction = false;
            String action = "";
            byte errCode[] = {-1};

            while (!validAction) {
                action = selectAction(board);
                validAction = checkAction(action, board, errCode);
            }
            setAction(action, board, true);
            printBoard(board);
            sendAction(action);
            validAction = false;
            while (!validAction && playing) {
                byte opcode = comUtils.readByte();
                if (opcode == 5) {
                    action = getAction();
                    validAction = checkAction(action, board, errCode);
                    if (!validAction) {
                        sendError(this.idSessio, errCode[0]);
                        continue ;
                    }
                    setAction(action, board, false);
                    printBoard(board);
                }
                else if (opcode == 6) {
                    win = getResult();
                    playing = false;
                } else if (opcode == 8) {
                    readError();
                    return ;
                } else {
                    throw new IllegalArgumentException
                                ("Expected 5, 6 or 8 but found " + opcode);
                }
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

    private void sendHello() throws IOException {
        //|opcode(1)|idSessio|userName   |00|
        //|byte     |int     |stringVariable|
        //|1 byte   |4 bytes |n + 2 bytes   |
        comUtils.writeByte((byte)1);
        comUtils.write_int32(this.idSessio);
        comUtils.writeStringVariable(this.userName);
    }

    private boolean waitReady() throws IOException, IllegalArgumentException {
        byte opcode = comUtils.readByte();
        if (opcode == 8) {
            int idSessio = comUtils.read_int32();
            byte errCode = comUtils.readByte();
            String errMsg = comUtils.readStringVariable();
            printError(idSessio, errCode, errMsg);
            return false;
        }
        if (opcode != 2)
            throw new IllegalArgumentException
                        ("Expected 2 (READY) but found " + opcode);
        int idSessio = comUtils.read_int32();
        if (this.idSessio != idSessio) {
            sendError(this.idSessio, (byte)9);
            throw new IllegalArgumentException
                        ("Expected " + this.idSessio + " but found " + idSessio);
        }
        return true;
    }

    private void sendPlay() throws IOException {
        //|opcode(3)|idSessio|
        //|byte     |int     |
        //|1 byte   |4 bytes |
        comUtils.writeByte((byte)3);
        comUtils.write_int32(this.idSessio);
    }

    private boolean waitAdmit() throws IOException, IllegalArgumentException {
        byte opcode = comUtils.readByte();
        if (opcode == 8) {
            int idSessio = comUtils.read_int32();
            byte errCode = comUtils.readByte();
            String errMsg = comUtils.readStringVariable();
            printError(idSessio, errCode, errMsg);
            return false;
        }
        if (opcode != 4)
            throw new IllegalArgumentException
                        ("Expected 4 (ADMIT) but found " + opcode);
        int idSessio = comUtils.read_int32();
        if (this.idSessio != idSessio) {
            sendError(this.idSessio, (byte)9);
            throw new IllegalArgumentException
                        ("Expected " + this.idSessio + " but found " + idSessio);
        }
        byte flag = comUtils.readByte();
        if (flag == 0)
            return false;
        else if (flag == 1)
            return true;
        else
            throw new IllegalArgumentException
                        ("Expected 0 or 1 but found " + flag);
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

    private void sendAction(String action) throws IOException {
        //|opcode(5)|idSessio|posicio       |
        //|byte     |int     |stringVariable|
        //|1 byte   |4 bytes |n + 2 bytes   |
        comUtils.writeByte((byte)5);
        comUtils.write_int32(this.idSessio);
        comUtils.writeStringVariable(action);
    }

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

    private byte getResult() throws IOException, IllegalArgumentException {
        int idSessio = comUtils.read_int32();
        if (this.idSessio != idSessio) {
            sendError(this.idSessio, (byte)9);
            throw new IllegalArgumentException
                        ("Expected " + this.idSessio + " but found " + idSessio);
        }
        String action = comUtils.readStringVariable();
        if (!action.equals("END"))
            throw new IllegalArgumentException
                    ("Expected END but found " + action);
        byte flag = comUtils.readByte();
        if (flag < 0 || flag > 2)
            throw new IllegalArgumentException
                    ("Expected value between 0~2 but found " + flag);
        return flag;
    }

    private void readError() throws IOException{
        int idSessio = comUtils.read_int32();
        byte errCode = comUtils.readByte();
        String errMsg = comUtils.readStringVariable();

        printError(idSessio, errCode, errMsg);
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
