import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

public class Main {
    public static void main(String[] args) throws IOException {
        Model model = new Model("Zagadnienie pośrednika");
        List<Participant> receivers = new ArrayList<>();
        List<Participant> deliverers = new ArrayList<>();
        int[][] transportCosts = new int[][]{
                {4, 7},
                {6, 5},
                {5, 8},
                {3, 2}
        };
        int fakeRouteIndex = -1;
        List<Integer> unitCosts = new ArrayList<>();
        List<Integer> routesMaxTransports = new ArrayList<>();

        //    CREATE RECEIVERS AND DELIVERS
        receivers.add(new Participant(20, 5, true));
        receivers.add(new Participant(35, 5, true));

        deliverers.add(new Participant(5, 7, true));
        deliverers.add(new Participant(15, 8, true));
        deliverers.add(new Participant(15, 5, true));
        deliverers.add(new Participant(20, 6, true));

        int numberOfReceivers = receivers.size();
        int numberOfDeliverers = deliverers.size();
        int blockedReceiver = 0;


        // ADD FAKE RECEIVER AND DELIVER IF NEEDED
        int deliversSum = getSumOfValues(deliverers);
        int receiversSum = getSumOfValues(receivers);
        receivers.add(new Participant(deliversSum, 0, false));
        numberOfReceivers += 1;
        deliverers.add(new Participant(receiversSum, 0, false));
        numberOfDeliverers += 1;

        int nRouts = numberOfDeliverers * numberOfReceivers;
        //    CALCULATE ROUTE MAX TRANSPORTS AND UNIT COSTS
        for (int i = 0; i < numberOfDeliverers; i++) {
            for (int j = 0; j < numberOfReceivers; j++) {
                int cost;
                if (!deliverers.get(i).isReal() || !receivers.get(j).isReal()) {
                    cost = 0;
                } else {
                    cost = receivers.get(j).getCost() - deliverers.get(i).getCost() - transportCosts[i][j];
                }
                if (!deliverers.get(i).isReal() && j == blockedReceiver) {
                    fakeRouteIndex = routesMaxTransports.size();
                }
                routesMaxTransports.add(
                        Math.min(deliverers.get(i).getValue(), receivers.get(j).getValue()));
                unitCosts.add(cost);
            }
        }
        //    DECLARE ROUTES TRANSPORTS VARIABLES
        IntVar[] routesTransports =
                IntStream.range(0, nRouts)
                        .mapToObj(i -> model.intVar("route: " + i, 0, routesMaxTransports.get(i)))
                        .toArray(IntVar[]::new);
        //    DECLARE GOAL FUNCTION VARIABLE
        IntVar goalFunction = model.intVar("goalFunction", IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND);
        //     ADD CONSTRAINTS FOR ROWS
        int iteratorOverColumns = 0;
        for (int i = 0; i < numberOfDeliverers; i++) {
            model
                    .sum(
                            java.util.Arrays.stream(
                                    routesTransports,
                                    iteratorOverColumns,
                                    iteratorOverColumns + numberOfReceivers)
                                    .toArray(IntVar[]::new),
                            "<=",
                            deliverers.get(i).getValue())
                    .post();
            iteratorOverColumns += numberOfReceivers;
        }
        //     ADD CONSTRAINTS FOR COLUMNS
        for (int i = 0; i < numberOfReceivers; i++) {
            int it = i;
            List<IntVar> columnConstraints = new ArrayList<>();
            for (int j = 0; j < numberOfDeliverers; j++) {
                columnConstraints.add(routesTransports[it]);
                it += numberOfReceivers;
            }
            model.sum(columnConstraints.toArray(IntVar[]::new), "<=", receivers.get(i).getValue()).post();
        }


        //    ADD CONSTRAINTS FOR BLOCKING ROUTE
        if (blockedReceiver >= 0) {
            if (fakeRouteIndex != -1) {
                model.arithm(routesTransports[fakeRouteIndex], "=", 0).post();
            }
            List<IntVar> blockedRoutes = new ArrayList<>();
            int bIterator = blockedReceiver;
            for (int i = 0; i < numberOfDeliverers - 1; i++) {
                blockedRoutes.add(routesTransports[bIterator]);
                bIterator += numberOfReceivers;
            }
            model
                    //.scalar(blockedRoutes.toArray(IntVar[]::new), "=", receivers.get(blockedReceiver).getValue())
                    .sum(blockedRoutes.toArray(IntVar[]::new), "=", receivers.get(blockedReceiver).getValue())
                    .post();
        }


        // ADD CONSTRAINT FOR ENSURE FULL DEMAND
        model.sum(routesTransports, "=", getSumOfValues(receivers)).post();


        //    COMPUTE GOAL FUNCTION
        model
                .scalar(routesTransports, unitCosts.stream().mapToInt(i -> i).toArray(), "=", goalFunction)
                .post();
        //    SOLVE MODEL
        Solver solver = model.getSolver();
        Solution solution = solver.findOptimalSolution(goalFunction, true);
        //    WRITE SOLUTION TO FILE
        writeSolutionToFile(
                solution, numberOfDeliverers, numberOfReceivers, routesTransports, goalFunction);
    }

    private static int getSumOfValues(List<Participant> participants) {
        return participants.stream().mapToInt(Participant::getValue).sum();
    }

    private static void writeSolutionToFile(
            Solution solution,
            int numberOfDeliverers,
            int numberOfReceivers,
            IntVar[] routesTransports,
            IntVar goalFunction)
            throws IOException {
        List<String> lines = new ArrayList<>();
        int iterator = 0;
        for (int i = 0; i < numberOfDeliverers; i++) {
            for (int j = 0; j < numberOfReceivers; j++) {
                lines.add(
                        String.format(
                                "Dostawca[%d] - Odbiorca[%d]: %d",
                                i, j, solution.getIntVal(routesTransports[iterator])));
                iterator++;
            }
        }
        lines.add(String.format("Zysk = %d", solution.getIntVal(goalFunction)));
        Path out = Paths.get("solution.txt");
        Files.write(out, lines, Charset.defaultCharset());
    }
}
