/*
 Copyright (C) 2007 Richard Gomes

 This file is part of JQuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://jquantlib.org/

 JQuantLib is free software: you can redistribute it and/or modify it
 under the terms of the QuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <jquantlib-dev@lists.sf.net>. The license is also available online at
 <http://jquantlib.org/license.shtml>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
 
 JQuantLib is based on QuantLib. http://quantlib.org/
 When applicable, the originating copyright notice follows below.
 */

/*
 Copyright (C) 2002, 2003, 2006 Ferdinando Ametrano
 Copyright (C) 2004, 2005, 2006, 2007 StatPro Italia srl

 This file is part of QuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://quantlib.org/

 QuantLib is free software: you can redistribute it and/or modify it
 under the terms of the QuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <quantlib-dev@lists.sf.net>. The license is also available online at
 <http://quantlib.org/license.shtml>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
*/

package org.jquantlib.math.interpolation;

import static org.jquantlib.math.Closeness.isClose;
import cern.colt.Sorting;


public abstract class Interpolation2D implements Extrapolator {


	protected double[] vx;
	protected double[] vy;
	protected double[][] mz;
	

	protected Interpolation2D(final double[] x, final double[] y, final double[][] z) {
		vx = x;
		vy = y;
		if (vx.length < 2 || vy.length < 2)
			throw new IllegalArgumentException("not enough points to interpolate");
		for (int i = 0; i < vx.length-1; i++) {
			if ( vx[i] > vx[i+1] )
				throw new IllegalArgumentException("unsorted values on array X");
			if ( vy[i] > vy[i+1] )
				throw new IllegalArgumentException("unsorted values on array Y");
		}
	}
	
	/**
	 * This method does not have anything to do with Observer.update()
	 */
	public abstract void calculate();
	public abstract double getValue(final double x, final double y);
	
	
	public final double getValue(final double x, final double y, boolean allowExtrapolation) {
        checkRange(x, y, allowExtrapolation);
        return getValue(x, y);
    }
    
	public void update() {
		calculate();
	}
	
	public double xMin() {
		return vx[0]; // get first element
	}

	public double xMax() {
		return vx[vx.length-1]; // get last element
	}

	public double yMin() {
		return  vy[0]; // get first element
	}

	public double yMax() {
		return vy[vy.length-1]; // get last element
	}

	public final double[] xValues() {
        return vx;
    }
	
	public final double[] yValues() {
        return vy;
    }

	public final double[][] zData() {
        return mz;
    }

    protected int locateX(double x) /* @ReadOnly */ {
        if (x < vx[0])
            return 0;
        else if (x > vx[vx.length-1])
            return vx.length-2;
        else
            return Sorting.binarySearchFromTo(vx, x, 0, vx.length-1)-1;
    }
    
    protected int locateY(double y) /* @ReadOnly */ {
        if (y < vy[0])
            return 0;
        else if (y > vy[vy.length-1])
            return vy.length-2;
        else
            return Sorting.binarySearchFromTo(vy, y, 0, vy.length-1)-1;
    }
    
	protected final boolean isInRange(final double x, final double y) {
		double x1 = xMin(), x2 = xMax();
        boolean xIsInrange = (x >= x1 && x <= x2) || isClose(x,x1) || isClose(x,x2);
        if (!xIsInrange) return false;

        double y1 = yMin(), y2 = yMax();
        return (y >= y1 && y <= y2) || isClose(y,y1) || isClose(y,y2);
    }

	protected final void checkRange(final double x, final double y, boolean extrapolate) {
		if (!(extrapolate || allowsExtrapolation() || isInRange(x, y))) {
			StringBuilder sb = new StringBuilder();
			sb.append("interpolation range is [");
			sb.append(xMin()).append(", ").append(xMax());
			sb.append("] x [");
			sb.append(yMin()).append(", ").append(yMax());
			sb.append("]: extrapolation at (");
			sb.append(x).append(",").append(y);
			sb.append(") not allowed");
			throw new IllegalArgumentException(sb.toString());
		}
	}

    
    
    

  
  
  
	/**
	 * Implements multiple inheritance via delegate pattern to an inner class
	 * 
	 * @see Extrapolator
	 */
	private DefaultExtrapolator delegatedExtrapolator = new DefaultExtrapolator();
	
	public final boolean allowsExtrapolation() {
		return delegatedExtrapolator.allowsExtrapolation();
	}

	public void disableExtrapolation() {
		delegatedExtrapolator.disableExtrapolation();
	}

	public void enableExtrapolation() {
		delegatedExtrapolator.enableExtrapolation();
	}


}
