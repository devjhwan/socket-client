package utils;

public class Board {
    private char board[][];
    private char empty;
    private char player;
    private char system;
    private byte errCode;

    public Board() {
        board = new char[3][3];
        for(int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                board[i][j] = ' ';
        empty = ' ';
        player = 'O';
        system = 'X';
    }

    public byte getErrCode() {
        return this.errCode;
    }

    public boolean checkAction(String action) {
        int i, j;
        
        if (action.length() != 3) {
            errCode = 0;
            return false;
        }
        i = action.charAt(0) - '0';
        j = action.charAt(2) - '0';
        if (i < 0 || i > 2 || j < 0 || j > 2) {
            errCode = 0;
            return false;
        }
        if (board[i][j] != this.empty) {
            errCode = 1;
            return false;
        }
        return true;
    }

    public void setAction(String action, boolean player) {
        int i, j;

        i = action.charAt(0) - '0';
        j = action.charAt(2) - '0';
        if (player)
            board[i][j] = this.player;
        else
            board[i][j] = this.system;
    }

    public boolean checkWin(String action) {
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

    public String autoSelectAction() {
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

    public void printBoard() {
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
