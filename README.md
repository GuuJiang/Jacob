# Jacob
A lightweight library to provide coroutines in Java

# Usage

## add dependency for jacob-core
```xml
<dependency>
    <groupId>com.guujiang</groupId>
    <artifactId>jacob-core</artifactId>
    <version>0.0.1</version>
</dependency>
```

## write a class contains one or more *generator methods*.  
A *generator method* is a method which returns ```Iterable``` and *generate* some values in the middle with the ```yield``` method.  
Here is a example.
```java
// strongly recomended to import the yield method as static to make the syntax more natural
import static com.guujiang.jacob.Stub.yield;

import com.guujiang.jacob.annotation.Generator;
import com.guujiang.jacob.annotation.GeneratorMethod;

// add the @Generator annotation to class
@Generator
class Generator {

    // add the @GeneratorMethod to methods
    @GeneratorMethod
    public Iterator<Integer> someMethod() {
        for (int i = 0; i < 5; ++i) {
            // call the yield method whenever you want to "return" a value out
            yield(i);
        }

        // a return statement must be added in the end to make the compiler happy
        // the return value does not matter, offen use null
        return null;
    }
}
```

## Enhance the bytecode
jacob convert your normal Java method into coroutine with bytecode manuplation, which can happen at two points. You only need to choose **one** of them.

1. Enhance at the compile time.(recommended)  
add the following plugin to your maven project's pom.xml.
```xml
<plugin>
    <groupId>com.guujiang</groupId>
    <artifactId>jacob-maven-plugin</artifactId>
    <version>0.0.1</version>
    <executions>
        <execution>
            <goals>
                <goal>enhance</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
Package jar file with ```mvn package```, the plugin will automatically convert all the classed with proper annotations into coroutine form. Then you just need to execute the generated jar file or use as dependency.

2. Enhance at runtime.  
If you cannot enhance the classes at compile time, you can also do it at classloading.  
First compile the ```jacob-core``` project with ```mvn package``` to get a file called ```jacob.jar```.
Then add the following parameters to your java command line when launch the program which used the generator code.
```
-javaagent:<path of jacob.jar>
```

## For more information, refer to the [jacob-samples](https://github.com/GuuJiang/Jacob/tree/master/jacob-samples) project
