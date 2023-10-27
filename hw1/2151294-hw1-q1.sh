#!/bin/bash

# 判断一个数是否为质数的函数
function prime () {
    if [ $1 -lt 2 ]; then
        return 1
    fi
    
    limit=$(echo "scale=0;sqrt($1)"|bc)
    
    for((i=2; i<=limit; i++)); do
        if [ $(($1 % i)) -eq 0 ]; then
            return 1
        fi
    done
    
    return 0
}

# 待检查的数和当前质数总和
num=0
sum=0

# 从0-100检查
for((num=0; num<101; num++)); do
    if prime $num; then
        sum=$((sum + num))
    fi
    
done

# 输出总和并重定向到日志文件
echo $sum
echo $sum > ./2151294-hw1-q1.log