Flink stream transformation
===========================

从 Flink 编程 API 的角度来看的，所谓的 transformation，用于转换一个或多个 DataStream 从而形成一个新的 DataStream 对象。Flink 提供编程接口，允许你组合这些 transformation 从而形成非常灵活的拓扑结构。

# StreamTransformation

StreamTransformation 是所有 transformation 的抽象类，提供了实现 transformation 的基础功能。每一个 DataStream 都有一个与之对应的 StreamTransformation。

一些 API 操作，比如 DataStream#map，将会在底层创建一个 StreamTransformation 树，而在程序的运行时，该拓扑结构会被 StreamGraphGenerator 翻译为 StreamGraph。

StreamTransformation 并不一定与运行时的物理操作相对应。有些 StreamTransformation 操作只是逻辑概念，例如 union、split/select data stream, partitioning

# 内置的 StreamTransformation

绝大部分 StreamTransformation 都需要依赖上游StreamTransformation作为输入SourceTransformation等少数特例除外；

如果没有特别说明，getTransitivePredecessors的实现逻辑都是，由自身加input(上游StreamTransformation)组成的集合。

根据实现，我们可以将它们分成两类：

I ：输入输出相关，需要自行定义name，都需要与之对应的operator，setChainingStrategy的实现都返回operator#setChainingStrategy

SourceTransformation
SinkTransformation
OneInputTransformation
TwoInputTransformation

II ：内置函数，name内部固定，无法更改，无需operator，setChainingStrategy的实现都只是抛出UnsupportedOperationException异常， 	
除了上面那些，其他所有的transformation

## SourceTransformation

它表示一个 source，它并不真正做转换工作，因为它没有输入，但它是任何拓扑的根StreamTransformation。

除了 StreamTransformation 构造器需要的那三个参数，SourceTransformation 还需要 StreamSource 类型的参数，它是真正执行转换的 operator。

值得一提的是，其getTransitivePredecessors抽象方法的实现：

```java
public Collection<StreamTransformation<?>> getTransitivePredecessors() {
	return Collections.<StreamTransformation<?>>singleton(this);
}
```

因为其没有前置转换器，所以其返回只存储自身实例的集合对象。

## SinkTransformation

它表示一个sink，创建的时候构造器需要operator 它是 StreamSink的实例，是最终做转换的operator。

getTransitivePredecessors方法的实现是将自身以及input#getTransitivePredecessors的返回值（之前的StreamTransformation集合）集合

该类有两个特别的属性：
 + stateKeySelector
 + stateKeyType
 
这两个属性的目的是因为sink的状态也可能是基于key分区的。