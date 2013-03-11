package edu.mayo.mprc.chem;

/**
 * MERCURY6 is a version of MERCURY5 that omitts outputing flanking
 * zeros in the ouput file.
 * <p/>
 * MERCURY5 is a version of MERCURY2 using (mostly) double percision.
 * instead of floating point arithmatic. It gives more accurate
 * intensity values than MERCURY2.
 * <p/>
 * MERCURY2 is an integer based version of MERCURY, although most of
 * the arithmetic is floating point. Using integer (changed to float)
 * values for isotopic masses, the calculation can be performed with
 * a much smaller data set and is extremely fast.  The ASCII output
 * file is a stick representation of the mass spectrum. There is no
 * ultrahigh resolution mode in this program.
 * <p/>
 * <dl>
 * <dt>Algorithm by</dt><dd>Alan L. Rockwood</dd>
 * <dt>Programming by</dt><dd>Steve Van Orden</dd>
 * <dt>C++ Mangling by</dt><dd>Christopher Mason</dd>
 * <dt>C++ to Java conversion by</dt><dd>Roman Zenka</dd>
 * </dl>
 */
public final class Mercury6 {

	private static final double PI = 3.14159265358979323846;
	private static final double TWOPI = 6.28318530717958647;
	private static final double HALFPI = 1.57079632679489666;

	private Mercury6() {
	}

	private static double electronMass(final Chemical chem) {
		return chem.getPeriodicTable().getElectronMass();
	}

	/**
	 * Number of stable isotopes
	 */
	private static int EZNI(final Chemical chem, final int z) {
		return chem.getElement(z).getNumIsotopes();
	}

	/**
	 * Isotopic Mass
	 */
	private static double EZM(final Chemical chem, final int z, final int i) {
		return chem.getElement(z).getIsotope(i).getMass();
	}

	/**
	 * Integer Isotopic Mass
	 */
	private static double EZI(final Chemical chem, final int z, final int i) {
		return chem.getElement(z).getIsotope(i).getIntMass();
	}

	/**
	 * Isopopic probability ("abundance")
	 */
	private static double EZP(final Chemical chem, final int z, final int i) {
		return chem.getElement(z).getIsotope(i).getAbundance();
	}

	/**
	 * Number of occurances of element in molecular formula
	 */
	private static double EZNA(final Chemical chem, final int z) {
		return chem.getAtomCount(z);
	}

	/**
	 * Chemicals contain Elements in that Chemical numbered 0..n-1; whereas his struct array contained all Elements.
	 */
	private static int atomicNum(final int i) {
		return i;
	}

	/**
	 * Called by main()
	 */
	private static double[] calcVariances(final double[] molVarAndIntMolVar, final int numElements, final Chemical chem) {
		double Var, intVar;
		double avemass, intAveMass;

		molVarAndIntMolVar[0] = 0;
		molVarAndIntMolVar[1] = 0;
		for (int i = 0; i < numElements; i++) {
			final int z = atomicNum(i);
			avemass = 0;
			intAveMass = 0;
			for (int j = 0; j < EZNI(chem, z); j++) {
				avemass += EZM(chem, z, j) * EZP(chem, z, j);
				intAveMass += EZI(chem, z, j) * EZP(chem, z, j);
			}
			Var = 0;
			intVar = 0;
			for (int j = 0; j < EZNI(chem, z); j++) {
				Var += (EZM(chem, z, j) - avemass) * (EZM(chem, z, j) - avemass) * EZP(chem, z, j);
				intVar += (EZI(chem, z, j) - intAveMass) * (EZI(chem, z, j) - intAveMass) * EZP(chem, z, j);
			}
			molVarAndIntMolVar[0] += EZNA(chem, z) * Var;
			molVarAndIntMolVar[1] += EZNA(chem, z) * intVar;
		}
		return molVarAndIntMolVar;
	}

	/**
	 * Called by main()
	 */
	static int calcMassRange(final double molVar, final int charge, final int type) {
		int i;
		int MassRange;

		if ((type == 1) || (charge == 0)) {
			MassRange = (int) (Math.sqrt(1 + molVar) * 10);
		} else {
			MassRange = (int) (Math.sqrt(1 + molVar) * 10 / charge);  /* +/- 5 sd's : Multiply charged */
		}

		/* Set to nearest (upper) power of 2 */
		for (i = 1024; i > 0; i /= 2) {
			if (i < MassRange) {
				MassRange = i * 2;
				i = 0;
			}
		}

		return MassRange;
	}

	/**
	 * Could be done with less code, but this
	 * saves a few operations.
	 */
	static void calcFreq(final double[] freqData, final int ecount, final int numPoints, final int massRange, final long massShift, final Chemical chem) {
		int i;
		double freq;

		/* Calculate first half of Frequency Domain (+)masses */
		for (i = 1; i <= numPoints / 2; i++) {
			freq = (double) (i - 1) / massRange;
			calculateHalf(freqData, ecount, massShift, chem, i, freq);
		}

		/* Calculate second half of Frequency Domain (-)masses */
		for (i = numPoints / 2 + 1; i <= numPoints; i++) {
			freq = (double) (i - numPoints - 1) / massRange;
			calculateHalf(freqData, ecount, massShift, chem, i, freq);
		}
	}

	private static void calculateHalf(final double[] freqData, final int ecount, final long massShift, final Chemical chem, final int i, final double freq) {
		double r;
		double theta;
		double imag;
		double real;
		double X;
		double tempr;
		final double a;
		final double b;
		final double c;
		final double d;
		r = 1;
		theta = 0;
		for (int j = 0; j < ecount; j++) {
			final int z = atomicNum(j);
			real = 0;
			imag = 0;
			for (int k = 0; k < EZNI(chem, z); k++) {
				X = TWOPI * EZI(chem, z, k) * freq;
				real += EZP(chem, z, k) * Math.cos(X);
				imag += EZP(chem, z, k) * Math.sin(X);
			}

			/* Convert to polar coordinates, r then theta */
			tempr = Math.sqrt(real * real + imag * imag);
			r *= Math.pow(tempr, (int) (EZNA(chem, z)));
			if (real > 0) {
				theta += EZNA(chem, z) * Math.atan(imag / real);
			} else if (real < 0) {
				theta += EZNA(chem, z) * (Math.atan(imag / real) + PI);
			} else if (imag > 0) {
				theta += EZNA(chem, z) * HALFPI;
			} else {
				theta += EZNA(chem, z) * -HALFPI;
			}

		}  /* end for(j) */

		/* Convert back to real:imag coordinates and store */
		a = r * Math.cos(theta);
		b = r * Math.sin(theta);
		c = Math.cos(TWOPI * massShift * freq);
		d = Math.sin(TWOPI * massShift * freq);
		freqData[2 * i - 1] = a * c - b * d; /* real data in odd index */
		freqData[2 * i] = b * c + a * d;   /* imag data in even index */
	}

	/**
	 * Taken from Numerical Recipies in C, 2nd Ed.
	 * Changed to work with double (not float).
	 * <p/>
	 * If isign=1 FFT, isign=-1 IFFT
	 */
	static void four1(final double[] data, final int nn, final int isign) {
		int i, j, m, n, mmax, istep;
		double wr, wpr, wpi, wi, theta;
		double wtemp, tempr, tempi;

		/* Perform bit reversal of data[] */
		n = nn << 1;
		j = 1;
		for (i = 1; i < n; i += 2) {
			if (j > i) {
				wtemp = data[i];
				data[i] = data[j];
				data[j] = wtemp;
				wtemp = data[i + 1];
				data[i + 1] = data[j + 1];
				data[j + 1] = wtemp;
			}
			m = n >> 1;
			while (m >= 2 && j > m) {
				j -= m;
				m >>= 1;
			}
			j += m;
		}

		/* Perform Danielson-Lanczos section of FFT */
		n = nn << 1;
		mmax = 2;
		while (n > mmax)  /* Loop executed log(2)nn times */ {
			istep = mmax << 1;
			theta = isign * (TWOPI / mmax);  /* Initialize the trigonimetric recurrance */
			wtemp = Math.sin(0.5 * theta);
			wpr = -2.0 * wtemp * wtemp;
			wpi = Math.sin(theta);
			wr = 1.0;
			wi = 0.0;
			for (m = 1; m < mmax; m += 2) {
				for (i = m; i <= n; i += istep) {
					j = i + mmax;                       /* The Danielson-Lanczos formula */
					tempr = wr * data[j] - wi * data[j + 1];
					tempi = wr * data[j + 1] + wi * data[j];
					data[j] = data[i] - tempr;
					data[j + 1] = data[i + 1] - tempi;
					data[i] += tempr;
					data[i + 1] += tempi;
				}
				wtemp = wr;
				wr = wr * wpr - wi * wpi + wr;
				wi = wi * wpr + wtemp * wpi + wi;
			}
			mmax = istep;
		}

		/* Normalize if FT */
		if (isign == 1) {
			for (i = 1; i <= nn; i++) {
				data[2 * i - 1] /= nn;
				data[2 * i] /= nn;
			}
		}

	}

	static void fillInDistribution(final MassIntensityArray a, final double[] data, final int numPoints, final int ptsPerAmu,
	                               final float mw, final double tempMW, final long intMW, final long mIintMW, final int charge,
	                               final double molVar, final double intMolVar, final PeriodicTable pt) {
		int i;
		double mass;
		double maxint = 0;
		final double ratio;
		final double CorrIntMW;

		/* Normalize intensity to 0%-100% scale */
		for (i = 1; i < 2 * numPoints; i += 2) {
			if (data[i] > maxint) {
				maxint = data[i];
			}
		}
		for (i = 1; i < 2 * numPoints; i += 2) {
			data[i] = 100 * data[i] / maxint;
		}

		if (intMolVar == 0) {
			ratio = 1;
		} else {
			ratio = Math.sqrt(molVar) / Math.sqrt(intMolVar);
		}
		CorrIntMW = tempMW * ratio;
		for (i = numPoints / 2 + 1; i <= numPoints; i++) {
			mass = (double) (i - numPoints - 1) / ptsPerAmu + intMW;
/*      mass += MIMW - mIintMW; */
			mass *= ratio;
			mass += mw - CorrIntMW;
			mass += (-charge) * pt.getElectronMass();
			mass /= charge;
			a.add(Math.abs(mass), data[2 * i - 1]);
		}
		for (i = 1; i <= numPoints / 2; i++) {
			mass = (double) (i - 1) / ptsPerAmu + intMW;
/*      mass += mw - mIintMW; */
			mass *= ratio;
			mass += mw - CorrIntMW;
			mass += (-charge) * pt.getElectronMass();
			mass /= charge;
			a.add(Math.abs(mass), data[2 * i - 1]);
		}
	}

	public static void mercury6(final Chemical chem, int charge, final MassIntensityArray a) {
		int numElements = 0;			/* Number of elements in molecular formula */
		final int MassRange;
		final int PtsPerAmu;
		final int NumPoints;			/* Working # of datapoints (real:imag) */
		final double[] FreqData;			/* Array of real:imaginary frequency values for FFT */
		float MW;
		double tempMW;
		final double molVar;
		final double intMolVar;
		final long intMW;
		long mIintMW;

		numElements = chem.getNumElements();

		MW = 0;
		tempMW = 0;
		mIintMW = 0;
		for (int j = 0; j < numElements; j++) {
			final int z = atomicNum(j);
			for (int k = 0; k < EZNI(chem, z); k++) {
				MW += EZNA(chem, z) * EZM(chem, z, k) * EZP(chem, z, k);
				//cout << EZNA(Z) << " " << EZM(Z,k) << " " << EZP(Z,k) << endl;
				tempMW += EZNA(chem, z) * EZI(chem, z, k) * EZP(chem, z, k);
				if (k == 0) {
					mIintMW += EZNA(chem, z) * EZI(chem, z, k);
				}
			}
		}
		MW -= electronMass(chem) * (float) charge;
		tempMW -= electronMass(chem) * (float) charge;
		intMW = (long) (tempMW + 0.5);

		/* Calculate mass range to use based on molecular variance */
		final double[] molIntVar = new double[2];
		calcVariances(molIntVar, numElements, chem);
		molVar = molIntVar[0];
		intMolVar = molIntVar[1];
		MassRange = calcMassRange(molVar, charge, 1);
		PtsPerAmu = 1;

		/* Allocate memory for Axis arrays */
		NumPoints = MassRange * PtsPerAmu;
		FreqData = new double[2 * NumPoints + 1];

		/* Start isotope distribution calculation */
		calcFreq(FreqData, numElements, NumPoints, MassRange, -intMW, chem);

		four1(FreqData, NumPoints, -1);

		if (charge == 0) {
			charge = 1;
		}
		fillInDistribution(a, FreqData, NumPoints, PtsPerAmu, MW, tempMW, intMW, mIintMW, charge, molVar, intMolVar, chem.getPeriodicTable());
	}
}
