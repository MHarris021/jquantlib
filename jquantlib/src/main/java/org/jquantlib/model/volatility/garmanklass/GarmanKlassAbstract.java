/*
 Copyright (C) 2006 Joseph Wang

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

package org.jquantlib.model.volatility.garmanklass;

import java.util.List;

import org.jquantlib.math.IntervalPrice;
import org.jquantlib.model.volatility.LocalVolatilityEstimator;
import org.jquantlib.util.Date;
import org.jquantlib.util.TimeSeries;

/**
 * Garman-Klass volatility model
 * <p>
 * This class implements a concrete volatility model based on high low formulas using the method of
 * Garman and Klass in their paper "On the Estimation of the Security Price from Historical Data" at
 * http://www.fea.com/resources/pdf/a_estimation_of_security_price.pdf
 * <p>
 * Volatilities are assumed to be expressed on an annual basis.
 * 
 * @author Anand Mani
 */
public abstract class GarmanKlassAbstract implements LocalVolatilityEstimator<IntervalPrice> {

	private final/* @Real */double yearFraction;

	public GarmanKlassAbstract(final/* @Real */double y) {
		this.yearFraction = y;
	}

	protected abstract/* @Real */Double calculatePoint(final IntervalPrice p);

	@Override
	public TimeSeries<Double> calculate(TimeSeries<IntervalPrice> quoteSeries) {
		final List<Date> dates = quoteSeries.dates();
		final List<IntervalPrice> values = quoteSeries.values();
		TimeSeries</* @Volatility */Double> retval = new TimeSeries</* @Volatility */Double>();
		IntervalPrice cur = null;
		for (int i = 1; i < values.size(); i++) {
			cur = values.get(i);
			double s = calculatePoint(cur) / Math.sqrt(yearFraction);
			retval.add(dates.get(i), s);
		}
		return retval;
	}

}
