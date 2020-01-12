mkdir auto-results
cd build/classes

sequentialExecution[1]="true"
sequentialExecution[2]="false"

for threads in 1
do
	for futures in 2 4 8 16
	do
	   for hot_spot_read_and_write in 1 10 50
	   do
	        	for spin in 10000
			do
			     for a in 1 
			     do
					../../libs/openjdk-continuation-vm2013-linux-amd64/bin/java -Xms8G -Xmx16G contlib.ArrayAccess.ArrayAccess 1000000 $threads $futures 60 48 10000 100 $hot_spot_read_and_write true $spin  >> ../../auto-results/T-1-F-$futures-hot_spot_read_and_write-$hot_spot_read_and_write-Iteration_loop-$spin-$a.data
			    done
		done
	    done
	done
done	
