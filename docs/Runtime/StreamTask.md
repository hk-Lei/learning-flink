StreamTask
==========

# Task 

Task 是 TaskManager 上并行子任务的运行时。Task 封装 Flink 的 Operator (可能是用户定义的函数) 并运行它, 提供所有必要的服务, 例如输入数据、生成结果 (中间结果分区) 以及与 JobManager 的通信。

Flink Operators (AbstractInvokable 的子类) 只有数据读取器、写入器和某些事件回调机制。Task 将这些连接到网络堆栈和 actor 消息，跟踪执行的状态并处理异常

Task 不知道它们是如何与其他 Task 相关联的，也不知道它们是第一次尝试执行，还是重复尝试。所有这一切只对 JobManager 来说是已知的。Task 只知道它自己的可运行代码、配置信息、以及用于消费和生成的中间结果的 IDs(如果有的话)

每个 Task 由一个专用线程运行

# AbstractInvokable

TaskManager 可以执行的每个任务的抽象基类。具体的任务（比如 批作业 BatchTask、流作业 StreamTask）都是继承自这个类。

TaskManager 执行 task 时调用 invoke() 方法，其所有操作都在该方法中(设置输入输出流阅读器和写入器，以及 task 的核心操作) 

# StreamTask

所有流 task 的基类，是 TaskManager 本地部署和执行的单元。每个运行一个或多个 (Operator Chain) 流算子 (StreamOperator), 被链接在一起的 Operator 会在一个线程内同步执行，即作用在相同的流分区上，比如常见的连续的 map/flatmap/filter 任务。

任务链包含一个 `head` Operator 和多个 Chained Operators ，StreamTask 专门用于 `head` 操作符的有 : one-input 和 two-input 任务，以及源、迭代头和迭代尾

这个类解决了流的读取 (使用 head Operator) 和流的产生 (操作符链末端的 Operator), 注意 : 链可能会分叉，因此可能有多个输出端

生命周期如下：

-- setInitialState : 提供链中的所有操作符的状态

-- invoke()
		+----> 创建基本的 utils(如配置等) 并加载操作符链
    	+----> operators.setup()
    	+----> task specific init()
    	+----> initialize-operator-states()
    	+----> open-operators()
    	+----> run()
    	+----> close-operators()
    	+----> dispose-operators() 
    	+----> common cleanup
    	+----> task specific cleanup()

StreamTask 有一个名为 lock 的锁对象，用于确保 StreamOperator 的所有方法调用都是同步的


## OneInputStreamTask

