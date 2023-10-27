#!/bin/bash

# 打开写文件句柄并清空文件内容
exec 3>./2151294-hw1-q2.log
:>&3

# 循环20次，每次将输出显示在控制台上同时重定向到日志文件，再停止10s
for((i=0;i<20;i++)); do
    message=$(uptime)
    echo $message
    echo "$message">&3
    sleep 10s
done

# 关闭写文件句柄
exec 3>&-
