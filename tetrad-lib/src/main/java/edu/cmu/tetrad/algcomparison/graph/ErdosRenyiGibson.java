package edu.cmu.tetrad.algcomparison.graph;

import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.util.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a random graph by selecting edges from among possible edges with equal probability. (Erdos-Renyi-Gibson.)
 *
 * @author jdramsey
 */
public class ErdosRenyiGibson implements RandomGraph {
    static final long serialVersionUID = 23L;

    @Override
    public Graph createGraph(Parameters parameters) {
        return GraphUtils.erdosRenyiGibsonDag(
                parameters.getInt("numMeasures") + parameters.getInt("numLatents"),
                parameters.getInt("numLatents"),
                parameters.getInt("avgDegree") * parameters.getInt("numMeasures") / 2,
                parameters.getInt("maxDegree"),
                parameters.getInt("maxIndegree"),
                parameters.getInt("maxOutdegree"),
                true);
    }

    @Override
    public String getDescription() {
        return "Erdos-Renyi-Gibson graph constructed by selecting edges from among possible edges with equal probability";
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add("numMeasures");
        parameters.add("avgDegree");
        return parameters;
    }
}