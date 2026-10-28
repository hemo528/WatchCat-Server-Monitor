#!/bin/sh
# WatchCat metrics probe — POSIX / Linux
# Emits @@SECTION blocks consumed by MetricsParser.kt
export LC_ALL=C

echo "@@HOST"
hostname 2>/dev/null || uname -n 2>/dev/null || echo "-"

echo "@@OS"
if [ -r /etc/os-release ]; then
  . /etc/os-release 2>/dev/null
  echo "${PRETTY_NAME:-${NAME:-Linux}}"
else
  uname -s 2>/dev/null || echo "Linux"
fi

echo "@@KERNEL"
uname -sr 2>/dev/null || echo "-"

echo "@@ARCH"
uname -m 2>/dev/null || echo "-"

echo "@@UPTIME"
cut -d. -f1 /proc/uptime 2>/dev/null || echo 0

echo "@@CORES"
getconf _NPROCESSORS_ONLN 2>/dev/null || grep -c '^processor' /proc/cpuinfo 2>/dev/null || echo 1

echo "@@LOAD"
cut -d' ' -f1-3 /proc/loadavg 2>/dev/null || echo "0 0 0"

c1=$(sed -n '1p' /proc/stat 2>/dev/null)
p1=$(grep '^cpu[0-9]' /proc/stat 2>/dev/null)
net1=$(awk 'NR>2 && $1!="lo:" {rx+=$2; tx+=$10} END{print rx+0" "tx+0}' /proc/net/dev 2>/dev/null)
io1=$(awk 'NR>2 && $3 !~ /^(loop|ram|dm-|sr)/ {r+=$6; w+=$10} END{print r*512+0" "w*512+0}' /proc/diskstats 2>/dev/null)

sleep 0.5

c2=$(sed -n '1p' /proc/stat 2>/dev/null)
p2=$(grep '^cpu[0-9]' /proc/stat 2>/dev/null)
net2=$(awk 'NR>2 && $1!="lo:" {rx+=$2; tx+=$10} END{print rx+0" "tx+0}' /proc/net/dev 2>/dev/null)
io2=$(awk 'NR>2 && $3 !~ /^(loop|ram|dm-|sr)/ {r+=$6; w+=$10} END{print r*512+0" "w*512+0}' /proc/diskstats 2>/dev/null)

echo "@@CPU"
printf '%s\n%s\n' "$c1" "$c2" | awk 'NF>4 {t=0; for(i=2;i<=NF;i++) t+=$i; if(NR==1){t1=t; i1=$5+$6} else {dt=t-t1; di=($5+$6)-i1; if(dt>0) printf "%.1f\n", (1-di/dt)*100; else print "0"}}'

echo "@@CORES_CPU"
printf '%s\n---\n%s\n' "$p1" "$p2" | awk '
$0=="---" {sep=1; next}
NF>4 {
  t=0; for(i=2;i<=NF;i++) t+=$i
  if(!sep){t1[$1]=t; i1[$1]=$5+$6}
  else if($1 in t1){dt=t-t1[$1]; di=($5+$6)-i1[$1]; printf "%.1f ", (dt>0?(1-di/dt)*100:0)}
}
END{print ""}'

echo "@@MEM"
awk '/^MemTotal:/{t=$2} /^MemAvailable:/{a=$2} /^MemFree:/{f=$2} /^Buffers:/{b=$2} /^Cached:/{c=$2} /^SwapTotal:/{st=$2} /^SwapFree:/{sf=$2} END{if(a==""){a=f+b+c} print t+0" "a+0" "st+0" "sf+0}' /proc/meminfo 2>/dev/null

echo "@@DISKS"
df -kP 2>/dev/null | awk 'NR>1 {
  dev=$1; mnt=$6;
  if (mnt ~ /^\/(snap|apex|bootstrap-apex|system|vendor|product|system_ext|dev|proc|sys|run)(\/|$)/) next;
  if (mnt != "/" && dev ~ /^\/dev\/loop/) next;
  if (mnt != "/" && dev ~ /^\/dev\/block\//) next;
  if (substr(dev,1,1)=="/" || mnt=="/") print dev"|"mnt"|"$2"|"$3"|"$4;
}'

echo "@@NET"
printf '%s\n%s\n' "$net1" "$net2" | awk 'NR==1{split($0,a," "); r1=a[1]; t1=a[2]} NR==2{split($0,b," "); printf "%d %d\n", b[1]-r1, b[2]-t1}'

echo "@@DISKIO"
printf '%s\n%s\n' "$io1" "$io2" | awk 'NR==1{split($0,a," "); r1=a[1]; t1=a[2]} NR==2{split($0,b," "); printf "%d %d\n", b[1]-r1, b[2]-t1}'

echo "@@TEMP"
t=$(cat /sys/class/thermal/thermal_zone*/temp 2>/dev/null | sort -n | tail -1)
if [ -n "$t" ] && [ "$t" -gt 1000 ] 2>/dev/null; then t=$((t/1000)); fi
echo "${t:-}"

echo "@@END"
exit 0
