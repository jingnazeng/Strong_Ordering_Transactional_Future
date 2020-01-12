mkdir auto-results
cd build/classes

sequentialExecution[1]="true"
sequentialExecution[2]="false"
            #input order of parameters 
                # 0: array length, number of items in the array
                #1: number of threads
                #2: number of siblings(futures or parallel nested branches)
                #3: number of bank agency
                #4: duration for the simulation,in seconds
                #5: max number of cores
                #6: obsolete  number of reads in prefix disjoint read
                #7: number of hot-spot in the whole array
                #8: obsolete number of read and write to the hot spots
                #9: whether write and read to hotspot is in high contention
                #10: spin between each read
		#11: Whehter to run it sequentially or with futures
		#12: The different transaction percentages in the bankagency

for threads in 1 
do
	for futures in 1 2 4 8
	do
	   for BankAgency in 1 4 8
	   do
		for seq in 2
		do
	        	for spin in 1000
			do
			     for a in 1 
			     do
					../../libs/openjdk-continuation-vm2013-linux-amd64/bin/java -Xms8G -Xmx16G contlib.ArrayAccess.ArrayAccess 100 $threads $futures $BankAgency 30 56 0 0 0 false $spin ${sequentialExecution[$seq]} 0.1 >> ../../auto-results/T-$threads-F-$futures-B-$BankAgency-spin-$spin-seq-${sequentialExecution[$seq]}-$a.data
			    done
		done
		done
	    done
	done
done	
