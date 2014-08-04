/*
 * Copyright 2014 Ghent University, Bayer CropScience.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jamesframework.core.search.neigh.subset;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.jamesframework.core.problems.solutions.SubsetSolution;
import org.jamesframework.core.search.neigh.Move;
import org.jamesframework.core.search.neigh.Neighbourhood;
import org.jamesframework.core.util.SetUtilities;

/**
 * <p>
 * A subset neighbourhood that generates swap moves only (see {@link SwapMove}). When applying moves generated by this neighbourhood
 * to a given subset solution, the set of selected IDs will always remain of the same size. Therefore, this neighbourhood is only
 * suited for fixed size subset selection problems. If desired, a set of fixed IDs can be provided which are not allowed to be
 * swapped.
 * </p>
 * <p>
 * Note that this neighbourhood is thread-safe: it can be safely used to concurrently generate moves in different searches running
 * in separate threads.
 * </p>
 * 
 * @author <a href="mailto:herman.debeukelaer@ugent.be">Herman De Beukelaer</a>
 */
public class SingleSwapNeighbourhood implements Neighbourhood<SubsetSolution> {

    // set of fixed IDs
    private final Set<Integer> fixedIDs;
    
    /**
     * Creates a basic single swap neighbourhood.
     */
    public SingleSwapNeighbourhood(){
        this(null);
    }
    
    /**
     * Creates a single swap neighbourhood with a given set of fixed IDs which are not allowed to be swapped. None of
     * the generated swap moves will add nor remove any of these IDs.
     * 
     * @param fixedIDs set of fixed IDs which are not allowed to be swapped
     */
    public SingleSwapNeighbourhood(Set<Integer> fixedIDs){
        this.fixedIDs = fixedIDs;
    }
    
    /**
     * Generates a random swap move for the given subset solution that removes a single ID from the set of currently selected IDs,
     * and replaces it with a random ID taken from the set of currently unselected IDs. Possible fixed IDs are not considered to be
     * swapped. If no swap move can be generated, <code>null</code> is returned.
     * 
     * @param solution solution for which a random swap move is generated
     * @return random swap move, <code>null</code> if no swap move can be generated
     */
    @Override
    public Move<SubsetSolution> getRandomMove(SubsetSolution solution) {
        // get set of candidate IDs for deletion and addition
        Set<Integer> deleteCandidates = solution.getSelectedIDs();
        Set<Integer> addCandidates = solution.getUnselectedIDs();
        // remove fixed IDs, if any, from candidates
        if(fixedIDs != null && !fixedIDs.isEmpty()){
            deleteCandidates = new HashSet<>(deleteCandidates);
            addCandidates = new HashSet<>(addCandidates);
            deleteCandidates.removeAll(fixedIDs);
            addCandidates.removeAll(fixedIDs);
        }
        // check if swap is possible
        if(deleteCandidates.isEmpty() || addCandidates.isEmpty()){
            // impossible to perform a swap
            return null;
        }
        // use thread local random for better concurrent performance
        Random rg = ThreadLocalRandom.current();
        // select random ID to remove from selection
        int del = SetUtilities.getRandomElement(deleteCandidates, rg);
        // select random ID to add to selection
        int add = SetUtilities.getRandomElement(addCandidates, rg);
        // create and return swap move
        return new SwapMove(add, del);
    }

    /**
     * Generates a set of all possible swap moves that transform the given subset solution by removing a single ID from
     * the current selection and replacing it with a new ID which is currently not selected. Possible fixed IDs are not 
     * considered to be swapped. May return an empty set if no swap moves can be generated.
     * 
     * @param solution solution for which all possible swap moves are generated
     * @return set of all swap moves, may be empty
     */
    @Override
    public Set<Move<SubsetSolution>> getAllMoves(SubsetSolution solution) {
        // create empty set to store generated moves
        Set<Move<SubsetSolution>> moves = new HashSet<>();
        // get set of candidate IDs for deletion and addition
        Set<Integer> deleteCandidates = solution.getSelectedIDs();
        Set<Integer> addCandidates = solution.getUnselectedIDs();
        // remove fixed IDs, if any, from candidates
        if(fixedIDs != null && !fixedIDs.isEmpty()){
            deleteCandidates = new HashSet<>(deleteCandidates);
            addCandidates = new HashSet<>(addCandidates);
            deleteCandidates.removeAll(fixedIDs);
            addCandidates.removeAll(fixedIDs);
        }
        // first check if swaps are possible, for efficiency (avoids unnecessary loops)
        if(deleteCandidates.isEmpty() || addCandidates.isEmpty()){
            // no swap moves can be applied, return empty set
            return moves;
        }
        // go through all possible IDs to delete
        for(int del : deleteCandidates){
            // go through all possible IDs to add
            for(int add : addCandidates){
                // add corresponding swap move
                moves.add(new SwapMove(add, del));
            }
        }
        // return swap moves
        return moves;
    }

}
