/*
 Copyright (C) 2008 Srinivas Hasti

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

package org.jquantlib.termstructures.yieldcurves;


// FIXME: move to org.jquantlib.termstructures.yieldcurves


import org.jquantlib.Validate;
import org.jquantlib.cashflow.FloatingRateCoupon;
import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.indexes.IborIndex;
import org.jquantlib.indexes.SwapIndex;
import org.jquantlib.instruments.MakeVanillaSwap;
import org.jquantlib.instruments.VanillaSwap;
import org.jquantlib.quotes.Handle;
import org.jquantlib.quotes.Quote;
import org.jquantlib.quotes.RelinkableHandle;
import org.jquantlib.termstructures.YieldTermStructure;
import org.jquantlib.time.BusinessDayConvention;
import org.jquantlib.time.Calendar;
import org.jquantlib.time.Frequency;
import org.jquantlib.time.Period;
import org.jquantlib.util.Date;
import org.jquantlib.util.stdlibc.Std;

/**
 * Rate helper for bootstrapping over swap rates
 * 
 * @author Richard Gomes
 */
//TODO: use input SwapIndex to create the swap
public class SwapRateHelper extends RelativeDateRateHelper {

    static final /*@Spread*/ double basisPoint = 1.0e-4;
    
    protected Period tenor;
    protected Calendar calendar;
    protected BusinessDayConvention fixedConvention;
    protected Frequency fixedFrequency;
    protected DayCounter fixedDayCount;
    protected IborIndex iborIndex;
    protected VanillaSwap swap;
    protected RelinkableHandle<YieldTermStructure> termStructureHandle;
    protected Handle<Quote> spread;
    protected Period fwdStart;

    
    //
    // public constructors
    //
    
    public SwapRateHelper(
                final Handle<Quote> rate,
                final SwapIndex swapIndex,
                final Handle<Quote> spread,
                final Period fwdStart) {
        super(rate);
        this.tenor = swapIndex.tenor();
        this.calendar = swapIndex.fixingCalendar();
        this.fixedConvention = swapIndex.fixedLegConvention();
        this.fixedFrequency = swapIndex.fixedLegTenor().frequency();
        this.fixedDayCount = swapIndex.dayCounter();
        this.iborIndex = swapIndex.iborIndex();
        this.spread = spread;
        this.fwdStart = fwdStart;

        this.iborIndex.addObserver(this);
        this.spread.addObserver(this);
        initializeDates();
    }

    public SwapRateHelper(
                final Handle<Quote> rate,
                final Period tenor,
                final Calendar calendar,
                final Frequency fixedFrequency,
                final BusinessDayConvention fixedConvention,
                final DayCounter fixedDayCount,
                final IborIndex iborIndex,
                final Handle<Quote> spread,
                final Period fwdStart) {
        super(rate);
        this.tenor = tenor;
        this.calendar = calendar;
        this.fixedConvention = fixedConvention;
        this.fixedFrequency = fixedFrequency;
        this.fixedDayCount = fixedDayCount;
        this.iborIndex = iborIndex;
        this.spread =spread;
        this.fwdStart =fwdStart;
        
        this.iborIndex.addObserver(this);
        this.spread.addObserver(this);
        initializeDates();
    }

    public SwapRateHelper(
                final /*@Rate*/ double rate,
                final Period tenor,
                final Calendar calendar,
                final Frequency fixedFrequency,
                final BusinessDayConvention fixedConvention,
                final DayCounter fixedDayCount,
                final IborIndex iborIndex,
                final Handle<Quote> spread,
                final Period fwdStart) {
        super(rate);
        this.tenor = tenor;
        this.calendar = calendar;
        this.fixedConvention = fixedConvention;
        this.fixedFrequency = fixedFrequency;
        this.fixedDayCount = fixedDayCount;
        this.iborIndex = iborIndex;
        this.spread = spread;
        this.fwdStart = fwdStart;
        
        this.iborIndex.addObserver(this);
        this.spread.addObserver(this);
        initializeDates();
    }

    public SwapRateHelper(
                final /*@Rate*/ double rate,
                final SwapIndex swapIndex,
                final Handle<Quote> spread,
                final Period fwdStart) {
        super(rate);
        this.tenor = swapIndex.tenor();
        this.calendar = swapIndex.fixingCalendar();
        this.fixedConvention = swapIndex.fixedLegConvention();
        this.fixedFrequency = swapIndex.fixedLegTenor().frequency();
        this.fixedDayCount = swapIndex.dayCounter();
        this.iborIndex = swapIndex.iborIndex();
        this.spread = spread;
        this.fwdStart = fwdStart;
            
        this.iborIndex.addObserver(this);
        this.spread.addObserver(this);
        initializeDates();
    }


    //
    // protected methods
    //
    
    /* (non-Javadoc)
     * @see org.jquantlib.termstructures.yield.RelativeDateRateHelper#initializeDates()
     */
    @Override
    //TODO: solve macros
    protected void initializeDates() {
         // dummy ibor index with curve/swap arguments
         IborIndex clonedIborIndex = iborIndex.clone(this.termStructureHandle);
    
         // do not pass the spread here, as it might be a Quote i.e. it can dinamically change
         this.swap = new MakeVanillaSwap(tenor, clonedIborIndex, 0.0, fwdStart)
             .withFixedLegDayCount(fixedDayCount)
             .withFixedLegTenor(new Period(fixedFrequency))
             .withFixedLegConvention(fixedConvention)
             .withFixedLegTerminationDateConvention(fixedConvention)
             .withFixedLegCalendar(calendar)
             .withFloatingLegCalendar(calendar).value();
    
         this.earliestDate = swap.startDate();
    
         // Usually...
         this.latestDate = swap.maturityDate();
    
         // ...but due to adjustments, the last floating coupon might need a later date for fixing
         // #ifdef QL_USE_INDEXED_COUPON
         FloatingRateCoupon lastFloating = (FloatingRateCoupon) swap.floatingLeg().last();
         Date fixingValueDate = iborIndex.valueDate(lastFloating.fixingDate());
         Date endValueDate = iborIndex.maturityDate(fixingValueDate);
         latestDate = Std.getInstance().max(latestDate, endValueDate);
         // #endif
     }

    
    /**
     * Do not set the relinkable handle as an observer.
     * Force recalculation when needed
     * 
     * @param t
     */
    public void setTermStructure(final YieldTermStructure t) {
        // TODO: code review :: please verify against original QL/C++ code
        // ---- termStructureHandle.linkTo( shared_ptr<YieldTermStructure>(t, no_deletion), false);
         termStructureHandle.setLink(t, false);
         
         super.setTermStructure(t);
     }

    
    /* (non-Javadoc)
     * @see org.jquantlib.termstructures.BootstrapHelper#getImpliedQuote()
     */
     public /*@Price*/ double impliedQuote() /* @ReadOnly */ {
         Validate.QL_REQUIRE(termStructure != null, "term structure not set");
         // we didn't register as observers - force calculation
         swap.recalculate();
         
         // weak implementation... to be improved
         /*@Price*/ double floatingLegNPV = swap.floatingLegNPV();
         /*@Spread*/ double spread = this.spread.empty() ? 0.0 : this.spread.getLink().evaluate();
         /*@Price*/ double spreadNPV = swap.floatingLegBPS()/basisPoint*spread;
         /*@Price*/ double totNPV = - (floatingLegNPV+spreadNPV);
         /*@Price*/ double result = totNPV/(swap.fixedLegBPS()/basisPoint);
         return result;
     }

     public /*@Spread*/ double spread() /* @ReadOnly */ {
         return spread.empty() ? 0.0 : spread.getLink().evaluate();
     }

     public VanillaSwap swap() /* @ReadOnly */ {
         return swap;
     }

     public final Period forwardStart() /* @ReadOnly */ {
         return fwdStart;
     }


     //
     // implements TypedVisitable
     //
     
     // TODO: code review :: object model needs to be validated and eventually refactored
     // TODO: code review :: please verify against original QL/C++ code
//     public void accept(final TypedVisitor<Event> v) {
//         Visitor<Event> v1 = (v != null) ? v.getVisitor(this.getClass()) : null;
//         if (v1 != null) {
//             v1.visit(this);
//         } else {
//             super.accept(v);
//         }
//     }
    
}
