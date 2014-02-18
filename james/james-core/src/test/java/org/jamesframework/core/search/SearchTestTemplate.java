//  Copyright 2014 Herman De Beukelaer
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.jamesframework.core.search;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.jamesframework.core.problems.SubsetProblemWithData;
import org.jamesframework.core.problems.solutions.SubsetSolution;
import org.jamesframework.core.search.stopcriteria.MaxRuntime;
import org.jamesframework.test.util.FakeSubsetData;
import org.jamesframework.test.util.FakeSubsetObjectiveWithData;
import org.jamesframework.test.util.FakeSubsetPenalizingConstraint;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Template for general search tests, using a random search stub to solve a subset problem. Can be extended to use the contained data.
 * 
 * @author Herman De Beukelaer <herman.debeukelaer@ugent.be>
 */
public class SearchTestTemplate {

    // fake subset data (scored entities)
    protected FakeSubsetData data;
    // dataset size
    protected static final int DATASET_SIZE = 500;
    // entity scores
    protected static double[] scores;
    
    // fake objective (sum of scores)
    protected FakeSubsetObjectiveWithData obj;

    // subset problem to solve (select SUBSET_SIZE out of DATASET_SIZE)
    protected SubsetProblemWithData<FakeSubsetData> problem;
    protected final int SUBSET_SIZE = 20;
    
    // fake constraint (not assigned by default)
    protected FakeSubsetPenalizingConstraint constraint;
    // minimum score diff imposed by fake constraint
    protected final double MIN_SCORE_DIFF = 0.02;
    
    // random generator
    protected static final Random RG = new Random();
    
    /**
     * Create data (same for every test method).
     */
    @BeforeClass
    public static void setUpClass() {
        scores = new double[DATASET_SIZE];
        for(int i=0; i<DATASET_SIZE; i++){
            scores[i] = RG.nextDouble();
        }
    }
    
    /**
     * Create search and components to work with in each test method.
     */
    @Before
    public void setUp(){
        data = new FakeSubsetData(scores);
        obj = new FakeSubsetObjectiveWithData();
        problem = new SubsetProblemWithData(obj, data, SUBSET_SIZE);
        constraint = new FakeSubsetPenalizingConstraint(MIN_SCORE_DIFF);
    }
    
    /**
     * Perform a single run of a search, with maximum runtime. It is verified that the best solution is not rejected, if any.
     */
    protected void singleRunWithMaxRuntime(Search<SubsetSolution> search, long maxRuntime, TimeUnit maxRuntimeTimeUnit){
        // set maximum runtime
        search.addStopCriterion(new MaxRuntime(maxRuntime, maxRuntimeTimeUnit));
        System.out.println("   >>> max time: " + maxRuntimeTimeUnit.toMillis(maxRuntime) + " ms");

        // run search (should stop)
        search.start();
        
        if(search.getBestSolution() == null){
            System.out.println("   >>> no valid solution found ...");
        } else {
            System.out.println("   >>> best solution: " + search.getBestSolutionEvaluation());
            // verify
            if(search.getBestSolution() != null){
                assertFalse(problem.rejectSolution(search.getBestSolution()));
            }
        }
    }
    
    /**
     * Perform multiple subsequent runs of the search, with a maximum runtime per run. If maximizing, it is verified that
     * the best solution evaluation always increases, else, it is verified whether it decreases over time.
     */
    protected void multiRunWithMaximumRuntime(Search<SubsetSolution> search, long maxRuntime, TimeUnit maxRuntimeTimeUnit,
                                                                int numRuns, boolean maximizing, boolean printEvaluations){
        // add stop criterion
        search.addStopCriterion(new MaxRuntime(maxRuntime, maxRuntimeTimeUnit));
        // set check period to same value for frequent enough checks
        search.setStopCriterionCheckPeriod(maxRuntime, maxRuntimeTimeUnit);
        // perform subsequent runs
        Double prevBestSolEval = null, bestSolEval = null;
        for(int i=0; i<numRuns; i++){
            // start search
            search.start();
            // check best solution evaluation
            if(search.getBestSolution() != null){
                bestSolEval = search.getBestSolutionEvaluation();
                if(printEvaluations) System.out.println("   >>> best: " + bestSolEval);
                // compare with previous best solution, if any
                if(prevBestSolEval != null){
                    if(maximizing){
                        assertTrue(bestSolEval >= prevBestSolEval);
                    } else {
                        assertTrue(bestSolEval <= prevBestSolEval);
                    }
                }
            } else if (printEvaluations) {
                System.out.println("   >>> no valid solution found yet ...");
            }
            prevBestSolEval = bestSolEval;
        }
    }
    
}