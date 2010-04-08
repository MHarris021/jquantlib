/*
 Copyright (C) 2008 Richard Gomes

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

/*
 Copyright (C) 2005, 2006, 2007 StatPro Italia srl

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

package org.jquantlib.termstructures.yieldcurves;

import java.lang.reflect.Constructor;
import java.util.List;

import org.jquantlib.QL;
import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.lang.exceptions.LibraryException;
import org.jquantlib.lang.reflect.TypeTokenTree;
import org.jquantlib.math.interpolations.Interpolation;
import org.jquantlib.math.interpolations.Interpolation.Interpolator;
import org.jquantlib.quotes.Handle;
import org.jquantlib.quotes.Quote;
import org.jquantlib.termstructures.Bootstrap;
import org.jquantlib.termstructures.Compounding;
import org.jquantlib.termstructures.InterestRate;
import org.jquantlib.termstructures.RateHelper;
import org.jquantlib.time.Calendar;
import org.jquantlib.time.Date;
import org.jquantlib.time.Frequency;
import org.jquantlib.time.Month;
import org.jquantlib.time.Period;
import org.jquantlib.util.LazyObject;
import org.jquantlib.util.Pair;

/**
 * Piecewise yield term structure
 * <p>
 * This term structure is bootstrapped on a number of interest rate instruments which are passed as a vector of handles to
 * RateHelper instances. Their maturities mark the boundaries of the interpolated segments.
 * <p>
 * Each segment is determined sequentially starting from the earliest period to the latest and is chosen so that the instrument
 * whose maturity marks the end of such segment is correctly repriced on the curve.
 *
 * @note The bootstrapping algorithm will raise an exception if any two instruments have the same maturity date.
 *
 *
 * @category yieldtermstructures
 *
 * @author Richard Gomes
 */
public class PiecewiseYieldCurve<
                C extends Traits,
                I extends Interpolator,
                B extends Bootstrap>
        extends LazyObject implements PiecewiseCurve<I> {

    //
    // private final fields
    //

    //std::vector<boost::shared_ptr<typename Traits::helper> > instruments_;
    private final RateHelper[] instruments;

    private final Handle<Quote>[] jumps;
    private final int nJumps;
    private final double accuracy;

    //
    // private fields
    //

    private Date[] jumpDates_;
    private /*@Time*/ double[] jumpTimes;
    private Date latestReference;


    //XXX
    //
    //    /**
    //     * Intended to hold a reference to <code>this</code> instance.
    //     */
    //    // typedef PiecewiseYieldCurve<Traits,Interpolator,Bootstrap> this_curve;
    //    private final PiecewiseCurve thisCurve;

    /**
     * Intended to hold a reference to a concrete implementation of <code>super</code>
     * as the base class of PiecewiseYieldCurve is virtual.
     * <p>
     * In order to provide a virtual base class in Java, we do not extend directly
     * but we implement the interface that the base class needs to implement and
     * we use a Delegate Pattern in order to forward calls to the base class.
     * <p>
     * The actual concrete implementation of <code>super</code> is chosen based
     * on class parameter <code>C</code>. The other class parameter <code>I</code>
     * is passed to the constructor.
     */
    // typedef typename Traits::template curve<Interpolator>::type base_curve;
    private final Traits.Curve baseCurve;






    //
    // package private fields
    //

    // THESE ARE ORIGINAL COMMENTS FROM QuantLib/C++ sources:
    //
    // bootstrapper classes are declared as friend to manipulate
    // the curve data. They might be passed the data instead, but
    // it would increase the complexity---which is high enough
    // already.
    //
    // friend class Bootstrap<this_curve>;
    // friend class BootstrapError<this_curve> ;
    // friend class PenaltyFunction<this_curve>;


    //
    // intended to hold class type of generic parameters
    //
    final private Traits        traits;
    final private Interpolator  interpolator;
    final private Bootstrap     bootstrap;



    public PiecewiseYieldCurve(
            final Date referenceDate,
            final RateHelper[] instruments,
            final DayCounter dayCounter,
            final Handle<Quote>[] jumps,
            final Date[] jumpDates) {
        this(referenceDate, instruments, dayCounter, jumps, jumpDates,
                1.0e-12,
                new TypeTokenTree(PiecewiseYieldCurve.class).getElement(1),
                new TypeTokenTree(PiecewiseYieldCurve.class).getElement(2));
    }

    public PiecewiseYieldCurve(
            final Date referenceDate,
            final RateHelper[] instruments,
            final DayCounter dayCounter,
            final Handle<Quote>[] jumps,
            final Date[] jumpDates,
            final /*@Real*/ double accuracy) {
        this(referenceDate, instruments, dayCounter, jumps, jumpDates,
                accuracy,
                new TypeTokenTree(PiecewiseYieldCurve.class).getElement(1),
                new TypeTokenTree(PiecewiseYieldCurve.class).getElement(2));
    }

    public PiecewiseYieldCurve(
            final Date referenceDate,
            final RateHelper[] instruments,
            final DayCounter dayCounter,
            final Handle<Quote>[] jumps,
            final Date[] jumpDates,
            final /*@Real*/ double accuracy, //TODO: default value: 1.0e-12
            final Class<?> interpolator) {
        this(referenceDate, instruments, dayCounter, jumps, jumpDates,
                accuracy,
                constructInterpolator(interpolator),
                constructBootstrap(new TypeTokenTree(PiecewiseYieldCurve.class).getElement(2)));
    }

    public PiecewiseYieldCurve(
            final Date referenceDate,
            final RateHelper[] instruments,
            final DayCounter dayCounter,
            final Handle<Quote>[] jumps,
            final Date[] jumpDates,
            final /*@Real*/ double accuracy, //TODO: default value: 1.0e-12
            final Class<?> interpolator,
            final Class<?> bootstrap) {
        this(referenceDate, instruments, dayCounter, jumps, jumpDates,
                accuracy,
                constructInterpolator(interpolator),
                constructBootstrap(bootstrap));
    }

    public PiecewiseYieldCurve(
            final Date referenceDate,
            final RateHelper[] instruments,
            final DayCounter dayCounter,
            final Handle<Quote>[] jumps,
            final Date[] jumpDates,
            final /*@Real*/ double accuracy, //TODO: default value: 1.0e-12
            final Interpolator interpolator, //TODO: default value: Interpolator()
            final Bootstrap bootstrap) { //TODO: default value: Bootstrap<this_curve>()

        QL.validateExperimentalMode();

        // retrieve generic parameters
        // *** NOTE:: NOT SURE IF THIS CODE IS CORRECT: I (interpolator) and B (bootstrap) are being passed as formal parameters.
        // *** See InterpolatedDiscountCurve: default formal arguments are "guessed" from generic parameters via ...
        // ***         new TypeTokenTree(InterpolatedDiscountCurve.class).getElement(0)
        // *** in some constructors, which assume defaults.
        final TypeTokenTree ttt = new TypeTokenTree(this.getClass());
        final Class<?> classT = ttt.getElement(0);
        final Class<?> classI = ttt.getElement(1);
        final Class<?> classB = ttt.getElement(2);
        QL.require(classT!=null , "T is null"); // TODO: message
        QL.require(classI!=null , "I is null"); // TODO: message
        QL.require(classB!=null , "B is null"); // TODO: message

        // instantiate base class and call super constructor
        this.baseCurve = constructBaseClass(referenceDate, dayCounter, classI, classT);

        this.traits = constructTraits(classT);
        this.interpolator = constructInterpolator(classI);
        this.bootstrap = constructBootstrap(classB);


        this.instruments = instruments;
        this.jumps = jumps;
        this.nJumps = jumps.length;
        this.jumpDates_ = jumpDates;
        this.jumpTimes = new double[jumpDates.length];
        this.accuracy = accuracy;

        setJumps();
        for (int i=0; i<jumps.length; ++i) {
            jumps[i].addObserver(this);
        }
        bootstrap.setup(this);
    }


    public PiecewiseYieldCurve(
            final /*@Natural*/ int settlementDays,
            final Calendar calendar,
            final RateHelper[] instruments,
            final DayCounter dayCounter,
            final Handle<Quote>[] jumps,
            final Date[] jumpDates,
            final /*@Real*/ double accuracy, //TODO: default value: 1.0e-12
            final Interpolator interpolator, //TODO: default value: Interpolator()
            final Bootstrap bootstrap) { //TODO: default value: Bootstrap<this_curve>()

        QL.validateExperimentalMode();

        // retrieve generic parameters
        // *** NOTE:: NOT SURE IF THIS CODE IS CORRECT: I (interpolator) and B (bootstrap) are being passed as formal parameters.
        // *** See InterpolatedDiscountCurve: default formal arguments are "guessed" from generic parameters via ...
        // ***         new TypeTokenTree(InterpolatedDiscountCurve.class).getElement(0)
        // *** in some constructors, which assume defaults.
        final TypeTokenTree ttt = new TypeTokenTree(this.getClass());
        final Class<?> classT = ttt.getElement(0);
        final Class<?> classI = ttt.getElement(1);
        final Class<?> classB = ttt.getElement(2);
        QL.require(classT!=null , "T is null"); // TODO: message
        QL.require(classI!=null , "I is null"); // TODO: message
        QL.require(classB!=null , "B is null"); // TODO: message

        // instantiate base class and call super constructor
        this.baseCurve = constructBaseClass(settlementDays, calendar, dayCounter, classT);

        this.traits = constructTraits(classT);
        this.interpolator = constructInterpolator(classI);
        this.bootstrap = constructBootstrap(classB);

        this.instruments = instruments;
        this.jumps = jumps;
        this.nJumps = jumps.length;
        this.jumpDates_ = jumpDates;
        this.jumpTimes = new double[jumpDates.length];
        this.accuracy = accuracy;

        setJumps();
        for (int i=0; i<jumps.length; ++i) {
            jumps[i].addObserver(this);
        }
        bootstrap.setup(this);
    }




    static private Traits.Curve constructBaseClass(
            final Date referenceDate,
            final DayCounter dayCounter,
            final Class<?> classI,
            final Class<?> classT) {
        if (classT == Discount.class)
            return new InterpolatedDiscountCurve(referenceDate, dayCounter, classI);
        else if (classT == ForwardRate.class)
            //TODO: this.baseCurve = new InterpolatedForwardCurve(referenceDate, dayCounter, classI);
            throw new UnsupportedOperationException();
        else if (classT == ZeroYield.class)
            //TODO: this.baseCurve = new InterpolatedZeroCurve(referenceDate, dayCounter, classI);
            throw new UnsupportedOperationException();
        else
            throw new LibraryException("only Discount, ForwardRate and ZeroYield are supported"); // TODO: message
    }

    static private Traits.Curve constructBaseClass(
            final /*@Natural*/ int settlementDays,
            final Calendar calendar,
            final DayCounter dayCounter,
            final Class<?> classT) {
        if (classT == Discount.class)
            return new InterpolatedDiscountCurve(settlementDays, calendar, dayCounter, classT);
        else if (classT == ForwardRate.class)
            //TODO: this.baseCurve = new InterpolatedForwardCurve(settlementDays, calendar, dayCounter, classT);
            throw new UnsupportedOperationException();
        else if (classT == ZeroYield.class)
            //TODO: this.baseCurve = new InterpolatedZeroCurve(settlementDays, calendar, dayCounter, classT);
            throw new UnsupportedOperationException();
        else
            throw new LibraryException("only Discount, ForwardRate and ZeroYield are supported"); // TODO: message
    }

    static private Traits constructTraits(final Class<?> classT) {
        if (Traits.class.isAssignableFrom(classT)) {
            try {
                return (Traits) classT.newInstance();
            } catch (final Exception e) {
                throw new LibraryException("could not instantiate Traits", e); // TODO: message
            }
        }

        throw new LibraryException("not a Traits"); // TODO: message
    }

    static private Interpolator constructInterpolator(final Class<?> classI) {
        if (Interpolator.class.isAssignableFrom(classI)) {
            try {
                return (Interpolator) classI.newInstance();
            } catch (final Exception e) {
                throw new LibraryException("could not instantiate Interpolator", e); // TODO: message
            }
        }

        throw new LibraryException("not an Interpolator"); // TODO: message
    }

    static private Bootstrap constructBootstrap(final Class<?> classB) {
        if (Bootstrap.class.isAssignableFrom(classB)) {
            try {
                final Constructor<Bootstrap> c = (Constructor<Bootstrap>) classB.getConstructor(Class.class);
                return c.newInstance(new Object[] { PiecewiseCurve.class });
            } catch (final Exception e) {
                throw new LibraryException("could not instantiate Bootstrap", e); // TODO: message
            }
        }

        throw new LibraryException("not a Bootstrap"); // TODO: message
    }




    //
    // implements PiecewiseCurve
    //

    @Override
    public Traits traits() /* @ReadOnly */ {
        return traits;
    }

    @Override
    public Interpolator interpolator() /* @ReadOnly */ {
        return interpolator;
    }

    public RateHelper[] instruments() /* @ReadOnly */ {
        return instruments;
    }

    @Override
    public double accuracy() {
        return accuracy;
    }

    @Override
    public Date maxDate() /* @ReadOnly */ {
        calculate();
        return baseCurve.maxDate();
    }

    @Override
    public /*@Time*/ double[] times() /* @ReadOnly */ {
        calculate();
        return baseCurve.times();
    }

    @Override
    public Date[] dates() /* @ReadOnly */ {
        calculate();
        return baseCurve.dates();
    }

    @Override
    public double[] data() /* @ReadOnly */ {
        calculate();
        return baseCurve.data();
    }

    @Override
    public List<Pair<Date, Double>> nodes() /* @ReadOnly */ {
        calculate();
        return baseCurve.nodes();
    }

    @Override
    public Date[] jumpDates() /* @ReadOnly */ {
        calculate();
        return baseCurve.dates();
    }

    @Override
    public /*@Time*/ double[] jumpTimes() /* @ReadOnly */ {
        calculate();
        return baseCurve.times();
    }

    @Override
    public void setData(final double[] data) {
        baseCurve.setData(data);
    }

    @Override
    public void setDates(final Date[] dates) {
        baseCurve.setDates(dates);
    }

    @Override
    public void setTimes(final double[] times) {
        baseCurve.setTimes(times);
    }

    @Override
    public Interpolation interpolation() {
        return baseCurve.interpolation();
    }

    @Override
    public void setInterpolation(final Interpolation interpolation) {
        baseCurve.setInterpolation(interpolation);
    }


    //
    // overrides LazyObject
    //

    @Override
    public void update() {
        baseCurve.update();
        super.update();
        if (baseCurve.referenceDate() != latestReference) {
            setJumps();
        }
    }


    //
    // private methods
    //

    public /*@DiscountFactor*/ double discountImpl(final /*@Time*/ double t) /* @ReadOnly */ {
        calculate();

        if (jumps.length > 0) {
            /*@DiscountFactor*/ double jumpEffect = 1.0;
            for (int i=0; i<nJumps && jumpTimes[i]<t; ++i) {
                QL.require(jumps[i].currentLink().isValid(), "invalid jump quote");
                /*@DiscountFactor*/ final double thisJump = jumps[i].currentLink().value();
                QL.require(thisJump > 0.0 && thisJump <= 1.0, "invalid  jump value");
                jumpEffect *= thisJump;
            }
            return jumpEffect * baseCurve.discountImpl(t);
        }

        return baseCurve.discountImpl(t);
    }

    public void setJumps() {
        final Date referenceDate = baseCurve.referenceDate();
        if (jumpDates_.length==0 && jumps.length!=0) { // turn of year dates
            this.jumpDates_ = new Date[nJumps];
            this.jumpTimes = new double[nJumps];
            for (int i=0; i<nJumps; ++i) {
                jumpDates_[i] = new Date(31, Month.December, referenceDate.year()+i);
            }
        } else { // fixed dats
            QL.require(jumpDates_.length==nJumps, "mismatch between number of jumps and jump dates");
        }
        for (int i=0; i<nJumps; ++i) {
            jumpTimes[i] = baseCurve.timeFromReference(jumpDates_[i]);
        }
        this.latestReference = referenceDate;
    }


    // template definitions

    @Override
    public void performCalculations() /* @ReadOnly */ {
        // just delegate to the bootstrapper
        bootstrap.calculate();
    }


    //
    // implements YieldTermStructure
    //

    @Override
    public double discount(final Date d, final boolean extrapolate) {
        return baseCurve.discount(d, extrapolate);
    }

    @Override
    public double discount(final Date d) {
        return baseCurve.discount(d);
    }

    @Override
    public double discount(final double t, final boolean extrapolate) {
        return baseCurve.discount(t, extrapolate);
    }

    @Override
    public double discount(final double t) {
        return baseCurve.discount(t);
    }

    @Override
    public InterestRate forwardRate(final Date d1, final Date d2, final DayCounter dayCounter, final Compounding comp, final Frequency freq, final boolean extrapolate) {
        return baseCurve.forwardRate(d1, d2, dayCounter, comp, freq, extrapolate);
    }

    @Override
    public InterestRate forwardRate(final Date d1, final Date d2, final DayCounter resultDayCounter, final Compounding comp, final Frequency freq) {
        return baseCurve.forwardRate(d1, d2, resultDayCounter, comp, freq);
    }

    @Override
    public InterestRate forwardRate(final Date d1, final Date d2, final DayCounter resultDayCounter, final Compounding comp) {
        return baseCurve.forwardRate(d1, d2, resultDayCounter, comp);
    }

    @Override
    public InterestRate forwardRate(final Date d, final Period p, final DayCounter dayCounter, final Compounding comp, final Frequency freq, final boolean extrapolate) {
        return baseCurve.forwardRate(d, p, dayCounter, comp, freq, extrapolate);
    }

    @Override
    public InterestRate forwardRate(final Date d, final Period p, final DayCounter resultDayCounter, final Compounding comp, final Frequency freq) {
        return baseCurve.forwardRate(d, p, resultDayCounter, comp, freq);
    }

    @Override
    public InterestRate forwardRate(final double time1, final double time2, final Compounding comp, final Frequency freq, final boolean extrapolate) {
        return baseCurve.forwardRate(time1, time2, comp, freq, extrapolate);
    }

    @Override
    public InterestRate forwardRate(final double t1, final double t2, final Compounding comp, final Frequency freq) {
        return baseCurve.forwardRate(t1, t2, comp, freq);
    }

    @Override
    public InterestRate forwardRate(final double t1, final double t2, final Compounding comp) {
        return baseCurve.forwardRate(t1, t2, comp);
    }

    @Override
    public double parRate(final Date[] dates, final Frequency freq, final boolean extrapolate) {
        return baseCurve.parRate(dates, freq, extrapolate);
    }


    @Override
    public double parRate(final double[] times, final Frequency frequency, final boolean extrapolate) {
        return baseCurve.parRate(times, frequency, extrapolate);
    }

    @Override
    public double parRate(final int tenor, final Date startDate, final Frequency freq, final boolean extrapolate) {
        return baseCurve.parRate(tenor, startDate, freq, extrapolate);
    }

    @Override
    public InterestRate zeroRate(final Date d, final DayCounter dayCounter, final Compounding comp, final Frequency freq, final boolean extrapolate) {
        return baseCurve.zeroRate(d, dayCounter, comp, freq, extrapolate);
    }

    @Override
    public InterestRate zeroRate(final Date d, final DayCounter resultDayCounter, final Compounding comp, final Frequency freq) {
        return baseCurve.zeroRate(d, resultDayCounter, comp, freq);
    }

    @Override
    public InterestRate zeroRate(final Date d, final DayCounter resultDayCounter, final Compounding comp) {
        return baseCurve.zeroRate(d, resultDayCounter, comp);
    }

    @Override
    public InterestRate zeroRate(final double time, final Compounding comp, final Frequency freq, final boolean extrapolate) {
        return baseCurve.zeroRate(time, comp, freq, extrapolate);
    }


    //
    // implements TermStructure
    //

    @Override
    public Calendar calendar() {
        return baseCurve.calendar();
    }

    @Override
    public DayCounter dayCounter() {
        return baseCurve.dayCounter();
    }

    @Override
    public double maxTime() {
        return baseCurve.maxTime();
    }

    @Override
    public Date referenceDate() {
        return baseCurve.referenceDate();
    }

    @Override
    public /*@Natural*/ int settlementDays() {
        return baseCurve.settlementDays();
    }

    @Override
    public double timeFromReference(final Date date) {
        return baseCurve.timeFromReference(date);
    }


    //
    // implements Extrapolator
    //

    @Override
    public boolean allowsExtrapolation() {
        return baseCurve.allowsExtrapolation();
    }

    @Override
    public void disableExtrapolation() {
        baseCurve.disableExtrapolation();
    }

    @Override
    public void enableExtrapolation() {
        baseCurve.enableExtrapolation();
    }







    //
    // inner classes
    //


    //TODO: Now there's a top level class called InterpolatedDiscountCurve which probably should be moved here.
    //
    //
    //    /**
    //     * Term structure based on interpolation of discount factors.
    //     *
    //     * @note LogLinear interpolation is assumed by default when no interpolation class is passed to constructors.
    //     * Log-linear interpolation guarantees piecewise-constant forward rates.
    //     *
    //     * @category yieldtermstructures
    //     *
    //     * @author Richard Gomes
    //     */
    //    private class InterpolatedDiscountCurve<I extends Interpolator> extends AbstractTermStructure implements Traits.Curve {
    //
    //        private final boolean isNegativeRates;
    //        private final Interpolation.Interpolator interpolator;
    //
    //        public InterpolatedDiscountCurve(final DayCounter dayCounter, final Class<I> interpolator) {
    //            super(dayCounter);
    //            this.isNegativeRates = new Settings().isNegativeRates();
    //            this.interpolator = (interpolator!=null) ? interpolator.newInstance() : new LogLinear();
    //        }
    //
    //        public InterpolatedDiscountCurve(final Date referenceDate, final DayCounter dayCounter, final Class<I> interpolator) {
    //            super(referenceDate, new Target(), dayCounter); // FIXME: code review :: default calendar
    //            this.isNegativeRates = new Settings().isNegativeRates();
    //            this.interpolator = (interpolator!=null) ? interpolator.newInstance() : new LogLinear();
    //        }
    //
    //        public InterpolatedDiscountCurve(final int settlementDays, final Calendar calendar, final DayCounter dayCounter, final Class<I> interpolator) {
    //            super(settlementDays, calendar, dayCounter);
    //            this.isNegativeRates = new Settings().isNegativeRates();
    //            this.interpolator = (interpolator!=null) ? interpolator.newInstance() : new LogLinear();
    //        }
    //
    //        //TODO: who's calling this constructor???
    //        private InterpolatedDiscountCurve(
    //                final Date[] dates,
    //                final /* @DiscountFactor */ Array discounts,
    //                final DayCounter dayCounter,
    //                final Calendar cal,
    //                final Class<I> interpolator) {
    //            super(dates[0], cal, dayCounter);
    //
    //            QL.require(dates.length > 1 , "too few dates"); // TODO: message
    //            QL.require(dates.length == discounts.size() , "dates/discount factors count mismatch"); // TODO: message
    //            QL.require(discounts.first() == 1.0 , "the first discount must be == 1.0 to flag the corrsponding date as settlement date"); // TODO: message
    //
    //            isNegativeRates = new Settings().isNegativeRates();
    //
    //            container.times = new Array(dates.length);
    //            for (int i = 1; i < dates.length; i++) {
    //                QL.require(dates[i].gt(dates[i-1]) , "dates must be in ascending order"); // TODO: message
    //                QL.require(isNegativeRates || discounts.get(i) >= 0.0 , "negative discount"); // TODO: message
    //                final double value = dayCounter.yearFraction(dates[0], dates[i]);
    //                times.set(i, value);
    //            }
    //
    //            container.dates = dates.clone();
    //            container.data = discounts.clone();
    //            container.interpolator = (interpolator!=null) ? interpolator : (I) new LogLinear();
    //            container.interpolation = container.interpolator.interpolate(container.times, container.data);
    //
    //            container.interpolation.update();
    //        }
    //
    //
    //        //
    //        // implements Traits.Curve
    //        //
    //
    //        @Override
    //        public final Date maxDate() /* @ReadOnly */{
    //            return dates[dates.length - 1];
    //        }
    //
    //        @Override
    //        public final Array times() /* @ReadOnly */{
    //            return times.clone();
    //        }
    //
    //        @Override
    //        public final Date[] dates() /* @ReadOnly */{
    //            return dates.clone();
    //        }
    //
    //        @Override
    //        public final Pair<Date, Double>[] nodes() /* @ReadOnly */{
    //            final Pair<Date, /*@Rate*/Double>[] results = new Pair /* <Date, @Rate Double> */[dates.length];
    //            for (int i = 0; i < dates.length; i++) {
    //                results[i] = new Pair<Date, Double>(dates[i], data.get(i));
    //            }
    //            return results;
    //        }
    //
    //        // exclusive to discount curve
    //        public /* @DiscountFactor */Array discounts() /* @ReadOnly */ {
    //            throw new UnsupportedOperationException();
    //        }
    //
    //        // exclusive to forward curve
    //        public /* @Rate */Array forwards() /* @ReadOnly */ {
    //            return data.clone();
    //        }
    //
    //        // exclusive to zero rate
    //        public /* @Rate */Array zeroRates() /* @ReadOnly */{
    //            throw new UnsupportedOperationException();
    //        }
    //
    //
    //        //
    //        // The following methods should be protected in order to mimick the way it is done in C++
    //        //
    //
    //        @Override
    //        public /* @DiscountFactor */ double discountImpl(final/* @Time */double t) /* @ReadOnly */ {
    //            return interpolation.op(t, true);
    //        }
    //
    //        //XXX
    //        //        @Override
    //        //        public /*@Rate*/ double forwardImpl(/*@Time*/final double t) /* @ReadOnly */{
    //        //            throw new UnsupportedOperationException();
    //        //        }
    //        //
    //        //        @Override
    //        //        public /*@Rate*/ double zeroYieldImpl(/*@Time*/final double t) /* @ReadOnly */{
    //        //            throw new UnsupportedOperationException();
    //        //        }
    //
    //    }



    //TODO: reimplement InterpolatedForwardCurve. Use InterpolatedDiscountCurve as template
    //
    //    /**
    //     * Term structure based on interpolation of forward rates
    //     *
    //     * @category yieldtermstructures
    //     *
    //     * @author Richard Gomes
    //     */
    //    private final class InterpolatedForwardCurve<I extends Interpolator> extends AbstractTermStructure implements Traits.Curve {
    //
    //        private boolean      isNegativeRates;
    //
    //        public InterpolatedForwardCurve(final DayCounter dayCounter, final Class<I> interpolator) {
    //            super(dayCounter);
    //            container.interpolator = (interpolator!=null) ? interpolator : (I) new BackwardFlat();
    //        }
    //
    //        public InterpolatedForwardCurve(final Date referenceDate, final DayCounter dayCounter, final I interpolator) {
    //            super(referenceDate, new Target(), dayCounter); // FIXME: code review:: default calendar
    //            container.interpolator = (interpolator!=null) ? interpolator : (I) new BackwardFlat();
    //        }
    //
    //        public InterpolatedForwardCurve(final int settlementDays, final Calendar calendar, final DayCounter dayCounter, final I interpolator) {
    //            super(settlementDays, calendar, dayCounter);
    //            container.interpolator = (interpolator!=null) ? interpolator : (I) new BackwardFlat();
    //        }
    //
    //        //TODO: who's calling this constructor???
    //        private InterpolatedForwardCurve(final Date[] dates, final /* @Rate */ Array forwards, final DayCounter dayCounter, final I interpolator) {
    //            // FIXME: code review: calendar
    //            super(dates[0], new Target(), dayCounter);
    //            QL.require(dates.length > 1 , "too few dates"); // TODO: message
    //            QL.require(dates.length == forwards.size() , "dates/yields count mismatch"); // TODO: message
    //
    //            isNegativeRates = new Settings().isNegativeRates();
    //            container.times = new Array(dates.length);
    //            for (int i = 1; i < dates.length; i++) {
    //                QL.require(dates[i].gt(dates[i-1]) , "dates must be in ascending order"); // TODO: message
    //                QL.require(isNegativeRates || forwards.get(i) >= 0.0 , "negative forward"); // TODO: message
    //                final double value = dayCounter.yearFraction(dates[0], dates[i]);
    //                times.set(i, value);
    //            }
    //
    //            container.dates = dates.clone();
    //            container.data = forwards.clone();
    //            container.interpolator = (interpolator!=null) ? interpolator : (I) new BackwardFlat();
    //            container.interpolation = container.interpolator.interpolate(container.times, container.data);
    //
    //            container.interpolation.update();
    //        }
    //
    //        //
    //        // implements Traits.Curve
    //        //
    //
    //        @Override
    //        public final Date maxDate() /* @ReadOnly */{
    //            return dates[dates.length - 1];
    //        }
    //
    //        @Override
    //        public final Array times() /* @ReadOnly */{
    //            return times.clone();
    //        }
    //
    //        @Override
    //        public final Date[] dates() /* @ReadOnly */{
    //            return dates.clone();
    //        }
    //
    //        @Override
    //        public final Pair<Date, Double>[] nodes() /* @ReadOnly */{
    //            final Pair<Date, /*@Rate*/Double>[] results = new Pair /* <Date, @Rate Double> */[dates.length];
    //            for (int i = 0; i < dates.length; i++) {
    //                results[i] = new Pair<Date, Double>(dates[i], data.get(i));
    //            }
    //            return results;
    //        }
    //
    //        // exclusive to discount curve
    //        public /* @DiscountFactor */Array discounts() /* @ReadOnly */ {
    //            throw new UnsupportedOperationException();
    //        }
    //
    //        // exclusive to forward curve
    //        public /* @Rate */Array forwards() /* @ReadOnly */ {
    //            return data.clone();
    //        }
    //
    //        // exclusive to zero rate
    //        public /* @Rate */Array zeroRates() /* @ReadOnly */{
    //            throw new UnsupportedOperationException();
    //        }
    //
    //
    //        //
    //        // The following methods should be protected in order to mimick the way it is done in C++
    //        //
    //
    //        @Override
    //        public /* @DiscountFactor */ double discountImpl(final/* @Time */double t) /* @ReadOnly */ {
    //            throw new UnsupportedOperationException();
    //        }
    //
    //        //XXX
    //        //        @Override
    //        //        public /*@Rate*/ double forwardImpl(/*@Time*/final double t) /* @ReadOnly */{
    //        //            return interpolation.op(t, true);
    //        //        }
    //        //
    //        //        @Override
    //        //        public /*@Rate*/ double zeroYieldImpl(/*@Time*/final double t) /* @ReadOnly */{
    //        //            if (t == 0.0) {
    //        //                return forwardImpl(0.0);
    //        //            } else {
    //        //                return interpolation.primitive(t, true) / t;
    //        //            }
    //        //        }
    //
    //    }




    //TODO: reimplement InterpolatedZeroCurve. Use InterpolatedDiscountCurve as template
    //
    //    /**
    //     * Term structure based on interpolation of zero yields
    //     *
    //     * @category yieldtermstructures
    //     *
    //     * @author Richard Gomes
    //     */
    //    private final class InterpolatedZeroCurve<I extends Interpolator> extends AbstractTermStructure implements Traits.Curve {
    //
    //        private boolean isNegativeRates;
    //
    //        public InterpolatedZeroCurve(final DayCounter dayCounter, final Class<I> interpolator) {
    //            super(dayCounter);
    //            container.interpolator = (interpolator!=null) ? interpolator : (I) new BackwardFlat();
    //        }
    //
    //        public InterpolatedZeroCurve(final Date referenceDate, final DayCounter dayCounter, final I interpolator) {
    //            super(referenceDate, new Target(), dayCounter); // FIXME: code review : default calendar?
    //            container.interpolator = (interpolator!=null) ? interpolator : (I) new BackwardFlat();
    //        }
    //
    //        public InterpolatedZeroCurve(final int settlementDays, final Calendar calendar, final DayCounter dayCounter, final I interpolator) {
    //            super(settlementDays,calendar, dayCounter);
    //            container.interpolator = (interpolator!=null) ? interpolator : (I) new BackwardFlat();
    //        }
    //
    //
    //        //
    //        // implements Traits.Curve
    //        //
    //
    //        @Override
    //        public final Date maxDate() /* @ReadOnly */{
    //            return dates[dates.length - 1];
    //        }
    //
    //        @Override
    //        public final Array times() /* @ReadOnly */{
    //            return times.clone();
    //        }
    //
    //        @Override
    //        public final Date[] dates() /* @ReadOnly */{
    //            return dates.clone();
    //        }
    //
    //        @Override
    //        public final Pair<Date, Double>[] nodes() /* @ReadOnly */{
    //            final Pair<Date, /*@Rate*/Double>[] results = new Pair /* <Date, @Rate Double> */[dates.length];
    //            for (int i = 0; i < dates.length; i++) {
    //                results[i] = new Pair<Date, Double>(dates[i], data.get(i));
    //            }
    //            return results;
    //        }
    //
    //        // exclusive to discount curve
    //        public /* @DiscountFactor */Array discounts() /* @ReadOnly */ {
    //            throw new UnsupportedOperationException();
    //        }
    //
    //        // exclusive to forward curve
    //        public /* @Rate */Array forwards() /* @ReadOnly */ {
    //            return data.clone();
    //        }
    //
    //        // exclusive to zero rate
    //        public /* @Rate */Array zeroRates() /* @ReadOnly */{
    //            throw new UnsupportedOperationException();
    //        }
    //
    //
    //        //
    //        // The following methods should be protected in order to mimick the way it is done in C++
    //        //
    //
    //        @Override
    //        public /* @DiscountFactor */ double discountImpl(final/* @Time */double t) /* @ReadOnly */ {
    //            throw new UnsupportedOperationException();
    //        }
    //
    //        //XXX
    //        //        @Override
    //        //        public /*@Rate*/ double forwardImpl(/*@Time*/final double t) /* @ReadOnly */{
    //        //            throw new UnsupportedOperationException();
    //        //        }
    //        //
    //        //        @Override
    //        //        public /*@Rate*/ double zeroYieldImpl(/*@Time*/final double t) /* @ReadOnly */{
    //        //            return interpolation.op(t, true);
    //        //        }
    //
    //    }

    //XXX
    //    private class ObjectiveFunction<C extends Traits, I extends Interpolator> implements Ops.DoubleOp {
    //
    //        private final PiecewiseYieldCurve<C, I> curve;
    //        private final RateHelper rateHelper;
    //        private final int segment;
    //
    //        public ObjectiveFunction(final PiecewiseYieldCurve<C, I> curve, final RateHelper rateHelper, final int segment) {
    //            this.curve = curve;
    //            this.rateHelper = rateHelper;
    //            this.segment = segment;
    //        }
    //
    //        @Override
    //        public double op(final double guess) /* @ReadOnly */{
    //            traits.updateGuess(this.curve.data, guess, this.segment);
    //            curve.interpolation.update();
    //            return rateHelper.quoteError();
    //        }
    //
    //    }

}