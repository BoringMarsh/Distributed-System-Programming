#!/bin/bash

# 1.打开日志文件的写句柄
exec 3>./2151294-hw1-q3.log

# 2.打开传进参数指代文件的读句柄
exec 4<"$1"

# 3.1 统计文件的行数，截取并写入日志文件
line=$(wc -l "$1")
line=${line% *}
echo "$line" >&3
# 3.2 统计文件的字符数，截取并写入日志文件
char=$(wc -m "$1")
char=${char% *}
echo "$char" >&3

# 4.准备相关变量
readonly date="2023-10-04"  # 某一天日期，便于到时转换为时间戳以计算时间差
first=$date  # 第一个时间戳
last=$date   # 最后一个时间戳
sum1=0.0     # 第1列总和
sum2=0.0     # 第2列总和
sum3=0.0     # 第3列总和

# 5.读取文件
for((i=0;i<line;i++)); do
    # 5.1 读取一行
    read -r str<&4

    # 5.2 若为第一行或最后一行，记录下时间戳
    if [ $i -eq 0 ]; then
        first="$first "${str::8}
    elif [ $((i + 1)) -eq $line ]; then
        last="$last "${str::8}
    fi

    # 5.3 截取后面的数据部分
    data=${str#*load average: }

    # 截下第一个数据并累加到sum1
    data1=${data%%,*}
    data=${data#* }
    sum1=$(echo "$sum1+$data1"|bc)

    # 截下第二个数据并累加到sum2
    data2=${data%%,*}
    data=${data#* }
    sum2=$(echo "$sum2+$data2"|bc)
    
    # 截下第三个数据并累加到sum3
    data3=$data
    sum3=$(echo "$sum3+$data3"|bc)
done

# 6.处理结果
# 6.1 计算结果
first=$(date +%s -d "$first")  # 将first转换为秒
last=$(date +%s -d "$last")    # 将last转换为秒
diff=$((last - first))         # 计算相差秒数
avg1=$(echo "$sum1 $line" | awk '{printf("%0.2f", $1 / $2)}')  # 计算第1列平均值
avg2=$(echo "$sum2 $line" | awk '{printf("%0.2f", $1 / $2)}')  # 计算第2列平均值
avg3=$(echo "$sum3 $line" | awk '{printf("%0.2f", $1 / $2)}')  # 计算第3列平均值
# 6.2 写入日志文件
echo "$diff""s" >&3
echo "$avg1 $avg2 $avg3" >&3
# 6.3 输出到控制台
echo "$line"
echo "$char"
echo "$diff""s"
echo "$avg1 $avg2 $avg3"

# 7.关闭文件句柄
exec 3>&-
exec 4<&-