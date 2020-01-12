mkdir auto-results
cd build/classes

sequentialExecution[1]="true"
sequentialExecution[2]="false"

for threads in 1
do
	for futures in 1
	do
	   for sequentialExecution in 1 2
	   do
		for read_in_RW in 10 100 1000 10000 100000
		do
	        	for spin in 0 100 1000 10000 100000
			do
			     for a in 1 
			     do
					../../libs/openjdk-continuation-vm2013-linux-amd64/bin/java -Xms8G -Xmx16G contlib.ArrayAccess.ArrayAccess 1000000 $threads $futures 60 0 $read_in_RW 10 0 true $spin ${sequentialExecution[$sequentialExecution]} >> ../../auto-results/T-$threads-F-$futures-Read_in_RW-$read_in_RW-IsItSeq-${sequentialExecution[$sequentialExecution]}-Iteration_loop-$spin-$a.data
			     done
			done
		done
	    done
	done
done	
