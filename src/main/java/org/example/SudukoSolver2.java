import org.sat4j.core.VecInt;
import org.sat4j.maxsat.SolverFactory;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.pb.SolverFactoryPB;
import org.sat4j.pb.tools.DependencyHelper;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SudokuSolver2 {
    private int[][] board;
    private int boardSize;
    private int boxSize;
    private int numClauses;

    public SudokuSolver2(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            List<String> lines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("c") || line.startsWith("p")) {
                    continue;
                }
                lines.add(line);
            }
            reader.close();
            boardSize = (int) Math.sqrt(lines.size());
            boxSize = (int) Math.sqrt(boardSize);
            board = new int[boardSize][boardSize];
            solver = new DependencyHelper<>(SolverFactory.newDefault());
            numClauses = 0;
            parseInput(lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseInput(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String[] tokens = lines.get(i).split("\\s+");
            for (int j = 0; j < tokens.length; j++) {
                int value = Integer.parseInt(tokens[j]);
                if (value > 0) {
                    board[i][j] = value;
                    addClause(i, j, value);
                }
            }
        }
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (board[row][col] == 0) {
                    addCellClauses(row, col);
                }
            }
        }
    }

    private void addClause(int row, int col, int value) {
        IVecInt literals = new VecInt(1);
        literals.push(getLiteral(row, col, value));
        try {
            solver.addClause(literals);
            numClauses++;
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
    }

    private void addCellClauses(int row, int col) {
        IVecInt literals = new VecInt(boardSize);
        for (int value = 1; value <= boardSize; value++) {
            literals.push(getLiteral(row, col, value));
        }
        try {
            solver.addClause(literals);
            numClauses++;
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
        for (int value1 = 1; value1 <= boardSize - 1; value1++) {
            for (int value2 = value1 + 1; value2 <= boardSize; value2++) {
                int[] cell1 = getBoxCoordinates(row, col, value1);
                int[] cell2 = getBoxCoordinates(row, col, value2);
                if (cell1[0] == cell2[0] && cell1[1] == cell2[1]) {
                    addBinaryClause(row, col, value1, value2);
                }
            }
        }
        for (int otherCol = 0; otherCol < boardSize; otherCol++) {
            if (otherCol != col) {
                addBinaryClause(row, col, row, otherCol);
            }
        }
        for (int otherRow = 0; otherRow < boardSize; otherRow++) {
            if (otherRow != row) {
                addBinaryClause(row, col, otherRow, col);
            }
        }
    }

    private void addBinaryClause(int row1, int col1, int row2, int col2) {
        for (int value = 1; value <= boardSize; value++) {
            IVecInt literals = new VecInt(2);
            literals.push(getLiteral(row1, col1, value));
            literals.push(getLiteral(row2, col2, value));
            try {
                solver.addClause(literals);
                numClauses++;
            } catch (ContradictionException e) {
                e.printStackTrace();
            }
        }
    }

    private int getLiteral(int row, int col, int value) {
        return (row * boardSize + col) * boardSize + value;
    }

    private int[] getBoxCoordinates(int row, int col, int value) {
        int[] coords = new int[2];
        coords[0] = boxSize * (row / boxSize) + (value - 1) / boxSize;
        coords[1] = boxSize * (col / boxSize) + (value - 1) % boxSize;
        return coords;
    }

    public void solve() {
        WeightedMaxSatDecorator solverPB = new WeightedMaxSatDecorator(SolverFactoryPB.newMaxSatHeap());
        solverPB.newVar(boardSize * boardSize * boardSize);
        solverPB.setExpectedNumberOfClauses(numClauses);
        try {
            for (IConstr constraint : solver.getConstraints()) {
                solverPB.addHardConstraint(constraint);
            }
            if (solverPB.isSatisfiable()) {
                int[] model = solverPB.model();
                for (int row = 0; row < boardSize; row++) {
                    for (int col = 0; col < boardSize; col++) {
                        for (int value = 1; value <= boardSize; value++) {
                            if (model[getLiteral(row, col, value) - 1] > 0) {
                                board[row][col] = value;
                                break;
                            }
                        }
                    }
                }
                printBoard();
            } else {
                System.out.println("Unsatisfiable");
            }
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void printBoard() {
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                System.out.print(board[row][col] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java SudokuSolver2 <filename>");
            return;
        }
        SudokuSolver2 solver = new SudokuSolver2(args[0]);
        solver.solve();
    }