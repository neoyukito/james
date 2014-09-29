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

package org.jamesframework.core.problems.constraints;

/**
 * <p>
 * Interface of a validation produced by a penalizing constraint. Extends the main {@link Validation}
 * interface with an additional method {@link #getPenalty()} to access the assigned penalty. A predefined
 * simple implementation is provided that wraps a double value (see {@link SimplePenalizingValidation}).
 * </p>
 * <p>
 * When implementing custom delta evaluations, the evaluation of the current solution of a neighbourhood
 * search is passed back to the problem to evaluate a move. Knowing only the double value of the current
 * solution's evaluation might not be sufficient to efficiently evaluate the modified solution. In such
 * case, custom evaluation objects can be designed that keep track of any additional metadata used for
 * efficient delta evaluation.
 * </p>
 * 
 * @author <a href="mailto:herman.debeukelaer@ugent.be">Herman De Beukelaer</a>
 */
public interface PenalizingValidation extends Validation {

    /**
     * Get the assigned penalty. Should return 0 if the corresponding solution is valid,
     * i.e. if {@link #passed()} returns <code>true</code>, and a positive double value
     * if the solution did not pass validation.
     * 
     * @return assigned penalty
     */
    public double getPenalty();
    
}
