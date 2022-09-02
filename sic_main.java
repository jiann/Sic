import java.io.*;
import java.util.*;

public class sic_main {
    static Map<String, ArrayList<Integer>> opTab = new HashMap<String, ArrayList<Integer>>();
    static ArrayList<myOutput> myOutputs = new ArrayList<>();
    static Map<String, Integer> symTable = new HashMap<String, Integer>();
    static Map<String, Integer> registerID = new HashMap<String, Integer>();
    static int baseRegister;
    public static void main(String[] args) {
        initialize();
        String startName = "";
        String[] line;
        try {
            Scanner scanner = new Scanner(new FileReader("input.txt"));
            line = scanner.nextLine().split("\\s+");

            int loc = 0X0000;                                       //initialize zero value
            if (line[1].equals("START")) {                          //check opcode
                startName = line[0];
                symTable.put(startName,loc);                        //generate SymbolTable
                loc += intToHexFormat(line[2]);                     //set START's value as starting address
                while(scanner.hasNext()) {
                    line = scanner.nextLine().split("\\s+");
                    if (!line[0].equals(".") && line.length>1) {
                        myOutput tempOutput = new myOutput();       //create a object to carry data
                        if(line[1].equals("END")) break;
////fill with empty, avoid exception (ex.RSUB)
                        try {
                            tempOutput.value = line[2];
                        }catch (Exception e){
                            tempOutput.value = "";
                        }
                        tempOutput.locctr = loc;
                        tempOutput.label = line[0];
                        tempOutput.opCode = line[1];
////if opCode exist in library, get length of the opcode
                        if (opTab.containsKey(tempOutput.opCode )) tempOutput.instructionLength = opTab.get(tempOutput.opCode).get(1);
///get length of opcode that not in library
                        try {
                            if (tempOutput.opCode.equals("WORD")) tempOutput.instructionLength = 3;
                            else if (tempOutput.opCode.equals("RESW"))
                                tempOutput.instructionLength = (3 * Integer.parseInt(line[2]));
                            else if (tempOutput.opCode.equals("RESB"))
                                tempOutput.instructionLength = Integer.parseInt(line[2]);
                            else if (tempOutput.opCode.equals("BYTE")) {
                                if (tempOutput.value.charAt(0) == 'C')
                                    tempOutput.instructionLength = (tempOutput.value.substring(2, line[2].length() - 1).length());
                                if (tempOutput.value.charAt(0) == 'X')
                                    tempOutput.instructionLength = (tempOutput.value.substring(2, line[2].length() - 1).length() / 2);
                            }
                        }catch (Exception e) {
                            System.out.println("error");
                        }
////check extend mode
                        if (tempOutput.opCode.contains("+")) {
                            tempOutput.opCode = tempOutput.opCode.substring(1);
                            tempOutput.extendMode = true;
                            tempOutput.instructionLength = 4;
                        }
////program-counter update
                        loc += tempOutput.instructionLength;
////check addressing mode //remove symbol << ex. @LDA to LDA >>
                            if (tempOutput.value.contains("@")) {
                                tempOutput.addressingMode = addressingMode.INDIRECT;
                                tempOutput.value = tempOutput.value.substring(1);
                            } else if (tempOutput.value.contains("#")) {
                                tempOutput.addressingMode = addressingMode.IMMEDIATE;
                                tempOutput.value = tempOutput.value.substring(1);
                            } else tempOutput.addressingMode = addressingMode.SIMPLE_XE;
////check index mode
                        try {
                            if (tempOutput.value.contains(",") && line[3].contains("X") || tempOutput.value.contains(",X")) {
                                tempOutput.value = tempOutput.value.substring(0, line[2].length() - 1);
                                tempOutput.indexMode = true;
                                if (line[3].contains("X")) line[3] = "";
                            }
                        } catch (Exception e) {
                            System.out.println("error");
                        }
//some exception
                        if (tempOutput.opCode.equals("BASE")) tempOutput.instructionLength = 0;
                        if (tempOutput.opCode.equals("COMPR")) {
                            tempOutput.value = line[2]+line[3];
                            tempOutput.addressingMode = addressingMode.SIMPLE_SIC;
                        }
////add object to object-list
                        myOutputs.add(tempOutput);
                    }
                }

            }
//------end pass1
////generate STMTAB
            for (int k=0; k<myOutputs.size(); k++) {
                if (!myOutputs.get(k).label.isEmpty())
                    if (!symTable.containsKey(myOutputs.get(k).label))  //check duplicate
                        symTable.put(myOutputs.get(k).label,myOutputs.get(k).locctr);
            }
////generate instruct code
            for (int i = 0; i < myOutputs.size(); i++)
                generateInstructCode(myOutputs.get(i));
////get total length of program
            int programLength = myOutputs.get(myOutputs.size()-1).locctr+myOutputs.get(myOutputs.size()-1).instructionLength;
////output to text file
            File outfile = new File("output.txt");
            generateOutput(myOutputs,outfile,startName,programLength);
            for (int i = 0; i < myOutputs.size(); i++) {
                System.out.println(myOutputs.get(i).toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void initialize(){
////OP Table (opcode, machine code, format(length) ) //register (name, id)
        String[] op_TAB = { "ADD", "ADDF", "ADDR", "AND", "CLEAR", "COMP", "COMPF", "COMPR", "DIV", "DIVF", "DIVR",
                "FIX", "FLOAT", "HIO", "J", "JEQ", "JGT", "JLT", "JSUB", "LDA", "LDB", "LDCH", "LDF", "LDL", "LDS",
                "LDT", "LDX", "LPS", "MUL", "MULF", "MULR", "NORM", "OR", "RD", "RMO", "RSUB", "SHIFTL", "SHIFTR",
                "SIO", "SSK", "STA", "STB", "STCH", "STF", "STI", "STL", "STS", "STSW", "STT", "STX", "SUB", "SUBF",
                "SUBR", "SVC", "TD", "TIO", "TIX", "TIXR", "WD" };
        int[] opCode = { 0X18, 0X58, 0X90, 0X40, 0XB4, 0X28, 0X88, 0XA0, 0X24, 0X64, 0X9C, 0XC4, 0XC0, 0XF4, 0X3C,
                0X30, 0X34, 0X38, 0X48, 0X00, 0X68, 0X50, 0X70, 0X08, 0X6C, 0X74, 0X04, 0XE0, 0X20, 0X60, 0X98, 0XC8,
                0X44, 0XD8, 0XAC, 0X4C, 0XA4, 0XA8, 0XF0, 0XEC, 0X0C, 0X78, 0X54, 0X80, 0XD4, 0X14, 0X7C, 0XE8, 0X84,
                0X10, 0X1C, 0X5C, 0X94, 0XB0, 0XE0, 0XF8, 0X2C, 0XB8, 0XDC };
        int[] format = {3, 3, 2, 3, 2, 3, 3, 2, 3, 3, 2, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
               3,3, 3, 3, 3, 2, 1, 3, 3, 2, 3, 2, 2, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 3, 1, 3, 2, 3};
////shift value to instruction code format
        int opCodeShift = 0;
        for (int i = 0; i < op_TAB.length; i++) {
            switch (format[i]) {
                case 1:
                    opCodeShift = opCode[i]; break;
                case 2:
                    opCodeShift = opCode[i]<<8; break;
                case 3:
                    opCodeShift = opCode[i]<<16; break;
            }
            ArrayList<Integer> temp = new ArrayList<>(Arrays.asList(opCodeShift,format[i]));
            opTab.put(op_TAB[i],temp);
        }
        String[] register = {"A","X","L","PC","SW","B","S","T","F"};
        int[] id = {0,1,2,8,9,3,4,5,6};
        for (int i = 0; i < register.length; i++)
            registerID.put(register[i],id[i]);
    }
    public static int intToHexFormat(String value) {
////translate string to hex value (same value) << ex. string(10) ~> hex(10) but not hex(A) >>
        int returnValue = 0X0000;
        switch (value.length()) {
            case 1:
                returnValue += (Integer.parseInt(String.valueOf(value.charAt(0)))* 0X0001);
                break;
            case 2:
                returnValue += (Integer.parseInt(String.valueOf(value.charAt(0))) * 0X0010);
                returnValue += (Integer.parseInt(String.valueOf(value.charAt(1))) * 0X0001);
                break;
            case 3:
                returnValue += (Integer.parseInt(String.valueOf(value.charAt(0))) * 0X0100);
                returnValue += (Integer.parseInt(String.valueOf(value.charAt(1))) * 0X0010);
                returnValue += (Integer.parseInt(String.valueOf(value.charAt(2))) * 0X0001);
                break;
            case 4:
                returnValue += (Integer.parseInt(String.valueOf(value.charAt(0))) * 0X1000);
                returnValue += (Integer.parseInt(String.valueOf(value.charAt(1))) * 0X0100);
                returnValue += (Integer.parseInt(String.valueOf(value.charAt(2))) * 0X0010);
                returnValue += (Integer.parseInt(String.valueOf(value.charAt(3))) * 0X0001);
                break;
        }
        return returnValue;
    }
    public static void generateInstructCode(myOutput obj) {
////generate instruction code
        int instructCode = 0x0000;
        obj.relativeMode = relativeMode.NON;
        switch (obj.opCode) {
            case "BASE" -> {
                baseRegister = symTable.get(obj.value);
                obj.instruction = -1;
            }
            case "COMPR" -> {
                String[] tempValue = obj.value.split(",");
                obj.instruction = opTab.get(obj.opCode).get(0) + (registerID.get(tempValue[0]) << 4) + registerID.get(tempValue[1]);
            }
            case "RSUB" -> {
                obj.instruction = 0X4F0000;
            }
            case "CLEAR", "TIXR" -> {
                obj.instruction = opTab.get(obj.opCode).get(0) + (registerID.get(obj.value)<<4);
            }
            case "BYTE" -> {
                if (obj.value.charAt(0) == 'C') {
                    char[] chars = obj.value.substring(2, obj.value.length() - 1).toCharArray();
                    instructCode += chars[0];
                    for (int i = 1; i < chars.length; i++) {
                        instructCode = instructCode << 8;
                        instructCode += chars[i];
                    }
                } else if (obj.value.charAt(0) == 'X') {
                    instructCode = Integer.parseInt(obj.value.substring(2, obj.value.length() - 1), 16);
                }
                obj.instruction = instructCode;
            }
            case "WORD" -> {
                obj.instruction = Integer.parseInt(obj.value);
            }
            case "RESW", "RESB" -> obj.instruction = 0;
            default -> {
                if (opTab.containsKey(obj.opCode)) {
                    obj.instruction += opTab.get(obj.opCode).get(0);
                    ///ni
                    switch (obj.addressingMode) {
                        case IMMEDIATE ->   obj.instruction += 0x10000;
                        case INDIRECT ->    obj.instruction += 0x20000;
                        case SIMPLE_XE ->   obj.instruction += 0x30000; //add 0 in SIMPLE_SIC
                    }
                    ///x
                    if (obj.indexMode)  obj.instruction += 0x8000;
                    ///e
                    if(obj.extendMode) {
                        obj.instruction += 0x1000;
                        obj.instruction = obj.instruction << 8;
                    }
                    ///bp and disp
                    int disp = 0;
                    if (symTable.containsKey(obj.value)) {
                        disp = symTable.get(obj.value) - (obj.locctr + obj.instructionLength);
                        if (disp>=-2048 && disp<=2047) {
                            obj.relativeMode = relativeMode.PROGRAM_COUNTER;
                        }else {
                            obj.relativeMode = relativeMode.BASE;
                        }
                        if(obj.extendMode)
                            obj.relativeMode = relativeMode.NON;

                    } else { // for ex: #4096
                        if(obj.addressingMode.equals(addressingMode.IMMEDIATE)) {
                            disp = Integer.parseInt(obj.value);
                        }
                    }
                    switch (obj.relativeMode) {
                        case PROGRAM_COUNTER -> {
                            obj.instruction += 0x2000;
                            if ((obj.locctr + obj.instructionLength) > symTable.get(obj.value))  // pc > target
                                disp += 0x1000;
                            }
                        case BASE -> {
                            obj.instruction += 0x4000;
                            disp = symTable.get(obj.value) - baseRegister;
                        }

                        case NON -> {
                            if (obj.addressingMode.equals(addressingMode.SIMPLE_XE)) {
                                disp = symTable.get(obj.value);
                            }
                        }
                    }
                    obj.instruction += disp;
                }

            }

            }

        }
    public static void generateOutput(ArrayList<myOutput> objects, File fileWriter,String programName,int lengthOfProgram) {
        try {
            FileOutputStream fos = new FileOutputStream(fileWriter);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));

            String hProgramName = "H^"+String.format("%-6s",programName);
            String hStartingAddress = "^"+String.format("%6s",Integer.toHexString(symTable.get(programName))).replace(" ","0");
            String hLength = "^"+String.format("%6s",Integer.toHexString(lengthOfProgram)).replace(" ","0");
            String hRecord = hProgramName + hStartingAddress + hLength;
////head record
            writer.write(hRecord);
            writer.newLine();

            int count = 0;
            boolean flag = true;
            while(flag) {
                String line = "";
                int lineLength = 0;
                int startingAddress = objects.get(count).locctr;
                while (lineLength <= 0X1D) {
                    if (objects.get(count).instruction == -1) {
                        count++;
                        continue;
                    }
                    if (objects.get(count).instruction == 0) break;
                    if (lineLength + objects.get(count).instructionLength > 0X1D) break;
                    lineLength += objects.get(count).instructionLength;

                    line += "^" + String.format("%" + (objects.get(count).instructionLength) * 2 + "s", Integer.toHexString(objects.get(count).instruction).toUpperCase()).replace(" ", "0");
                    if (count >= objects.size() - 1) {
                        flag = false;
                        break;
                    }
                    count++;
                }
                while (objects.get(count).instruction == 0) {
                    count++;
                }

                String tStartingAddress = (String.format("^%6s", Integer.toHexString(startingAddress).toUpperCase())).replace(" ", "0");
                String tLengthRecord = (String.format("^%2s", Integer.toHexString(lineLength).toUpperCase())).replace(" ", "0");
                String tRecord = "T" + tStartingAddress + tLengthRecord + line;
////text record
                writer.write(tRecord);
                writer.newLine();
            }
            for (int i = 0; i < objects.size(); i++) {
                if (objects.get(i).extendMode && objects.get(i).addressingMode != addressingMode.IMMEDIATE ) {
                    String mLine = (String.format("%6s",Integer.toHexString(objects.get(i).locctr+1).toUpperCase())).replace(" ", "0");
                    String mLength = (String.format("^%2s", 5)).replace(" ","0");
                    String mRecord = "M^"+ mLine +mLength;
                    writer.write(mRecord);
                    writer.newLine();

                }
            }
            int firstExecutable = 0;
            for (int i = 0; i < objects.size(); i++) {
                if (opTab.containsKey(objects.get(i).opCode)) {
                    firstExecutable = objects.get(i).locctr;
                    break;
                }
            }
            String eRecord = (String.format("E^%6s",Integer.toHexString(firstExecutable)).toUpperCase()).replace(" ","0");
////end record
            writer.write(eRecord);
            writer.close();

        }catch (Exception e) {
            System.out.println("ERROR");
            e.printStackTrace();
        }
    }
}
