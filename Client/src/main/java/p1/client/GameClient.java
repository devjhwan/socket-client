package p1.client;

import utils.ComUtils;
import utils.ComErr;
import utils.Board;

import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class GameClient {
    /*
    TO DO.
    Class that encapsulates the game's logic. Sequence of states following the established protocol .
     */
    private ComUtils comUtils;
    private ComErr comErr;
    private Board board;
    private Scanner sc;
    private int idSessio;
    private String userName;
    
    public GameClient(ComUtils comUtils){
        this.comUtils = comUtils;
        this.comErr = new ComErr(comUtils);
        this.board = new Board();
        sc = new Scanner(System.in);
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
        board.printBoard();
        
        while (playing) {
            boolean validAction = false;
            String action = "";
            byte errCode[] = {-1};

            while (!validAction) {
                action = board.autoSelectAction();
                validAction = board.checkAction(action);
            }
            board.setAction(action, true);
            board.printBoard();
            sendAction(action);
            validAction = false;
            while (!validAction && playing) {
                byte opcode = comUtils.readByte();
                if (opcode == 5) {
                    action = readAction();
                    validAction = board.checkAction(action);
                    if (!validAction) {
                        comErr.sendError(this.idSessio, errCode[0]);
                        continue ;
                    }
                    board.setAction(action, false);
                    board.printBoard();
                }
                else if (opcode == 6) {
                    win = readResult();
                    playing = false;
                } else if (opcode == 8) {
                    String errMsg = this.comErr.readError();
                    System.err.println(errMsg);
                    return ;
                } else {
                    throw new IllegalArgumentException
                                ("Expected 5, 6 or 8 but found " + opcode);
                }
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
            String errMsg = this.comErr.readError();
            System.err.println(errMsg);
            return false;
        }
        if (opcode != 2) {
            comErr.sendError(this.idSessio, (byte)8);
            throw new IllegalArgumentException
                        ("Expected 2 (READY) but found " + opcode);
        }
        int idSessio = comUtils.read_int32();
        if (this.idSessio != idSessio) {
            comErr.sendError(this.idSessio, (byte)9);
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
            String errMsg = this.comErr.readError();
            System.err.println(errMsg);
            return false;
        }
        if (opcode != 4){
            comErr.sendError(this.idSessio, (byte)8);
            throw new IllegalArgumentException
                        ("Expected 4 (ADMIT) but found " + opcode);
        }
        int idSessio = comUtils.read_int32();
        if (this.idSessio != idSessio) {
            comErr.sendError(this.idSessio, (byte)9);
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

    private void sendAction(String action) throws IOException {
        //|opcode(5)|idSessio|posicio       |
        //|byte     |int     |stringVariable|
        //|1 byte   |4 bytes |n + 2 bytes   |
        comUtils.writeByte((byte)5);
        comUtils.write_int32(this.idSessio);
        comUtils.writeStringVariable(action);
    }

    private String readAction() throws IOException, IllegalArgumentException {
        int idSessio = comUtils.read_int32();
        if (this.idSessio != idSessio) {
            comErr.sendError(this.idSessio, (byte)9);
            throw new IllegalArgumentException
                        ("Expected " + this.idSessio + " but found " + idSessio);
        }
        String action = comUtils.readStringVariable();
        return action;
    }

    private byte readResult() throws IOException, IllegalArgumentException {
        int idSessio = comUtils.read_int32();
        if (this.idSessio != idSessio) {
            comErr.sendError(this.idSessio, (byte)9);
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
}
