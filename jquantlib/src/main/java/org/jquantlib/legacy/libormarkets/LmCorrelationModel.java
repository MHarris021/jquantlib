/*
 Copyright (C) 
 2009 Ueli Hofstetter

 This source code is release under the BSD License.
 
 This file is part of JQuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://jquantlib.org/

 JQuantLib is free software: you can redistribute it and/or modify it
 under the terms of the JQuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <jquant-devel@lists.sourceforge.net>. The license is also available online at
 <http://www.jquantlib.org/index.php/LICENSE.TXT>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
 
 JQuantLib is based on QuantLib. http://quantlib.org/
 When applicable, the original copyright notice follows this notice.
 */

package org.jquantlib.legacy.libormarkets;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import org.jquantlib.JQuantlib;
import org.jquantlib.lang.annotation.Real;
import org.jquantlib.math.Array;
import org.jquantlib.math.Matrix;
import org.jquantlib.math.matrixutilities.PseudoSqrt.SalvagingAlgorithm;
import org.jquantlib.model.Parameter;

public abstract class LmCorrelationModel {

    protected int size_;
    protected List<Parameter> arguments_;

    public LmCorrelationModel(int size, int nArguments) {
        this.size_ = size;
        this.arguments_ = new ArrayList<Parameter>(nArguments);
    }

    public int size() {
        return size_;
    }

    public int factors() {
        return size_;
    }

    public boolean isTimeIndependent() {
        return false;
    }

    public Matrix pseudoSqrt(
    /* @Time */double t, final Array x) {
        return JQuantlib.pseudoSqrt(this.correlation(t, x), SalvagingAlgorithm.Spectral);
    }

    public double correlation(int i, int j, /* @Time */double t, final Array x) {
        // inefficient implementation, please overload in derived classes
        return correlation(t, x).get(i, j);
    }

    public double correlation(int i, int j, /* @Time */double t) {
        // inefficient implementation, please overload in derived classes
        return correlation(t, new Array()).get(i, j);
    }

    public abstract Matrix correlation(
    /* @Time */double t, final Array x);

    public Matrix correlation(
    /* @Time */double t) {
        return correlation(t, new Array());
    }

    public List<Parameter> params() {
        return arguments_;
    }

    public void setParams(final List<Parameter> arguments) {
        arguments_ = arguments;
        generateArguments();
    }

    protected abstract void generateArguments();

}