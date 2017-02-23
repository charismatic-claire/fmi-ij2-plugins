package org.doube.geometry;

public class Centroid {

	/**
	 * Find the centroid of an array in double[n][i] format, where n = number of
	 * points and i = number of dimensions
	 * 
	 * @param points Array of points
	 * @return centroid coordinates
	 */
	public static double[] getCentroid(final double[][] points) {
		final int nDimensions = points[0].length;

		switch (nDimensions) {
		case 1:
			return getCentroid1D(points);
		case 2:
			return getCentroid2D(points);
		case 3:
			return getCentroid3D(points);
		default:
			return getCentroidND(points);
		}
	}

	/**
	 * Find the centroid of a set of points in double[n][1] format
	 */
	private static double[] getCentroid1D(final double[][] points) {
		final double[] centroid = new double[1];
		double sumX = 0;
		final int nPoints = points.length;

		for (int n = 0; n < nPoints; n++) {
			sumX += points[n][0];
		}

		centroid[0] = sumX / nPoints;

		return centroid;
	}

	/**
	 * Find the centroid of a set of points in double[n][2] format
	 */
	private static double[] getCentroid2D(final double[][] points) {
		final double[] centroid = new double[2];
		double sumX = 0;
		double sumY = 0;
		final int nPoints = points.length;

		for (int n = 0; n < nPoints; n++) {
			sumX += points[n][0];
			sumY += points[n][1];
		}

		centroid[0] = sumX / nPoints;
		centroid[1] = sumY / nPoints;

		return centroid;
	}

	/**
	 * Find the centroid of a set of points in double[n][3] format
	 */
	private static double[] getCentroid3D(final double[][] points) {
		final double[] centroid = new double[3];
		double sumX = 0;
		double sumY = 0;
		double sumZ = 0;
		final int nPoints = points.length;

		for (int n = 0; n < nPoints; n++) {
			sumX += points[n][0];
			sumY += points[n][1];
			sumZ += points[n][2];
		}

		centroid[0] = sumX / nPoints;
		centroid[1] = sumY / nPoints;
		centroid[2] = sumZ / nPoints;

		return centroid;
	}

	/**
	 * Find the centroid of a set of points in double[n][i] format
	 */
	private static double[] getCentroidND(final double[][] points) {
		final int nPoints = points.length;
		final int nDimensions = points[0].length;
		final double[] centroid = new double[nDimensions];
		final double[] sums = new double[nDimensions];

		for (int n = 0; n < nPoints; n++) {
			if (points[n].length != nDimensions)
				throw new IllegalArgumentException(
						"Number of dimensions must be equal");
			for (int i = 0; i < nDimensions; i++) {
				sums[i] += points[n][i];
			}
		}

		for (int i = 0; i < nDimensions; i++) {
			centroid[i] = sums[i] / nPoints;
		}

		return centroid;
	}

	/**
	 * Return the centroid of a 1D array, which is its mean value
	 * 
	 * @param points Array of 1D points
	 * @return mean of the points
	 */
	public static double getCentroid(final double[] points) {
		final int nPoints = points.length;
		double sum = 0;
		for (int n = 0; n < nPoints; n++) {
			sum += points[n];
		}
		return sum / nPoints;
	}
}
