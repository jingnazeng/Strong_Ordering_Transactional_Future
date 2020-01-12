
mkdir auto-results
ant clean
ant compile
cd build/classes

contention[1]="true"
contention[0]="false"

wait_until_finish() {
pid3=$1
sleeptime=$((($3+2) * $2))
echo "process is $pid3"
LIMIT=30
for ((j = 0; j < $LIMIT; ++j)); do
kill -s 0 $pid3
rc=$?
if [[ $rc != 0 ]] ; then
echo "returning"
return;
fi
sleep $sleeptime
done
kill -9 $pid3
}

for threads in 1 2
do
    for futures in 0 2 4 16
    do
        for read_in_RW in 10 100 1000 10000 100000 500000
        do
	 for read_percentage in 0 0.2 0.5 
	do 
	for contention in 0 1
	 do
            for a in 1 2 3 4 5
	    do
		../../libs/openjdk-continuation-vm2013-linux-amd64/bin/java -Xms8G -Xmx16G contlib.ArrayAccess.ArrayAccess 1000000 $threads $futures 15 100 $read_in_RW 10 $read_percentage ${contention[$contention]} >> ../../auto-results/T-$threads-F-$futures-Read_in_RW-$read_in_RW-Contention-$contention-$a.data &pid=$!; wait_until_finish $pid $threads $futures; wait $pid; rc=$?
                if [[ $rc != 0 ]] ; then
                echo "Error within: running: !workload ${workloads[$workload]} | threads $threads |future $futures |attempt $a" >> ../../auto-results/error.out
                fi
            done
        done
    done
done
done
done
