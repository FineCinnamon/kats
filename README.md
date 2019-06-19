# arrow-meta-prototype

**Run the sample higherkind plugin with**
```
./gradlew clean :app:build :consumer:execute -Dorg.gradle.debug=true -Dkotlin.compiler.execution.strategy="in-process"
```

**Please document all findings in the code**

Sample project demonstrating how to create a basic Kotlin compiler plugin.

Run this command to see it in action:

```text
$ ./gradlew :app:build
```

# Resources

## Projects
 - Top to bottom from most simple to most complex


 - [Sample kotlin compiler plugin](https://github.com/Takhion/sample-kotlin-compiler-plugin) a hello world compiler plugin by @Takhion
 - [Redacted](https://github.com/ZacSweers/redacted-compiler-plugin) port of auto-value redacted for classes by @ZacSweers
 - [Spek compiler plugin](https://github.com/spekframework/spek/pull/657/files) compiler plugin that writes code using the IR backend.
 - [DebugLog](https://github.com/kevinmost/debuglog) bad gradle setup, check [Takhion's](https://github.com/Takhion/sample-kotlin-compiler-plugin) gradle setup. DebugLog uses `org.jetbrains.org.objectweb.asm.` bytecode DSL. (Java only) @kevinmost
 - [Parcelize](https://github.com/JetBrains/kotlin/tree/master/plugins/android-extensions/android-extensions-compiler/src/org/jetbrains/kotlin/android/parcel) plugin to add parcelable logic to data classes. Uses the `org.jetbrains.org.objectweb.asm.` bytecode DSL. (Java only)
 - [Kotlinx serialization](https://github.com/JetBrains/kotlin/tree/master/plugins/kotlin-serialization/kotlin-serialization-compiler/src/org/jetbrains/kotlinx/serialization/compiler) uses all backend-ends + Kotlin IR.
 - [Android synthetic extensions](https://github.com/JetBrains/kotlin/tree/master/plugins/android-extensions/android-extensions-compiler/src/org/jetbrains/kotlin/android/synthetic) generates synthetic methods on Activity/View and Fragment to access views directly by their xml id.
 
 - **currently broken** [purity](https://github.com/pardom/purity) a plugin that checks if functions and lambdas with @Pure are actually pure.


 ## Slack
  - [Progress on IR](https://kotlinlang.slack.com/archives/C7L3JB43G/p1551303086009100?thread_ts=1551303086.009100&cid=C7L3JB43G)


## Gathered Resources
#### On IR (Intermediate Representation) and LLVM (umbrella project for several compiler tools, e.g.: Debugger, IR, C++ StdLib)
```markdown
The basic idea behind IR:
"LLVM can provide the middle layers of a complete compiler system, taking intermediate representation (IR) code 
 from a compiler and emitting an optimized IR. 
 This new IR can then be converted and linked into machine-dependent assembly language code for a target platform."
 - Amir Jalal [link](https://www.linkedin.com/pulse/what-llvm-ir-kotlin-behind-scenes-amirhossein-jalalhosseini)
 // in our case MacOs, JS, etc.
```
A more coherent version to understand IR is this:
```markdown
"An Intermediate representation (IR) is the `data structure`
or `code` used internally by a compiler or virtual machine `to represent` source code. 
An IR is designed to be conducive for further processing, such as optimization and translation."
- Amir Jalal [link](https://www.linkedin.com/pulse/what-llvm-ir-kotlin-behind-scenes-amirhossein-jalalhosseini)
```
To sum up:
**IR represents source code and we process/ intercept IR for further optimization/ codegen/ checkups.**

Example (for more examples [dig into the ir modules](https://github.com/pyos/kotlin/commit/f47d9d54c0c14be9c386f6023e614229b2c15717)):
Kotlin
```kotlin
// WITH_JDK
// FILE: samOperators.kt
fun f() {}

fun J.test1() {
    this[::f]
    this[::f, ::f]
}
// FILE: J.java
public class J {
   public void get(Runnable k) {}
   public void get(Runnable k, Runnable m) {}
   public void set(Runnable k, Runnable v) {}
   public void set(Runnable k, Runnable m, Runnable v) {}
   public void plusAssign(Runnable i) {}
   public void minusAssign(Runnable i) {}
}
```
IR:
```markdown
FILE fqName:<root> fileName:/samOperators.kt
  FUN name:f visibility:public modality:FINAL <> () returnType:kotlin.Unit
	  BLOCK_BODY
  
  FUN name:test1 visibility:public modality:FINAL <> ($receiver:<root>.J) returnType:kotlin.Unit
	    $receiver: VALUE_PARAMETER name:<this> type:<root>.J
	    BLOCK_BODY
	      CALL 'public open fun get (k: java.lang.Runnable?): kotlin.Unit declared in <root>.J' type=kotlin.Unit origin=GET_ARRAY_ELEMENT
	        $this: GET_VAR '<this>: <root>.J declared in <root>.test1' type=<root>.J origin=null
	        k: TYPE_OP type=java.lang.Runnable? origin=SAM_CONVERSION typeOperand=java.lang.Runnable?
	          FUNCTION_REFERENCE 'public final fun f (): kotlin.Unit declared in <root>' type=kotlin.reflect.KFunction0<kotlin.Unit> origin=null
	      CALL 'public open fun get (k: java.lang.Runnable?, m: java.lang.Runnable?): kotlin.Unit declared in <root>.J' type=kotlin.Unit origin=GET_ARRAY_ELEMENT
	        $this: GET_VAR '<this>: <root>.J declared in <root>.test1' type=<root>.J origin=null
	        k: TYPE_OP type=java.lang.Runnable? origin=SAM_CONVERSION typeOperand=java.lang.Runnable?
	          FUNCTION_REFERENCE 'public final fun f (): kotlin.Unit declared in <root>' type=kotlin.reflect.KFunction0<kotlin.Unit> origin=null
	        m: TYPE_OP type=java.lang.Runnable? origin=SAM_CONVERSION typeOperand=java.lang.Runnable?
	          FUNCTION_REFERENCE 'public final fun f (): kotlin.Unit declared in <root>' type=kotlin.reflect.KFunction0<kotlin.Unit> origin=null
```
- [What means frontend and backend in LLVM, and how it is relted to IR](https://idea.popcount.org/2013-07-24-ir-is-better-than-assembly/)