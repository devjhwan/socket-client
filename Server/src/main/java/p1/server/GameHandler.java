package p1.server;
import java.io.IOException;
import java.net.Socket;

import utils.ComUtils;
import utils.ComErr;
import utils.Board;

public class GameHandler extends Thread {

    /*
    TO DO
    Protocol dynamics from Server.
    Methods: run(), init(), play().
     */
    private ComUtils comUtils;
    private ComErr comErr;
    private Socket socket;
    private Board board;
    private int idSessio;
    private String userName;

    public GameHandler(ComUtils comUtils, Socket socket) {
        this.comUtils = comUtils;
        this.comErr = new ComErr(comUtils);
        this.socket = socket;
        this.board = new Board();
    }

    public void run() {
        System.out.println("GameHandler started");
        try {
            init();
            play();
        } catch (IOException e) {
            System.out.println("Desconectem el socket");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void init() throws IOException, IllegalArgumentException{
        waitHello();
        //print user info
        System.out.println("Session id: " + this.idSessio);
        System.out.println("User name: " + this.userName);
        sendReady();
        System.out.println("Asking for player to ready");
        waitPlay();
        sendAdmit(true);
        System.out.println("Player admited");
    }

    private void play() throws IOException, IllegalArgumentException {
        boolean playing = true;
        byte win = 0;
        int actionCount = 0;

        board.printBoard();

        while (playing) {
            boolean validAction = false;
            String action = "";
            byte errCode[] = {-1};

            while (!validAction && playing) {
                byte opcode = comUtils.readByte();
                if (opcode == 5) {
                    action = readAction();
                    validAction = board.checkAction(action);
                    if (!validAction) {
                        this.comErr.sendError(this.idSessio, errCode[0]);
                        continue ;
                    }
                    board.setAction(action, true);
                    board.printBoard();
                    actionCount++;
                    if (board.checkWin(action)) {
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
                    String errMsg = this.comErr.readError();
                    System.err.println(errMsg);
                    return ;
                } else {
                    comErr.sendError(this.idSessio, (byte)8);
                    throw new IllegalArgumentException
                                ("Expected 5 or 8 but found " + opcode);
                }
            }
            if (!playing)
                break ;
            validAction = false;
            while (!validAction) {
                action = board.autoSelectAction();
                validAction = board.checkAction(action);
            }
            board.setAction(action, false);
            board.printBoard();
            actionCount++;
            if (board.checkWin(action)) {
                win = 0;
                sendResult(win);
                playing = false;
                break ;
            }
            else {
                sendAction(action);
            }
        }
        board.printBoard();
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
            String errMsg = this.comErr.readError();
            throw new IllegalArgumentException(errMsg);
        }
        if (opcode != 1) {
            this.comErr.sendError(this.idSessio, (byte)8);
            throw new IllegalArgumentException 
                        ("Expected 1 (HELLO) but found " + opcode);
        }
        
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
            String errMsg = this.comErr.readError();
            throw new IllegalArgumentException(errMsg);
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

    //GamePlay Logic

    private String readAction() throws IOException, IllegalArgumentException {
        int idSessio = comUtils.read_int32();
        if (this.idSessio != idSessio) {
            this.comErr.sendError(this.idSessio, (byte)9);
            throw new IllegalArgumentException
                        ("Expected " + this.idSessio + " but found " + idSessio);
        }
        String action = comUtils.readStringVariable();
        return action;
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
}
