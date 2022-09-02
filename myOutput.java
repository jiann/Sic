public class myOutput {
    int locctr;
    String label;
    String opCode;
    String value;
    int instruction;
    String comment;
    int instructionLength;
    addressingMode addressingMode;
    relativeMode relativeMode;
    boolean indexMode = false;
    boolean extendMode = false;
    public myOutput() {
    }


    @Override
    public String toString() {
        return ( Integer.toHexString(locctr).toUpperCase() )+" "+
                label  +" " +
                opCode +" " +
                value  +" " +
                ( Integer.toHexString(instruction).toUpperCase() )
                ;

    }
}