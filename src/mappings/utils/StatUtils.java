package mappings.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public class StatUtils {

	public static double getMean(Collection<Double> values) {
		if (values != null && values.size() > 0) {
			List<Double> valList = values.stream().collect(Collectors.toList());
			Collections.sort(valList);
			int size = valList.size();
			int mid = valList.size() / 2; // integer division -> index OK

			if (size % 2 == 0) {
//			System.out.println("pair");
				double d2 = valList.get(mid - 1);
				double d1 = valList.get(mid);
				return (d1 + d2) / 2;
			} else {
				return valList.get(mid);
			}
		} else {
			return 0;
		}
	}

	public static double getAverage(Collection<Double> values) {
		if (values != null && values.size() > 0) {
			double sum = values.stream().reduce(0.0, (n1, n2) -> n1 + n2);
			return sum / values.size();
		} else {
			return 0.0;
		}
	}

	public static double getStandardDeviation(Collection<Double> values) {
		if (values != null & values.size() > 0) {
			double ave = getAverage(values);
			double size = values.size();
			double sqrdSum = values.stream().map(n -> Math.pow((n - ave), 2)).reduce(0.0, (n1, n2) -> n1 + n2);
			return Math.sqrt(sqrdSum / (double) size);
		} else {
			return 0;
		}

	}

	public static double getIQROutLayersCutoff(Collection<Double> values, boolean top) {
		List<Double> valList = values.stream().collect(Collectors.toList());
		Collections.sort(valList);
		int size = valList.size();
		int q25i = size / 4;
		int q75i = size * 3 / 4;
		double q25;
		double q75;
		if (size % 4 == 0) {
//			System.out.println("pair");
			double q251 = valList.get(q25i - 1);
			double q252 = valList.get(q25i);
			q25 = (q251 + q252) / 2;

			double q751 = valList.get(q75i - 1);
			double q752 = valList.get(q75i);
			q75 = (q751 + q752) / 2;

		} else {
			q25 = valList.get(q25i);
			q75 = valList.get(q75i);
		}
		double iqr = q75 - q25;
		double cutoff = iqr * 1.5;
		if (top) {
			return q75 + cutoff;
		} else {
			return q25 - cutoff;
		}
	}

	public static double getTopnCutoff(Set<Double> values, int n) {
		if (values != null && values.size() > n) {
			List<Double> valList = values.stream().collect(Collectors.toList());
			Collections.sort(valList);
			return valList.get(valList.size() - (n + 1));
		} else {
			return 0; // should return all values
		}
	}

	public static double getBotnCutoff(Set<Double> values, int n) {
		if (values != null && values.size() > n) {
			List<Double> valList = values.stream().collect(Collectors.toList());
			Collections.sort(valList);
			return valList.get(n);
		} else {
			return 1; // should return all values
		}
	}

	public static void main(String[] args) {
		Set<Double> set = new HashSet<>();
		set.add(1.0);
		set.add(4.0);
		set.add(3.3);
		set.add(13.2);
		set.add(9.2);
		System.out.println("mean expected: 4.0 : " + getMean(set));
		System.out.println("average expected: 6.14: " + getAverage(set));
		System.out.println("std dev expected: 4,43332 : " + getStandardDeviation(set));
		System.out.println("iqr top cutoff: " + getIQROutLayersCutoff(set, true));
		System.out.println("iqr bot cutoff: " + getIQROutLayersCutoff(set, false));
		System.out.println("bot2: expected 4.0: " + getBotnCutoff(set, 2));
		System.out.println("top2: expected 4.0: " + getTopnCutoff(set, 2));

		set.add(32.1);

		System.out.println("mean expected: 6.6 : " + getMean(set));
		System.out.println("average expected: 10.4666667 :" + getAverage(set));
		System.out.println("std dev expected: 10.487 : " + getStandardDeviation(set));
		System.out.println("iqr top cutoff: " + getIQROutLayersCutoff(set, true));
		System.out.println("iqr bot cutoff: " + getIQROutLayersCutoff(set, false));
		System.out.println("top2: expected 9.2: " + getTopnCutoff(set, 2));


	}
}
