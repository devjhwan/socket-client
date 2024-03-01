package utils;

import java.io.IOException;

public class ComErr {
    private ComUtils comUtils;
    private int idSessio;
    private byte errCode;
    private String errMsg;
    
    public ComErr(ComUtils comUtils) {
        this.comUtils = comUtils;
    }

    public void sendError(int idSessio, byte errCode) throws IOException {
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

    public String readError() throws IOException {
        StringBuilder sb = new StringBuilder();

        this.idSessio = comUtils.read_int32();
        this.errCode = comUtils.readByte();
        this.errMsg = comUtils.readStringVariable();

        sb.append("ErrCode ").append(errCode).append("\n")
        .append("Error detail:\n")
        .append("\tRegistered session id: ").append(idSessio).append('\n')
        .append("\tError message: ").append(errMsg);
        return sb.toString();
    }
}
