package org.jquantlib.testsuite.math.interpolations;

import static org.junit.Assert.assertFalse;

import org.jquantlib.math.interpolations.CubicSplineInterpolation;
import org.jquantlib.math.interpolations.factories.NaturalCubicSpline;
import org.jquantlib.math.interpolations.factories.NaturalMonotonicCubicSpline;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NaturalMonotonicCubicSplineInterpolationTest extends
		InterpolationTestBase {

	private final static Logger logger = LoggerFactory.getLogger(NaturalMonotonicCubicSplineInterpolationTest.class);

	public NaturalMonotonicCubicSplineInterpolationTest() {
		logger.info("\n\n::::: "+this.getClass().getSimpleName()+" :::::");
	}

	//TODO: check the locate() method in AbstractInterpolation which leads to java.lang.ArrayIndexOutOfBoundsException. 
	//@Ignore("Not Ready to Run")
	@Test
	public void testNaturalSplineOnRPN15AValues(){
		
		logger.info("Testing Natural Monotonic Cubic Spline interpolation on RPN15A data set...");

	    final double RPN15A_x[] = {7.99, 8.09, 8.19, 8.7, 9.2, 10.0, 12.0, 15.0, 20.0};
	    final double RPN15A_y[] = {0.0, 2.76429e-5, 4.37498e-5, 0.169183, 0.469428, 0.943740, 0.998636, 0.999919, 0.999994};

	    double interpolated;

	    // Natural spline
	    CubicSplineInterpolation interpolation = new NaturalMonotonicCubicSpline().interpolate(RPN15A_x, RPN15A_y);
	    
	    checkValues("MC Natural spline", interpolation, RPN15A_x, RPN15A_y);
	        
	    // good performance
	    double x_bad = 11.0;
	    interpolated = interpolation.evaluate(x_bad);
	    assertFalse("MC Natural spline interpolation good performance unverified"
				+"\n    at x = "+x_bad
				+"\n    interpolated value: "+interpolated
				+"\n    expected value < 1.0",
				interpolated>1.0);

	}
	
	
}
