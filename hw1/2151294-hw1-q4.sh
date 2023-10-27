#!/bin/bash

# 1.设传入为x秒，则循环x次，每次sleep1秒
for((i=0;i<$1;i++)); do
    # 若经过时间为10s的倍数，则向stdout和stderr输出剩余秒数
    # 在本地执行时，stdout的内容会重定向到本地文件，stderr的内容则会到控制台上，保证两边都有数据
    if [ $((i % 10)) -eq 0 ]; then
        ((left = $1 - i))
        echo $left
        echo $left>&2
    fi
    
    sleep 1s
done

# 2.scp ./2151294-hw1-q4.sh chemistrymaster@192.168.106.129:/home/chemistrymaster/桌面
# 3.输入密码
# 4.ssh chemistrymaster@192.168.106.129 bash ./桌面/2151294-hw1-q4.sh 15 > ./2151294-hw1-q4.log
# 5.输入密码