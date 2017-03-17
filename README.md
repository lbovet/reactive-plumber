# Rx-Plumber

You want to use [RxJava](https://github.com/ReactiveX/RxJava) with a modular, readable and safe abstraction.

Rx-Plumber let you write your RxJava plumbing in a Groovy DSL and also visualize it graphically.

It is intended to be used in Java or Groovy applications.

<img align="right" src="https://cloud.githubusercontent.com/assets/692124/23836787/2761ce26-077e-11e7-97f0-ffda49431851.png">

```groovy
def data = pipe {
    from input map wrap
}

def printer = {
    from it doOnNext print
}

def renderer = pipe {
    parallel from(data) map renderThread
}

def count = pipe {
    from data count()
}

def size = pipe {
    from data \
    compose attach(count) \
    map renderSize \
    to printer
}

def thread = pipe {
    from renderer to printer
}

drain size, thread
```

Built using these outstanding projects:
 - [Groovy](http://groovy-lang.org)
 - [RxJava](https://github.com/ReactiveX/RxJava)
 - [graphviz-java](https://github.com/nidi3/graphviz-java)
 
  
## The DSL

Have a look at the [examples scripts](https://github.com/lbovet/rx-plumber/tree/master/src/test/groovy/examples).

### Pipes

The idea is to write the pipelines in Groovy, using the parenthese-less method chaining.

So a simple definition of a pipe could be:

```groovy
def items = pipe {
    Flowable.just 1, 2, 3 \
    map Integer.&bitCount \
    map String.&valueOf
}
```

This defines a _pipe_, which is actually a _Flowable_. In the closure block, we simply chain normal RxJava methods. Here we count the bits in each numbers and convert them into strings.

### From

To connect pipes together, use _from_:

```groovy
def printer = pipe {
    from items \
    to { x -> print x }
}
```

### Drain

It is not sufficient to connect pipes together in order to define your plumbing. You also need to declare on which pipes you will _suck_ the data.

```groovy
drain printer
```

### Tools

You will expose your own functions to be used in the plumbing. Do that in a sub-class of `Plumbing`, so that when you import statically your functions, you will also get the Rx-Plumber builtin functions.

_Tools.groovy_:
```groovy
import li.chee.rx.plumber.Plumbing

abstract class Tools extends Plumbing {
    static input = Domain.&input
    static print = Domain.&print
    static renderThread = Domain.&renderThread
    static renderSize = Domain.&renderSize
}
```

_script.groovy_:
```groovy
import static Tools.*;
```

### Pipe Functions

The builtin pipe functions are

| *Function*        | *Description* |
| ------------------|-----------------------------|
| pipe              | Declares a pipe. |
| from              | Connects this pipe's input to a previously declared pipe. |
| to                | Forwards to a reusable pipe part. |
| drain             | Declare a pipe as terminal. |
| split             | Returns an array of pipes filtered with a predicate array. |
| parallel          | Processes the pipe in parallel. |

You can also dynamically create pipes. This uses _GroupedFlowable_ under the hood:

| *Function*        | *Description* |
| ------------------|-----------------------------|
| groups            | Return a pipe of pipes grouped by a grouping function. |
| key               | Extracts the key of a grouped pipe. |
| each              | Declares a pipe to apply to each grouped pipe outputed by `groups` |

### Box Functions

This library provides a monadic wrapper type _Box_ that allows to transport context along with values throughout pipes.

| *Function*        | *Description* |
| ------------------|-----------------------------|
| wrap              | Creates a box around a value. |
| unwrap            | Removes the box and returns the value. |
| attach            | Add a context to a box. |
| mapper            | Applies a function to a value inside a box. |
| bind              | Flat-Map the box, apply a function on the value returning a box. | context           | Add a context to the box using the grouping key of grouped pipes. |

## Examples

### One

<img align="right" src="https://cloud.githubusercontent.com/assets/692124/23836790/27652d5a-077e-11e7-80eb-bbeed7c43a28.png">

[one.groovy](https://github.com/lbovet/rx-plumber/tree/master/src/test/groovy/examples/one/one.groovy)

[Tools.groovy](https://github.com/lbovet/rx-plumber/tree/master/src/test/groovy/examples/one/Tools.groovy)

output:
```
[1 [RxComputationThreadPool-1]|]
[1 / 4|]
[2 [RxComputationThreadPool-2]|]
[2 / 4|]
[3 [RxComputationThreadPool-3]|]
[4 [RxComputationThreadPool-4]|]
[3 / 4|]
[4 / 4|]














.
```

### Two

<img align="right" src="https://cloud.githubusercontent.com/assets/692124/23836791/27760f4e-077e-11e7-91c3-45e8f069e6ec.png">

[two.groovy](https://github.com/lbovet/rx-plumber/tree/master/src/test/groovy/examples/two/two.groovy)

[Tools.groovy](https://github.com/lbovet/rx-plumber/tree/master/src/test/groovy/examples/two/Tools.groovy)

output:
```
2 EVEN
3 ODD













.
```

### Three

<img align="right" src="https://cloud.githubusercontent.com/assets/692124/23836789/2764949e-077e-11e7-98d8-780363963865.png">

[three.groovy](https://github.com/lbovet/rx-plumber/tree/master/src/test/groovy/examples/three/three.groovy)

[Tools.groovy](https://github.com/lbovet/rx-plumber/tree/master/src/test/groovy/examples/three/Tools.groovy)

output:
```
1
2
FIZZ
4
BUZZ
FIZZ
7
8
FIZZ
BUZZ
11
FIZZ
13
14
FIZZBUZZ
FIZZ: 4
BUZZ: 2
FIZZBUZZ: 1



.
```

### Four

<img align="right" src="https://cloud.githubusercontent.com/assets/692124/23836788/27643a8a-077e-11e7-8cd7-8c2b5f3904d4.png">

[four.groovy](https://github.com/lbovet/rx-plumber/tree/master/src/test/groovy/examples/four/four.groovy)

[Tools.groovy](https://github.com/lbovet/rx-plumber/tree/master/src/test/groovy/examples/four/Tools.groovy)

output:
```
FIZZ: 4
BUZZ: 2
FIZZBUZZ: 1










.
```

### Five

<img align="right" src="https://cloud.githubusercontent.com/assets/692124/23836786/2760f15e-077e-11e7-99ca-4bcbb3944878.png">

[five.groovy](https://github.com/lbovet/rx-plumber/tree/master/src/test/groovy/examples/five/five.groovy)

[Tools.groovy](https://github.com/lbovet/rx-plumber/tree/master/src/test/groovy/examples/five/Tools.groovy)

output:
```
strings, [hello|], [world|]
numbers, [5|], [3|]






.
```

## Graph Visualization

Rx-Plumber can create graph images of your plumbing like the ones above by analyzing the Groovy AST of the script.

```java
Runtime runtime = new Runtime().withGraphTheme(Runtime.GraphTheme.LIGHT); // White background
runtime.generateGraph("src/test/groovy/examples/one/one.groovy", new File("target/graph.png"));
runtime.generateGraph("src/test/groovy/examples/one/one.groovy", "svg", System.out));
```