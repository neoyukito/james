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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.jamesframework.core.exceptions.SearchException;
import org.jamesframework.core.problems.Problem;
import org.jamesframework.core.problems.solutions.Solution;
import org.jamesframework.core.search.cache.EvaluatedMoveCache;
import org.jamesframework.core.search.cache.SingleEvaluatedMoveCache;
import org.jamesframework.core.search.listeners.NeighbourhoodSearchListener;
import org.jamesframework.core.search.listeners.SearchListener;
import org.jamesframework.core.search.neigh.Move;
import org.jamesframework.core.util.JamesConstants;

/**
 * A neighbourhood search extends the general abstract search by adding a concept of current solution, which is repeatedly modified
 * by applying moves generated by a neighbourhood. The initial solution may be specified using {@link #setCurrentSolution(Solution)}
 * before running the search, else, a random initial solution will be constructed. A neighbourhood search contains additional metadata
 * that applies to the current run only: the number of accepted and rejected moves. All other data is retained across subsequent runs,
 * including the current solution and its evaluation. This means that upon restarting a neighbourhood search, it will continue from
 * where it had arrived in the previous run.
 * 
 * @param <SolutionType> solution type of the problems that may be solved using this search, required to extend {@link Solution}
 * @author <a href="mailto:herman.debeukelaer@ugent.be">Herman De Beukelaer</a>
 */
public abstract class NeighbourhoodSearch<SolutionType extends Solution> extends Search<SolutionType> {

    /******************/
    /* PRIVATE FIELDS */
    /******************/
    
    // number of accepted/rejected moves during current run
    private long numAcceptedMoves, numRejectedMoves;
    
    // current solution and its corresponding evaluation
    private SolutionType curSolution;
    private double curSolutionEvaluation;
    
    // evaluated move cache
    private EvaluatedMoveCache cache;
    
    /************************/
    /* PRIVATE FINAL FIELDS */
    /************************/
    
    // list containing neighbourhood search listeners attached to this search
    private final List<NeighbourhoodSearchListener<? super SolutionType>> neighSearchListeners;
    
    /***************/
    /* CONSTRUCTOR */
    /***************/
    
    /**
     * Create a new neighbourhood search to solve the given problem,
     * with default name "NeighbourhoodSearch".
     * 
     * @throws NullPointerException if <code>problem</code> is <code>null</code>
     * @param problem problem to solve
     */
    public NeighbourhoodSearch(Problem<SolutionType> problem){
        this(null, problem);
    }
    
    /**
     * Create a new neighbourhood search to solve the given problem,
     * with a custom name. If <code>name</code> is <code>null</code>,
     * the default name "NeighbourhoodSearch" will be assigned.
     * 
     * @throws NullPointerException if <code>problem</code> is <code>null</code>
     * @param problem problem to solve
     * @param name custom search name
     */
    public NeighbourhoodSearch(String name, Problem<SolutionType> problem){
        super(name != null ? name : "NeighbourhoodSearch", problem);
        // initialize per run metadata
        numAcceptedMoves = JamesConstants.INVALID_MOVE_COUNT;
        numRejectedMoves = JamesConstants.INVALID_MOVE_COUNT;
        // initially, current solution is null and its evaluation
        // is arbitrary (as defined in getCurrentSolutionEvaluation())
        curSolution = null;
        curSolutionEvaluation = 0.0; // arbitrary value
        // initialize list for neighbourhood search listeners
        neighSearchListeners = new ArrayList<>();
        // set default (single) evaluated move cache
        cache = new SingleEvaluatedMoveCache();
    }
    
    /*********/
    /* CACHE */
    /*********/
    
    /**
     * Sets a custom evaluated move cache. By default, a {@link SingleEvaluatedMoveCache} is used.
     * Note that this method may only be called when the search is idle.
     * 
     * @param cache custom evaluated move cache
     * @throws SearchException if the search is not idle
     */
    public void setEvaluatedMoveCache(EvaluatedMoveCache cache){
        // acquire status lock
        synchronized(getStatusLock()){
            // assert idle
            assertIdle("Cannot set custom evaluated move cache in neighbourhood search.");
            // set cache
            this.cache = cache;
        }
    }
    
    /******************/
    /* INITIALIZATION */
    /******************/
    
    /**
     * Resets neighbourhood search specific, per run metadata: number of accepted and rejected moves.
     */
    @Override
    protected void searchStarted(){
        // call super
        super.searchStarted();
        // reset neighbourhood search specific, per run metadata
        numAcceptedMoves = 0;
        numRejectedMoves = 0;
        // create random initial solution if none is set
        if(curSolution == null){
            adjustCurrentSolution(getProblem().createRandomSolution());
        }
    }
    
    /**
     * Private method to adjust the current solution, which does not verify the search status and may
     * therefore be called when the search is not idle. Called when creating a random initial solution
     * during initialization, and from within the public {@link #setCurrentSolution(Solution)} after
     * verifying the search status.
     * <p>
     * Clears the evaluated move cache, as this cache is no longer valid for the new current solution.
     * 
     * @param solution new current solution
     */
    private void adjustCurrentSolution(SolutionType solution){
        // clear evaluated move cache
        cache.clear();
        // set current solution
        curSolution = solution;
        // evaluate
        curSolutionEvaluation = getProblem().evaluate(solution);
        // check if new best solution
        if(!getProblem().rejectSolution(solution)){
            updateBestSolution(curSolution, curSolutionEvaluation);
        }
    }
    
    /**************************************************/
    /* OVERRIDDEN METHODS FOR ADDING SEARCH LISTENERS */
    /**************************************************/
    
    /**
     * Add a search listener, if not already added before. Passes the listener to
     * its parent (general search), but also stores it locally in case it is a
     * neighbourhood search listener for neighbourhood search specific callbacks.
     * Note that this method may only be called when the search is idle.
     * 
     * @param listener search listener to add to the search
     * @throws SearchException if the search is not idle
     * @return <code>true</code> if the search listener had not been added before
     */
    @Override
    public boolean addSearchListener(SearchListener<? super SolutionType> listener){
        // acquire status lock
        synchronized(getStatusLock()){
            // pass to super (also checks whether search is idle)
            boolean a = super.addSearchListener(listener);
            // store locally if neighbourhood listener
            if(listener instanceof NeighbourhoodSearchListener){
                neighSearchListeners.add((NeighbourhoodSearchListener<? super SolutionType>) listener);
            }
            return a;
        }
    }
    
    /**
     * Remove the given search listener. If the search listener had not been added, <code>false</code> is returned.
     * Calls its parent (general search) to remove the listener, and also removes it locally in case it is a
     * neighbourhood search listener. Note that this method may only be called when the search is idle.
     * 
     * @param listener search listener to be removed
     * @throws SearchException if the search is not idle
     * @return <code>true</code> if the listener has been successfully removed
     */
    @Override
    public boolean removeSearchListener(SearchListener<? super SolutionType> listener){
        // acquire status lock
        synchronized(getStatusLock()){
            // call super (also verifies status)
            boolean r = super.removeSearchListener(listener);
            // also remove locally if neighbourhood search listener
            if(listener instanceof NeighbourhoodSearchListener){
                neighSearchListeners.remove((NeighbourhoodSearchListener<? super SolutionType>) listener);
            }
            return r;
        }
    }
    
    /**********************************************************************/
    /* PRIVATE METHODS FOR FIRING NEIGHBOURHOOD SEARCH LISTENER CALLBACKS */
    /**********************************************************************/
    
    /**
     * Calls {@link SearchListener#searchStarted(Search)} on every attached search listener.
     * Should only be executed when search is active (initializing, running or terminating).
     */
    private void fireModifiedCurrentSolution(SolutionType newCurrentSolution, double newCurrentSolutionEvaluation){
        for(NeighbourhoodSearchListener<? super SolutionType> listener : neighSearchListeners){
            listener.modifiedCurrentSolution(this, newCurrentSolution, newCurrentSolutionEvaluation);
        }
    }
    
    /*****************************************/
    /* METADATA APPLYING TO CURRENT RUN ONLY */
    /*****************************************/
    
    /**
     * <p>
     * Get the number of moves accepted during the <i>current</i> (or last) run. The precise return value
     * depends on the status of the search:
     * </p>
     * <ul>
     *  <li>
     *   If the search is either RUNNING or TERMINATING, this method returns the number of moves accepted
     *   since the current run was started.
     *  </li>
     *  <li>
     *   If the search is IDLE, the total number of moves accepted during the last run is returned, if any.
     *   Before the first run, {@link JamesConstants#INVALID_MOVE_COUNT}.
     *  </li>
     *  <li>
     *   While INITIALIZING the current run, {@link JamesConstants#INVALID_MOVE_COUNT} is returned.
     *  </li>
     * </ul>
     * <p>
     * The return value is always positive, except in those cases when {@link JamesConstants#INVALID_MOVE_COUNT}
     * is returned.
     * </p>
     * 
     * @return number of moves accepted during the current (or last) run
     */
    public long getNumAcceptedMoves(){
        // depends on search status: synchronize with status updates
        synchronized(getStatusLock()){
            if(getStatus() == SearchStatus.INITIALIZING){
                // initializing
                return JamesConstants.INVALID_MOVE_COUNT;
            } else {
                // idle, running or terminating
                return numAcceptedMoves;
            }
        }
    }
    
    /**
     * <p>
     * Get the number of moves rejected during the <i>current</i> (or last) run. The precise return value
     * depends on the status of the search:
     * </p>
     * <ul>
     *  <li>
     *   If the search is either RUNNING or TERMINATING, this method returns the number of moves rejected
     *   since the current run was started.
     *  </li>
     *  <li>
     *   If the search is IDLE, the total number of moves rejected during the last run is returned, if any.
     *   Before the first run, {@link JamesConstants#INVALID_MOVE_COUNT}.
     *  </li>
     *  <li>
     *   While INITIALIZING the current run, {@link JamesConstants#INVALID_MOVE_COUNT} is returned.
     *  </li>
     * </ul>
     * <p>
     * The return value is always positive, except in those cases when {@link JamesConstants#INVALID_MOVE_COUNT}
     * is returned.
     * </p>
     * 
     * @return number of moves rejected during the current (or last) run
     */
    public long getNumRejectedMoves(){
        // depends on search status: synchronize with status updates
        synchronized(getStatusLock()){
            if(getStatus() == SearchStatus.INITIALIZING){
                // initializing
                return JamesConstants.INVALID_MOVE_COUNT;
            } else {
                // idle, running or terminating
                return numRejectedMoves;
            }
        }
    }
    
    /******************************************/
    /* STATE ACCESSORS (RETAINED ACROSS RUNS) */
    /******************************************/
    
    /**
     * Returns the current solution. The current solution might be worse than the best solution found so far.
     * Note that it is <b>retained</b> across subsequent runs of the same search. May return <code>null</code>
     * if no current solution has been set yet, for example when the search has just been created or is still
     * initializing the current run.
     * 
     * @return current solution, if set; <code>null</code> otherwise
     */
    public SolutionType getCurrentSolution(){
        return curSolution;
    }
    
    /**
     * Get the evaluation of the current solution. The current solution and its evaluation are <b>retained</b>
     * across subsequent runs of the same search. If the current solution is not yet defined, i.e. when
     * {@link #getCurrentSolution()} return <code>null</code>, the result of this method is undefined;
     * in such case it may return any arbitrary value.
     * 
     * @return evaluation of current solution, if already defined; arbitrary value otherwise
     */
    public double getCurrentSolutionEvaluation(){
        return curSolutionEvaluation;
    }
    
    /*************************/
    /* PUBLIC STATE MUTATORS */
    /*************************/
    
    /**
     * Sets the current solution. The given solution is automatically evaluated and compared with the
     * currently known best solution, to check if it improves on this solution. This method may for
     * example be used to specify a custom initial solution before starting the search. Note that it
     * may only be called when the search is idle.
     * <p>
     * Clears the evaluated move cache, as this cache is no longer valid for the new current solution.
     * 
     * @throws SearchException if the search is not idle
     * @throws NullPointerException if <code>solution</code> is <code>null</code>
     * @param solution current solution to be adopted
     */
    public void setCurrentSolution(SolutionType solution){
        // synchronize with status updates
        synchronized(getStatusLock()){
            // assert idle
            assertIdle("Cannot set current solution.");
            // check not null
            if(solution == null){
                throw new NullPointerException("Cannot set current solution: received null.");
            }
            // go ahead and adjust current solution
            adjustCurrentSolution(solution);
        }
    }
    
    /***********************/
    /* PROTECTED UTILITIES */
    /***********************/
    
    /**
     * Evaluates the neighbour obtained by applying the given move to the current solution. If this
     * move has been evaluated before and the computed value is still available in the cache, the
     * cached value will be returned. Else, the evaluation will be computed and offered to the cache.
     * 
     * @param move move applied to the current solution
     * @return evaluation of obtained neighbour, possibly retrieved from the evaluated move cache
     */
    protected double evaluateMove(Move<? super SolutionType> move){
        // check cache
        Double eval = cache.getCachedMoveEvaluation(move);
        if(eval != null){
            // cache hit: return cached value
            return eval;
        } else {
            // cache miss: evaluate and cache
            move.apply(curSolution);                        // apply move
            eval = getProblem().evaluate(curSolution);      // evaluate neighbour
            cache.cacheMoveEvaluation(move, eval);          // cache evaluation
            move.undo(curSolution);                         // undo move
            return eval;                                    // return evaluation
        }
    }
    
    /**
     * Validates the neighbour obtained by applying the given move to the current solution. If this
     * move has been validated before and the result is still available in the cache, the cached result
     * will be returned. Else, the neighbour will be validated and the result is offered to the cache.
     * 
     * @param move move applied to the current solution
     * @return <code>true</code> if the obtained neighbour is <b>not</b> rejected,
     *         possibly retrieved from the evaluated move cache
     */
    protected boolean validateMove(Move<? super SolutionType> move){
        // check cache
        Boolean reject = cache.getCachedMoveRejection(move);
        if(reject != null){
            // cache hit: return cached value
            return !reject;
        } else {
            // cache miss: validate and cache
            move.apply(curSolution);                                // apply move
            reject = getProblem().rejectSolution(curSolution);      // validate neighbour
            cache.cacheMoveRejection(move, reject);                 // cache validity
            move.undo(curSolution);                                 // undo move
            return !reject;                                         // return validity
        }
    }
    
    /**
     * Checks whether the given move leads to an improvement when being applied to the current solution.
     * An improvement is made if and only if the given move is <b>not</b> <code>null</code>, the neighbour
     * obtained by applying the move is <b>not</b> rejected (see {@link Problem#rejectSolution(Solution)})
     * and this neighbour has a better evaluation than the current solution (i.e. a positive delta is
     * observed, see {@link #computeDelta(double, double)}).
     * <p>
     * Note that computed values are cached to prevent multiple evaluations or validations of the same move.
     * 
     * @param move move to be applied to the current solution
     * @return <code>true</code> if applying this move yields an improvement
     */
    protected boolean isImprovement(Move<? super SolutionType> move){
        return move != null
                && validateMove(move)
                && computeDelta(evaluateMove(move), curSolutionEvaluation) > 0;
    }
    
    /**
     * Given a collection of possible moves, get the move which yields the largest delta (see {@link #computeDelta(double, double)})
     * when applying it to the current solution, where only those moves leading to a valid neighbour are considered (those moves for
     * which {@link Problem#rejectSolution(Solution)} returns <code>false</code>). If <code>positiveDeltasOnly</code> is set to
     * <code>true</code>, only moves yielding a (strictly) positive delta, i.e. an improvement, are considered. May return
     * <code>null</code> if all moves lead to invalid solutions, or if no valid move with positive delta is found, in case
     * <code>positiveDeltasOnly</code> is set to <code>true</code>.
     * <p>
     * Note that all computed values are cached to prevent multiple evaluations or validations of the same move. Before returning
     * the selected "best" move, if any, its evaluation and validity are cached again to maximize the probability that these values
     * will remain available in the cache.
     * 
     * @param moves collection of possible moves
     * @param positiveDeltasOnly if set to <code>true</code>, only moves with <code>delta &gt; 0</code> are considered
     * @return valid move with largest delta, may be <code>null</code>
     */
    protected Move<? super SolutionType> getMoveWithLargestDelta(Collection<? extends Move<? super SolutionType>> moves, boolean positiveDeltasOnly){
        // track best move and corresponding delta
        Move<? super SolutionType> bestMove = null, curMove;
        double bestMoveDelta = -Double.MAX_VALUE, curMoveDelta, curMoveEval;
        Double bestMoveEval = null;
        // go through all moves
        Iterator<? extends Move<? super SolutionType>> it = moves.iterator();
        while(it.hasNext()){
            curMove = it.next();
            // validate move
            if(validateMove(curMove)){
                // evaluate move
                curMoveEval = evaluateMove(curMove);
                // compute delta
                curMoveDelta = computeDelta(curMoveEval, curSolutionEvaluation);
                // compare with current best move
                if(curMoveDelta > bestMoveDelta                             // higher delta
                        && (!positiveDeltasOnly || curMoveDelta > 0)){      // ensure positive delta, if required
                    bestMove = curMove;
                    bestMoveDelta = curMoveDelta;
                    bestMoveEval = curMoveEval;
                }
            }
        }
        // recache best move, if any
        if(bestMove != null){
            cache.cacheMoveRejection(bestMove, false);              // best move is surely not rejected
            cache.cacheMoveEvaluation(bestMove, bestMoveEval);      // cache best move evaluation 
        }
        // return best move
        return bestMove;
    }
    
    /**
     * Accept the given move by applying it to the current solution. Updates the evaluation of the current solution and compares
     * it with the currently known best solution to check whether a new best solution has been found. Note that this method does
     * <b>not</b> verify whether the given move yields a valid neighbour, but assumes that this has already been checked <i>prior</i>
     * to deciding to accept the move. Therefore, it should <b>never</b> be called with a move that results in a solution for which
     * {@link Problem#rejectSolution(Solution)} returns <code>true</code>.
     * <p>
     * After updating the current solution, the evaluated move cache is cleared as this cache is no longer valid for the new current
     * solution. Furthermore, any neighbourhood search listeners are informed and the number of accepted moves is updated.
     * 
     * @param move accepted move to be applied to the current solution
     */
    protected void acceptMove(Move<? super SolutionType> move){
        // update evaluation (likely present in cache)
        curSolutionEvaluation = evaluateMove(move); 
        // apply move to current solution
        move.apply(curSolution);
        // clear evaluated move cache
        cache.clear();
        // update best solution
        updateBestSolution(curSolution, curSolutionEvaluation);
        // increase accepted move counter
        numAcceptedMoves++;
        // inform listeners
        fireModifiedCurrentSolution(curSolution, curSolutionEvaluation);
    }
    
    /**
     * Rejects the given move. This method only updates the rejected move counter. If this method
     * is called for every rejected move, the number of rejected moves will be correctly reported.
     * 
     * @param move rejected move
     */
    protected void rejectMove(Move<? super SolutionType> move){
        numRejectedMoves++;
    }
    
}
