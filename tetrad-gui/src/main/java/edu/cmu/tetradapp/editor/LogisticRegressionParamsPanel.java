///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.NumberFormatUtil;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetradapp.model.DagWrapper;
import edu.cmu.tetradapp.model.DataWrapper;
import edu.cmu.tetradapp.model.GraphWrapper;
import edu.cmu.tetradapp.util.DoubleTextField;
import edu.cmu.tetradapp.util.StringTextField;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Edits parameters for Markov blanket search algorithm.
 *
 * @author Frank Wimberly
 */
final class LogisticRegressionParamsPanel extends JPanel
        implements ActionListener, ListSelectionListener {

    /**
     * The parameter object being edited.
     */
    private Parameters params;

    /**
     * The name of the target variable or node in the regression.
     */
    private String targetName;

    /**
     * The variable names from the object being searched over (usually data).
     */
    private List<String> varNames;

    private transient DataModel dataModel;

    private List<String> regressorNames;

    private JTextField responseVar;

    private JList availableVarsList;
    private JList predictorVarListbox;

    private ListSelectionModel varsSelModel;
    private ListSelectionModel predsSelModel;

    private ArrowButton responseButton;

    final private static String INCLUDE_RESPONSE = "includeResponse";
    final private static String INCLUDE_PREDICTOR = "includePredictor";
    final private static String EXCLUDE_PREDICTOR = "excludePredictor";

    /**
     * Opens up an editor to let the user view the given RegressionRunner.
     */
    public LogisticRegressionParamsPanel(Parameters params,
                                         Object[] parentModels) {
        for (Object parentModel : parentModels) {
            if (parentModel instanceof DataWrapper) {
                DataWrapper dataWrapper = (DataWrapper) parentModel;
                dataModel = dataWrapper.getSelectedDataModel();
            } else if (parentModel instanceof DataModel) {
                dataModel = (DataModel) parentModel;
            }
        }

        if (dataModel == null) {
            new JOptionPane("Null DataModel");
        }

        if (params == null) {
            throw new NullPointerException(
                    "Parameters must not be null.");
        }

        this.params = params;

        Object varNames = params.get("varNames", null);

        if (varNames != null) {
            this.varNames = (List<String>) varNames;
        }

        if (this.varNames == null) {
            this.varNames = getVarsFromData(parentModels);

            if (this.varNames == null) {
                this.varNames = getVarsFromGraph(parentModels);
            }

            params.set("varNames", this.varNames);

            if (this.varNames == null) {
                throw new IllegalStateException(
                        "Variables are not accessible.");
            }

            params().set("varNames", this.varNames);
        }

        JLabel instructions =
                new JLabel("Select response and predictor variables:");
        JLabel varsLabel = new JLabel("Variables");
        JLabel responseLabel = new JLabel("Response");
        JLabel predictorLabel = new JLabel("Predictor(s)");

        JScrollPane varListbox = (JScrollPane) createVarListbox();
        JScrollPane predictorListbox =
                (JScrollPane) createPredictorVarListbox();

        responseButton = new ArrowButton(this, INCLUDE_RESPONSE);
        ArrowButton predictorInButton =
                new ArrowButton(this, INCLUDE_PREDICTOR);
        ArrowButton predictorOutButton =
                new ArrowButton(this, EXCLUDE_PREDICTOR, false);

        responseVar = new StringTextField("", 10);
        responseVar.setEditable(false);
        responseVar.setBackground(Color.white);
        responseVar.setPreferredSize(new Dimension(150, 30));
        responseVar.setFont(new Font("SanSerif", Font.BOLD, 12));

        //TEST
        responseVar.setText(params.getString("targetName", null));
        if (!responseVar.getText().equals("") &&
                responseButton.getText().equals(">")) {
            responseButton.toggleInclude();
        }

        DefaultListModel predsModel =
                (DefaultListModel) this.predictorVarListbox.getModel();
        List<String> paramNames = (List<String>) params.get("regressorNames", null);
        for (String paramName : paramNames) {
            predsModel.addElement(paramName);
        }

        //Construct availableVarsList of variable names not response nor in predictors.
        List<String> varListNames = new ArrayList<>(this.varNames);
        String targetName = params.getString("targetName", null);
        if (varListNames.contains(targetName)) {
            varListNames.remove(targetName);
        }

        List<String> regNames = (List<String>) params.get("regressorNames", null);
        for (String regName : regNames) {
            if (varListNames.contains(regName)) {
                varListNames.remove(regName);
            }
        }

        DefaultListModel varsModel =
                (DefaultListModel) availableVarsList.getModel();
        varsModel.clear();
        for (String varListName : varListNames) {
            varsModel.addElement(varListName);
        }

        varsSelModel = availableVarsList.getSelectionModel();
        varsSelModel.setSelectionMode(
                ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        predsSelModel = predictorVarListbox.getSelectionModel();
        predsSelModel.setSelectionMode(
                ListSelectionModel.SINGLE_INTERVAL_SELECTION);


        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Box b = Box.createVerticalBox();

        Box b0 = Box.createHorizontalBox();
        b0.add(instructions);

        Box b1 = Box.createHorizontalBox();

        Box b2 = Box.createVerticalBox();
        Box b3 = Box.createVerticalBox();
        Box b4 = Box.createVerticalBox();
        Box b5 = Box.createVerticalBox();

        DoubleTextField alphaField = new DoubleTextField(params.getDouble("alpha", 0.001), 4,
                NumberFormatUtil.getInstance().getNumberFormat());
        alphaField.setFilter(new DoubleTextField.Filter() {
            public double filter(double value, double oldValue) {
                try {
                    params().set("alpha", 0.001);
                    Preferences.userRoot().putDouble("alpha",
                            params().getDouble("alpha", 0.001));
                    return value;
                }
                catch (Exception e) {
                    return oldValue;
                }
            }
        });

        JLabel alphaLabel = new JLabel("Alpha:");

        b2.add(varsLabel);
        b2.add(varListbox);
        //b2.add(availableVarsList);
        b2.add(alphaLabel);
        b2.add(alphaField);

        b3.add(responseButton);
        Component strut3 = Box.createVerticalStrut(90);
        b3.add(strut3);
        b3.add(predictorInButton);
        b3.add(predictorOutButton);

        responseLabel.setPreferredSize(new Dimension(80, 30));
        b4.add(responseLabel);
        Component strut42 = Box.createVerticalStrut(120);
        b4.add(strut42);
        b4.add(predictorLabel);

        b5.add(responseVar);
        Component strut5 = Box.createVerticalStrut(10);
        b5.add(strut5);
        b5.add(predictorListbox);

        b1.add(b2);
        b1.add(b3);
        b1.add(b4);
        b1.add(b5);

        b.add(b0);
        b.add(b1);

        add(b);
    }

    private JComponent createVarListbox() {
        availableVarsList = new JList(new DefaultListModel());
        DefaultListModel varsModel =
                (DefaultListModel) availableVarsList.getModel();

        for (String varName : varNames) {
            varsModel.addElement(varName);
        }

        availableVarsList.setVisibleRowCount(5);
        availableVarsList.setFixedCellWidth(100);
        availableVarsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableVarsList.setSelectedIndex(0);

        return new JScrollPane(availableVarsList);
    }

    private JComponent createPredictorVarListbox() {
        predictorVarListbox = new JList(new DefaultListModel());
        predictorVarListbox.setVisibleRowCount(4);
        predictorVarListbox.setFixedCellWidth(100);
        predictorVarListbox.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        predictorVarListbox.setSelectedIndex(0);

        return new JScrollPane(predictorVarListbox);
    }


    private List<String> getVarsFromData(Object[] parentModels) {
        DataModel dataModel = null;

        for (Object parentModel : parentModels) {
            if (parentModel instanceof DataWrapper) {
                DataWrapper dataWrapper = (DataWrapper) parentModel;
                dataModel = dataWrapper.getSelectedDataModel();
            }
        }

        if (dataModel == null) {
            return null;
        } else {
            return new ArrayList<>(dataModel.getVariableNames());
        }
    }

    private List<String> getVarsFromGraph(Object[] parentModels) {
        Object graphWrapper = null;

        for (Object parentModel : parentModels) {
            if (parentModel instanceof GraphWrapper) {
                graphWrapper = parentModel;
            } else if (parentModel instanceof DagWrapper) {
                graphWrapper = parentModel;
            }
        }

        if (graphWrapper == null) {
            return null;
        } else {
            Graph graph = null;

            if (graphWrapper instanceof GraphWrapper) {
                graph = ((GraphWrapper) graphWrapper).getGraph();
            } else if (graphWrapper instanceof DagWrapper) {
                graph = ((DagWrapper) graphWrapper).getDag();
            }

            List<Node> nodes = graph.getNodes();
            List<String> nodeNames = new LinkedList<>();

            for (Node node : nodes) {
                nodeNames.add(node.getName());
            }

            return nodeNames;
        }
    }

    private void setRegressorNames(List<String> names) {
        regressorNames = names;
    }

    private Parameters params() {
        return this.params;
    }

    private String targetName() {
        return targetName;
    }

    private void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public void valueChanged(ListSelectionEvent e) {

    }

    public void actionPerformed(ActionEvent e) {
        String varName;
        int varSelectionIndex, predictorVarSelectionIndex;

        DefaultListModel varsModel =
                (DefaultListModel) availableVarsList.getModel();
        DefaultListModel predsModel =
                (DefaultListModel) predictorVarListbox.getModel();

        int startIndexVars = varsSelModel.getMinSelectionIndex();
        int endIndexVars = varsSelModel.getMaxSelectionIndex();

        int startIndexPreds = predsSelModel.getMinSelectionIndex();
        int endIndexPreds = predsSelModel.getMaxSelectionIndex();

        String[] varSelectedNames =
                new String[endIndexVars - startIndexVars + 1];
        String[] predSelectedNames =
                new String[endIndexPreds - startIndexPreds + 1];

        if (varsModel.size() != 0) {
            varName = (String) availableVarsList.getSelectedValue();
        } else {
            varName = "";
        }

        /* include/exclude response variable */
        if (e.getActionCommand().equals(INCLUDE_RESPONSE)) {
            if ((availableVarsList.isSelectionEmpty()) &&
                    (responseButton.getIsIncluded())) {
                return;
            }
            //if(varsSelModel.getMaxSelectionIndex() == -1 && responseButton.getIsIncluded()) return;


            if (responseButton.getIsIncluded()) {

                //Make sure response variable is dichotomous
                DataSet cds = (DataSet) dataModel;
                int nrows = cds.getNumRows();
                Node variable = cds.getVariable(varName);

                int varIndex = cds.getVariables().indexOf(variable);

                // make sure the variable is binary.
                if (variable instanceof DiscreteVariable) {
                    for (int i = 0; i < nrows; i++) {
                        int value = cds.getInt(i, varIndex);
                        if (value != 0 && value != 1) {
                            JOptionPane.showMessageDialog(this, "The given target was discrete but not binary");
                            return;
                        }
                    }
                } else {
                    for(int i = 0; i<nrows; i++){
                        double value = cds.getDouble(i, varIndex);
                        if(value != 0.0d && value != 1.0d){
                            JOptionPane.showMessageDialog(this, "Target must be a dictotomous variable");
                            return;
                        }
                    }
                }

                responseVar.setText(varName);
                varsModel.removeElement(varName);

            } else {
                varsModel.addElement(responseVar.getText());

                responseVar.setText("");
                responseButton.toggleInclude();  //Test
                return;                          //Test
            }
            responseButton.toggleInclude();

            String newTargetName = responseVar.getText();
            setTargetName(newTargetName);
            params().set("targetName", targetName());
        }

        // include predictor variable.
        else if (e.getActionCommand().equals(INCLUDE_PREDICTOR)) {
            if (availableVarsList.isSelectionEmpty()) {
                return;
            }

            int intervalSize = endIndexVars - startIndexVars + 1;
            for (int i = 0; i < intervalSize; i++) {
                varSelectedNames[i] =
                        (String) availableVarsList.getSelectedValuesList().get(i);
            }

            for (int i = 0; i < intervalSize; i++) {
                varsModel.removeElement(varSelectedNames[i]);
                predsModel.addElement(varSelectedNames[i]);
            }
        }
        // exclude predictor variable.
        else if (e.getActionCommand().equals(EXCLUDE_PREDICTOR)) {
            if (predictorVarListbox.isSelectionEmpty()) {
                return;
            }

            int intervalSize = endIndexPreds - startIndexPreds + 1;
            for (int i = 0; i < intervalSize; i++) {
                predSelectedNames[i] =
                        (String) predictorVarListbox.getSelectedValuesList().get(i);
            }

            for (int i = 0; i < intervalSize; i++) {
                predsModel.removeElement(predSelectedNames[i]);
                varsModel.addElement(predSelectedNames[i]);
            }

        } else {
            return;
        }

        // updates expt variables and predictor listbox.
        varSelectionIndex = availableVarsList.getSelectedIndex();
        predictorVarSelectionIndex = predictorVarListbox.getSelectedIndex();

        if (varSelectionIndex > 0) {
            varSelectionIndex--;
        }
        if (varSelectionIndex != -1) {
            availableVarsList.setSelectedIndex(varSelectionIndex);
        }

        if (predictorVarSelectionIndex > 0) {
            predictorVarSelectionIndex--;
        }
        if (predictorVarSelectionIndex != -1) {
            predictorVarListbox.setSelectedIndex(predictorVarSelectionIndex);
        }

        int numPredictors = predsModel.size();
        Object[] predictors = new Object[numPredictors];

        List<String> regNames = new ArrayList<>();

        for (int i = 0; i < numPredictors; i++) {
            predictors[i] = predsModel.getElementAt(i);
            regNames.add((String) predsModel.getElementAt(i));
        }

        setRegressorNames(regNames);

        params().set("regressorNames", regressorNames);
    }


    /**
     * Private Inner Class to manipulate the arrow buttons for
     * including/excluding variables for x/y-axis
     */
    public class ArrowButton extends JButton {
        private boolean isInclude;

        public ArrowButton(LogisticRegressionParamsPanel listener,
                           String command) {
            this(listener, command, true);
        }

        public ArrowButton(LogisticRegressionParamsPanel listener,
                           String command, boolean isInclude) {
            this.isInclude = isInclude;
            addActionListener(listener);
            setActionCommand(command);

            if (isInclude) {
                setText(">");
            } else {
                setText("<");
            }
        }

        public void toggleInclude() {
            if (isInclude) {
                setText("<");
                isInclude = false;
            } else {
                setText(">");
                isInclude = true;
            }
        }

        public boolean getIsIncluded() {
            return isInclude;
        }
    }
}





