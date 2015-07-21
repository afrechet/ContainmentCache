package containmentcache;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

@Slf4j
public class TestUtils {

	public static void printMethodRuntimes(Map<Method,DescriptiveStatistics> stats, boolean CSVoutput)
	{
		System.out.println("Runtime (ms) statistics:");
		
		if(CSVoutput)
		{
			System.out.printf("%s,%s,%s,%s,%s,%s,%s,%s\n","Method","Mean","StdDev","Min","Q25","Median","Q75","Max");
		}
		else
		{
			System.out.printf("%-30s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n","Method","Mean","StdDev","Min","Q25","Median","Q75","Max");
		}
		final List<Method> methods = new LinkedList<Method>(stats.keySet());
		Collections.sort(methods,new Comparator<Method>(){
			@Override
			public int compare(Method o1, Method o2) {
				return o1.getName().compareTo(o2.getName());
		}});
		
		for(Method method : methods)
		{
			final DescriptiveStatistics stat = stats.get(method);
			
			if(CSVoutput)
			{
				System.out.printf("%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n",
						"\""+method.getName()+"\"",
						stat.getMean(),
						stat.getStandardDeviation(),
						stat.getMin(),
						stat.getPercentile(25),
						stat.getPercentile(50),
						stat.getPercentile(75),
						stat.getMax());
			}
			else
			{
				System.out.printf("%-30s %-10.3f %-10.3f %-10.3f %-10.3f %-10.3f %-10.3f %-10.3f\n",
						"\""+method.getName()+"\"",
						stat.getMean(),
						stat.getStandardDeviation(),
						stat.getMin(),
						stat.getPercentile(25),
						stat.getPercentile(50),
						stat.getPercentile(75),
						stat.getMax());
			}
		}
	}
	
	public static void checkMem() {
        System.gc();
        System.gc();
     	
        int mb = 1024*1024;
         
        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();
         
        log.info("##### Heap utilization statistics [MB] #####");
         
        //Print used memory
        log.info("Used Memory:"
            + (runtime.totalMemory() - runtime.freeMemory()) / mb);

        //Print free memory
        log.info("Free Memory:"
            + runtime.freeMemory() / mb);
         
        //Print total available memory
        log.info("Total Memory:" + runtime.totalMemory() / mb);

        //Print Maximum available memory
        log.info("Max Memory:" + runtime.maxMemory() / mb);
	}
	
}
